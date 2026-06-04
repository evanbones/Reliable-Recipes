package com.evandev.reliable_recipes.recipe;

import com.evandev.reliable_recipes.Constants;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.Map;

public class RecipeJsonMutator {

    /**
     * Mutates the JSON and returns true if any changes were made.
     */
    public static boolean mutateRecipe(JsonElement element, Map<String, JsonElement> inputReplacements, Map<String, JsonElement> outputReplacements) {
        if ((inputReplacements == null || inputReplacements.isEmpty()) &&
                (outputReplacements == null || outputReplacements.isEmpty())) {
            return false;
        }
        return mutateRecursively(element, inputReplacements, outputReplacements, false);
    }

    private static boolean mutateRecursively(JsonElement element, Map<String, JsonElement> inputReps, Map<String, JsonElement> outputReps, boolean isOutputContext) {
        boolean changed = false;

        if (element.isJsonObject()) {
            JsonObject obj = element.getAsJsonObject();
            for (Map.Entry<String, JsonElement> entry : obj.entrySet()) {
                String key = entry.getKey();
                JsonElement val = entry.getValue();

                if (key.equals("type")) continue;

                boolean nextContext = isOutputContext;
                if (key.equals("result") || key.equals("results")) {
                    nextContext = true;
                } else if (key.equals("ingredients") || key.equals("ingredient") || key.equals("key") || key.equals("base") || key.equals("addition")) {
                    nextContext = false;
                }

                Map<String, JsonElement> activeReps = nextContext ? outputReps : inputReps;

                if (val.isJsonPrimitive() && val.getAsJsonPrimitive().isString()) {
                    String strVal = val.getAsString();
                    if (activeReps.containsKey(strVal)) {
                        JsonElement replacement = activeReps.get(strVal).deepCopy();

                        if (key.equals("id") || key.equals("item") || key.equals("tag")) {
                            if (replacement.isJsonArray() && !replacement.getAsJsonArray().isEmpty()) {
                                replacement = replacement.getAsJsonArray().get(0);
                            }
                            if (!replacement.isJsonPrimitive() || !replacement.getAsJsonPrimitive().isString()) {
                                continue;
                            }
                        }

                        if (nextContext && replacement.isJsonPrimitive() && replacement.getAsJsonPrimitive().isString()) {
                            if (replacement.getAsString().startsWith("#")) {
                                Constants.LOG.debug("Refusing to apply tag replacement '{}' in output context for key '{}'", replacement.getAsString(), key);
                                continue;
                            }
                        }

                        obj.add(key, replacement);
                        changed = true;
                    }
                } else {
                    changed |= mutateRecursively(val, inputReps, outputReps, nextContext);
                }
            }
        } else if (element.isJsonArray()) {
            JsonArray arr = element.getAsJsonArray();
            Map<String, JsonElement> activeReps = isOutputContext ? outputReps : inputReps;

            for (int i = 0; i < arr.size(); i++) {
                JsonElement val = arr.get(i);
                if (val.isJsonPrimitive() && val.getAsJsonPrimitive().isString()) {
                    String strVal = val.getAsString();
                    if (activeReps.containsKey(strVal)) {
                        JsonElement replacement = activeReps.get(strVal).deepCopy();

                        if (isOutputContext && replacement.isJsonPrimitive() && replacement.getAsJsonPrimitive().isString()) {
                            if (replacement.getAsString().startsWith("#")) {
                                Constants.LOG.debug("Refusing to apply tag replacement '{}' in output array", replacement.getAsString());
                                continue;
                            }
                        }

                        arr.set(i, replacement);
                        changed = true;
                    }
                } else {
                    changed |= mutateRecursively(val, inputReps, outputReps, isOutputContext);
                }
            }
        }

        return changed;
    }
}