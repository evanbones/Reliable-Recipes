package com.evandev.reliable_recipes.recipe;

import net.minecraft.resources.ResourceLocation;

import java.util.function.Predicate;

public record TagRule(Action action, Predicate<ResourceLocation> itemMatcher, Predicate<ResourceLocation> tagMatcher) {
    public enum Action {REMOVE_ALL_TAGS, REMOVE_FROM_TAG, CLEAR_TAG}
}