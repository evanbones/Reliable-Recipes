package com.evandev.reliable_recipes.mixin.accessor;

import net.minecraft.world.item.crafting.RecipeManager;
import org.spongepowered.asm.mixin.Mixin;
//? if >=1.21.2 {
import net.minecraft.world.item.crafting.RecipeMap;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;
//?}

@Mixin(RecipeManager.class)
public interface RecipeManagerAccessor {
    //? if >=1.21.2 {

    @Accessor("recipes")
    RecipeMap reliableRecipes$getRecipeMap();

    @Mutable
    @Accessor("recipes")
    void reliableRecipes$setRecipeMap(RecipeMap recipeMap);
    //?}
}
