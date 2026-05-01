package com.evandev.reliable_recipes.api;

import net.minecraft.core.RegistryAccess;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;

import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiPredicate;
import java.util.function.Predicate;

public class ReliableRecipesAPI {
    private static final List<Predicate<ItemStack>> ITEM_HIDERS = new ArrayList<>();
    private static final List<BiPredicate<ItemStack, String>> CONTEXTUAL_HIDERS = new ArrayList<>();
    private static final List<Predicate<ItemStack>> REPAIR_BLOCKERS = new ArrayList<>();
    private static final Map<String, String> ITEM_REPLACEMENTS = new HashMap<>();

    private static final Map<Class<?>, Method> RECIPE_OUTPUT_METHODS = new ConcurrentHashMap<>();
    private static final Set<Class<?>> RECIPE_NO_OUTPUT_METHODS = Collections.newSetFromMap(new ConcurrentHashMap<>());

    private static final Map<Class<?>, Method> EXTRACT_METHODS = new ConcurrentHashMap<>();
    private static final Set<Class<?>> NO_EXTRACT_METHODS = Collections.newSetFromMap(new ConcurrentHashMap<>());

    private static final Map<Item, Ingredient> CUSTOM_REPAIR_MATERIALS = new HashMap<>();

    /**
     * Registers an item to be replaced by another item globally in recipes.
     */
    public static void registerItemReplacement(String originalId, String replacementId) {
        ITEM_REPLACEMENTS.put(originalId, replacementId);
    }

    public static Map<String, String> getReplacements() {
        return ITEM_REPLACEMENTS;
    }

    /**
     * Register a predicate that determines if an item should be blocked from being repaired.
     */
    public static void registerRepairBlocker(Predicate<ItemStack> predicate) {
        REPAIR_BLOCKERS.add(predicate);
    }

    /**
     * Checks if an item is blocked from being repaired.
     */
    public static boolean isRepairBlocked(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        for (Predicate<ItemStack> blocker : REPAIR_BLOCKERS) {
            if (blocker.test(stack)) return true;
        }
        return false;
    }

    public static void clearRepairBlockers() {
        REPAIR_BLOCKERS.clear();
    }

    /**
     * Register a predicate that determines if an item should be hidden from recipes, tags, etc.
     */
    public static void registerItemHider(Predicate<ItemStack> predicate) {
        ITEM_HIDERS.add(predicate);
    }

    /**
     * Checks if an item is hidden by any registered mod.
     */
    public static void registerContextualItemHider(BiPredicate<ItemStack, String> predicate) {
        CONTEXTUAL_HIDERS.add(predicate);
    }

    public static boolean isItemHidden(ItemStack stack) {
        return isItemHidden(stack, "item");
    }

    public static boolean isItemHidden(ItemStack stack, String context) {
        if (stack == null || stack.isEmpty()) return false;

        for (BiPredicate<ItemStack, String> hider : CONTEXTUAL_HIDERS) {
            if (hider.test(stack, context)) return true;
        }

        for (Predicate<ItemStack> hider : ITEM_HIDERS) {
            if (hider.test(stack)) return true;
        }
        return false;
    }

    /**
     * Checks if any hiding capabilities have been registered.
     */
    public static boolean hasItemHidingCapabilities() {
        return !ITEM_HIDERS.isEmpty() || !CONTEXTUAL_HIDERS.isEmpty();
    }

    public static void registerCustomRepairMaterial(Item item, Ingredient material) {
        CUSTOM_REPAIR_MATERIALS.put(item, material);
    }

    public static Ingredient getCustomRepairMaterial(Item item) {
        return CUSTOM_REPAIR_MATERIALS.get(item);
    }

    public static void clearCustomRepairMaterials() {
        CUSTOM_REPAIR_MATERIALS.clear();
    }

    /**
     * Extracts all possible result ItemStacks from a given recipe using reflection.
     */
    public static List<ItemStack> getRecipeResults(Recipe<?> recipe) {
        List<ItemStack> results = new ArrayList<>();
        try {
            ItemStack primary = recipe.getResultItem(RegistryAccess.EMPTY);
            if (!primary.isEmpty()) results.add(primary);
        } catch (Exception ignored) {
        }

        Class<?> recipeClass = recipe.getClass();

        if (!RECIPE_NO_OUTPUT_METHODS.contains(recipeClass)) {
            Method method = RECIPE_OUTPUT_METHODS.get(recipeClass);

            if (method == null) {
                String[] methodNames = {"getResults", "getOutputs", "getRollableResults", "getRecipeOutputs"};
                for (String name : methodNames) {
                    try {
                        method = recipeClass.getMethod(name);
                        RECIPE_OUTPUT_METHODS.put(recipeClass, method);
                        break;
                    } catch (Throwable ignored) {
                    }
                }
                if (method == null) {
                    RECIPE_NO_OUTPUT_METHODS.add(recipeClass);
                }
            }

            if (method != null) {
                try {
                    Object result = method.invoke(recipe);
                    if (result instanceof Collection<?> coll) {
                        for (Object obj : coll) {
                            if (obj instanceof ItemStack stack) {
                                if (!stack.isEmpty()) results.add(stack);
                            } else if (obj != null) {
                                ItemStack stack = tryExtractStack(obj);
                                if (stack != null && !stack.isEmpty()) results.add(stack);
                            }
                        }
                    }
                } catch (Exception ignored) {
                }
            }
        }
        return results;
    }

    /**
     * Attempts to dynamically extract an ItemStack from an unknown object.
     */
    public static ItemStack tryExtractStack(Object obj) {
        if (obj instanceof ItemStack s) return s;

        Class<?> objClass = obj.getClass();

        if (!NO_EXTRACT_METHODS.contains(objClass)) {
            Method method = EXTRACT_METHODS.get(objClass);

            if (method == null) {
                String[] methods = {"getStack", "getItemStack", "getItem", "stack", "item"};
                for (String m : methods) {
                    try {
                        method = objClass.getMethod(m);
                        EXTRACT_METHODS.put(objClass, method);
                        break;
                    } catch (Throwable ignored) {
                    }
                }
                if (method == null) {
                    NO_EXTRACT_METHODS.add(objClass);
                }
            }

            if (method != null) {
                try {
                    Object res = method.invoke(obj);
                    if (res instanceof ItemStack s) return s;
                    if (res instanceof Item item) return new ItemStack(item);
                } catch (Exception ignored) {
                }
            }
        }
        return null;
    }
}