package com.evandev.reliable_recipes.recipe;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import java.util.*;

public final class GlobalReplacements extends AbstractMap<String, String> {
    private final Map<String, String> replacements;
    private final List<Map<String, JsonElement>> steps = new ArrayList<>();
    private final Map<String, Integer> positions = new HashMap<>();
    private final boolean hasWildcard;

    public GlobalReplacements(Map<String, String> replacements) {
        this.replacements = Collections.unmodifiableMap(new LinkedHashMap<>(replacements));
        for (Map.Entry<String, String> entry : this.replacements.entrySet()) {
            positions.put(entry.getKey(), steps.size());
            steps.add(Map.of(entry.getKey(), new JsonPrimitive(entry.getValue())));
        }
        this.hasWildcard = this.replacements.containsKey("*") || this.replacements.containsKey("");
    }

    private static void collectStrings(JsonElement element, Set<String> out) {
        if (element.isJsonObject()) {
            for (Map.Entry<String, JsonElement> entry : element.getAsJsonObject().entrySet())
                collectStrings(entry.getValue(), out);
        } else if (element.isJsonArray()) {
            for (JsonElement child : element.getAsJsonArray()) collectStrings(child, out);
        } else if (element.isJsonPrimitive() && element.getAsJsonPrimitive().isString()) {
            out.add(element.getAsString());
        }
    }

    @Override
    public Set<Entry<String, String>> entrySet() {
        return replacements.entrySet();
    }

    @Override
    public String get(Object key) {
        return replacements.get(key);
    }

    @Override
    public boolean containsKey(Object key) {
        return replacements.containsKey(key);
    }

    @Override
    public int size() {
        return replacements.size();
    }

    public void applyTo(JsonObject recipe) {
        if (hasWildcard) {
            for (Map<String, JsonElement> step : steps) {
                RecipeJsonMutator.mutateRecipe(recipe, step, step, RecipeJsonMutator.NATIVE_FORMAT);
            }
            return;
        }

        Set<String> strings = null;
        int last = -1;
        while (true) {
            if (strings == null) {
                strings = new HashSet<>();
                collectStrings(recipe, strings);
            }
            int next = nextStep(strings, last);
            if (next < 0) return;
            Map<String, JsonElement> step = steps.get(next);
            if (RecipeJsonMutator.mutateRecipe(recipe, step, step, RecipeJsonMutator.NATIVE_FORMAT)) strings = null;
            last = next;
        }
    }

    private int nextStep(Set<String> strings, int after) {
        int best = Integer.MAX_VALUE;
        for (String value : strings) {
            best = earliest(best, value, after);
            best = earliest(best, "#" + value, after);
            if (value.startsWith("#")) best = earliest(best, value.substring(1), after);
        }
        return best == Integer.MAX_VALUE ? -1 : best;
    }

    private int earliest(int best, String key, int after) {
        Integer position = positions.get(key);
        return position != null && position > after && position < best ? position : best;
    }
}
