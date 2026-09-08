package cn.scex.ae2blast.test;

import cn.scex.ae2blast.*;
import cn.scex.ae2blast.client.BlastChamberScreen;
import appeng.core.definitions.AEItems;
import net.minecraft.client.*;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.core.*;
import net.minecraft.core.registries.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;

/** Isolated GUI probe, never included in the production addon. Reuses SCEX's remote desktop runner contract. */
@EventBusSubscriber(modid="ae2blast_test",value=Dist.CLIENT)
public final class BlastClientProbe {
    private static final BlockPos CLOSED=new BlockPos(2,71,3), WORKING=new BlockPos(4,71,3);
    private static int phase,ticks,shots;
    private static long started;
    private static boolean done;
    private static volatile boolean prepared,produced;
    private static volatile int observedOutputCount;
    private static volatile Throwable failure;
    private static Path output;
    private static String capture;
    private static CompletableFuture<Void> reload;
    @SubscribeEvent public static void tick(ClientTickEvent.Post e) {
        if(!Boolean.getBoolean("ae2blast.clientProbe") || done)return;
        var mc=Minecraft.getInstance();
        try {
            if(started==0) {
                started=System.nanoTime();output=mc.gameDirectory.toPath().resolve("verification");Files.createDirectories(output);
                mc.options.pauseOnLostFocus=false;mc.options.renderDistance().set(4);mc.options.simulationDistance().set(5);mc.options.framerateLimit().set(30);mc.options.guiScale().set(2);
                mc.getTutorial().setStep(net.minecraft.client.tutorial.TutorialSteps.NONE);
            }
            if(failure!=null)throw new IllegalStateException("Server probe",failure);
            if(System.nanoTime()-started>180_000_000_000L)throw new IllegalStateException("Probe timeout phase "+phase);
            mc.getToasts().clear();
            if(phase==0 && mc.screen instanceof net.minecraft.client.gui.screens.AccessibilityOnboardingScreen && mc.getOverlay()==null){mc.options.onboardingAccessibilityFinished();mc.setScreen(new TitleScreen());}
            if(phase==0 && mc.screen instanceof TitleScreen && mc.getOverlay()==null) {
                phase=1;
                var settings=new LevelSettings("Blast Chamber isolated visual check",GameType.CREATIVE,false,Difficulty.PEACEFUL,true,new GameRules(),WorldDataConfiguration.DEFAULT);
                mc.createWorldOpenFlows().createFreshLevel("blast-visual-"+System.currentTimeMillis(),settings,new WorldOptions(20260908L,false,false),r->r.registryOrThrow(Registries.WORLD_PRESET).getHolderOrThrow(WorldPresets.FLAT).value().createWorldDimensions(),new TitleScreen());
            } else if(phase==1 && mc.level!=null && mc.player!=null && mc.getSingleplayerServer()!=null && mc.screen==null){
                phase=2;mc.getSingleplayerServer().execute(()->{try{prepare(mc.getSingleplayerServer());prepared=true;}catch(Throwable ex){failure=ex;}});
            } else if(phase==2 && prepared && ++ticks>50) {
                audit(mc);mc.options.hideGui=true;capture="01-closed-and-working.png";phase=3;ticks=0;
            } else if(phase==4 && ++ticks>5){capture="02-flow-next-frame.png";phase=5;ticks=0;}
            else if(phase==6) {
                phase=16;ticks=0;
            } else if(phase==16 && produced) {
                phase=7;ticks=0;mc.options.hideGui=false;
                mc.getSingleplayerServer().execute(()->{try{
                    var server=mc.getSingleplayerServer();var player=server.getPlayerList().getPlayers().getFirst();
                    var be=(BlastChamberBlockEntity)server.overworld().getBlockEntity(WORKING);
                    player.openMenu(be,WORKING);
                }catch(Throwable ex){failure=ex;}});
            } else if(phase==16 && ++ticks%10==0) {
                mc.getSingleplayerServer().execute(()->{try{observeOutput(mc.getSingleplayerServer());}catch(Throwable ex){failure=ex;}});
            } else if(phase==7 && mc.screen instanceof BlastChamberScreen && ++ticks>20) {
                if(!(mc.player.containerMenu instanceof BlastChamberMenu menu) || menu.data.get(3)!=1)throw new IllegalStateException("Menu or server enable sync failed");
                capture="03-machine-menu.png";phase=8;ticks=0;
            } else if(phase==9) {mc.gameMode.handleInventoryButtonClick(mc.player.containerMenu.containerId,0);phase=10;ticks=0;}
            else if(phase==10 && ++ticks>15) {
                if(!(mc.player.containerMenu instanceof BlastChamberMenu menu) || menu.data.get(3)!=0 || mc.level.getBlockState(WORKING).getValue(BlastChamberBlock.ACTIVE))throw new IllegalStateException("GUI stop failed to close machine");
                capture="04-switched-off.png";phase=11;
            } else if(phase==12) {mc.gameMode.handleInventoryButtonClick(mc.player.containerMenu.containerId,0);mc.player.closeContainer();reload=mc.reloadResourcePacks();phase=13;ticks=0;}
            else if(phase==13 && reload.isDone() && mc.getOverlay()==null && ++ticks>30) {
                reload.join();audit(mc);mc.options.hideGui=true;capture="05-after-resource-reload.png";phase=14;
            } else if(phase==15) {
                if(!produced)throw new IllegalStateException("No actual crafting output observed");
                Files.writeString(output.resolve("result.json"),new com.google.gson.Gson().toJson(Map.of("status","captured","screenshots",shots,"craftedOutput",true,"observedOutputCount",observedOutputCount,"guiToggle",true,"resourceReload",true,"animationFrames",4)));
                done=true;mc.stop();
            }
        }catch(Throwable ex){done=true;org.slf4j.LoggerFactory.getLogger(BlastClientProbe.class).error("BLAST_CLIENT_VERIFICATION_FAILED phase "+phase,ex);try{Files.writeString(output.resolve("failure.txt"),ex.toString());}catch(Exception ignored){}mc.stop();}
    }
    @SubscribeEvent public static void frame(RenderFrameEvent.Post e) {
        if(capture==null || done)return;
        try(var image=Screenshot.takeScreenshot(Minecraft.getInstance().getMainRenderTarget())) {image.writeToFile(output.resolve(capture));capture=null;shots++;phase++;}
        catch(Exception ex){failure=ex;capture=null;}
    }
    private static void prepare(MinecraftServer server) {
        var level=server.overworld();level.setDayTime(6000);level.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false,server);
        for(int x=-1;x<=8;x++)for(int z=-3;z<=8;z++)level.setBlock(new BlockPos(x,70,z),Blocks.SMOOTH_QUARTZ.defaultBlockState(),3);
        level.setBlock(CLOSED,BlastChamberMod.CHAMBER.get().defaultBlockState(),3);
        ((BlastChamberBlockEntity)level.getBlockEntity(CLOSED)).toggleEnabled();
        level.setBlock(WORKING.below(2),Blocks.CHEST.defaultBlockState(),3);level.setBlock(WORKING.below(),Blocks.HOPPER.defaultBlockState(),3);
        level.setBlock(WORKING,BlastChamberMod.CHAMBER.get().defaultBlockState(),3);
        var be=(BlastChamberBlockEntity)level.getBlockEntity(WORKING);
        be.inventory.setStackInSlot(0,AEItems.SINGULARITY.stack(32));be.inventory.setStackInSlot(1,new ItemStack(Items.ENDER_PEARL,16));be.inventory.setStackInSlot(9,new ItemStack(Items.TNT,8));
        var player=server.getPlayerList().getPlayers().getFirst();player.teleportTo(level,3.5,71,-1.5,0,10);player.getAbilities().flying=true;player.onUpdateAbilities();
    }
    private static void observeOutput(MinecraftServer server) {
        var level=server.overworld();var be=(BlastChamberBlockEntity)level.getBlockEntity(WORKING);
        int count=0;
        for(int s=10;s<14;s++)if(AEItems.QUANTUM_ENTANGLED_SINGULARITY.is(be.inventory.getStackInSlot(s)))count+=be.inventory.getStackInSlot(s).getCount();
        for(var pos:List.of(WORKING.below(),WORKING.below(2)))if(level.getBlockEntity(pos) instanceof Container inventory)
            for(int s=0;s<inventory.getContainerSize();s++)if(AEItems.QUANTUM_ENTANGLED_SINGULARITY.is(inventory.getItem(s)))count+=inventory.getItem(s).getCount();
        observedOutputCount=count;produced=count>=2;
    }
    private static void audit(Minecraft mc)throws Exception {
        var id=BlastChamberMod.id("blast_chamber");
        var model=mc.getItemRenderer().getModel(new ItemStack(BlastChamberMod.CHAMBER_ITEM.get()),mc.level,mc.player,0);
        if(model==mc.getModelManager().getMissingModel())throw new IllegalStateException("Item model missing");
        var sprite=mc.getTextureAtlas(net.minecraft.world.inventory.InventoryMenu.BLOCK_ATLAS).apply(BlastChamberMod.id("block/chamber_active"));
        if(sprite.contents().getUniqueFrames().count()!=4 || sprite.contents().width()!=64)throw new IllegalStateException("Animated texture did not load four 64px frames");
        for(var direction:Direction.Plane.HORIZONTAL)for(boolean active:new boolean[]{true,false}) {
            var state=BlastChamberMod.CHAMBER.get().defaultBlockState().setValue(BlastChamberBlock.FACING,direction).setValue(BlastChamberBlock.ACTIVE,active);
            if(mc.getBlockRenderer().getBlockModel(state)==mc.getModelManager().getMissingModel())throw new IllegalStateException("Blockstate model missing");
        }
        Files.writeString(output.resolve("item-model-audit.json"),new com.google.gson.Gson().toJson(Map.of("registeredItems",List.of(id.toString()),"missingItemModels",List.of(),"animatedFrames",4)));
    }
}
