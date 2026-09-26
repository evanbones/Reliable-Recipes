package com.evandev.reliable_recipes.api;

import com.evandev.reliable_recipes.config.RecipeRuleParser;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
//? if <1.21.2 {
/*import net.minecraft.core.RegistryAccess;
*///?} else {
import net.minecraft.util.context.ContextMap;
import net.minecraft.world.item.crafting.display.RecipeDisplay;
import net.minecraft.world.item.crafting.display.SlotDisplay;
import net.minecraft.world.item.crafting.display.SlotDisplayContext;
//?}
//? if >=1.21.2 && <=26.2 {
import com.evandev.reliable_recipes.recipe.BrewingRecipe;
//?} else if >26.2 {
/*import net.minecraft.world.item.crafting.BrewingRecipe;
*///?}

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiPredicate;
import java.util.function.Predicate;

public class ReliableRecipesAPI {

    private static final Map<String, String> REPLACEMENTS = new HashMap<>();
    private static final List<Predicate<ItemStack>> REPAIR_BLOCKERS = new ArrayList<>();
    private static final List<Predicate<ItemStack>> ITEM_HIDERS = new ArrayList<>();
    private static final List<BiPredicate<ItemStack, String>> CONTEXTUAL_HIDERS = new ArrayList<>();
    private static final Map<Item, Ingredient> CUSTOM_REPAIR_MATERIALS = new HashMap<>();

    /**
     * Determines whether item hiding functionality is currently enabled and capable.
     *
     * @return true if an item hiding capability is active, false otherwise.
     */
    public static boolean hasItemHidingCapabilities() {
        return !ITEM_HIDERS.isEmpty() || !CONTEXTUAL_HIDERS.isEmpty();
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
     * Registers a predicate that determines if an item should be hidden from recipes, tags,
     * and recipe viewers, given the context it's being checked in (e.g. {@code "item"},
     * {@code "tag:item"}).
     *
     * @param predicate The condition defining whether the stack is hidden for a given context.
     */
    public static void registerContextualItemHider(BiPredicate<ItemStack, String> predicate) {
        CONTEXTUAL_HIDERS.add(predicate);
    }

    /**
     * Checks if a specific ItemStack is flagged to be hidden in the default "item" context.
     *
     * @param stack The ItemStack to check.
     * @return true if the item should be hidden.
     */
    public static boolean isItemHidden(ItemStack stack) {
        return isItemHidden(stack, "item");
    }

    /**
     * Checks if a specific ItemStack is flagged to be hidden for the given context.
     *
     * @param stack   The ItemStack to check.
     * @param context The context string (e.g. "item", "tag:item").
     * @return true if the item should be hidden.
     */
    public static boolean isItemHidden(ItemStack stack, String context) {
        if (stack == null || stack.isEmpty()) return false;

        for (Predicate<ItemStack> hider : ITEM_HIDERS) {
            if (hider.test(stack)) {
                return true;
            }
        }
        for (BiPredicate<ItemStack, String> hider : CONTEXTUAL_HIDERS) {
            if (hider.test(stack, context)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Retrieves the outputs of a recipe.
     *
     * @param recipe The recipe to analyze.
     * @return A list of ItemStacks representing the outputs of the recipe.
     */
    public static List<ItemStack> getRecipeResults(Recipe<?> recipe) {
        //? if <1.21.2 {
        /*ItemStack result = recipe.getResultItem(RegistryAccess.EMPTY);
        return result.isEmpty() ? List.of() : List.of(result);
        *///?} else {
        //? if <=26.2 {
        if (recipe instanceof BrewingRecipe brewingRecipe) {
            return List.of(brewingRecipe.getOutput());
        }
        //?} else {
        /*if (recipe instanceof BrewingRecipe brewingRecipe) {
            return List.of(brewingRecipe.getOutput().create());
        }
        *///?}
        List<ItemStack> results = new ArrayList<>();
        List<RecipeDisplay> displays = recipe.display();
        //? if <=26.2 {
        ContextMap emptyContext = new ContextMap.Builder().create(SlotDisplayContext.CONTEXT);
        //?} else {
        /*ContextMap emptyContext = ContextMap.EMPTY;
        *///?}

        for (RecipeDisplay display : displays) {
            SlotDisplay resultDisplay = display.result();
            results.addAll(resultDisplay.resolveForStacks(emptyContext));
        }
        return results;
        //?}
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
     * Clears item replacements (primarily for hot reloading).
     */
    public static void clearItemReplacements() {
        REPLACEMENTS.clear();
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
