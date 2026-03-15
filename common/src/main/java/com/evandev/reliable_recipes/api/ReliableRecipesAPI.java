package com.evandev.reliable_recipes.api;

import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiPredicate;
import java.util.function.Predicate;

public class ReliableRecipesAPI {
    private static final List<Predicate<ItemStack>> ITEM_HIDERS = new ArrayList<>();
    private static final List<BiPredicate<ItemStack, String>> CONTEXTUAL_HIDERS = new ArrayList<>();
    private static final List<Predicate<ItemStack>> REPAIR_BLOCKERS = new ArrayList<>();

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
}