package cn.scex.ae2blast;

import cn.scex.ae2blast.recipe.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.capabilities.*;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.registries.*;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;

@Mod(BlastChamberMod.ID)
public final class BlastChamberMod {
    public static final String ID = "ae2blast";
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(ID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(ID);
    public static final DeferredRegister<BlockEntityType<?>> ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, ID);
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, ID);
    public static final DeferredRegister<RecipeType<?>> TYPES = DeferredRegister.create(Registries.RECIPE_TYPE, ID);
    public static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS = DeferredRegister.create(Registries.RECIPE_SERIALIZER, ID);
    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, ID);
    public static final DeferredBlock<BlastChamberBlock> CHAMBER = BLOCKS.register("blast_chamber", () -> new BlastChamberBlock(BlockBehaviour.Properties.of().strength(4, 1200).requiresCorrectToolForDrops().lightLevel(s -> s.getValue(BlastChamberBlock.ACTIVE) ? 9 : 0)));
    public static final DeferredItem<BlockItem> CHAMBER_ITEM = ITEMS.registerSimpleBlockItem(CHAMBER);
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<BlastChamberBlockEntity>> ENTITY = ENTITIES.register("blast_chamber", () -> BlockEntityType.Builder.of(BlastChamberBlockEntity::new, CHAMBER.get()).build(null));
    public static final DeferredHolder<MenuType<?>, MenuType<BlastChamberMenu>> MENU = MENUS.register("blast_chamber", () -> IMenuTypeExtension.create(BlastChamberMenu::new));
    public static final DeferredHolder<RecipeType<?>, RecipeType<BlastRecipe>> RECIPE_TYPE = TYPES.register("blasting", () -> new RecipeType<>() { public String toString() { return ID + ":blasting"; } });
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<BlastRecipe>> SERIALIZER = SERIALIZERS.register("blasting", BlastRecipe.Serializer::new);
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> TAB = TABS.register("main", () -> CreativeModeTab.builder().title(net.minecraft.network.chat.Component.translatable("itemGroup.ae2blast")).icon(() -> new ItemStack(CHAMBER_ITEM.get())).displayItems((p, o) -> o.accept(CHAMBER_ITEM.get())).build());

    public BlastChamberMod(IEventBus bus, ModContainer container) {
        BLOCKS.register(bus); ITEMS.register(bus); ENTITIES.register(bus); MENUS.register(bus);
        TYPES.register(bus); SERIALIZERS.register(bus); TABS.register(bus);
        bus.addListener(this::capabilities);
        container.registerConfig(ModConfig.Type.SERVER, BlastConfig.SPEC);
        bus.addListener((net.neoforged.fml.event.config.ModConfigEvent.Reloading e) -> { if (e.getConfig().getSpec() == BlastConfig.SPEC) RecipeCatalog.invalidate(); });
        NeoForge.EVENT_BUS.addListener((OnDatapackSyncEvent e) -> { if (e.getPlayer() == null) RecipeCatalog.invalidate(); });
        NeoForge.EVENT_BUS.addListener((ServerStoppedEvent e) -> RecipeCatalog.invalidate());
    }
    private void capabilities(RegisterCapabilitiesEvent e) {
        e.registerBlockEntity(Capabilities.ItemHandler.BLOCK, ENTITY.get(), (be, side) -> be.automation(side));
    }
    public static ResourceLocation id(String path) { return ResourceLocation.fromNamespaceAndPath(ID, path); }
}
