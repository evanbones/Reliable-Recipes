package com.evandev.reliable_recipes.recipe;

import com.evandev.reliable_recipes.Constants;
import com.evandev.reliable_recipes.util.CompatUtil;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
//? if >=1.21.2 {
import com.evandev.reliable_recipes.mixin.accessor.RecipeManagerAccessor;
import net.minecraft.world.item.crafting.RecipeMap;
//?}

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Tracks recipes removed at runtime so they can be restored by {@code /rrecipes_undo} without needing a full datapack
 * reload.
 */
public class RecipeUndoCache {
    private static final Map<ResourceKey<Recipe<?>>, RecipeHolder<?>> DELETED_RECIPES = new HashMap<>();

    /**
     * Removes a recipe from the given manager, remembering it so it can be restored later.
     *
     * @return the removed recipe, or null if the manager doesn't contain it.
     */
    public static RecipeHolder<?> removeRecipe(RecipeManager manager, ResourceKey<Recipe<?>> recipeKey) {
        List<RecipeHolder<?>> updatedRecipes = new ArrayList<>();
        RecipeHolder<?> removed = null;
        for (RecipeHolder<?> holder : getRecipes(manager)) {
            if (CompatUtil.recipeId(holder).equals(CompatUtil.keyId(recipeKey))) {
                removed = holder;
            } else {
                updatedRecipes.add(holder);
            }
        }

        if (removed != null) {
            DELETED_RECIPES.put(recipeKey, removed);
            setRecipes(manager, updatedRecipes);
        }
        return removed;
    }

    /**
     * Restores a recipe previously removed by {@link #removeRecipe} or by a config rule.
     *
     * @return the restored recipe, or null if it isn't in the cache.
     */
    public static RecipeHolder<?> restoreRecipe(RecipeManager manager, ResourceKey<Recipe<?>> recipeKey) {
        RecipeHolder<?> recipe = DELETED_RECIPES.remove(recipeKey);
        if (recipe == null) return null;

        List<RecipeHolder<?>> updatedRecipes = new ArrayList<>(getRecipes(manager));
        updatedRecipes.removeIf(holder -> CompatUtil.recipeId(holder).equals(CompatUtil.keyId(recipeKey)));
        updatedRecipes.add(recipe);
        setRecipes(manager, updatedRecipes);

        Constants.LOG.info("Restored recipe: {}", CompatUtil.keyId(recipeKey));
        return recipe;
    }

    public static void put(RecipeHolder<?> holder) {
        //? if <1.21.2 {
        /*DELETED_RECIPES.put(ResourceKey.create(net.minecraft.core.registries.Registries.RECIPE, holder.id()), holder);
        *///?} else {
        DELETED_RECIPES.put(holder.id(), holder);
        //?}
    }

    public static void clear() {
        DELETED_RECIPES.clear();
    }

    private static List<RecipeHolder<?>> getRecipes(RecipeManager manager) {
        //? if <1.21.2 {
        /*return new ArrayList<>(manager.getRecipes());
        *///?} else {
        RecipeMap currentMap = ((RecipeManagerAccessor) manager).reliableRecipes$getRecipeMap();
        return new ArrayList<>(currentMap.values());
        //?}
    }

    private static void setRecipes(RecipeManager manager, List<RecipeHolder<?>> recipes) {
        //? if <1.21.2 {
        /*manager.replaceRecipes(recipes);
        *///?} else {
        ((RecipeManagerAccessor) manager).reliableRecipes$setRecipeMap(RecipeModifier.createRecipeMap(recipes));
        //?}
    }
}
