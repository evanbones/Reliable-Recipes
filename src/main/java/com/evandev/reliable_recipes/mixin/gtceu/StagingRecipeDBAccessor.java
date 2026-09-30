package com.evandev.reliable_recipes.mixin.gtceu;

//? if forge {
/*import com.gregtechceu.gtceu.api.recipe.GTRecipe;
import com.gregtechceu.gtceu.api.recipe.lookup.StagingRecipeDB;
import com.moulberry.mixinconstraints.annotations.IfModLoaded;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@IfModLoaded("gtceu")
@Mixin(value = StagingRecipeDB.class, remap = false)
public interface StagingRecipeDBAccessor {
    @Accessor("recipes")
    ObjectOpenHashSet<GTRecipe> getRecipes();
}
*///?}
