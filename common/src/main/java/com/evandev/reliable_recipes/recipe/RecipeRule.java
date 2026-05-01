package com.evandev.reliable_recipes.recipe;

import com.google.gson.JsonElement;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;

import java.util.Optional;
import java.util.function.Predicate;

public class RecipeRule {
    private final Action action;
    private final Predicate<RecipeHolder<?>> filter;
    private final Optional<Ingredient> targetInput;
    private final String replaceTargetStr;
    private final JsonElement replaceWithEl;
    private final Optional<Ingredient> newInput;

    // Removals
    public RecipeRule(Action action, Predicate<RecipeHolder<?>> filter) {
        this(action, filter, Optional.empty(), Optional.empty(), null, null);
    }

    // Prevent Repair
    public RecipeRule(Action action, Predicate<RecipeHolder<?>> filter, Optional<Ingredient> target) {
        this(action, filter, target, Optional.empty(), null, null);
    }

    // Set Repair Material
    public RecipeRule(Action action, Predicate<RecipeHolder<?>> filter, Optional<Ingredient> target, Optional<Ingredient> material) {
        this(action, filter, target, material, null, null);
    }

    // Replacements
    public RecipeRule(Action action, Predicate<RecipeHolder<?>> filter, String targetStr, JsonElement replacementEl) {
        this(action, filter, Optional.empty(), Optional.empty(), targetStr, replacementEl);
    }

    private RecipeRule(Action action, Predicate<RecipeHolder<?>> filter, Optional<Ingredient> targetInput, Optional<Ingredient> newInput, String targetStr, JsonElement replacementEl) {
        this.action = action;
        this.filter = filter;
        this.targetInput = targetInput;
        this.newInput = newInput;
        this.replaceTargetStr = targetStr;
        this.replaceWithEl = replacementEl;
    }

    public Optional<Ingredient> getNewInput() {
        return newInput;
    }

    public boolean test(RecipeHolder<?> holder) {
        return filter.test(holder);
    }

    public Action getAction() {
        return action;
    }

    public Optional<Ingredient> getTargetInput() {
        return targetInput;
    }

    public String getReplaceTargetStr() {
        return replaceTargetStr;
    }

    public JsonElement getReplaceWithEl() {
        return replaceWithEl;
    }

    public enum Action {REMOVE, REPLACE_INPUT, REPLACE_OUTPUT, PREVENT_REPAIR, SET_REPAIR_MATERIAL}
}