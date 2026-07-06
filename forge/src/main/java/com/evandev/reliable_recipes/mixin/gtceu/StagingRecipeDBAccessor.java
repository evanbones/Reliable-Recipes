package com.evandev.reliable_recipes.mixin.gtceu;

import com.gregtechceu.gtceu.api.recipe.GTRecipe;
import com.gregtechceu.gtceu.api.recipe.lookup.StagingRecipeDB;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value = StagingRecipeDB.class, remap = false)
public interface StagingRecipeDBAccessor {
    @Accessor("recipes")
    ObjectOpenHashSet<GTRecipe> getRecipes();
}
