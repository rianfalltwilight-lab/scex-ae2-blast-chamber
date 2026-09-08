package cn.scex.ae2blast.recipe;
import java.util.List;
import java.util.function.BiFunction;
import net.minecraft.core.HolderLookup;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.crafting.SizedIngredient;

/** Immutable recipe description. Assemblers must be world-free and called once per committed batch. */
public record BlastRecipeView(ResourceLocation id, List<SizedIngredient> ingredients, ItemStack preview,
                              int charges, int ticks, BiFunction<List<ItemStack>, HolderLookup.Provider, ItemStack> assembler) {
    public BlastRecipeView {
        ingredients = List.copyOf(ingredients); preview = preview.copy();
        if (ingredients.isEmpty() || ingredients.size() > 9 || ingredients.stream().anyMatch(i -> i.count() < 1 || i.count() > 64)
            || preview.isEmpty() || preview.getCount() > 256 || charges < 1 || charges > 64 || ticks < 1 || ticks > 1200) {
            throw new IllegalArgumentException("Unsupported blast recipe bounds: " + id);
        }
    }
}
