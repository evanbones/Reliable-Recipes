package com.evandev.reliable_recipes.recipe;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import java.util.Map;

public class RecipeJsonMutator {

    /**
     * Mutates the JSON and returns true if any changes were made.
     */
    public static boolean mutateRecipe(JsonElement element, Map<String, String> replacements) {
        if (replacements == null || replacements.isEmpty()) return false;
        return mutateRecursively(element, replacements);
    }

    private static boolean mutateRecursively(JsonElement element, Map<String, String> replacements) {
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
                        obj.addProperty(key, replacements.get(strVal));
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
                        arr.set(i, new JsonPrimitive(replacements.get(strVal)));
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