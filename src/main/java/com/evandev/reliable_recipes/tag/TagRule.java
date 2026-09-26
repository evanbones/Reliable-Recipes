package com.evandev.reliable_recipes.tag;

import net.minecraft.resources.Identifier;

import java.util.function.Predicate;

public record TagRule(Action action, Predicate<Identifier> itemMatcher, Predicate<Identifier> tagMatcher) {
    public enum Action {REMOVE_ALL_TAGS, REMOVE_FROM_TAG, CLEAR_TAG}
}
