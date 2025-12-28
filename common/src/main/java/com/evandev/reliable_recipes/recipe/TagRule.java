package com.evandev.reliable_recipes.recipe;

import net.minecraft.resources.ResourceLocation;
import java.util.List;

public record TagRule(Action action, List<ResourceLocation> items, List<ResourceLocation> tags) {
    public enum Action {REMOVE_ALL_TAGS, REMOVE_FROM_TAG, CLEAR_TAG}
}