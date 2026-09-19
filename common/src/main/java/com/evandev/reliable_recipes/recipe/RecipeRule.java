package com.evandev.reliable_recipes.recipe;

import com.evandev.reliable_recipes.config.RecipeRuleParser;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiPredicate;
import java.util.regex.Pattern;

public class RecipeRule {
    private final Action action;
    private final BiPredicate<ResourceLocation, JsonObject> jsonFilter;
    private final Ingredient targetInput;
    private final Ingredient newInput;
    private final List<String> rawTargets;
    private final JsonElement rawReplacement;

    // Removals
    public RecipeRule(Action action, BiPredicate<ResourceLocation, JsonObject> filter) {
        this(action, filter, Ingredient.EMPTY, Ingredient.EMPTY, List.of(), null);
    }

    // JSON replacements
    public RecipeRule(Action action, BiPredicate<ResourceLocation, JsonObject> filter, List<String> rawTargets, JsonElement rawReplacement) {
        this(action, filter, Ingredient.EMPTY, Ingredient.EMPTY, rawTargets, rawReplacement);
    }

    // Repair interactions
    public RecipeRule(Action action, BiPredicate<ResourceLocation, JsonObject> filter, Ingredient target, Ingredient rep) {
        this(action, filter, target, rep, List.of(), null);
    }

    private RecipeRule(Action action, BiPredicate<ResourceLocation, JsonObject> filter, Ingredient target, Ingredient rep, List<String> rawTargets, JsonElement rawReplacement) {
        this.action = action;
        this.jsonFilter = filter;
        this.targetInput = target;
        this.newInput = rep;
        this.rawTargets = rawTargets;
        this.rawReplacement = rawReplacement;
    }

    private static Ingredient stringsToIngredient(List<String> values) {
        List<Ingredient> ingredients = new ArrayList<>();
        for (String value : values) {
            Ingredient parsed = RecipeRuleParser.parseIngredientString(value);
            if (!parsed.isEmpty()) {
                ingredients.add(parsed);
            }
        }
        return RecipeRuleParser.mergeIngredients(ingredients);
    }

    private static boolean matchesAnyItemString(List<String> values, ItemStack stack) {
        if (stack.isEmpty()) return false;
        String itemId = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
        String itemPath = BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath();
        for (String value : values) {
            if (value.startsWith("#") || value.startsWith("tag:")) {
                String tagPath = value.startsWith("#") ? value.substring(1) : value.substring(4);
                ResourceLocation tagLoc = ResourceLocation.tryParse(tagPath);
                if (tagLoc != null && stack.is(TagKey.create(Registries.ITEM, tagLoc))) {
                    return true;
                }
            } else if (value.startsWith("/") && value.endsWith("/") && value.length() > 2) {
                try {
                    Pattern pattern = Pattern.compile(value.substring(1, value.length() - 1));
                    if (pattern.matcher(itemId).matches() || pattern.matcher(itemPath).matches()) {
                        return true;
                    }
                } catch (Exception ignored) {
                }
            } else if (value.startsWith("item:")) {
                if (value.substring(5).equals(itemId)) {
                    return true;
                }
            } else if (value.equals(itemId)) {
                return true;
            } else {
                ResourceLocation loc = ResourceLocation.tryParse(value);
                if (loc != null && !BuiltInRegistries.ITEM.containsKey(loc)) {
                    if (stack.is(TagKey.create(Registries.ITEM, loc))) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public boolean testJson(ResourceLocation id, JsonObject recipe) {
        return jsonFilter.test(id, recipe);
    }

    public Action getAction() {
        return action;
    }

    public Ingredient getTargetInput() {
        return targetInput;
    }

    public Ingredient getNewInput() {
        return newInput;
    }

    public List<String> getRawTargets() {
        return rawTargets;
    }

    public JsonElement getRawReplacement() {
        return rawReplacement;
    }

    public boolean targetsMatch(ItemStack stack) {
        return matchesAnyItemString(rawTargets, stack);
    }

    public boolean replacementMatches(ItemStack stack) {
        return matchesAnyItemString(replacementAsStrings(), stack);
    }

    public Ingredient replacementIngredient() {
        return stringsToIngredient(replacementAsStrings());
    }

    public Ingredient targetsIngredient() {
        return stringsToIngredient(rawTargets);
    }

    private List<String> replacementAsStrings() {
        if (rawReplacement == null) return List.of();
        List<String> values = new ArrayList<>();
        if (rawReplacement.isJsonArray()) {
            for (JsonElement e : rawReplacement.getAsJsonArray()) {
                if (e.isJsonPrimitive()) values.add(e.getAsString());
            }
        } else if (rawReplacement.isJsonPrimitive()) {
            values.add(rawReplacement.getAsString());
        }
        return values;
    }

    public enum Action {REMOVE, REPLACE_INPUT, REPLACE_OUTPUT, PREVENT_REPAIR, SET_REPAIR_MATERIAL}
}