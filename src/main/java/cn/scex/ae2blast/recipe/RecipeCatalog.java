package cn.scex.ae2blast.recipe;

import java.util.*;
import appeng.recipes.transform.*;
import cn.scex.ae2blast.BlastConfig;
import net.minecraft.core.HolderLookup;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.*;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.neoforge.common.crafting.SizedIngredient;
import org.slf4j.LoggerFactory;

public final class RecipeCatalog {
    private static final Map<ResourceLocation, BlastRecipeAdapter> ADAPTERS = new LinkedHashMap<>();
    private static final Map<RecipeManager, RecipeCatalog> CACHE = new IdentityHashMap<>();
    private static long generation;
    private final List<BlastRecipeView> recipes;
    public final long revision;
    private RecipeCatalog(List<BlastRecipeView> recipes) { this.recipes = recipes; revision = generation; }
    /** Call during common setup. Adapters run only when a server recipe catalog is rebuilt. */
    public static synchronized void registerAdapter(ResourceLocation id, BlastRecipeAdapter adapter) {
        if (ADAPTERS.putIfAbsent(id, Objects.requireNonNull(adapter)) != null) throw new IllegalArgumentException("Duplicate adapter " + id);
        invalidate();
    }
    public static synchronized void invalidate() { CACHE.clear(); generation++; }
    public static synchronized RecipeCatalog get(ServerLevel level) {
        return CACHE.computeIfAbsent(level.getRecipeManager(), rm -> new RecipeCatalog(discover(rm, level.registryAccess())));
    }
    public List<BlastRecipeView> recipes() { return recipes; }
    public static synchronized List<BlastRecipeView> discover(RecipeManager manager, HolderLookup.Provider registries) {
        var found = new TreeMap<ResourceLocation, BlastRecipeView>();
        int rejected = 0;
        for (var holder : manager.getRecipes()) {
            try {
                if (holder.value() instanceof BlastRecipe recipe) {
                    found.put(holder.id(), recipe.view(holder.id()));
                } else if (BlastConfig.DISCOVER_AE.get() && holder.value() instanceof TransformRecipe recipe && recipe.circumstance.isExplosion()) {
                    found.put(holder.id(), new BlastRecipeView(holder.id(), recipe.ingredients.stream().map(i -> new SizedIngredient(i, 1)).toList(),
                        recipe.getResultItem(registries), BlastConfig.AE_CHARGE.get(), BlastConfig.AE_TICKS.get(),
                        (input, access) -> recipe.assemble(new TransformRecipeInput(input), access)));
                } else {
                    for (var adapter : ADAPTERS.values()) {
                        var result = adapter.adapt(holder, registries);
                        if (result.isPresent()) { found.put(holder.id(), result.get()); break; }
                    }
                }
            } catch (RuntimeException ex) {
                rejected++;
                LoggerFactory.getLogger(RecipeCatalog.class).warn("Blast recipe {} could not be adapted: {}", holder.id(), ex.toString());
            }
        }
        LoggerFactory.getLogger(RecipeCatalog.class).info("Discovered {} blast recipes; {} unsupported recipes rejected", found.size(), rejected);
        return List.copyOf(found.values());
    }
    private RecipeCatalog() { throw new AssertionError(); }
}
