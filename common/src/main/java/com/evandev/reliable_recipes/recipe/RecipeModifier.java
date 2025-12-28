package com.evandev.reliable_recipes.recipe;

import com.evandev.reliable_recipes.Constants;
import com.evandev.reliable_recipes.config.RecipeConfigIO;
import com.evandev.reliable_recipes.mixin.accessor.*;
import com.evandev.reliable_recipes.platform.Services;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RecipeModifier {
    private static final Map<ResourceLocation, RecipeHolder<?>> DELETED_RECIPES_CACHE = new HashMap<>();

    public static void apply(RecipeManager manager) {
        List<RecipeRule> rules = RecipeConfigIO.loadRules();

        if (rules.isEmpty() && !Services.PLATFORM.hasItemHidingCapabilities()) return;

        RecipeManagerAccessor managerAccessor = (RecipeManagerAccessor) manager;

        Multimap<RecipeType<?>, RecipeHolder<?>> immutableByType = managerAccessor.getByType();
        Map<ResourceLocation, RecipeHolder<?>> immutableByName = managerAccessor.getByName();

        ArrayListMultimap<RecipeType<?>, RecipeHolder<?>> recipesByType = ArrayListMultimap.create(immutableByType);
        Map<ResourceLocation, RecipeHolder<?>> recipesByName = new HashMap<>(immutableByName);

        List<ResourceLocation> toRemove = new ArrayList<>();

        for (RecipeHolder<?> holder : recipesByName.values()) {
            try {
                Recipe<?> recipe = holder.value();
                ResourceLocation id = holder.id();

                boolean shouldRemove = false;

                ItemStack result = getResult(recipe);
                if (!result.isEmpty() && Services.PLATFORM.isItemHidden(result)) {
                    shouldRemove = true;
                }

                if (!shouldRemove) {
                    for (RecipeRule rule : rules) {
                        if (rule.test(holder)) {
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
                    toRemove.add(id);
                }
            } catch (Exception e) {
                Constants.LOG.error("Error processing recipe {}: {}", holder.id(), e.getMessage());
            }
        }

        for (ResourceLocation id : toRemove) {
            RecipeHolder<?> holder = recipesByName.remove(id);
            if (holder != null) {
                recipesByType.remove(holder.value().getType(), holder);
            }
        }

        managerAccessor.setByType(ImmutableMultimap.copyOf(recipesByType));
        managerAccessor.setByName(ImmutableMap.copyOf(recipesByName));

        if (!toRemove.isEmpty()) {
            Constants.LOG.info("RecipeModifier removed {} recipes.", toRemove.size());
        }
    }

    public static boolean removeRecipe(RecipeManager manager, ResourceLocation recipeId) {
        RecipeManagerAccessor managerAccessor = (RecipeManagerAccessor) manager;

        Map<ResourceLocation, RecipeHolder<?>> recipesByName = new HashMap<>(managerAccessor.getByName());
        ArrayListMultimap<RecipeType<?>, RecipeHolder<?>> recipesByType = ArrayListMultimap.create(managerAccessor.getByType());

        RecipeHolder<?> holder = recipesByName.remove(recipeId);

        if (holder != null) {
            DELETED_RECIPES_CACHE.put(recipeId, holder);
            recipesByType.remove(holder.value().getType(), holder);

            managerAccessor.setByName(ImmutableMap.copyOf(recipesByName));
            managerAccessor.setByType(ImmutableMultimap.copyOf(recipesByType));

            return true;
        }
        return false;
    }

    public static boolean restoreRecipe(RecipeManager manager, ResourceLocation recipeId) {
        RecipeHolder<?> holder = DELETED_RECIPES_CACHE.remove(recipeId);
        if (holder == null) return false;

        RecipeManagerAccessor managerAccessor = (RecipeManagerAccessor) manager;

        Map<ResourceLocation, RecipeHolder<?>> recipesByName = new HashMap<>(managerAccessor.getByName());

        ArrayListMultimap<RecipeType<?>, RecipeHolder<?>> recipesByType = ArrayListMultimap.create(managerAccessor.getByType());

        recipesByName.put(recipeId, holder);
        recipesByType.put(holder.value().getType(), holder);

        managerAccessor.setByName(ImmutableMap.copyOf(recipesByName));
        managerAccessor.setByType(ImmutableMultimap.copyOf(recipesByType));

        Constants.LOG.info("Restored recipe: {}", recipeId);
        return true;
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
        if (recipe instanceof SingleItemRecipe single && ingredientMatches(single.getIngredients().getFirst(), target)) {
            ((SingleItemRecipeAccessor) single).setIngredient(replacement);
        }
        if (recipe instanceof AbstractCookingRecipe cooking && ingredientMatches(cooking.getIngredients().getFirst(), target)) {
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
        } catch (Exception ignored) {}
        return false;
    }
}