package com.evandev.reliable_recipes.recipe;

import com.evandev.reliable_recipes.Constants;
import com.evandev.reliable_recipes.api.ReliableRecipesAPI;
import com.evandev.reliable_recipes.config.RecipeConfigIO;
import com.evandev.reliable_recipes.mixin.accessor.RecipeManagerAccessor;
import com.google.common.collect.ImmutableMap;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;

import java.util.*;

public class RecipeModifier {
    private static final Map<ResourceLocation, Recipe<?>> DELETED_RECIPES_CACHE = new HashMap<>();
    private static List<RecipeRule> cachedRules = null;

    /**
     * Called during datapack load before recipes are parsed.
     */
    public static void modifyRecipesJson(Map<ResourceLocation, JsonElement> map) {
        cachedRules = new ArrayList<>(RecipeConfigIO.loadRules());
        Map<ResourceLocation, JsonElement> newMap = new HashMap<>();

        for (Map.Entry<ResourceLocation, JsonElement> entry : map.entrySet()) {
            ResourceLocation id = entry.getKey();
            JsonElement element = entry.getValue();

            if (!element.isJsonObject()) {
                newMap.put(id, element);
                continue;
            }

            JsonObject recipeJson = element.getAsJsonObject().deepCopy();
            JsonObject modified = modifySingleRecipeJson(id, recipeJson);
            if (modified != null) {
                newMap.put(id, modified);
            }
        }

        map.clear();
        map.putAll(newMap);
    }

    public static JsonObject modifySingleRecipeJson(ResourceLocation id, JsonObject recipeJson) {
        List<RecipeRule> rules = cachedRules != null ? cachedRules : RecipeConfigIO.loadRules();
        Map<String, String> globalReplacements = ReliableRecipesAPI.getReplacements();
        boolean shouldRemove = false;

        // Evaluate user rules from config
        for (RecipeRule rule : rules) {
            if (rule.testJson(id, recipeJson)) {
                if (rule.getAction() == RecipeRule.Action.REMOVE) {
                    shouldRemove = true;
                    break;
                } else if (rule.getAction() == RecipeRule.Action.REPLACE_INPUT || rule.getAction() == RecipeRule.Action.REPLACE_OUTPUT) {
                    boolean startContext = (rule.getAction() == RecipeRule.Action.REPLACE_INPUT);
                    mutateJsonRecursively(recipeJson, rule.getRawTargets(), rule.getRawReplacement(), rule.getAction(), startContext);
                }
            }
        }

        if (shouldRemove) {
            return null;
        }

        // Global API Replacements
        if (!globalReplacements.isEmpty()) {
            for (Map.Entry<String, String> rep : globalReplacements.entrySet()) {
                mutateJsonRecursively(recipeJson, List.of(rep.getKey()), new JsonPrimitive(rep.getValue()), null, true);
            }
        }

        // Hidden items output check
        if (ReliableRecipesAPI.hasItemHidingCapabilities() && shouldHideRecipeJson(recipeJson)) {
            return null;
        }

        return recipeJson;
    }

    private static void mutateJsonRecursively(JsonElement parent, List<String> targets, JsonElement rawReplacement, RecipeRule.Action action, boolean isTargetContext) {
        if (parent.isJsonObject()) {
            JsonObject obj = parent.getAsJsonObject();
            List<Map.Entry<String, JsonElement>> entries = new ArrayList<>(obj.entrySet());

            for (Map.Entry<String, JsonElement> entry : entries) {
                String key = entry.getKey();
                JsonElement child = entry.getValue();

                boolean nextContext = getNextContext(action, isTargetContext, key);

                if (child.isJsonObject()) {
                    JsonObject childObj = child.getAsJsonObject();
                    String matchedKey = getMatchingKey(childObj, targets);

                    if (matchedKey != null && nextContext) {
                        if (rawReplacement.isJsonArray()) {
                            JsonArray newArr = new JsonArray();
                            for (JsonElement rep : rawReplacement.getAsJsonArray()) {
                                if (rep.isJsonPrimitive()) {
                                    JsonObject newObj = childObj.deepCopy();
                                    String repStr = rep.getAsString();
                                    newObj.remove(matchedKey);
                                    if (repStr.startsWith("#")) newObj.addProperty("tag", repStr.substring(1));
                                    else newObj.addProperty(matchedKey.equals("tag") ? "item" : matchedKey, repStr);
                                    newArr.add(newObj);
                                } else {
                                    newArr.add(rep.deepCopy());
                                }
                            }
                            obj.add(key, newArr);
                        } else if (rawReplacement.isJsonPrimitive()) {
                            String repStr = rawReplacement.getAsString();
                            childObj.remove(matchedKey);
                            if (repStr.startsWith("#")) childObj.addProperty("tag", repStr.substring(1));
                            else childObj.addProperty(matchedKey.equals("tag") ? "item" : matchedKey, repStr);
                        } else {
                            obj.add(key, rawReplacement.deepCopy());
                        }
                    } else {
                        mutateJsonRecursively(child, targets, rawReplacement, action, nextContext);
                    }
                } else if (child.isJsonPrimitive() && child.getAsJsonPrimitive().isString()) {
                    if (targets.contains(child.getAsString()) && nextContext) {
                        if (key.equals("tag") && rawReplacement.isJsonPrimitive()) {
                            String repStr = rawReplacement.getAsString();
                            obj.remove("tag");
                            if (repStr.startsWith("#")) obj.addProperty("tag", repStr.substring(1));
                            else obj.addProperty("item", repStr);
                        } else {
                            obj.add(key, rawReplacement.deepCopy());
                        }
                    }
                } else {
                    mutateJsonRecursively(child, targets, rawReplacement, action, nextContext);
                }
            }
        } else if (parent.isJsonArray()) {
            JsonArray oldArray = parent.getAsJsonArray();
            JsonArray newArray = new JsonArray();
            boolean changed = false;

            for (int i = 0; i < oldArray.size(); i++) {
                JsonElement child = oldArray.get(i);

                if (child.isJsonObject()) {
                    JsonObject childObj = child.getAsJsonObject();
                    String matchedKey = getMatchingKey(childObj, targets);

                    if (matchedKey != null && isTargetContext) {
                        changed = true;
                        if (rawReplacement.isJsonArray()) {
                            for (JsonElement rep : rawReplacement.getAsJsonArray()) {
                                if (rep.isJsonPrimitive()) {
                                    JsonObject newObj = childObj.deepCopy();
                                    String repStr = rep.getAsString();
                                    newObj.remove(matchedKey);
                                    if (repStr.startsWith("#")) newObj.addProperty("tag", repStr.substring(1));
                                    else newObj.addProperty(matchedKey.equals("tag") ? "item" : matchedKey, repStr);
                                    newArray.add(newObj);
                                } else {
                                    newArray.add(rep.deepCopy());
                                }
                            }
                        } else if (rawReplacement.isJsonPrimitive()) {
                            JsonObject newObj = childObj.deepCopy();
                            String repStr = rawReplacement.getAsString();
                            newObj.remove(matchedKey);
                            if (repStr.startsWith("#")) newObj.addProperty("tag", repStr.substring(1));
                            else newObj.addProperty(matchedKey.equals("tag") ? "item" : matchedKey, repStr);
                            newArray.add(newObj);
                        } else {
                            newArray.add(rawReplacement.deepCopy());
                        }
                    } else {
                        newArray.add(child);
                        mutateJsonRecursively(child, targets, rawReplacement, action, isTargetContext);
                    }
                } else if (child.isJsonPrimitive() && child.getAsJsonPrimitive().isString()) {
                    if (targets.contains(child.getAsString()) && isTargetContext) {
                        changed = true;
                        if (rawReplacement.isJsonArray()) {
                            for (JsonElement rep : rawReplacement.getAsJsonArray()) newArray.add(rep.deepCopy());
                        } else {
                            newArray.add(rawReplacement.deepCopy());
                        }
                    } else {
                        newArray.add(child);
                    }
                } else {
                    newArray.add(child);
                    mutateJsonRecursively(child, targets, rawReplacement, action, isTargetContext);
                }
            }

            if (changed) {
                while (!oldArray.isEmpty()) oldArray.remove(0);
                for (int i = 0; i < newArray.size(); i++) oldArray.add(newArray.get(i));
            }
        }
    }

    private static boolean getNextContext(RecipeRule.Action action, boolean isTargetContext, String key) {
        boolean isOutputKey = key.equals("result") || key.equals("results") || key.equals("output");

        if (action == RecipeRule.Action.REPLACE_OUTPUT && isOutputKey) {
            return true;
        } else if (action == RecipeRule.Action.REPLACE_INPUT && isOutputKey) {
            return false;
        }

        return isTargetContext;
    }

    private static String getMatchingKey(JsonObject obj, List<String> targets) {
        if (obj.has("item") && obj.get("item").isJsonPrimitive() && targets.contains(obj.get("item").getAsString()))
            return "item";
        if (obj.has("id") && obj.get("id").isJsonPrimitive() && targets.contains(obj.get("id").getAsString()))
            return "id";
        if (obj.has("tag") && obj.get("tag").isJsonPrimitive()) {
            String tagVal = obj.get("tag").getAsString();
            if (targets.contains("#" + tagVal) || targets.contains(tagVal))
                return "tag";
        }
        return null;
    }

    private static boolean shouldHideRecipeJson(JsonObject jsonObject) {
        try {
            JsonElement resultElement = jsonObject.has("result") ? jsonObject.get("result") :
                    (jsonObject.has("results") ? jsonObject.get("results") :
                            (jsonObject.has("output") ? jsonObject.get("output") : null));
            if (resultElement == null) return false;

            if (resultElement.isJsonObject()) {
                return isItemHidden(getResultItemId(resultElement.getAsJsonObject()));
            } else if (resultElement.isJsonPrimitive() && resultElement.getAsJsonPrimitive().isString()) {
                return isItemHidden(resultElement.getAsString());
            } else if (resultElement.isJsonArray()) {
                for (JsonElement element : resultElement.getAsJsonArray()) {
                    if (element.isJsonObject() && isItemHidden(getResultItemId(element.getAsJsonObject()))) return true;
                    if (element.isJsonPrimitive() && isItemHidden(element.getAsString())) return true;
                }
            }
        } catch (Exception ignored) {
        }
        return false;
    }

    private static String getResultItemId(JsonObject resultObject) {
        if (resultObject.has("item")) return GsonHelper.getAsString(resultObject, "item");
        if (resultObject.has("id")) return GsonHelper.getAsString(resultObject, "id");
        if (resultObject.has("result")) return GsonHelper.getAsString(resultObject, "result");
        if (resultObject.has("output")) return GsonHelper.getAsString(resultObject, "output");
        return null;
    }

    private static boolean isItemHidden(String itemId) {
        if (itemId == null || itemId.isEmpty()) return false;
        ResourceLocation location = ResourceLocation.tryParse(itemId);
        if (location == null) return false;
        Item item = BuiltInRegistries.ITEM.get(location);
        return ReliableRecipesAPI.isItemHidden(item.getDefaultInstance());
    }

    /**
     * Called at the end of datapack reload to apply vanilla runtime logic (repair rules).
     */
    public static void apply() {
        reset();
        List<RecipeRule> rules = cachedRules != null ? cachedRules : RecipeConfigIO.loadRules();

        for (RecipeRule rule : rules) {
            if (rule.getAction() == RecipeRule.Action.PREVENT_REPAIR) {
                ReliableRecipesAPI.registerRepairBlocker(stack -> rule.getTargetInput().test(stack));
            } else if (rule.getAction() == RecipeRule.Action.SET_REPAIR_MATERIAL) {
                for (Item item : BuiltInRegistries.ITEM) {
                    if (rule.getTargetInput().test(item.getDefaultInstance())) {
                        ReliableRecipesAPI.registerCustomRepairMaterial(item, rule.getNewInput());
                    }
                }
            }
        }
    }

    public static boolean removeRecipe(RecipeManager manager, ResourceLocation recipeId) {
        RecipeManagerAccessor managerAccessor = (RecipeManagerAccessor) manager;
        Map<ResourceLocation, Recipe<?>> recipesByName = new LinkedHashMap<>(managerAccessor.getByName());

        Recipe<?> recipe = recipesByName.remove(recipeId);
        if (recipe != null) {
            DELETED_RECIPES_CACHE.put(recipeId, recipe);

            Map<RecipeType<?>, Map<ResourceLocation, Recipe<?>>> recipesByType = new LinkedHashMap<>();
            for (var entry : managerAccessor.getRecipes().entrySet()) {
                recipesByType.put(entry.getKey(), new LinkedHashMap<>(entry.getValue()));
            }

            Map<ResourceLocation, Recipe<?>> typeMap = recipesByType.get(recipe.getType());
            if (typeMap != null) typeMap.remove(recipeId);

            managerAccessor.setByName(ImmutableMap.copyOf(recipesByName));
            managerAccessor.setRecipes(ImmutableMap.copyOf(recipesByType));
            return true;
        }
        return false;
    }

    public static boolean restoreRecipe(RecipeManager manager, ResourceLocation recipeId) {
        Recipe<?> recipe = DELETED_RECIPES_CACHE.remove(recipeId);
        if (recipe == null) return false;

        RecipeManagerAccessor managerAccessor = (RecipeManagerAccessor) manager;
        Map<ResourceLocation, Recipe<?>> recipesByName = new LinkedHashMap<>(managerAccessor.getByName());

        Map<RecipeType<?>, Map<ResourceLocation, Recipe<?>>> recipesByType = new LinkedHashMap<>();
        for (var entry : managerAccessor.getRecipes().entrySet()) {
            recipesByType.put(entry.getKey(), new LinkedHashMap<>(entry.getValue()));
        }

        recipesByName.put(recipeId, recipe);
        recipesByType.computeIfAbsent(recipe.getType(), k -> new LinkedHashMap<>()).put(recipeId, recipe);

        managerAccessor.setByName(ImmutableMap.copyOf(recipesByName));
        managerAccessor.setRecipes(ImmutableMap.copyOf(recipesByType));

        Constants.LOG.info("Restored recipe: {}", recipeId);
        return true;
    }

    public static void reset() {
        DELETED_RECIPES_CACHE.clear();
        ReliableRecipesAPI.clearRepairBlockers();
        ReliableRecipesAPI.clearCustomRepairMaterials();
        cachedRules = null;
    }
}