package com.evandev.reliable_recipes.recipe;

import com.evandev.reliable_recipes.Constants;
import com.evandev.reliable_recipes.config.RecipeConfigIO;
import com.evandev.reliable_recipes.mixin.accessor.*;
import com.evandev.reliable_recipes.platform.Services;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RecipeModifier {

    public static void apply(RecipeManager manager) {
        int lastErrorCount = 0;
        List<RecipeRule> rules = RecipeConfigIO.loadRules();

        if (rules.isEmpty() && !Services.PLATFORM.hasItemHidingCapabilities()) return;

        RecipeManagerAccessor managerAccessor = (RecipeManagerAccessor) manager;
        Map<RecipeType<?>, Map<ResourceLocation, Recipe<?>>> recipesByType = new HashMap<>(managerAccessor.getRecipes());
        Map<ResourceLocation, Recipe<?>> recipesByName = new HashMap<>(managerAccessor.getByName());

        List<ResourceLocation> toRemove = new ArrayList<>();

        for (Recipe<?> recipe : recipesByName.values()) {
            try {
                boolean shouldRemove = false;

                ItemStack result = getResult(recipe);
                if (!result.isEmpty() && Services.PLATFORM.isItemHidden(result)) {
                    shouldRemove = true;
                }

                if (!shouldRemove) {
                    for (RecipeRule rule : rules) {
                        if (rule.test(recipe)) {
                            if (rule.getAction() == RecipeRule.Action.REMOVE) {
                                shouldRemove = true;
                                break;
                            } else if (rule.getAction() == RecipeRule.Action.REPLACE_INPUT) {
                                replaceInputInRecipe(recipe, rule.getTargetInput(), rule.getNewInput());
                            } else if (rule.getAction() == RecipeRule.Action.REPLACE_OUTPUT) {
                                replaceOutputInRecipe(recipe, rule.getNewOutput());
                            }
                        }
                    }
                }

                if (shouldRemove) {
                    toRemove.add(recipe.getId());
                }
            } catch (Exception e) {
                lastErrorCount++;
                Constants.LOG.error("Error processing recipe {}: {}", recipe.getId(), e.getMessage());
            }
        }

        for (ResourceLocation id : toRemove) {
            Recipe<?> recipe = recipesByName.remove(id);
            if (recipe != null) {
                Map<ResourceLocation, Recipe<?>> typeMap = recipesByType.get(recipe.getType());
                if (typeMap != null) {
                    if (!(typeMap instanceof HashMap)) {
                        typeMap = new HashMap<>(typeMap);
                        recipesByType.put(recipe.getType(), typeMap);
                    }
                    typeMap.remove(id);
                }
            }
        }

        managerAccessor.setRecipes(recipesByType);
        managerAccessor.setByName(recipesByName);

        if (!toRemove.isEmpty()) {
            Constants.LOG.info("RecipeModifier removed {} recipes.", toRemove.size());
        }
        if (lastErrorCount > 0) {
            Constants.LOG.warn("RecipeModifier encountered {} errors during execution.", lastErrorCount);
        }
    }

    private static ItemStack getResult(Recipe<?> recipe) {
        try {
            return recipe.getResultItem(RegistryAccess.EMPTY);
        } catch (Exception e) {
            return ItemStack.EMPTY;
        }
    }

    private static void replaceInputInRecipe(Recipe<?> recipe, Ingredient target, Ingredient replacement) {
        for (int i = 0; i < recipe.getIngredients().size(); i++) {
            if (ingredientMatches(recipe.getIngredients().get(i), target)) {
                recipe.getIngredients().set(i, replacement);
            }
        }
        if (recipe instanceof SingleItemRecipe single && ingredientMatches(single.getIngredients().get(0), target)) {
            ((SingleItemRecipeAccessor) single).setIngredient(replacement);
        }
        if (recipe instanceof AbstractCookingRecipe cooking && ingredientMatches(cooking.getIngredients().get(0), target)) {
            ((AbstractCookingRecipeAccessor) cooking).setIngredient(replacement);
        }
    }

    private static void replaceOutputInRecipe(Recipe<?> recipe, ItemStack newResult) {
        if (recipe instanceof ShapedRecipe shaped) ((ShapedRecipeAccessor) shaped).setResult(newResult);
        else if (recipe instanceof ShapelessRecipe shapeless)
            ((ShapelessRecipeAccessor) shapeless).setResult(newResult);
        else if (recipe instanceof AbstractCookingRecipe cooking)
            ((AbstractCookingRecipeAccessor) cooking).setResult(newResult);
        else if (recipe instanceof SingleItemRecipe single) ((SingleItemRecipeAccessor) single).setResult(newResult);
    }

    private static boolean ingredientMatches(Ingredient ing, Ingredient target) {
        if (ing == null || ing.isEmpty() || target == null || target.isEmpty()) return false;

        try {
            for (ItemStack targetItem : target.getItems()) {
                if (ing.test(targetItem)) return true;
            }
        } catch (Exception ignored) {
        }
        return false;
    }
}