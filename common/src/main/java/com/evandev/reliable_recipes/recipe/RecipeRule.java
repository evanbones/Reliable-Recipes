package com.evandev.reliable_recipes.recipe;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;

import java.util.function.Predicate;

public class RecipeRule {
    public enum Action { REMOVE, REPLACE_INPUT, REPLACE_OUTPUT }

    private final Action action;
    private final Predicate<RecipeHolder<?>> filter;

    private final Ingredient targetInput;
    private final Ingredient newInput;
    private final ItemStack newOutput;

    // Removals
    public RecipeRule(Action action, Predicate<RecipeHolder<?>> filter) {
        this(action, filter, Ingredient.EMPTY, Ingredient.EMPTY, ItemStack.EMPTY);
    }

    // Input Replacement
    public RecipeRule(Action action, Predicate<RecipeHolder<?>> filter, Ingredient target, Ingredient replacement) {
        this(action, filter, target, replacement, ItemStack.EMPTY);
    }

    // Output Replacement
    public RecipeRule(Action action, Predicate<RecipeHolder<?>> filter, ItemStack output) {
        this(action, filter, Ingredient.EMPTY, Ingredient.EMPTY, output);
    }

    private RecipeRule(Action action, Predicate<RecipeHolder<?>> filter, Ingredient target, Ingredient rep, ItemStack out) {
        this.action = action;
        this.filter = filter;
        this.targetInput = target;
        this.newInput = rep;
        this.newOutput = out;
    }

    public boolean test(RecipeHolder<?> holder) {
        return filter.test(holder);
    }

    public Action getAction() { return action; }
    public Ingredient getTargetInput() { return targetInput; }
    public Ingredient getNewInput() { return newInput; }
    public ItemStack getNewOutput() { return newOutput; }
}