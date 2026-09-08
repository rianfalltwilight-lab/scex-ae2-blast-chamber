package cn.scex.ae2blast.recipe;
import java.util.Optional;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.item.crafting.RecipeHolder;
/** Opt-in integration for other mods' real recipe semantics. No name-based guessing or network access. */
@FunctionalInterface
public interface BlastRecipeAdapter {
    Optional<BlastRecipeView> adapt(RecipeHolder<?> holder, HolderLookup.Provider registries);
}
