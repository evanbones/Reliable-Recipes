package com.evandev.reliable_recipes.mixin.accessor;

import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeMap;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(RecipeManager.class)
public interface RecipeManagerAccessor {

    @Accessor("recipes")
    RecipeMap reliableRecipes$getRecipeMap();

    @Accessor("recipes")
    void reliableRecipes$setRecipeMap(RecipeMap recipeMap);
}