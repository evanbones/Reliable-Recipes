package com.evandev.reliable_recipes.recipe;

//? if <=26.2 {
import com.evandev.reliable_recipes.api.ReliableRecipesAPI;
import com.evandev.reliable_recipes.config.RecipeConfigIO;
import com.evandev.reliable_recipes.util.CompatUtil;
import com.google.gson.JsonObject;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
//? if >=1.21.2 {
import com.evandev.reliable_recipes.mixin.accessor.RecipeManagerAccessor;
//?}

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class BrewingRecipeManager {
    private static final List<BrewingRecipe> BREWING_RECIPES = new ArrayList<>();
    private static final ThreadLocal<Boolean> SUPPRESS_OVERRIDES = ThreadLocal.withInitial(() -> false);

    public static boolean isSuppressingOverrides() {
        return SUPPRESS_OVERRIDES.get();
    }

    public static void withVanillaBehavior(Runnable action) {
        SUPPRESS_OVERRIDES.set(true);
        try {
            action.run();
        } finally {
            SUPPRESS_OVERRIDES.set(false);
        }
    }

    public static void reload(RecipeManager recipeManager) {
        BREWING_RECIPES.clear();
        //? if <1.21.2 {
        /*Iterable<? extends RecipeHolder<?>> holders = recipeManager.getAllRecipesFor(BrewingRecipe.TYPE);
        *///?} else {
        Iterable<? extends RecipeHolder<?>> holders = ((RecipeManagerAccessor) recipeManager).reliableRecipes$getRecipeMap().byType(BrewingRecipe.TYPE);
        //?}
        for (RecipeHolder<?> holder : holders) {
            if (holder.value() instanceof BrewingRecipe brewingRecipe) {
                BREWING_RECIPES.add(brewingRecipe);
            }
        }
    }

    public static Optional<BrewingRecipe> findRecipe(ItemStack input, ItemStack reagent) {
        for (BrewingRecipe recipe : BREWING_RECIPES) {
            if (recipe.matches(input, reagent)) {
                if (ReliableRecipesAPI.hasItemHidingCapabilities()) {
                    if (ReliableRecipesAPI.isItemHidden(input) || ReliableRecipesAPI.isItemHidden(reagent) || ReliableRecipesAPI.isItemHidden(recipe.getOutput())) {
                        continue;
                    }
                }
                return Optional.of(recipe);
            }
        }
        return Optional.empty();
    }

    public static boolean hasMix(ItemStack input, ItemStack reagent) {
        return findRecipe(input, reagent).isPresent();
    }

    public static ItemStack mix(ItemStack reagent, ItemStack input) {
        return findRecipe(input, reagent).map(r -> r.getOutput().copy()).orElse(ItemStack.EMPTY);
    }

    public static boolean isReagent(ItemStack stack) {
        if (ReliableRecipesAPI.hasItemHidingCapabilities() && ReliableRecipesAPI.isItemHidden(stack)) {
            return false;
        }
        for (BrewingRecipe recipe : BREWING_RECIPES) {
            if (recipe.getReagent().test(stack)) {
                if (ReliableRecipesAPI.hasItemHidingCapabilities() && ReliableRecipesAPI.isItemHidden(recipe.getOutput())) {
                    continue;
                }
                return true;
            }
        }
        for (RecipeRule rule : RecipeConfigIO.loadRules()) {
            if (rule.getAction() == RecipeRule.Action.REPLACE_INPUT && rule.replacementMatches(stack)) {
                return true;
            }
        }
        return false;
    }

    public static boolean isInput(ItemStack stack) {
        if (ReliableRecipesAPI.hasItemHidingCapabilities() && ReliableRecipesAPI.isItemHidden(stack)) {
            return false;
        }
        for (BrewingRecipe recipe : BREWING_RECIPES) {
            if (recipe.getInputMatcher().matches(stack)) {
                if (ReliableRecipesAPI.hasItemHidingCapabilities() && ReliableRecipesAPI.isItemHidden(recipe.getOutput())) {
                    continue;
                }
                return true;
            }
        }
        return false;
    }

    public static List<BrewingRecipe> getBrewingRecipes() {
        return BREWING_RECIPES;
    }

    public static Optional<ItemStack> getReplacedReagent(ItemStack stack) {
        for (RecipeRule rule : RecipeConfigIO.loadRules()) {
            if (rule.getAction() == RecipeRule.Action.REPLACE_INPUT && rule.replacementMatches(stack)) {
                for (String rawTarget : rule.getRawTargets()) {
                    Identifier loc = Identifier.tryParse(rawTarget.startsWith("#") ? rawTarget.substring(1) : rawTarget);
                    if (loc != null) {
                        Item targetItem = CompatUtil.getItem(loc);
                        if (targetItem != Items.AIR) {
                            return Optional.of(new ItemStack(targetItem));
                        }
                    }
                }
            }
        }
        return Optional.empty();
    }

    public static boolean isBrewingRecipeRemoved(ItemStack input, ItemStack reagent, ItemStack output) {
        if (ReliableRecipesAPI.hasItemHidingCapabilities()) {
            if (ReliableRecipesAPI.isItemHidden(input) || ReliableRecipesAPI.isItemHidden(reagent) || ReliableRecipesAPI.isItemHidden(output)) {
                return true;
            }
        }

        List<RecipeRule> rules = RecipeConfigIO.loadRules();
        if (rules.isEmpty()) return false;

        JsonObject dummyJson = createBrewingRecipeJson(input, reagent, output);
        Identifier dummyId = Identifier.withDefaultNamespace("brewing_vanilla_mix");

        for (RecipeRule rule : rules) {
            if (rule.getAction() == RecipeRule.Action.REMOVE && rule.testJson(dummyId, dummyJson)) {
                return true;
            }
            if (rule.getAction() == RecipeRule.Action.REPLACE_INPUT && rule.targetsMatch(reagent)) {
                return true;
            }
        }
        return false;
    }

    public static JsonObject createBrewingRecipeJson(ItemStack input, ItemStack reagent, ItemStack output) {
        JsonObject json = new JsonObject();
        json.addProperty("type", "minecraft:brewing");

        json.add("input", stackToJson(input));
        json.add("reagent", stackToJson(reagent));

        JsonObject outputObj = stackToJson(output);
        json.add("output", outputObj);
        json.add("result", outputObj);

        return json;
    }

    public static JsonObject stackToJson(ItemStack stack) {
        JsonObject obj = new JsonObject();
        Identifier itemKey = BuiltInRegistries.ITEM.getKey(stack.getItem());
        obj.addProperty("item", itemKey.toString());
        obj.addProperty("id", itemKey.toString());

        PotionContents contents = stack.get(DataComponents.POTION_CONTENTS);
        if (contents != null && contents.potion().isPresent()) {
            contents.potion().get().unwrapKey().ifPresent(key -> {
                JsonObject potionContentsObj = new JsonObject();
                potionContentsObj.addProperty("potion", CompatUtil.keyId(key).toString());
                potionContentsObj.addProperty("potions", CompatUtil.keyId(key).toString());
                obj.add("potion_contents", potionContentsObj);

                JsonObject componentsObj = new JsonObject();
                componentsObj.add("minecraft:potion_contents", potionContentsObj);
                obj.add("components", componentsObj);
            });
        }
        return obj;
    }
}
//?} else {
/*public class BrewingRecipeManager {}
*///?}
