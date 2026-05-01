package com.evandev.reliable_recipes.api;

import net.minecraft.util.context.ContextMap;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.display.RecipeDisplay;
import net.minecraft.world.item.crafting.display.SlotDisplay;
import net.minecraft.world.item.crafting.display.SlotDisplayContext;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

public class ReliableRecipesAPI {

    private static final Map<String, String> REPLACEMENTS = new HashMap<>();
    private static final List<Predicate<ItemStack>> REPAIR_BLOCKERS = new ArrayList<>();
    private static final List<Predicate<ItemStack>> ITEM_HIDERS = new ArrayList<>();
    private static final Map<Item, Ingredient> CUSTOM_REPAIR_MATERIALS = new HashMap<>();

    /**
     * Determines whether item hiding functionality is currently enabled and capable.
     *
     * @return true if an item hiding capability is active, false otherwise.
     */
    public static boolean hasItemHidingCapabilities() {
        return !ITEM_HIDERS.isEmpty();
    }

    /**
     * Registers a predicate that determines if a specific ItemStack should be hidden.
     *
     * @param predicate The condition defining whether the stack is hidden.
     */
    public static void registerItemHider(Predicate<ItemStack> predicate) {
        ITEM_HIDERS.add(predicate);
    }

    /**
     * Checks if a specific ItemStack is flagged to be hidden by any registered predicate.
     *
     * @param stack The ItemStack to check.
     * @return true if the item should be hidden.
     */
    public static boolean isItemHidden(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;

        for (Predicate<ItemStack> hider : ITEM_HIDERS) {
            if (hider.test(stack)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Retrieves the outputs of a recipe by parsing its display configuration.
     *
     * @param recipe The recipe to analyze.
     * @return A list of ItemStacks representing the outputs of the recipe.
     */
    public static List<ItemStack> getRecipeResults(Recipe<?> recipe) {
        List<ItemStack> results = new ArrayList<>();
        List<RecipeDisplay> displays = recipe.display();
        ContextMap emptyContext = new ContextMap.Builder().create(SlotDisplayContext.CONTEXT);

        for (RecipeDisplay display : displays) {
            SlotDisplay resultDisplay = display.result();
            results.addAll(resultDisplay.resolveForStacks(emptyContext));
        }
        return results;
    }

    /**
     * Checks if the given ItemStack is blocked from being used in repairs.
     *
     * @param stack The ItemStack to check.
     * @return true if repairing should be prevented.
     */
    public static boolean isRepairBlocked(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;

        for (Predicate<ItemStack> blocker : REPAIR_BLOCKERS) {
            if (blocker.test(stack)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Gets the map of configured string replacements for items.
     *
     * @return A map containing replacement rules.
     */
    public static Map<String, String> getReplacements() {
        return REPLACEMENTS;
    }

    /**
     * Registers an item to be replaced by another item globally in recipes.
     *
     * @param originalId    The original item ID.
     * @param replacementId The replacement item ID.
     */
    public static void registerItemReplacement(String originalId, String replacementId) {
        REPLACEMENTS.put(originalId, replacementId);
    }

    /**
     * Registers a condition to block repairs for certain items.
     *
     * @param predicate The condition evaluated when repairing.
     */
    public static void registerRepairBlocker(Predicate<ItemStack> predicate) {
        REPAIR_BLOCKERS.add(predicate);
    }

    /**
     * Clears all registered repair blockers.
     */
    public static void clearRepairBlockers() {
        REPAIR_BLOCKERS.clear();
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
}