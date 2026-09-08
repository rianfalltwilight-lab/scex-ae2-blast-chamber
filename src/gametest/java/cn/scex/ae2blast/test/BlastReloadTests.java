package cn.scex.ae2blast.test;
import cn.scex.ae2blast.*;
import cn.scex.ae2blast.recipe.RecipeCatalog;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.*;
import net.minecraft.world.level.storage.LevelResource;
import net.neoforged.neoforge.gametest.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

@GameTestHolder("ae2blast_test") @PrefixGameTestTemplate(false)
public final class BlastReloadTests {
    @GameTest(template="empty",batch="zz_reload",timeoutTicks=5000)
    public static void datapackReloadInvalidatesNegativeCache(GameTestHelper h)throws Exception {
        var pos=new BlockPos(2,2,2);h.setBlock(pos,BlastChamberMod.CHAMBER.get());
        var b=(BlastChamberBlockEntity)h.getLevel().getBlockEntity(h.absolutePos(pos));
        b.inventory.setStackInSlot(0,new ItemStack(Items.STICK));b.inventory.setStackInSlot(9,new ItemStack(Items.GUNPOWDER));
        var before=RecipeCatalog.get(h.getLevel());
        var failure=new AtomicReference<Throwable>();var completed=new java.util.concurrent.atomic.AtomicBoolean();
        h.runAtTickTime(25,()->{
            try {
                h.assertTrue(!b.hasPending(),"Unmatched input should remain cached before reload");
                var server=h.getLevel().getServer();var root=server.getWorldPath(LevelResource.DATAPACK_DIR).resolve("blast-reload-fixture");
                Files.createDirectories(root.resolve("data/otherpack/recipe"));
                Files.writeString(root.resolve("pack.mcmeta"),"{\"pack\":{\"pack_format\":48,\"description\":\"Isolated reload test only\"}}");
                Files.writeString(root.resolve("data/otherpack/recipe/reloaded_stick.json"),"{\"type\":\"ae2:transform\",\"circumstance\":{\"type\":\"explosion\"},\"ingredients\":[{\"item\":\"minecraft:stick\"}],\"result\":{\"id\":\"minecraft:emerald\",\"count\":2}}");
                server.getPackRepository().reload();var packs=new ArrayList<>(server.getPackRepository().getSelectedIds());packs.add("file/blast-reload-fixture");
                server.reloadResources(packs).whenComplete((unused,ex)->{failure.set(ex);completed.set(true);});
            }catch(Throwable ex){failure.set(ex);completed.set(true);}
        });
        h.succeedWhen(()->{
            if(failure.get()!=null)throw new IllegalStateException("Reload failed",failure.get());
            h.assertTrue(completed.get(),"Reload still pending");
            h.assertTrue(RecipeCatalog.get(h.getLevel())!=before,"Old catalog retained after actual reload");
            h.assertTrue(b.inventory.getStackInSlot(10).is(Items.EMERALD) && b.inventory.getStackInSlot(10).getCount()==2,"Cached unmatched machine did not discover newly loaded recipe");
        });
    }
}
