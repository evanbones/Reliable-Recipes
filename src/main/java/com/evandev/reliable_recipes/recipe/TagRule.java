package com.evandev.reliable_recipes.recipe;

import net.minecraft.resources.Identifier;

import java.util.List;

public record TagRule(Action action, List<Identifier> items, List<Identifier> tags) {
    public enum Action {REMOVE_ALL_TAGS, REMOVE_FROM_TAG, CLEAR_TAG}
}