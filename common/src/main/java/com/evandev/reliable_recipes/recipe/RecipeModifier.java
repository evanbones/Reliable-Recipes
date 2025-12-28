package com.evandev.reliable_recipes.recipe;

import com.evandev.reliable_recipes.Constants;
import com.evandev.reliable_recipes.config.RecipeConfigIO;
import com.evandev.reliable_recipes.mixin.accessor.*;
import com.evandev.reliable_recipes.platform.Services;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import org.jetbrains.annotations.UnknownNullability;

import java.util.*;

public class RecipeModifier {
    private static final Map<ResourceLocation, RecipeHolder<?>> DELETED_RECIPES_CACHE = new HashMap<>();

    public static void apply(RecipeManager manager) {
        int lastErrorCount = 0;
        List<RecipeRule> rules = RecipeConfigIO.loadRules();

        if (rules.isEmpty() && !Services.PLATFORM.hasItemHidingCapabilities()) return;

        RecipeManagerAccessor managerAccessor = (RecipeManagerAccessor) manager;
        Multimap<RecipeType<?>, RecipeHolder<?>> recipesByType = HashMultimap.create(managerAccessor.getRecipes());
        Map<ResourceLocation, RecipeHolder<?>> recipesByName = new HashMap<>(managerAccessor.getByName());

        List<ResourceLocation> toRemove = new ArrayList<>();

        for (RecipeHolder<?> recipe : recipesByName.values()) {
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
                    toRemove.add(recipe.id());
                }
            } catch (Exception e) {
                lastErrorCount++;
                Constants.LOG.error("Error processing recipe {}: {}", recipe.id(), e.getMessage());
            }
        }

        for (ResourceLocation id : toRemove) {
            RecipeHolder<?> recipe = recipesByName.remove(id);
            if (recipe != null) {
                recipesByType.get(recipe.value().getType())
                        .removeIf(r -> r.id().equals(id));
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

    public static boolean removeRecipe(RecipeManager manager, ResourceLocation recipeId) {
        RecipeManagerAccessor managerAccessor = (RecipeManagerAccessor) manager;

        Map<ResourceLocation, RecipeHolder<?>> recipesByName = new HashMap<>(managerAccessor.getByName());
        Multimap<RecipeType<?>, RecipeHolder<?>> recipesByType = HashMultimap.create(managerAccessor.getRecipes());

        RecipeHolder<?> recipe = recipesByName.remove(recipeId);

        if (recipe != null) {
            DELETED_RECIPES_CACHE.put(recipeId, recipe);

            recipesByType.get(recipe.value().getType())
                    .removeIf(r -> r.id().equals(recipeId));

            managerAccessor.setByName(recipesByName);
            managerAccessor.setRecipes(recipesByType);

            return true;
        }
        return false;
    }

    public static boolean restoreRecipe(RecipeManager manager, ResourceLocation recipeId) {
        RecipeHolder<?> recipe = DELETED_RECIPES_CACHE.remove(recipeId);
        if (recipe == null) return false;

        RecipeManagerAccessor managerAccessor = (RecipeManagerAccessor) manager;

        Map<ResourceLocation, RecipeHolder<?>> recipesByName = new HashMap<>(managerAccessor.getByName());
        HashMultimap<RecipeType<?>, RecipeHolder<?>> recipesByType = HashMultimap.create(managerAccessor.getRecipes());

        recipesByName.put(recipeId, recipe);

        recipesByType.put(recipe.value().getType(), recipe);

        managerAccessor.setByName(recipesByName);
        managerAccessor.setRecipes(recipesByType);

        Constants.LOG.info("Restored recipe: {}", recipeId);
        return true;
    }

    private static ItemStack getResult(RecipeHolder<?> recipe) {
        try {
            return recipe.value().getResultItem(RegistryAccess.EMPTY);
        } catch (Exception e) {
            return ItemStack.EMPTY;
        }
    }

    private static void replaceInputInRecipe(@UnknownNullability RecipeHolder<?> holder, Ingredient target, Ingredient replacement) {
        var recipe = holder.value();
        for (int i = 0; i < recipe.getIngredients().size(); i++) {
            if (ingredientMatches(recipe.getIngredients().get(i), target)) {
                recipe.getIngredients().set(i, replacement);
            }
        }
        if (recipe instanceof SingleItemRecipe single && ingredientMatches(single.getIngredients().getFirst(), target)) {
            ((SingleItemRecipeAccessor) single).setIngredient(replacement);
        }
        if (recipe instanceof AbstractCookingRecipe cooking && ingredientMatches(cooking.getIngredients().getFirst(), target)) {
            ((AbstractCookingRecipeAccessor) cooking).setIngredient(replacement);
        }
    }

    private static void replaceOutputInRecipe(RecipeHolder<?> holder, ItemStack newResult) {
        var recipe = holder.value();
        switch (recipe) {
            case ShapedRecipe shaped -> ((ShapedRecipeAccessor) shaped).setResult(newResult);
            case ShapelessRecipe shapeless -> ((ShapelessRecipeAccessor) shapeless).setResult(newResult);
            case AbstractCookingRecipe cooking -> ((AbstractCookingRecipeAccessor) cooking).setResult(newResult);
            case SingleItemRecipe single -> ((SingleItemRecipeAccessor) single).setResult(newResult);
            default -> {
            }
        }
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