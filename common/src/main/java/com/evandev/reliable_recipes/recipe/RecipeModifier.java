package com.evandev.reliable_recipes.recipe;

import com.evandev.reliable_recipes.Constants;
import com.evandev.reliable_recipes.api.ReliableRecipesAPI;
import com.evandev.reliable_recipes.config.RecipeConfigIO;
import com.evandev.reliable_recipes.mixin.accessor.*;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.*;

import java.util.*;

public class RecipeModifier {
    private static final Map<ResourceLocation, RecipeHolder<?>> DELETED_RECIPES_CACHE = new HashMap<>();
    private static boolean hasBeenApplied = false;

    public static void apply(RecipeManager manager) {
        if (hasBeenApplied) {
            Constants.LOG.debug("RecipeModifier already applied, skipping duplicate call");
            return;
        }
        hasBeenApplied = true;

        int lastErrorCount = 0;
        List<RecipeRule> rules = new ArrayList<>(RecipeConfigIO.loadRules());

        for (Map.Entry<String, String> entry : ReliableRecipesAPI.getReplacements().entrySet()) {
            ResourceLocation targetId = ResourceLocation.tryParse(entry.getKey());
            ResourceLocation replaceId = ResourceLocation.tryParse(entry.getValue());

            if (targetId != null && replaceId != null) {
                Item targetItem = BuiltInRegistries.ITEM.get(targetId);
                Item replaceItem = BuiltInRegistries.ITEM.get(replaceId);

                if (targetItem != Items.AIR && replaceItem != Items.AIR) {
                    Ingredient targetIng = Ingredient.of(targetItem);
                    Ingredient replaceIng = Ingredient.of(replaceItem);
                    ItemStack replaceStack = new ItemStack(replaceItem);

                    rules.add(new RecipeRule(RecipeRule.Action.REPLACE_INPUT, r -> true, targetIng, replaceIng));
                    rules.add(new RecipeRule(RecipeRule.Action.REPLACE_OUTPUT, r -> {
                        try {
                            ItemStack out = r.value().getResultItem(RegistryAccess.EMPTY);
                            return !out.isEmpty() && BuiltInRegistries.ITEM.getKey(out.getItem()).equals(targetId);
                        } catch (Exception e) {
                            return false;
                        }
                    }, replaceStack));
                }
            }
        }

        for (RecipeRule rule : rules) {
            if (rule.getAction() == RecipeRule.Action.PREVENT_REPAIR) {
                ReliableRecipesAPI.registerRepairBlocker(stack -> rule.getTargetInput().test(stack));
            }
        }

        if (rules.isEmpty() && !ReliableRecipesAPI.hasItemHidingCapabilities())
            return;

        RecipeManagerAccessor managerAccessor = (RecipeManagerAccessor) manager;

        Map<ResourceLocation, RecipeHolder<?>> recipesByName = new LinkedHashMap<>(managerAccessor.getByName());
        Multimap<RecipeType<?>, RecipeHolder<?>> recipesByType = LinkedHashMultimap.create(managerAccessor.getRecipes());

        List<RecipeHolder<?>> toRemove = new ArrayList<>();

        for (RecipeHolder<?> recipeHolder : recipesByName.values()) {
            try {
                boolean shouldRemove = false;
                Recipe<?> recipe = recipeHolder.value();

                for (RecipeRule rule : rules) {
                    if (rule.test(recipeHolder)) {
                        if (rule.getAction() == RecipeRule.Action.REPLACE_INPUT) {
                            replaceInputInRecipe(recipe, rule.getTargetInput(), rule.getNewInput());
                        } else if (rule.getAction() == RecipeRule.Action.REPLACE_OUTPUT) {
                            replaceOutputInRecipe(recipe, rule.getNewOutput());
                        }
                    }
                }

                ItemStack result = getResult(recipeHolder);
                if (!result.isEmpty() && (ReliableRecipesAPI.isItemHidden(result))) {
                    shouldRemove = true;
                }

                if (!shouldRemove) {
                    try {
                        for (Ingredient ingredient : recipe.getIngredients()) {
                            ItemStack[] items = ingredient.getItems();
                            if (items.length > 0) {
                                boolean allHidden = true;
                                for (ItemStack item : items) {
                                    if (!ReliableRecipesAPI.isItemHidden(item)) {
                                        allHidden = false;
                                        break;
                                    }
                                }
                                if (allHidden) {
                                    shouldRemove = true;
                                    break;
                                }
                            }
                        }
                    } catch (Exception ignored) {
                    }
                }

                if (!shouldRemove) {
                    for (RecipeRule rule : rules) {
                        if (rule.test(recipeHolder) && rule.getAction() == RecipeRule.Action.REMOVE) {
                            shouldRemove = true;
                            break;
                        }
                    }
                }

                if (shouldRemove) {
                    toRemove.add(recipeHolder);
                }
            } catch (Exception e) {
                lastErrorCount++;
                Constants.LOG.error("Error processing recipe {}: {}", recipeHolder.id(), e.getMessage());
            }
        }

        for (RecipeHolder<?> recipe : toRemove) {
            recipesByName.remove(recipe.id());
            recipesByType.remove(recipe.value().getType(), recipe);
        }

        managerAccessor.setByName(ImmutableMap.copyOf(recipesByName));
        managerAccessor.setRecipes(ImmutableMultimap.copyOf(recipesByType));

        if (!toRemove.isEmpty()) {
            Constants.LOG.info("RecipeModifier removed {} recipes.", toRemove.size());
        }
        if (lastErrorCount > 0) {
            Constants.LOG.warn("RecipeModifier encountered {} errors during execution.", lastErrorCount);
        }
    }

    public static boolean removeRecipe(RecipeManager manager, ResourceLocation recipeId) {
        RecipeManagerAccessor managerAccessor = (RecipeManagerAccessor) manager;

        Map<ResourceLocation, RecipeHolder<?>> recipesByName = new LinkedHashMap<>(managerAccessor.getByName());
        Multimap<RecipeType<?>, RecipeHolder<?>> recipesByType = LinkedHashMultimap.create(managerAccessor.getRecipes());

        RecipeHolder<?> recipe = recipesByName.remove(recipeId);

        if (recipe != null) {
            DELETED_RECIPES_CACHE.put(recipeId, recipe);

            recipesByType.remove(recipe.value().getType(), recipe);

            managerAccessor.setByName(ImmutableMap.copyOf(recipesByName));
            managerAccessor.setRecipes(ImmutableMultimap.copyOf(recipesByType));

            return true;
        }
        return false;
    }

    public static boolean restoreRecipe(RecipeManager manager, ResourceLocation recipeId) {
        RecipeHolder<?> recipe = DELETED_RECIPES_CACHE.remove(recipeId);
        if (recipe == null) return false;

        RecipeManagerAccessor managerAccessor = (RecipeManagerAccessor) manager;

        Map<ResourceLocation, RecipeHolder<?>> recipesByName = new LinkedHashMap<>(managerAccessor.getByName());
        Multimap<RecipeType<?>, RecipeHolder<?>> recipesByType = LinkedHashMultimap.create(managerAccessor.getRecipes());

        recipesByName.put(recipeId, recipe);
        recipesByType.put(recipe.value().getType(), recipe);

        managerAccessor.setByName(ImmutableMap.copyOf(recipesByName));
        managerAccessor.setRecipes(ImmutableMultimap.copyOf(recipesByType));

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
        ItemStack copy = newResult.copy();

        if (recipe instanceof ShapedRecipe shaped) ((ShapedRecipeAccessor) shaped).setResult(copy);
        else if (recipe instanceof ShapelessRecipe shapeless) ((ShapelessRecipeAccessor) shapeless).setResult(copy);
        else if (recipe instanceof AbstractCookingRecipe cooking)
            ((AbstractCookingRecipeAccessor) cooking).setResult(copy);
        else if (recipe instanceof SingleItemRecipe single) ((SingleItemRecipeAccessor) single).setResult(copy);
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

    public static void reset() {
        hasBeenApplied = false;
        DELETED_RECIPES_CACHE.clear();
        ReliableRecipesAPI.clearRepairBlockers();
    }
}