package com.evandev.reliable_recipes.recipe;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;

import java.util.Optional;
import java.util.function.Predicate;

public class RecipeRule {
    private final Action action;
    private final Predicate<RecipeHolder<?>> filter;
    private final Optional<Ingredient> targetInput;
    private final Optional<Ingredient> newInput;
    private final ItemStack newOutput;

    // Removals
    public RecipeRule(Action action, Predicate<RecipeHolder<?>> filter) {
        this(action, filter, Optional.empty(), Optional.empty(), ItemStack.EMPTY);
    }

    // Input Replacement
    public RecipeRule(Action action, Predicate<RecipeHolder<?>> filter, Optional<Ingredient> target, Optional<Ingredient> replacement) {
        this(action, filter, target, replacement, ItemStack.EMPTY);
    }

    // Output Replacement
    public RecipeRule(Action action, Predicate<RecipeHolder<?>> filter, ItemStack output) {
        this(action, filter, Optional.empty(), Optional.empty(), output);
    }

    private RecipeRule(Action action, Predicate<RecipeHolder<?>> filter, Optional<Ingredient> target, Optional<Ingredient> rep, ItemStack out) {
        this.action = action;
        this.filter = filter;
        this.targetInput = target;
        this.newInput = rep;
        this.newOutput = out;
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

    public Optional<Ingredient> getNewInput() {
        return newInput;
    }

    public ItemStack getNewOutput() {
        return newOutput;
    }

    public enum Action {REMOVE, REPLACE_INPUT, REPLACE_OUTPUT, PREVENT_REPAIR}
}