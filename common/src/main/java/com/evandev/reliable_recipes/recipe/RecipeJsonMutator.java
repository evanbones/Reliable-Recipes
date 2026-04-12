package com.evandev.reliable_recipes.recipe;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.Map;

public class RecipeJsonMutator {

    /**
     * Mutates the JSON and returns true if any changes were made.
     */
    public static boolean mutateRecipe(JsonElement element, Map<String, JsonElement> replacements) {
        if (replacements == null || replacements.isEmpty()) return false;
        return mutateRecursively(element, replacements);
    }

    private static boolean mutateRecursively(JsonElement element, Map<String, JsonElement> replacements) {
        boolean changed = false;

        if (element.isJsonObject()) {
            JsonObject obj = element.getAsJsonObject();
            for (Map.Entry<String, JsonElement> entry : obj.entrySet()) {
                String key = entry.getKey();
                JsonElement val = entry.getValue();

                if (key.equals("type")) continue;

                if (val.isJsonPrimitive() && val.getAsJsonPrimitive().isString()) {
                    String strVal = val.getAsString();
                    if (replacements.containsKey(strVal)) {
                        obj.add(key, replacements.get(strVal).deepCopy());
                        changed = true;
                    }
                } else {
                    changed |= mutateRecursively(val, replacements);
                }
            }
        } else if (element.isJsonArray()) {
            JsonArray arr = element.getAsJsonArray();
            for (int i = 0; i < arr.size(); i++) {
                JsonElement val = arr.get(i);
                if (val.isJsonPrimitive() && val.getAsJsonPrimitive().isString()) {
                    String strVal = val.getAsString();
                    if (replacements.containsKey(strVal)) {
                        arr.set(i, replacements.get(strVal).deepCopy());
                        changed = true;
                    }
                } else {
                    changed |= mutateRecursively(val, replacements);
                }
            }
        }

        return changed;
    }
}