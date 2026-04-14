package com.evandev.reliable_recipes.recipe;

import com.evandev.reliable_recipes.Constants;
import com.evandev.reliable_recipes.api.ReliableRecipesAPI;
import com.evandev.reliable_recipes.config.RecipeConfigIO;
import com.evandev.reliable_recipes.mixin.accessor.*;
import com.google.common.collect.ImmutableMap;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.*;

import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class RecipeModifier {
    private static final Map<ResourceLocation, Recipe<?>> DELETED_RECIPES_CACHE = new HashMap<>();
    private static final Map<Class<?>, List<Field>> CLASS_FIELD_CACHE = new ConcurrentHashMap<>();
    private static boolean hasBeenApplied = false;

    private static List<Field> getCachedFields(Class<?> clazz) {
        return CLASS_FIELD_CACHE.computeIfAbsent(clazz, c -> {
            List<Field> fields = new ArrayList<>();
            Class<?> current = c;
            while (current != Object.class && current != null) {
                try {
                    for (Field field : current.getDeclaredFields()) {
                        field.setAccessible(true);
                        fields.add(field);
                    }
                } catch (Throwable ignored) {
                }
                current = current.getSuperclass();
            }
            return fields;
        });
    }

    public static void apply(RecipeManager manager) {
        if (hasBeenApplied) return;
        hasBeenApplied = true;

        List<RecipeRule> rules = new ArrayList<>(RecipeConfigIO.loadRules());

        for (RecipeRule rule : rules) {
            if (rule.getAction() == RecipeRule.Action.PREVENT_REPAIR) {
                ReliableRecipesAPI.registerRepairBlocker(stack -> rule.getTargetInput().test(stack));
            }
        }

        if (rules.isEmpty() && ReliableRecipesAPI.getReplacements().isEmpty() && !ReliableRecipesAPI.hasItemHidingCapabilities())
            return;

        RecipeManagerAccessor managerAccessor = (RecipeManagerAccessor) manager;

        Map<ResourceLocation, Recipe<?>> recipesByName = new LinkedHashMap<>(managerAccessor.getByName());
        Map<RecipeType<?>, Map<ResourceLocation, Recipe<?>>> recipesByType = new LinkedHashMap<>();
        for (var entry : managerAccessor.getRecipes().entrySet()) {
            recipesByType.put(entry.getKey(), new LinkedHashMap<>(entry.getValue()));
        }

        List<Recipe<?>> toRemove = new ArrayList<>();

        for (Recipe<?> recipe : recipesByName.values()) {
            try {
                boolean shouldRemove = false;

                for (Map.Entry<String, String> entry : ReliableRecipesAPI.getReplacements().entrySet()) {
                    ResourceLocation targetId = ResourceLocation.tryParse(entry.getKey());
                    ResourceLocation replaceId = ResourceLocation.tryParse(entry.getValue());
                    if (targetId != null && replaceId != null) {
                        Item targetItem = BuiltInRegistries.ITEM.get(targetId);
                        Item replaceItem = BuiltInRegistries.ITEM.get(replaceId);
                        if (targetItem != Items.AIR && replaceItem != Items.AIR) {
                            replaceInputInRecipe(recipe, Ingredient.of(targetItem), Ingredient.of(replaceItem));
                            replaceOutputInRecipe(recipe, new ItemStack(targetItem), new ItemStack(replaceItem));
                        }
                    }
                }

                for (RecipeRule rule : rules) {
                    if (rule.test(recipe)) {
                        if (rule.getAction() == RecipeRule.Action.REPLACE_INPUT)
                            replaceInputInRecipe(recipe, rule.getTargetInput(), rule.getNewInput());
                        else if (rule.getAction() == RecipeRule.Action.REPLACE_OUTPUT)
                            replaceOutputInRecipe(recipe, ItemStack.EMPTY, rule.getNewOutput());
                        else if (rule.getAction() == RecipeRule.Action.REMOVE) shouldRemove = true;
                    }
                }

                if (!shouldRemove && shouldHideRecipe(recipe)) {
                    shouldRemove = true;
                }

                if (shouldRemove) {
                    toRemove.add(recipe);
                }
            } catch (Exception e) {
                Constants.LOG.error("Error processing recipe: {}", e.getMessage());
            }
        }

        for (Recipe<?> recipe : toRemove) {
            ResourceLocation id = recipe.getId();
            recipesByName.remove(id);
            Map<ResourceLocation, Recipe<?>> typeMap = recipesByType.get(recipe.getType());
            if (typeMap != null) typeMap.remove(id);
        }

        managerAccessor.setByName(ImmutableMap.copyOf(recipesByName));
        managerAccessor.setRecipes(ImmutableMap.copyOf(recipesByType));
    }

    public static boolean removeRecipe(RecipeManager manager, ResourceLocation recipeId) {
        RecipeManagerAccessor managerAccessor = (RecipeManagerAccessor) manager;
        Map<ResourceLocation, Recipe<?>> recipesByName = new LinkedHashMap<>(managerAccessor.getByName());

        Recipe<?> recipe = recipesByName.remove(recipeId);
        if (recipe != null) {
            DELETED_RECIPES_CACHE.put(recipeId, recipe);

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
        Recipe<?> recipe = DELETED_RECIPES_CACHE.remove(recipeId);
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

    private static boolean shouldHideRecipe(Recipe<?> recipe) {
        List<ItemStack> outputs = ReliableRecipesAPI.getRecipeResults(recipe);
        if (outputs.isEmpty()) return false;

        for (ItemStack stack : outputs) {
            if (!stack.isEmpty() && ReliableRecipesAPI.isItemHidden(stack)) {
                return true;
            }
        }
        return false;
    }

    @SuppressWarnings("unchecked")
    private static void replaceInputInRecipe(Recipe<?> recipe, Ingredient target, Ingredient replacement) {
        try {
            List<Ingredient> ingredients = recipe.getIngredients();
            for (int i = 0; i < ingredients.size(); i++) {
                if (ingredientMatches(ingredients.get(i), target)) {
                    try {
                        ingredients.set(i, replacement);
                    } catch (Exception ignored) {
                    }
                }
            }
        } catch (Exception ignored) {
        }

        if (recipe instanceof SingleItemRecipe single && ingredientMatches(single.getIngredients().get(0), target))
            ((SingleItemRecipeAccessor) single).setIngredient(replacement);
        if (recipe instanceof AbstractCookingRecipe cooking && ingredientMatches(cooking.getIngredients().get(0), target))
            ((AbstractCookingRecipeAccessor) cooking).setIngredient(replacement);

        for (Field field : getCachedFields(recipe.getClass())) {
            try {
                Object val = field.get(recipe);
                if (val instanceof Ingredient ing && ingredientMatches(ing, target)) {
                    field.set(recipe, replacement);
                } else if (val instanceof Ingredient[] ings) {
                    for (int i = 0; i < ings.length; i++) {
                        if (ings[i] != null && ingredientMatches(ings[i], target)) ings[i] = replacement;
                    }
                } else if (val instanceof List<?> list) {
                    for (int i = 0; i < list.size(); i++) {
                        if (list.get(i) instanceof Ingredient ing && ingredientMatches(ing, target)) {
                            try {
                                ((List<Ingredient>) list).set(i, replacement);
                            } catch (Exception ignored) {
                            }
                        }
                    }
                } else if (val != null && val.getClass().getSimpleName().contains("ShapedRecipePattern")) {
                    for (Field patternField : getCachedFields(val.getClass())) {
                        try {
                            Object patternVal = patternField.get(val);
                            if (patternVal instanceof List<?> patternList) {
                                for (int i = 0; i < patternList.size(); i++) {
                                    if (patternList.get(i) instanceof Ingredient ing && ingredientMatches(ing, target)) {
                                        try {
                                            ((List<Ingredient>) patternList).set(i, replacement);
                                        } catch (Exception ignored) {
                                        }
                                    }
                                }
                            }
                        } catch (Exception ignored) {
                        }
                    }
                }
            } catch (Exception ignored) {
            }
        }
    }

    @SuppressWarnings("unchecked")
    private static void replaceOutputInRecipe(Recipe<?> recipe, ItemStack targetStack, ItemStack newResult) {
        ItemStack copy = newResult.copy();
        boolean checkMatch = !targetStack.isEmpty();

        if (recipe instanceof ShapedRecipe shaped && (!checkMatch || ItemStack.isSameItem(shaped.getResultItem(RegistryAccess.EMPTY), targetStack)))
            ((ShapedRecipeAccessor) shaped).setResult(copy);
        else if (recipe instanceof ShapelessRecipe shapeless && (!checkMatch || ItemStack.isSameItem(shapeless.getResultItem(RegistryAccess.EMPTY), targetStack)))
            ((ShapelessRecipeAccessor) shapeless).setResult(copy);
        else if (recipe instanceof AbstractCookingRecipe cooking && (!checkMatch || ItemStack.isSameItem(cooking.getResultItem(RegistryAccess.EMPTY), targetStack)))
            ((AbstractCookingRecipeAccessor) cooking).setResult(copy);
        else if (recipe instanceof SingleItemRecipe single && (!checkMatch || ItemStack.isSameItem(single.getResultItem(RegistryAccess.EMPTY), targetStack)))
            ((SingleItemRecipeAccessor) single).setResult(copy);

        if (checkMatch) {
            for (Field field : getCachedFields(recipe.getClass())) {
                try {
                    Object val = field.get(recipe);
                    if (val instanceof ItemStack stack && ItemStack.isSameItem(stack, targetStack)) {
                        field.set(recipe, newResult.copy());
                    } else if (val instanceof ItemStack[] stacks) {
                        for (int i = 0; i < stacks.length; i++) {
                            if (stacks[i] != null && ItemStack.isSameItem(stacks[i], targetStack))
                                stacks[i] = newResult.copy();
                        }
                    } else if (val instanceof List<?> list) {
                        for (int i = 0; i < list.size(); i++) {
                            Object obj = list.get(i);
                            if (obj instanceof ItemStack stack && ItemStack.isSameItem(stack, targetStack)) {
                                try {
                                    ((List<ItemStack>) list).set(i, newResult.copy());
                                } catch (Exception ignored) {
                                }
                            } else if (obj != null) {
                                ItemStack extracted = ReliableRecipesAPI.tryExtractStack(obj);
                                if (extracted != null && ItemStack.isSameItem(extracted, targetStack)) {
                                    for (Field wrapperField : getCachedFields(obj.getClass())) {
                                        try {
                                            Object fieldVal = wrapperField.get(obj);
                                            if (fieldVal instanceof ItemStack ws && ItemStack.isSameItem(ws, targetStack)) {
                                                wrapperField.set(obj, newResult.copy());
                                            }
                                        } catch (Exception ignored) {
                                        }
                                    }
                                }
                            }
                        }
                    }
                } catch (Exception ignored) {
                }
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

    public static void reset() {
        hasBeenApplied = false;
        DELETED_RECIPES_CACHE.clear();
        ReliableRecipesAPI.clearRepairBlockers();
    }
}