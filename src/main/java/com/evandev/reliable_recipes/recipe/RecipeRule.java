package com.evandev.reliable_recipes.recipe;

import com.google.gson.JsonElement;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.regex.Pattern;

public class RecipeRule {
    private final Action action;
    private final Predicate<RecipeHolder<?>> filter;
    private final Optional<Ingredient> targetInput;
    private final List<String> replaceTargetStrs;
    private final JsonElement replaceWithEl;
    private final Optional<Ingredient> newInput;

    // Removals
    public RecipeRule(Action action, Predicate<RecipeHolder<?>> filter) {
        this(action, filter, Optional.empty(), Optional.empty(), List.of(), null);
    }

    // Prevent Repair
    public RecipeRule(Action action, Predicate<RecipeHolder<?>> filter, Optional<Ingredient> target) {
        this(action, filter, target, Optional.empty(), List.of(), null);
    }

    // Set Repair Material
    public RecipeRule(Action action, Predicate<RecipeHolder<?>> filter, Optional<Ingredient> target, Optional<Ingredient> material) {
        this(action, filter, target, material, List.of(), null);
    }

    // Replacements (single target)
    public RecipeRule(Action action, Predicate<RecipeHolder<?>> filter, String targetStr, JsonElement replacementEl) {
        this(action, filter, Optional.empty(), Optional.empty(), targetStr != null && !targetStr.isEmpty() ? List.of(targetStr) : List.of(), replacementEl);
    }

    // Replacements (multiple targets)
    public RecipeRule(Action action, Predicate<RecipeHolder<?>> filter, List<String> targetStrs, JsonElement replacementEl) {
        this(action, filter, Optional.empty(), Optional.empty(), targetStrs, replacementEl);
    }

    private RecipeRule(Action action, Predicate<RecipeHolder<?>> filter, Optional<Ingredient> targetInput, Optional<Ingredient> newInput, List<String> targetStrs, JsonElement replacementEl) {
        this.action = action;
        this.filter = filter;
        this.targetInput = targetInput;
        this.newInput = newInput;
        this.replaceTargetStrs = targetStrs;
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
        return replaceTargetStrs.isEmpty() ? null : replaceTargetStrs.get(0);
    }

    public List<String> getReplaceTargetStrs() {
        return replaceTargetStrs;
    }

    public JsonElement getReplaceWithEl() {
        return replaceWithEl;
    }

    public boolean targetsMatch(ItemStack stack) {
        return matchesAnyItemString(replaceTargetStrs, stack);
    }

    public boolean replacementMatches(ItemStack stack) {
        return matchesAnyItemString(replacementAsStrings(), stack);
    }

    private List<String> replacementAsStrings() {
        if (replaceWithEl == null) return List.of();
        List<String> values = new ArrayList<>();
        if (replaceWithEl.isJsonArray()) {
            for (JsonElement e : replaceWithEl.getAsJsonArray()) {
                if (e.isJsonPrimitive()) values.add(e.getAsString());
            }
        } else if (replaceWithEl.isJsonPrimitive()) {
            values.add(replaceWithEl.getAsString());
        }
        return values;
    }

    private static boolean matchesAnyItemString(List<String> values, ItemStack stack) {
        if (stack.isEmpty()) return false;
        String itemId = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
        String itemPath = BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath();
        for (String value : values) {
            if (value.startsWith("#") || value.startsWith("tag:")) {
                String tagPath = value.startsWith("#") ? value.substring(1) : value.substring(4);
                Identifier tagLoc = Identifier.tryParse(tagPath);
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
                Identifier loc = Identifier.tryParse(value);
                if (loc != null && !BuiltInRegistries.ITEM.containsKey(loc)) {
                    if (stack.is(TagKey.create(Registries.ITEM, loc))) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public enum Action {REMOVE, REPLACE_INPUT, REPLACE_OUTPUT, PREVENT_REPAIR, SET_REPAIR_MATERIAL}
}