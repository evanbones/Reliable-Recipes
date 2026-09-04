package com.evandev.reliable_recipes.api;

import com.evandev.reliable_recipes.config.RecipeRuleParser;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiPredicate;
import java.util.function.Predicate;

public class ReliableRecipesAPI {
    private static final List<BiPredicate<ItemStack, String>> CONTEXTUAL_HIDERS = new ArrayList<>();
    private static final List<Predicate<ItemStack>> REPAIR_BLOCKERS = new ArrayList<>();
    private static final Map<String, String> ITEM_REPLACEMENTS = new HashMap<>();
    private static final Map<Item, Ingredient> CUSTOM_REPAIR_MATERIALS = new HashMap<>();

    /**
     * Registers an item to be replaced by another item globally in recipes.
     */
    public static void registerItemReplacement(String originalId, String replacementId) {
        ITEM_REPLACEMENTS.put(originalId, replacementId);
    }

    /**
     * Clears item replacements (primarily for hot reloading).
     */
    public static void clearItemReplacements() {
        ITEM_REPLACEMENTS.clear();
    }

    /**
     * Used internally by recipe JSON rewriting to read replacements registered via
     * {@link #registerItemReplacement}.
     */
    public static Map<String, String> getReplacements() {
        return ITEM_REPLACEMENTS;
    }

    /**
     * Registers a predicate that determines if an item should be blocked from being repaired
     * (anvil, grindstone, and vanilla repair recipes).
     */
    public static void registerRepairBlocker(Predicate<ItemStack> predicate) {
        REPAIR_BLOCKERS.add(predicate);
    }

    /**
     * Checks if an item is blocked from being repaired by any registered blocker.
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
     * Registers a predicate that determines if an item should be hidden from recipes, tags,
     * and recipe viewers, given the context it's being checked in (e.g. {@code "item"},
     * {@code "tag:item"}).
     */
    public static void registerContextualItemHider(BiPredicate<ItemStack, String> predicate) {
        CONTEXTUAL_HIDERS.add(predicate);
    }

    /**
     * Checks if an item is hidden by any registered hider, in the default {@code "item"}
     * context.
     */
    public static boolean isItemHidden(ItemStack stack) {
        return isItemHidden(stack, "item");
    }

    /**
     * Checks if an item is hidden by any registered hider for the given context.
     */
    public static boolean isItemHidden(ItemStack stack, String context) {
        if (stack == null || stack.isEmpty()) return false;

        for (BiPredicate<ItemStack, String> hider : CONTEXTUAL_HIDERS) {
            if (hider.test(stack, context)) return true;
        }
        return false;
    }

    /**
     * Checks if any hiding capabilities have been registered, to short-circuit hiding checks
     * when no addon is hiding items.
     */
    public static boolean hasItemHidingCapabilities() {
        return !CONTEXTUAL_HIDERS.isEmpty();
    }

    /**
     * Registers a replacement repair material for an item, used by the anvil, grindstone,
     * and vanilla item-repair recipes in place of the item's default repair material.
     * If a custom repair material is already registered for this item, the materials are merged.
     */
    public static void registerCustomRepairMaterial(Item item, Ingredient material) {
        if (item == null || material == null || material.isEmpty()) return;
        CUSTOM_REPAIR_MATERIALS.compute(item, (k, existing) -> {
            if (existing == null || existing.isEmpty()) {
                return material;
            }
            return RecipeRuleParser.mergeIngredients(List.of(existing, material));
        });
    }

    public static Ingredient getCustomRepairMaterial(Item item) {
        return CUSTOM_REPAIR_MATERIALS.get(item);
    }

    public static void clearCustomRepairMaterials() {
        CUSTOM_REPAIR_MATERIALS.clear();
    }
}
