package cn.scex.ae2blast.client;

import java.util.*;
import cn.scex.ae2blast.BlastChamberMod;
import cn.scex.ae2blast.recipe.*;
import mezz.jei.api.*;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import mezz.jei.api.registration.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.*;

@JeiPlugin
public final class BlastJeiPlugin implements IModPlugin {
    public static final RecipeType<BlastRecipeView> TYPE = RecipeType.create(BlastChamberMod.ID,"blasting",BlastRecipeView.class);
    public ResourceLocation getPluginUid() { return BlastChamberMod.id("jei"); }
    public void registerCategories(IRecipeCategoryRegistration r) { r.addRecipeCategories(new Category(r.getJeiHelpers().getGuiHelper())); }
    public void registerRecipes(IRecipeRegistration r) {
        var mc=Minecraft.getInstance();
        if(mc.level!=null) r.addRecipes(TYPE,RecipeCatalog.discover(mc.level.getRecipeManager(),mc.level.registryAccess()));
    }
    public void registerRecipeCatalysts(IRecipeCatalystRegistration r) { r.addRecipeCatalyst(new ItemStack(BlastChamberMod.CHAMBER_ITEM.get()),TYPE); }
    private static final class Category implements IRecipeCategory<BlastRecipeView> {
        private final IDrawable icon;
        Category(IGuiHelper gui) { icon=gui.createDrawableIngredient(VanillaTypes.ITEM_STACK,new ItemStack(BlastChamberMod.CHAMBER_ITEM.get())); }
        public RecipeType<BlastRecipeView> getRecipeType() { return TYPE; }
        public Component getTitle() { return Component.translatable("jei.ae2blast.title"); }
        public IDrawable getIcon() { return icon; }
        public int getWidth() { return 156; }
        public int getHeight() { return 90; }
        public ResourceLocation getRegistryName(BlastRecipeView recipe) { return recipe.id(); }
        public void setRecipe(IRecipeLayoutBuilder b,BlastRecipeView r,IFocusGroup focus) {
            for(int i=0;i<r.ingredients().size();i++) {
                var ingredient=r.ingredients().get(i);
                b.addInputSlot(4+i%3*18,3+i/3*18).addItemStacks(Arrays.stream(ingredient.ingredient().getItems()).map(s->s.copyWithCount(ingredient.count())).toList());
            }
            b.addInputSlot(76,42).addItemStacks(List.of(new ItemStack(Items.GUNPOWDER,r.charges()),new ItemStack(Items.TNT,(r.charges()+4)/5)));
            b.addOutputSlot(130,21).addItemStack(r.preview());
        }
        public void draw(BlastRecipeView r,IRecipeSlotsView slots,GuiGraphics g,double x,double y) {
            g.fill(65,24,116,29,0xff9660c0);g.fill(111,20,116,33,0xffdcaa44);
            g.drawString(Minecraft.getInstance().font,Component.translatable("jei.ae2blast.cost",r.charges(),r.ticks()),3,67,0xff444444,false);
            g.drawString(Minecraft.getInstance().font,Component.literal("1 TNT = 5"),73,55,0xff775122,false);
        }
    }
}
