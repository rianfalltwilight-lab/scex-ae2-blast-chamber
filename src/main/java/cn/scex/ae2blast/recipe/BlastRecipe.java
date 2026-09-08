package cn.scex.ae2blast.recipe;

import java.util.List;
import com.mojang.serialization.*;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.*;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.crafting.SizedIngredient;
import cn.scex.ae2blast.BlastChamberMod;
import appeng.recipes.transform.TransformRecipeInput;
import appeng.core.definitions.AEItems;
import appeng.blockentity.qnb.QuantumBridgeBlockEntity;

public record BlastRecipe(List<SizedIngredient> inputs, ItemStack result, int charges, int ticks) implements Recipe<TransformRecipeInput> {
    public static final MapCodec<BlastRecipe> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
        SizedIngredient.NESTED_CODEC.listOf().validate(v -> v.isEmpty() || v.size() > 9 || v.stream().anyMatch(x -> x.count() > 64) ? DataResult.error(() -> "Require 1-9 ingredients, each at most 64") : DataResult.success(v)).fieldOf("ingredients").forGetter(BlastRecipe::inputs),
        ItemStack.STRICT_CODEC.fieldOf("result").forGetter(BlastRecipe::result),
        Codec.intRange(1,64).optionalFieldOf("charges",1).forGetter(BlastRecipe::charges),
        Codec.intRange(1,1200).optionalFieldOf("ticks",60).forGetter(BlastRecipe::ticks)
    ).apply(i, BlastRecipe::new));
    public static final StreamCodec<RegistryFriendlyByteBuf, BlastRecipe> STREAM = StreamCodec.composite(
        SizedIngredient.STREAM_CODEC.apply(ByteBufCodecs.list()), BlastRecipe::inputs,
        ItemStack.STREAM_CODEC, BlastRecipe::result,
        ByteBufCodecs.VAR_INT, BlastRecipe::charges, ByteBufCodecs.VAR_INT, BlastRecipe::ticks, BlastRecipe::new);
    public BlastRecipe { inputs = List.copyOf(inputs); result = result.copy(); }
    public BlastRecipeView view(ResourceLocation id) { return new BlastRecipeView(id, inputs, result, charges, ticks, (input, access) -> assemble(new TransformRecipeInput(input), access)); }
    public boolean matches(TransformRecipeInput input, Level level) { return false; }
    public ItemStack assemble(TransformRecipeInput input, HolderLookup.Provider registries) {
        var output = result.copy();
        if (AEItems.QUANTUM_ENTANGLED_SINGULARITY.is(output) && output.getCount() > 1) QuantumBridgeBlockEntity.assignFrequency(output);
        return output;
    }
    public boolean canCraftInDimensions(int width, int height) { return false; }
    public ItemStack getResultItem(HolderLookup.Provider registries) { return result; }
    public RecipeSerializer<?> getSerializer() { return BlastChamberMod.SERIALIZER.get(); }
    public RecipeType<?> getType() { return BlastChamberMod.RECIPE_TYPE.get(); }
    public boolean isSpecial() { return true; }
    public NonNullList<Ingredient> getIngredients() { return NonNullList.of(Ingredient.EMPTY, inputs.stream().map(SizedIngredient::ingredient).toArray(Ingredient[]::new)); }
    public static final class Serializer implements RecipeSerializer<BlastRecipe> {
        public MapCodec<BlastRecipe> codec() { return CODEC; }
        public StreamCodec<RegistryFriendlyByteBuf, BlastRecipe> streamCodec() { return STREAM; }
    }
}
