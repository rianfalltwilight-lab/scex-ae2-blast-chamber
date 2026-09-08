package cn.scex.ae2blast.test;

import cn.scex.ae2blast.*;

import java.util.*;
import cn.scex.ae2blast.recipe.*;
import appeng.core.definitions.AEItems;
import appeng.api.ids.AEComponents;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.entity.item.ItemEntity;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.gametest.*;
import net.neoforged.neoforge.common.crafting.SizedIngredient;
import net.neoforged.neoforge.items.ItemStackHandler;

@Mod("ae2blast_test")
@GameTestHolder("ae2blast_test")
@PrefixGameTestTemplate(false)
public final class BlastGameTests {
    private static final BlockPos POS=new BlockPos(2,2,2);
    private static BlastChamberBlockEntity machine(GameTestHelper h) {
        h.setBlock(POS,BlastChamberMod.CHAMBER.get());
        return (BlastChamberBlockEntity)h.getLevel().getBlockEntity(h.absolutePos(POS));
    }
    private static void feedQuantum(BlastChamberBlockEntity b,int count,ItemStack fuel) {
        b.automation(Direction.NORTH).insertItem(0,AEItems.SINGULARITY.stack(count),false);
        b.automation(Direction.NORTH).insertItem(1,new ItemStack(Items.ENDER_PEARL,count),false);
        b.automation(Direction.UP).insertItem(0,fuel,false);
    }
    private static int outputs(BlastChamberBlockEntity b) { int c=0;for(int s=10;s<14;s++)c+=b.inventory.getStackInSlot(s).getCount();return c; }

    @GameTest(template="empty",timeoutTicks=120)
    public static void quantumPairUsesOriginalAssemblerAndGunpowder(GameTestHelper h) {
        var b=machine(h);feedQuantum(b,1,new ItemStack(Items.GUNPOWDER));
        h.succeedWhen(() -> {
            h.assertTrue(outputs(b)==2,"Expected a quantum pair from real AE2 recipe");
            var pair=b.inventory.getStackInSlot(10);
            h.assertTrue(AEItems.QUANTUM_ENTANGLED_SINGULARITY.is(pair),"Wrong output");
            h.assertTrue(pair.has(AEComponents.ENTANGLED_SINGULARITY_ID),"Frequency component absent");
            h.assertTrue(b.inventory.getStackInSlot(0).isEmpty() && b.inventory.getStackInSlot(1).isEmpty() && b.inventory.getStackInSlot(9).isEmpty(),"Inputs/fuel not consumed exactly");
            h.assertTrue(!b.getBlockState().getValue(BlastChamberBlock.ACTIVE),"Completed machine must stop animation");
        });
    }
    @GameTest(template="empty",timeoutTicks=320)
    public static void tntCreditAndIndependentQuantumFrequencies(GameTestHelper h) {
        var b=machine(h);feedQuantum(b,3,new ItemStack(Items.TNT));
        h.succeedWhen(() -> {
            h.assertTrue(outputs(b)==6,"Three batches must yield six singularities");
            var ids=new HashSet<Long>();
            for(int s=10;s<13;s++) ids.add(b.inventory.getStackInSlot(s).get(AEComponents.ENTANGLED_SINGULARITY_ID));
            h.assertTrue(ids.size()==3 && !ids.contains(null),"Batch frequencies must be distinct");
            h.assertTrue(b.data.get(2)==2 && b.inventory.getStackInSlot(9).isEmpty(),"TNT should leave two charges");
        });
    }
    @GameTest(template="empty",timeoutTicks=150)
    public static void noFuelAndFullOutputPreserveInputs(GameTestHelper h) {
        var b=machine(h);feedQuantum(b,1,ItemStack.EMPTY);
        h.runAtTickTime(30,() -> {
            h.assertTrue(!b.hasPending() && b.inventory.getStackInSlot(0).getCount()==1,"No fuel may not consume inputs");
            for(int s=10;s<14;s++) b.inventory.setStackInSlot(s,new ItemStack(Items.COBBLESTONE,64));
            b.automation(Direction.UP).insertItem(0,new ItemStack(Items.GUNPOWDER),false);
        });
        h.runAtTickTime(60,() -> {
            h.assertTrue(!b.hasPending() && b.inventory.getStackInSlot(9).getCount()==1,"Full output consumed fuel or started");
            for(int s=10;s<14;s++) b.inventory.setStackInSlot(s,ItemStack.EMPTY);
        });
        h.succeedWhen(() -> h.assertTrue(outputs(b)==2,"Must wake after output clears"));
    }
    @GameTest(template="empty",timeoutTicks=170)
    public static void redstoneAndManualSwitchControlRealAnimation(GameTestHelper h) {
        var b=machine(h);b.toggleEnabled();feedQuantum(b,1,new ItemStack(Items.GUNPOWDER));
        h.runAtTickTime(25,() -> {
            h.assertTrue(!b.hasPending() && !b.getBlockState().getValue(BlastChamberBlock.ACTIVE),"Disabled machine started");
            h.setBlock(POS.above(),Blocks.REDSTONE_BLOCK);b.toggleEnabled();
        });
        h.runAtTickTime(45,() -> {
            h.assertTrue(!b.hasPending(),"Redstone high must pause");h.setBlock(POS.above(),Blocks.AIR);
        });
        h.runAtTickTime(65,() -> h.assertTrue(b.hasPending() && b.getBlockState().getValue(BlastChamberBlock.ACTIVE),"Working animation not synchronized"));
        h.succeedWhen(() -> h.assertTrue(outputs(b)==2 && !b.getBlockState().getValue(BlastChamberBlock.ACTIVE),"Machine did not finish and close"));
    }
    @GameTest(template="empty",timeoutTicks=150)
    public static void saveRestoreResumesCommittedBatch(GameTestHelper h) {
        var b=machine(h);feedQuantum(b,1,new ItemStack(Items.TNT));
        final BlastChamberBlockEntity[] loaded={b};
        h.runAtTickTime(30,() -> {
            h.assertTrue(b.hasPending(),"Batch did not start before save");
            var tag=b.saveWithFullMetadata(h.getLevel().registryAccess());
            // Serialization roundtrip of a committed batch, not a claim of real chunk unload testing.
            var restored=new BlastChamberBlockEntity(b.getBlockPos(),b.getBlockState());
            restored.loadWithComponents(tag,h.getLevel().registryAccess());h.getLevel().removeBlockEntity(b.getBlockPos());h.getLevel().setBlockEntity(restored);loaded[0]=restored;
        });
        h.succeedWhen(() -> { var r=loaded[0];h.assertTrue(outputs(r)==2 && r.data.get(2)==4,"Restored result or charge lost/duplicated"); });
    }
    @GameTest(template="empty",timeoutTicks=70)
    public static void breakRefundsEscrowAndDoesNotCraftEarly(GameTestHelper h) {
        var b=machine(h);feedQuantum(b,1,new ItemStack(Items.GUNPOWDER));
        h.runAtTickTime(25,() -> {
            h.assertTrue(b.hasPending(),"Expected running batch");h.setBlock(POS,Blocks.AIR);
            var drops=h.getLevel().getEntitiesOfClass(ItemEntity.class,new net.minecraft.world.phys.AABB(h.absolutePos(POS)).inflate(1));
            int singularity=0,pearls=0,result=0;
            for(var e:drops){var s=e.getItem();if(AEItems.SINGULARITY.is(s))singularity+=s.getCount();if(s.is(Items.ENDER_PEARL))pearls+=s.getCount();if(AEItems.QUANTUM_ENTANGLED_SINGULARITY.is(s))result+=s.getCount();}
            h.assertTrue(singularity==1 && pearls==1 && result==0,"Break must refund raw materials exactly");h.succeed();
        });
    }
    @GameTest(template="empty")
    public static void portsSimulationAndOverlapMatching(GameTestHelper h) {
        var b=machine(h);var side=b.automation(Direction.NORTH);var top=b.automation(Direction.UP);var bottom=b.automation(Direction.DOWN);
        h.assertTrue(side.insertItem(0,new ItemStack(Items.IRON_INGOT,12),true).isEmpty() && b.inventory.getStackInSlot(0).isEmpty(),"Simulate mutated input");
        h.assertTrue(top.insertItem(0,new ItemStack(Items.DIAMOND),false).getCount()==1,"Fuel accepted nonexplosive");
        h.assertTrue(bottom.insertItem(0,new ItemStack(Items.DIAMOND),false).getCount()==1,"Output accepted insertion");
        side.insertItem(0,new ItemStack(Items.IRON_INGOT),false); side.insertItem(1,new ItemStack(Items.GOLD_INGOT),false);
        h.assertTrue(side.extractItem(0,1,false).isEmpty(),"Automation extracted raw input");
        var list=List.of(new SizedIngredient(Ingredient.of(Items.IRON_INGOT,Items.GOLD_INGOT),1),new SizedIngredient(Ingredient.of(Items.IRON_INGOT),1));
        var allocation=IngredientAllocator.allocate(b.inventory,list);
        h.assertTrue(allocation!=null && allocation[0]==1 && allocation[1]==1,"Overlapping ingredient allocation failed");
        h.assertTrue(IngredientAllocator.allocate(b.inventory,List.of(new SizedIngredient(Ingredient.of(Items.IRON_INGOT),2)))==null,"Must not reuse same item twice");
        h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=130)
    public static void foreignNamespaceDiscoveryAndCountedDatapackRecipe(GameTestHelper h) {
        var b=machine(h);
        var ids=RecipeCatalog.get(h.getLevel()).recipes().stream().map(r->r.id().toString()).toList();
        h.assertTrue(ids.contains("otherpack:blast_iron") && ids.contains("otherpack:counted_gold"),"Foreign namespace recipes not automatically discovered");
        h.assertTrue(!ids.contains("otherpack:water_only"),"Fluid transform was incorrectly classified as explosion");
        b.inventory.setStackInSlot(0,new ItemStack(Items.GOLD_INGOT,3));b.inventory.setStackInSlot(9,new ItemStack(Items.GUNPOWDER,2));
        h.succeedWhen(() -> {h.assertTrue(b.inventory.getStackInSlot(10).is(Items.DIAMOND),"Counted datapack recipe failed");h.assertTrue(b.inventory.getStackInSlot(0).isEmpty() && b.inventory.getStackInSlot(9).isEmpty(),"Custom quantities not conserved");});
    }
    @GameTest(template="empty",timeoutTicks=130)
    public static void dgModulesTinyChaosMatchesRealExplosion(GameTestHelper h) {
        var catalog=RecipeCatalog.get(h.getLevel()).recipes();
        var id=BlastChamberMod.id("compat/dgmodules/tiny_chaos_fragment");
        if(!net.neoforged.fml.ModList.get().isLoaded("dgmodules")) {
            h.assertTrue(catalog.stream().noneMatch(r->r.id().equals(id)),"DG recipe enabled without its source mod");h.succeed();return;
        }
        var b=machine(h);
        var items=net.minecraft.core.registries.BuiltInRegistries.ITEM;
        var heart=items.get(net.minecraft.resources.ResourceLocation.parse("draconicevolution:dragon_heart"));
        var fragment=items.get(net.minecraft.resources.ResourceLocation.parse("draconicevolution:small_chaos_frag"));
        h.assertTrue(heart!=Items.AIR && fragment!=Items.AIR,"Actual DE registrations missing");
        b.inventory.setStackInSlot(0,new ItemStack(heart));b.inventory.setStackInSlot(1,new ItemStack(Items.NETHER_STAR));b.inventory.setStackInSlot(2,new ItemStack(Items.DIAMOND));b.inventory.setStackInSlot(9,new ItemStack(Items.GUNPOWDER));
        var point=net.minecraft.world.phys.Vec3.atCenterOf(h.absolutePos(new BlockPos(4,2,4)));
        var raw=new ArrayList<ItemEntity>();
        for(var item:List.of(heart,Items.NETHER_STAR,Items.DIAMOND)) {
            var entity=new ItemEntity(h.getLevel(),point.x,point.y,point.z,new ItemStack(item));entity.setNoGravity(true);h.getLevel().addFreshEntity(entity);raw.add(entity);
        }
        // A real world explosion invokes DG's registered event handler, independently of our recipe.
        h.getLevel().explode(null,point.x,point.y,point.z,1f,net.minecraft.world.level.Level.ExplosionInteraction.NONE);
        var drops=h.getLevel().getEntitiesOfClass(ItemEntity.class,new net.minecraft.world.phys.AABB(point,point).inflate(2));
        h.assertTrue(drops.stream().filter(e->e.getItem().is(fragment)).mapToInt(e->e.getItem().getCount()).sum()==2,"Original DG explosion did not yield two tiny fragments");
        h.assertTrue(raw.stream().allMatch(e->!e.isAlive()),"Original DG explosion did not consume each ingredient once");
        h.succeedWhen(()-> {h.assertTrue(b.inventory.getStackInSlot(10).is(fragment) && outputs(b)==2,"Machine differs from original DG explosion");for(int s=0;s<3;s++)h.assertTrue(b.inventory.getStackInSlot(s).isEmpty(),"DG input not consumed");});
    }
}
