package com.evandev.reliable_recipes.recipe;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.Ingredient;

import java.util.List;
import java.util.function.BiPredicate;

public class RecipeRule {
    private final Action action;
    private final BiPredicate<ResourceLocation, JsonObject> jsonFilter;
    private final Ingredient targetInput;
    private final Ingredient newInput;

    // Raw strings for JSON mutation
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

    public enum Action {REMOVE, REPLACE_INPUT, REPLACE_OUTPUT, PREVENT_REPAIR, SET_REPAIR_MATERIAL}
}