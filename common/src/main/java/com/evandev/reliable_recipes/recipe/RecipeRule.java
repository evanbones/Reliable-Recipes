package com.evandev.reliable_recipes.recipe;

import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;

import java.util.Optional;
import java.util.function.Predicate;

public class RecipeRule {
    private final Action action;
    private final Predicate<RecipeHolder<?>> filter;
    private final Optional<Ingredient> targetInput;
    private final String replaceTargetStr;
    private final String replaceWithStr;

    // Removals
    public RecipeRule(Action action, Predicate<RecipeHolder<?>> filter) {
        this(action, filter, Optional.empty(), null, null);
    }

    // Prevent Repair
    public RecipeRule(Action action, Predicate<RecipeHolder<?>> filter, Optional<Ingredient> target) {
        this(action, filter, target, null, null);
    }

    // Replacements
    public RecipeRule(Action action, Predicate<RecipeHolder<?>> filter, String targetStr, String replacementStr) {
        this(action, filter, Optional.empty(), targetStr, replacementStr);
    }

    private RecipeRule(Action action, Predicate<RecipeHolder<?>> filter, Optional<Ingredient> targetInput, String targetStr, String replacementStr) {
        this.action = action;
        this.filter = filter;
        this.targetInput = targetInput;
        this.replaceTargetStr = targetStr;
        this.replaceWithStr = replacementStr;
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

    public String getReplaceWithStr() {
        return replaceWithStr;
    }

    public enum Action {REMOVE, REPLACE_INPUT, REPLACE_OUTPUT, PREVENT_REPAIR}
}