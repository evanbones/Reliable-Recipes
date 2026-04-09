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

import java.lang.reflect.Field;
import java.util.*;

public class RecipeModifier {
    private static final Map<ResourceLocation, RecipeHolder<?>> DELETED_RECIPES_CACHE = new HashMap<>();
    private static boolean hasBeenApplied = false;

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
        Map<ResourceLocation, RecipeHolder<?>> recipesByName = new LinkedHashMap<>(managerAccessor.getByName());
        Multimap<RecipeType<?>, RecipeHolder<?>> recipesByType = LinkedHashMultimap.create(managerAccessor.getRecipes());
        List<RecipeHolder<?>> toRemove = new ArrayList<>();

        for (RecipeHolder<?> recipeHolder : recipesByName.values()) {
            try {
                boolean shouldRemove = false;
                Recipe<?> recipe = recipeHolder.value();

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
                    if (rule.test(recipeHolder)) {
                        if (rule.getAction() == RecipeRule.Action.REPLACE_INPUT)
                            replaceInputInRecipe(recipe, rule.getTargetInput(), rule.getNewInput());
                        else if (rule.getAction() == RecipeRule.Action.REPLACE_OUTPUT)
                            replaceOutputInRecipe(recipe, ItemStack.EMPTY, rule.getNewOutput());
                        else if (rule.getAction() == RecipeRule.Action.REMOVE) shouldRemove = true;
                    }
                }

                if (!shouldRemove && shouldHideRecipe(recipeHolder)) {
                    shouldRemove = true;
                }

                if (shouldRemove) {
                    toRemove.add(recipeHolder);
                }
            } catch (Exception e) {
                Constants.LOG.error("Error processing recipe {}: {}", recipeHolder.id(), e.getMessage());
            }
        }

        for (RecipeHolder<?> recipe : toRemove) {
            recipesByName.remove(recipe.id());
            recipesByType.remove(recipe.value().getType(), recipe);
        }

        managerAccessor.setByName(ImmutableMap.copyOf(recipesByName));
        managerAccessor.setRecipes(ImmutableMultimap.copyOf(recipesByType));
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

    private static boolean shouldHideRecipe(RecipeHolder<?> holder) {
        List<ItemStack> outputs = ReliableRecipesAPI.getRecipeResults(holder.value());
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
            for (int i = 0; i < recipe.getIngredients().size(); i++) {
                if (ingredientMatches(recipe.getIngredients().get(i), target))
                    recipe.getIngredients().set(i, replacement);
            }
        } catch (Exception ignored) {
        }

        if (recipe instanceof SingleItemRecipe single && ingredientMatches(single.getIngredients().getFirst(), target))
            ((SingleItemRecipeAccessor) single).setIngredient(replacement);
        if (recipe instanceof AbstractCookingRecipe cooking && ingredientMatches(cooking.getIngredients().getFirst(), target))
            ((AbstractCookingRecipeAccessor) cooking).setIngredient(replacement);

        Class<?> clazz = recipe.getClass();
        while (clazz != Object.class && clazz != null) {
            try {
                for (Field field : clazz.getDeclaredFields()) {
                    field.setAccessible(true);
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
                        Class<?> patternClass = val.getClass();
                        while (patternClass != Object.class && patternClass != null) {
                            for (Field patternField : patternClass.getDeclaredFields()) {
                                patternField.setAccessible(true);
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
                            patternClass = patternClass.getSuperclass();
                        }
                    }
                }
            } catch (Exception ignored) {
            }
            clazz = clazz.getSuperclass();
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
            Class<?> clazz = recipe.getClass();
            while (clazz != Object.class && clazz != null) {
                try {
                    for (Field field : clazz.getDeclaredFields()) {
                        field.setAccessible(true);
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
                                        Class<?> wrapperClass = obj.getClass();
                                        while (wrapperClass != Object.class && wrapperClass != null) {
                                            for (Field wrapperField : wrapperClass.getDeclaredFields()) {
                                                wrapperField.setAccessible(true);
                                                try {
                                                    Object fieldVal = wrapperField.get(obj);
                                                    if (fieldVal instanceof ItemStack ws && ItemStack.isSameItem(ws, targetStack)) {
                                                        wrapperField.set(obj, newResult.copy());
                                                    }
                                                } catch (Exception ignored) {
                                                }
                                            }
                                            wrapperClass = wrapperClass.getSuperclass();
                                        }
                                    }
                                }
                            }
                        }
                    }
                } catch (Exception ignored) {
                }
                clazz = clazz.getSuperclass();
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