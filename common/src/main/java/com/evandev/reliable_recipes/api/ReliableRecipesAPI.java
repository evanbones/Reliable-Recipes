package com.evandev.reliable_recipes.api;

import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

public class ReliableRecipesAPI {
    private static final List<Predicate<ItemStack>> ITEM_HIDERS = new ArrayList<>();

    /**
     * Register a predicate that determines if an item should be hidden from recipes, tags, etc.
     */
    public static void registerItemHider(Predicate<ItemStack> predicate) {
        ITEM_HIDERS.add(predicate);
    }

    /**
     * Checks if an item is hidden by any registered mod.
     */
    public static boolean isItemHidden(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        for (Predicate<ItemStack> hider : ITEM_HIDERS) {
            if (hider.test(stack)) return true;
        }
        return false;
    }

    /**
     * Checks if any hiding capabilities have been registered.
     */
    public static boolean hasItemHidingCapabilities() {
        return !ITEM_HIDERS.isEmpty();
    }
}