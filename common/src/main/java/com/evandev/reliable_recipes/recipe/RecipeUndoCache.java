package com.evandev.reliable_recipes.recipe;

import com.evandev.reliable_recipes.Constants;
import com.evandev.reliable_recipes.mixin.accessor.RecipeManagerAccessor;
import com.google.common.collect.ImmutableMap;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Tracks recipes removed at runtime so they can be restored by {@code /rrecipes_undo} without needing a full datapack
 * reload.
 */
public class RecipeUndoCache {
    private static final Map<ResourceLocation, Recipe<?>> DELETED_RECIPES = new HashMap<>();

    public static boolean removeRecipe(RecipeManager manager, ResourceLocation recipeId) {
        RecipeManagerAccessor managerAccessor = (RecipeManagerAccessor) manager;
        Map<ResourceLocation, Recipe<?>> recipesByName = new LinkedHashMap<>(managerAccessor.getByName());

        Recipe<?> recipe = recipesByName.remove(recipeId);
        if (recipe != null) {
            DELETED_RECIPES.put(recipeId, recipe);

            Map<RecipeType<?>, Map<ResourceLocation, Recipe<?>>> recipesByType = new LinkedHashMap<>();
            for (var entry : managerAccessor.getRecipes().entrySet()) {
                recipesByType.put(entry.getKey(), new LinkedHashMap<>(entry.getValue()));
            }

            Map<ResourceLocation, Recipe<?>> typeMap = recipesByType.get(recipe.getType());
            if (typeMap != null) typeMap.remove(recipeId);

            managerAccessor.setByName(ImmutableMap.copyOf(recipesByName));
            managerAccessor.setRecipes(ImmutableMap.copyOf(recipesByType));
            return true;
        }
        return false;
    }

    public static boolean restoreRecipe(RecipeManager manager, ResourceLocation recipeId) {
        Recipe<?> recipe = DELETED_RECIPES.remove(recipeId);
        if (recipe == null) return false;

        RecipeManagerAccessor managerAccessor = (RecipeManagerAccessor) manager;
        Map<ResourceLocation, Recipe<?>> recipesByName = new LinkedHashMap<>(managerAccessor.getByName());

        Map<RecipeType<?>, Map<ResourceLocation, Recipe<?>>> recipesByType = new LinkedHashMap<>();
        for (var entry : managerAccessor.getRecipes().entrySet()) {
            recipesByType.put(entry.getKey(), new LinkedHashMap<>(entry.getValue()));
        }

        recipesByName.put(recipeId, recipe);
        recipesByType.computeIfAbsent(recipe.getType(), k -> new LinkedHashMap<>()).put(recipeId, recipe);

        managerAccessor.setByName(ImmutableMap.copyOf(recipesByName));
        managerAccessor.setRecipes(ImmutableMap.copyOf(recipesByType));

        Constants.LOG.info("Restored recipe: {}", recipeId);
        return true;
    }

    public static void clear() {
        DELETED_RECIPES.clear();
    }
}
