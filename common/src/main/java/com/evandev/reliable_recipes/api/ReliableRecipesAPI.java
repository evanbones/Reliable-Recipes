package com.evandev.reliable_recipes.api;

import net.minecraft.util.context.ContextMap;
import net.minecraft.world.item.ItemStack;
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
    private static boolean ITEM_HIDING_CAPABLE = false;
    private static Predicate<ItemStack> HIDING_PREDICATE = stack -> false;

    /**
     * Determines whether item hiding functionality is currently enabled and capable.
     *
     * @return true if an item hiding capability is active, false otherwise.
     */
    public static boolean hasItemHidingCapabilities() {
        return ITEM_HIDING_CAPABLE;
    }

    /**
     * Registers a predicate that determines if a specific ItemStack should be hidden.
     *
     * @param predicate The condition defining whether the stack is hidden.
     */
    public static void setItemHidingCapability(Predicate<ItemStack> predicate) {
        ITEM_HIDING_CAPABLE = true;
        HIDING_PREDICATE = predicate;
    }

    /**
     * Checks if a specific ItemStack is flagged to be hidden by the registered predicate.
     *
     * @param stack The ItemStack to check.
     * @return true if the item should be hidden.
     */
    public static boolean isItemHidden(ItemStack stack) {
        return HIDING_PREDICATE.test(stack);
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
}