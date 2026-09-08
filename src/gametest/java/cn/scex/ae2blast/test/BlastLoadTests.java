package cn.scex.ae2blast.test;
import cn.scex.ae2blast.*;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.item.*;
import net.neoforged.neoforge.gametest.*;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.common.NeoForge;
import java.util.*;
import java.nio.file.*;
import java.util.function.Consumer;

/** Short controlled load comparison. GameTest runs uncapped: these are work times, not real-time TPS claims. */
@GameTestHolder("ae2blast_test") @PrefixGameTestTemplate(false)
public final class BlastLoadTests {
    @GameTest(template="large",batch="zy_load",timeoutTicks=1400)
    public static void boundedIdleWorkingAndBackpressure(GameTestHelper h) {
        var samples=new LinkedHashMap<String,List<Double>>();
        for(String key:List.of("baseline","idle_128","working_128","blocked_128"))samples.put(key,new ArrayList<>());
        var phase=new String[]{""};var begin=new long[1];var machines=new ArrayList<BlastChamberBlockEntity>();
        Consumer<ServerTickEvent.Pre> pre=e->begin[0]=System.nanoTime();
        Consumer<ServerTickEvent.Post> post=e->{if(!phase[0].isEmpty())samples.get(phase[0]).add((System.nanoTime()-begin[0])/1e6);};
        NeoForge.EVENT_BUS.addListener(pre);NeoForge.EVENT_BUS.addListener(post);
        h.runAtTickTime(30,()->phase[0]="baseline");h.runAtTickTime(180,()->{
            phase[0]="";
            for(int z=0;z<8;z++)for(int x=0;x<16;x++) {
                var pos=new BlockPos(x+1,2,z+1);h.setBlock(pos,BlastChamberMod.CHAMBER.get());
                machines.add((BlastChamberBlockEntity)h.getLevel().getBlockEntity(h.absolutePos(pos)));
            }
        });
        h.runAtTickTime(220,()->phase[0]="idle_128");h.runAtTickTime(370,()->{
            phase[0]="";
            for(var b:machines){b.inventory.setStackInSlot(0,new ItemStack(Items.GOLD_INGOT,48));b.inventory.setStackInSlot(9,new ItemStack(Items.TNT,16));}
        });
        h.runAtTickTime(390,()->phase[0]="working_128");h.runAtTickTime(650,()->{
            phase[0]="";
            for(var b:machines)for(int s=10;s<14;s++)b.inventory.setStackInSlot(s,new ItemStack(Items.COBBLESTONE,64));
        });
        h.runAtTickTime(720,()->phase[0]="blocked_128");
        h.runAtTickTime(880,()->{
            phase[0]="";NeoForge.EVENT_BUS.unregister(pre);NeoForge.EVENT_BUS.unregister(post);
            var report=new LinkedHashMap<String,Object>();
            report.put("scope","single short uncapped GameTest comparison; 128 physically placed machines; not full-pack MSPT or long-term GC evidence");
            for(var entry:samples.entrySet()) {
                var v=new ArrayList<>(entry.getValue());Collections.sort(v);
                h.assertTrue(v.size()>=100,"Insufficient tick samples");
                double p95=v.get((int)Math.ceil(.95*v.size())-1);
                report.put(entry.getKey(),Map.of("n",v.size(),"p50",v.get(v.size()/2),"p95",p95,"p99",v.get((int)Math.ceil(.99*v.size())-1),"max",v.getLast(),"over50ms",v.stream().filter(t->t>50).count()));
                h.assertTrue(p95<50,"Short-run p95 exceeds 50 ms in "+entry.getKey());
            }
            h.assertTrue(machines.stream().allMatch(b->b.data.get(4)==2),"Output backpressure not reached");
            try{Files.writeString(Path.of("load-result.json"),new com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(report));}catch(Exception ex){throw new IllegalStateException(ex);}
            h.succeed();
        });
    }
}
