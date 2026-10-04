package com.evandev.reliable_recipes.recipe;

import com.evandev.reliable_recipes.Constants;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class RecipeJsonMutator {

    public enum IngredientFormat {
        OBJECT(Set.of("result", "results", "output"), Set.of(), false),
        STRING(Set.of("result", "results", "output", "outputs"),
                Set.of("ingredients", "ingredient", "key", "base", "addition", "input", "inputs", "reagent", "material"),
                true);

        private final Set<String> outputKeys;
        private final Set<String> inputKeys;
        private final boolean refusesOutputTags;

        IngredientFormat(Set<String> outputKeys, Set<String> inputKeys, boolean refusesOutputTags) {
            this.outputKeys = outputKeys;
            this.inputKeys = inputKeys;
            this.refusesOutputTags = refusesOutputTags;
        }

        private boolean contextFor(String key, boolean current) {
            if (outputKeys.contains(key)) return true;
            if (inputKeys.contains(key)) return false;
            return current;
        }

        private boolean refuses(JsonElement rep, boolean output) {
            return refusesOutputTags && output && isString(rep) && rep.getAsString().startsWith("#");
        }
    }

    //? if <1.21.2 {
    /*public static final IngredientFormat NATIVE_FORMAT = IngredientFormat.OBJECT;
    *///?} else {
    public static final IngredientFormat NATIVE_FORMAT = IngredientFormat.STRING;
    //?}

    private static final Set<String> ITEM_VALUE_KEYS = Set.of(
            "item", "items", "id", "tag", "ingredient", "ingredients", "base", "addition",
            "input", "inputs", "result", "results", "output", "outputs"
    );

    private static final List<String> OBJECT_ITEM_KEYS = List.of("item", "id", "result", "output");

    private record Expansion(List<JsonElement> elements, boolean alternatives) {
        static Expansion single(JsonElement element) {
            return new Expansion(List.of(element), false);
        }
    }

    private record ObjectMatch(String key, JsonElement replacement) {
    }

    public static void applyRule(JsonObject recipe, RecipeRule rule, IngredientFormat format) {
        applyReplacement(recipe, rule.getAction(), rule.getRawTargets(), rule.getRawReplacement(), format);
    }

    public static void applyReplacement(JsonObject recipe, RecipeRule.Action action, List<String> targets, JsonElement replacement, IngredientFormat format) {
        if (replacement == null) return;

        Map<String, JsonElement> replacements = new HashMap<>();
        if (action == RecipeRule.Action.REPLACE_OUTPUT && targets.isEmpty()) {
            replacements.put("", replacement);
        } else {
            for (String target : targets) {
                if (target != null && !target.isEmpty()) {
                    replacements.put(target, replacement);
                }
            }
        }

        if (action == RecipeRule.Action.REPLACE_INPUT) {
            mutateRecipe(recipe, replacements, Map.of(), format);
        } else {
            mutateRecipe(recipe, Map.of(), replacements, format);
        }
    }

    public static boolean mutateRecipe(JsonElement element, Map<String, JsonElement> inputReplacements, Map<String, JsonElement> outputReplacements, IngredientFormat format) {
        Map<String, JsonElement> in = inputReplacements == null ? Map.of() : inputReplacements;
        Map<String, JsonElement> out = outputReplacements == null ? Map.of() : outputReplacements;
        if (in.isEmpty() && out.isEmpty()) return false;
        return walk(element, in, out, format, false, true, false);
    }

    private static boolean walk(JsonElement element, Map<String, JsonElement> in, Map<String, JsonElement> out, IngredientFormat format, boolean output, boolean isRoot, boolean ingredientList) {
        if (element.isJsonObject()) return walkObject(element.getAsJsonObject(), in, out, format, output, isRoot);
        if (element.isJsonArray()) return walkArray(element.getAsJsonArray(), in, out, format, output, ingredientList);
        return false;
    }

    private static boolean walkObject(JsonObject obj, Map<String, JsonElement> in, Map<String, JsonElement> out, IngredientFormat format, boolean output, boolean isRoot) {
        boolean changed = false;
        boolean typedNested = !isRoot && obj.has("type");

        for (Map.Entry<String, JsonElement> entry : new ArrayList<>(obj.entrySet())) {
            String key = entry.getKey();
            if (key.equals("type")) continue;

            JsonElement value = entry.getValue();
            boolean childOutput = format.contextFor(key, output);
            Map<String, JsonElement> reps = childOutput ? out : in;

            if (value.isJsonObject()) {
                Expansion expansion = replaceObject(value.getAsJsonObject(), reps, childOutput, format);
                if (expansion != null) {
                    obj.add(key, expansion.alternatives() ? toArray(expansion.elements()) : expansion.elements().get(0));
                    changed = true;
                    continue;
                }
            }

            if (value.isJsonPrimitive() && value.getAsJsonPrimitive().isString()) {
                if (typedNested && !ITEM_VALUE_KEYS.contains(key)) continue;
                changed |= replaceStringField(obj, key, value.getAsString(), reps, childOutput, format);
            } else {
                changed |= walk(value, in, out, format, childOutput, false, isIngredientListKey(key));
            }
        }
        return changed;
    }

    private static boolean walkArray(JsonArray arr, Map<String, JsonElement> in, Map<String, JsonElement> out, IngredientFormat format, boolean output, boolean ingredientList) {
        Map<String, JsonElement> reps = output ? out : in;
        JsonArray rebuilt = new JsonArray();
        boolean changed = false;

        for (JsonElement child : arr) {
            if (child.isJsonObject()) {
                Expansion expansion = replaceObject(child.getAsJsonObject(), reps, output, format);
                if (expansion != null) {
                    changed = true;
                    if (!expansion.alternatives()) {
                        rebuilt.add(expansion.elements().get(0));
                    } else if (ingredientList) {
                        rebuilt.add(toArray(expansion.elements()));
                    } else {
                        expansion.elements().forEach(rebuilt::add);
                    }
                    continue;
                }
            } else if (child.isJsonPrimitive() && child.getAsJsonPrimitive().isString()) {
                JsonElement rep = lookup(reps, child.getAsString(), output, format);
                if (rep == null || format.refuses(rep, output)) {
                    rebuilt.add(child);
                    continue;
                }
                changed = true;
                if (format == IngredientFormat.OBJECT && rep.isJsonArray()) {
                    if (ingredientList) {
                        rebuilt.add(stringAlternatives(rep.getAsJsonArray(), "item"));
                    } else {
                        rep.getAsJsonArray().forEach(r -> rebuilt.add(r.deepCopy()));
                    }
                } else {
                    rebuilt.add(rep.deepCopy());
                }
                continue;
            }

            rebuilt.add(child);
            changed |= walk(child, in, out, format, output, false, false);
        }

        if (changed) {
            while (!arr.isEmpty()) arr.remove(0);
            arr.addAll(rebuilt);
        }
        return changed;
    }

    private static Expansion replaceObject(JsonObject obj, Map<String, JsonElement> reps, boolean output, IngredientFormat format) {
        JsonElement wildcard = output ? wildcard(reps) : null;
        if (wildcard != null && wildcard.isJsonObject()) return Expansion.single(wildcard.deepCopy());

        ObjectMatch match = matchObject(obj, reps, wildcard);
        if (match == null) return null;

        JsonElement rep = match.replacement();
        if (rep.isJsonObject()) return Expansion.single(rep.deepCopy());
        if (format != IngredientFormat.OBJECT) return null;

        if (rep.isJsonArray()) {
            List<JsonElement> copies = new ArrayList<>();
            for (JsonElement r : rep.getAsJsonArray()) {
                copies.add(r.isJsonPrimitive() ? withReplaced(obj, match.key(), r.getAsString()) : r.deepCopy());
            }
            return new Expansion(copies, true);
        }

        return Expansion.single(withReplaced(obj, match.key(), rep.getAsString()));
    }

    private static ObjectMatch matchObject(JsonObject obj, Map<String, JsonElement> reps, JsonElement wildcard) {
        if (wildcard != null) {
            for (String key : OBJECT_ITEM_KEYS) {
                if (isString(obj.get(key))) return new ObjectMatch(key, wildcard);
            }
        }
        for (String key : OBJECT_ITEM_KEYS) {
            JsonElement value = obj.get(key);
            if (isString(value) && reps.containsKey(value.getAsString())) {
                return new ObjectMatch(key, reps.get(value.getAsString()));
            }
        }
        JsonElement tag = obj.get("tag");
        if (isString(tag)) {
            String tagValue = tag.getAsString();
            if (reps.containsKey("#" + tagValue)) return new ObjectMatch("tag", reps.get("#" + tagValue));
            if (reps.containsKey(tagValue)) return new ObjectMatch("tag", reps.get(tagValue));
        }
        return null;
    }

    private static boolean replaceStringField(JsonObject obj, String key, String value, Map<String, JsonElement> reps, boolean output, IngredientFormat format) {
        JsonElement rep = lookup(reps, value, output, format);
        if (rep == null) return false;

        if (format == IngredientFormat.OBJECT) {
            if (key.equals("tag") && rep.isJsonPrimitive()) {
                String repStr = rep.getAsString();
                obj.remove("tag");
                if (repStr.startsWith("#")) obj.addProperty("tag", repStr.substring(1));
                else obj.addProperty("item", repStr);
                return true;
            }
            if (rep.isJsonArray()) {
                obj.add(key, stringAlternatives(rep.getAsJsonArray(), key.equals("tag") ? "tag" : "item"));
                return true;
            }
        } else if (key.equals("id") || key.equals("item") || key.equals("tag")) {
            if (rep.isJsonArray() && !rep.getAsJsonArray().isEmpty()) rep = rep.getAsJsonArray().get(0);
            if (!isString(rep)) return false;
        }

        if (format.refuses(rep, output)) {
            Constants.LOG.debug("Refusing to apply tag replacement '{}' in output context for key '{}'", rep.getAsString(), key);
            return false;
        }

        obj.add(key, rep.deepCopy());
        return true;
    }

    private static JsonObject withReplaced(JsonObject obj, String key, String replacement) {
        JsonObject copy = obj.deepCopy();
        copy.remove(key);
        if (replacement.startsWith("#")) copy.addProperty("tag", replacement.substring(1));
        else copy.addProperty(key.equals("tag") ? "item" : key, replacement);
        return copy;
    }

    private static JsonArray stringAlternatives(JsonArray replacements, String itemKey) {
        JsonArray alternatives = new JsonArray();
        for (JsonElement rep : replacements) {
            if (rep.isJsonPrimitive()) {
                String repStr = rep.getAsString();
                JsonObject ingredient = new JsonObject();
                if (repStr.startsWith("#")) ingredient.addProperty("tag", repStr.substring(1));
                else ingredient.addProperty(itemKey, repStr);
                alternatives.add(ingredient);
            } else {
                alternatives.add(rep.deepCopy());
            }
        }
        return alternatives;
    }

    private static JsonElement lookup(Map<String, JsonElement> reps, String value, boolean output, IngredientFormat format) {
        JsonElement rep = reps.get(value);
        if (rep == null && format == IngredientFormat.STRING && value.startsWith("#")) rep = reps.get(value.substring(1));
        if (rep == null && output) rep = wildcard(reps);
        return rep;
    }

    private static JsonElement wildcard(Map<String, JsonElement> reps) {
        JsonElement rep = reps.get("*");
        return rep != null ? rep : reps.get("");
    }

    private static boolean isIngredientListKey(String key) {
        String lower = key.toLowerCase(Locale.ROOT);
        return lower.endsWith("ingredients") || lower.endsWith("inputs");
    }

    private static boolean isString(JsonElement element) {
        return element != null && element.isJsonPrimitive() && element.getAsJsonPrimitive().isString();
    }

    private static JsonArray toArray(List<JsonElement> elements) {
        JsonArray array = new JsonArray();
        elements.forEach(array::add);
        return array;
    }
}
