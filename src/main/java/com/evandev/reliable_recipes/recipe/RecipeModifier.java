package com.evandev.reliable_recipes.recipe;

import com.evandev.reliable_recipes.Constants;
import com.evandev.reliable_recipes.api.ReliableRecipesAPI;
import com.evandev.reliable_recipes.config.RecipeConfigIO;
import com.evandev.reliable_recipes.config.RecipeRuleParser;
import com.evandev.reliable_recipes.util.CompatUtil;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.Item;
//? if <1.21.2 {
/*import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.tags.TagLoader;
*///?} else {
import com.evandev.reliable_recipes.mixin.accessor.HolderReferenceAccessor;
import com.evandev.reliable_recipes.mixin.accessor.RecipeManagerAccessor;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.HolderSet;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeMap;
import net.minecraft.world.item.enchantment.Repairable;
//?}
//? if >26.2 {
/*import net.minecraft.core.Holder;
import net.minecraft.core.HolderOwner;
import java.util.stream.Stream;
*///?}

import java.util.*;

public class RecipeModifier {
    private static final ThreadLocal<Boolean> MODIFYING_JSON = ThreadLocal.withInitial(() -> false);
    private static List<RecipeRule> cachedRules = null;
    private static Map<Identifier, Set<Item>> currentItemTags = null;

    //? if >=1.21.2 && <=26.2 {
    private static final Codec<Recipe<?>> RECIPE_CODEC = Recipe.CODEC;
    //?} else if >26.2 {
    /*private static final Codec<Recipe<?>> RECIPE_CODEC = Recipe.DIRECT_CODEC;
    *///?}

    public static boolean isModifyingJson() {
        return MODIFYING_JSON.get();
    }

    //? if <1.21.2 {
    /*public static void modifyRecipesJson(Map<Identifier, JsonElement> map, ResourceManager resourceManager) {
        if (resourceManager != null) {
            try {
                TagLoader<Item> tagLoader = new TagLoader<>(BuiltInRegistries.ITEM::getOptional, Registries.tagsDirPath(Registries.ITEM));
                Map<Identifier, Collection<Item>> rawTags = tagLoader.loadAndBuild(resourceManager);
                Map<Identifier, Set<Item>> itemTags = new HashMap<>();
                for (Map.Entry<Identifier, Collection<Item>> entry : rawTags.entrySet()) {
                    itemTags.put(entry.getKey(), new HashSet<>(entry.getValue()));
                }
                currentItemTags = itemTags;
            } catch (Exception e) {
                Constants.LOG.error("Failed to preload item tags for recipe filtering", e);
            }
        }
        try {
            modifyRecipesJson(map);
        } finally {
            currentItemTags = null;
        }
    }
    *///?}

    public static boolean isItemInTag(Item item, Identifier tagId) {
        if (currentItemTags != null) {
            Set<Item> items = currentItemTags.get(tagId);
            if (items != null) {
                return items.contains(item);
            }
        }
        TagKey<Item> tagKey = TagKey.create(Registries.ITEM, tagId);
        return item.builtInRegistryHolder().is(tagKey);
    }

    /**
     * Applies the configured rules to a map of raw recipe JSON (as found in data packs),
     * removing, mutating and adding recipes in place.
     */
    public static void modifyRecipesJson(Map<Identifier, JsonElement> map) {
        MODIFYING_JSON.set(true);
        try {
            cachedRules = new ArrayList<>(RecipeConfigIO.loadRules());
            Map<Identifier, JsonElement> newMap = new HashMap<>();
            Map<String, String> globalReplacements = ReliableRecipesAPI.getReplacements();

            for (Map.Entry<Identifier, JsonElement> entry : map.entrySet()) {
                Identifier id = entry.getKey();
                JsonElement element = entry.getValue();

                if (!element.isJsonObject()) {
                    newMap.put(id, element);
                    continue;
                }

                JsonObject recipeJson = element.getAsJsonObject();
                if (processRecipeJson(id, recipeJson, cachedRules, globalReplacements)) {
                    newMap.put(id, recipeJson);
                }
            }

            // Custom recipes loaded from reliable_recipes/
            Map<Identifier, JsonElement> customRecipes = RecipeConfigIO.loadCustomRecipes();
            for (Map.Entry<Identifier, JsonElement> customEntry : customRecipes.entrySet()) {
                Identifier id = customEntry.getKey();
                JsonElement element = customEntry.getValue();

                if (!element.isJsonObject()) {
                    newMap.put(id, element);
                    continue;
                }

                JsonObject recipeJson = element.getAsJsonObject().deepCopy();
                if (processCustomRecipeJson(recipeJson, globalReplacements)) {
                    newMap.put(id, recipeJson);
                }
            }

            if (!customRecipes.isEmpty()) {
                Constants.LOG.info("Loaded {} custom recipe(s) from reliable_recipes", customRecipes.size());
            }

            map.clear();
            map.putAll(newMap);
        } finally {
            MODIFYING_JSON.set(false);
        }
    }

    /**
     * Runs the configured rules against a single recipe's JSON, mutating it in place.
     *
     * @return false if the recipe should be removed.
     */
    private static boolean processRecipeJson(Identifier id, JsonObject recipeJson, List<RecipeRule> rules, Map<String, String> globalReplacements) {
        // Evaluate user rules from config
        for (RecipeRule rule : rules) {
            if (rule.testJson(id, recipeJson)) {
                if (rule.getAction() == RecipeRule.Action.REMOVE) {
                    return false;
                } else if (rule.getAction() == RecipeRule.Action.REPLACE_INPUT || rule.getAction() == RecipeRule.Action.REPLACE_OUTPUT) {
                    applyRuleReplacement(recipeJson, rule);
                }
            }
        }

        return processCustomRecipeJson(recipeJson, globalReplacements);
    }

    /**
     * Applies API replacements and hidden-item checks, which apply to custom recipes as well.
     *
     * @return false if the recipe should be removed.
     */
    private static boolean processCustomRecipeJson(JsonObject recipeJson, Map<String, String> globalReplacements) {
        // Global API Replacements
        for (Map.Entry<String, String> rep : globalReplacements.entrySet()) {
            applyGlobalReplacement(recipeJson, rep.getKey(), rep.getValue());
        }

        // Hidden items output check
        return !ReliableRecipesAPI.hasItemHidingCapabilities() || !shouldHideRecipeJson(recipeJson);
    }

    private static void applyRuleReplacement(JsonObject recipeJson, RecipeRule rule) {
        //? if <1.21.2 {
        /*boolean startContext = (rule.getAction() == RecipeRule.Action.REPLACE_INPUT);
        mutateJsonRecursively(recipeJson, rule.getRawTargets(), rule.getRawReplacement(), rule.getAction(), startContext);
        *///?} else {
        // 26.x recipe JSON uses plain strings for items and tags, which RecipeJsonMutator understands
        JsonElement replacement = rule.getRawReplacement();
        if (replacement == null) return;

        Map<String, JsonElement> replacements = new HashMap<>();
        if (rule.getAction() == RecipeRule.Action.REPLACE_OUTPUT && rule.getRawTargets().isEmpty()) {
            replacements.put("", replacement);
        } else {
            for (String target : rule.getRawTargets()) {
                if (target != null && !target.isEmpty()) {
                    replacements.put(target, replacement);
                }
            }
        }

        if (rule.getAction() == RecipeRule.Action.REPLACE_INPUT) {
            RecipeJsonMutator.mutateRecipe(recipeJson, replacements, Map.of());
        } else {
            RecipeJsonMutator.mutateRecipe(recipeJson, Map.of(), replacements);
        }
        //?}
    }

    private static void applyGlobalReplacement(JsonObject recipeJson, String from, String to) {
        //? if <1.21.2 {
        /*mutateJsonRecursively(recipeJson, List.of(from), new JsonPrimitive(to), null, true);
        *///?} else {
        Map<String, JsonElement> replacements = Map.of(from, new JsonPrimitive(to));
        RecipeJsonMutator.mutateRecipe(recipeJson, replacements, replacements);
        //?}
    }

    public static void mutateJsonRecursively(JsonElement parent, List<String> targets, JsonElement rawReplacement, RecipeRule.Action action, boolean isTargetContext) {
        mutateJsonRecursively(parent, targets, rawReplacement, action, isTargetContext, false);
    }

    public static void mutateJsonRecursively(JsonElement parent, List<String> targets, JsonElement rawReplacement, RecipeRule.Action action, boolean isTargetContext, boolean isIngredientList) {
        if (parent.isJsonObject()) {
            JsonObject obj = parent.getAsJsonObject();
            List<Map.Entry<String, JsonElement>> entries = new ArrayList<>(obj.entrySet());

            for (Map.Entry<String, JsonElement> entry : entries) {
                String key = entry.getKey();
                JsonElement child = entry.getValue();

                boolean nextContext = getNextContext(action, isTargetContext, key);
                boolean childIsIngredientList = isIngredientListKey(key);

                if (child.isJsonObject()) {
                    JsonObject childObj = child.getAsJsonObject();
                    String matchedKey = getMatchingKey(childObj, targets, action, nextContext);

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
                    } else if (action == RecipeRule.Action.REPLACE_OUTPUT && nextContext && targets.isEmpty() && rawReplacement.isJsonObject()) {
                        obj.add(key, rawReplacement.deepCopy());
                    } else {
                        mutateJsonRecursively(child, targets, rawReplacement, action, nextContext, childIsIngredientList);
                    }
                } else if (child.isJsonPrimitive() && child.getAsJsonPrimitive().isString()) {
                    boolean isMatch = nextContext && (targets.contains(child.getAsString()) || (action == RecipeRule.Action.REPLACE_OUTPUT && targets.isEmpty()));
                    if (isMatch) {
                        if (key.equals("tag") && rawReplacement.isJsonPrimitive()) {
                            String repStr = rawReplacement.getAsString();
                            obj.remove("tag");
                            if (repStr.startsWith("#")) obj.addProperty("tag", repStr.substring(1));
                            else obj.addProperty("item", repStr);
                        } else if (rawReplacement.isJsonArray()) {
                            JsonArray newArr = new JsonArray();
                            for (JsonElement rep : rawReplacement.getAsJsonArray()) {
                                if (rep.isJsonPrimitive()) {
                                    JsonObject newObj = new JsonObject();
                                    String repStr = rep.getAsString();
                                    if (repStr.startsWith("#")) newObj.addProperty("tag", repStr.substring(1));
                                    else newObj.addProperty(key.equals("tag") ? "tag" : "item", repStr);
                                    newArr.add(newObj);
                                } else {
                                    newArr.add(rep.deepCopy());
                                }
                            }
                            obj.add(key, newArr);
                        } else {
                            obj.add(key, rawReplacement.deepCopy());
                        }
                    }
                } else {
                    mutateJsonRecursively(child, targets, rawReplacement, action, nextContext, childIsIngredientList);
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
                    String matchedKey = getMatchingKey(childObj, targets, action, isTargetContext);

                    if (matchedKey != null && isTargetContext) {
                        changed = true;
                        if (rawReplacement.isJsonArray()) {
                            if (isIngredientList) {
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
                                newArray.add(newArr);
                            } else {
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
                    } else if (action == RecipeRule.Action.REPLACE_OUTPUT && isTargetContext && targets.isEmpty() && rawReplacement.isJsonObject()) {
                        changed = true;
                        newArray.add(rawReplacement.deepCopy());
                    } else {
                        newArray.add(child);
                        mutateJsonRecursively(child, targets, rawReplacement, action, isTargetContext, false);
                    }
                } else if (child.isJsonPrimitive() && child.getAsJsonPrimitive().isString()) {
                    boolean isMatch = isTargetContext && (targets.contains(child.getAsString()) || (action == RecipeRule.Action.REPLACE_OUTPUT && targets.isEmpty()));
                    if (isMatch) {
                        changed = true;
                        if (rawReplacement.isJsonArray()) {
                            if (isIngredientList) {
                                JsonArray newArr = new JsonArray();
                                for (JsonElement rep : rawReplacement.getAsJsonArray()) {
                                    if (rep.isJsonPrimitive()) {
                                        String repStr = rep.getAsString();
                                        JsonObject newObj = new JsonObject();
                                        if (repStr.startsWith("#")) newObj.addProperty("tag", repStr.substring(1));
                                        else newObj.addProperty("item", repStr);
                                        newArr.add(newObj);
                                    } else {
                                        newArr.add(rep.deepCopy());
                                    }
                                }
                                newArray.add(newArr);
                            } else {
                                for (JsonElement rep : rawReplacement.getAsJsonArray()) newArray.add(rep.deepCopy());
                            }
                        } else {
                            newArray.add(rawReplacement.deepCopy());
                        }
                    } else {
                        newArray.add(child);
                    }
                } else {
                    newArray.add(child);
                    mutateJsonRecursively(child, targets, rawReplacement, action, isTargetContext, false);
                }
            }

            if (changed) {
                while (!oldArray.isEmpty()) oldArray.remove(0);
                for (int i = 0; i < newArray.size(); i++) oldArray.add(newArray.get(i));
            }
        }
    }

    private static boolean isIngredientListKey(String key) {
        String lower = key.toLowerCase(Locale.ROOT);
        return lower.endsWith("ingredients") || lower.endsWith("inputs");
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

    private static String getMatchingKey(JsonObject obj, List<String> targets, RecipeRule.Action action, boolean isTargetContext) {
        if (action == RecipeRule.Action.REPLACE_OUTPUT && isTargetContext && targets.isEmpty()) {
            if (obj.has("id") && obj.get("id").isJsonPrimitive()) return "id";
            if (obj.has("item") && obj.get("item").isJsonPrimitive()) return "item";
            if (obj.has("result") && obj.get("result").isJsonPrimitive()) return "result";
            if (obj.has("output") && obj.get("output").isJsonPrimitive()) return "output";
        }
        if (obj.has("item") && obj.get("item").isJsonPrimitive() && targets.contains(obj.get("item").getAsString()))
            return "item";
        if (obj.has("id") && obj.get("id").isJsonPrimitive() && targets.contains(obj.get("id").getAsString()))
            return "id";
        if (obj.has("result") && obj.get("result").isJsonPrimitive() && targets.contains(obj.get("result").getAsString()))
            return "result";
        if (obj.has("output") && obj.get("output").isJsonPrimitive() && targets.contains(obj.get("output").getAsString()))
            return "output";
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
            if (resultElement != null) {
                if (resultElement.isJsonObject() && isItemHidden(getResultItemId(resultElement.getAsJsonObject())))
                    return true;
                if (resultElement.isJsonPrimitive() && isItemHidden(resultElement.getAsJsonPrimitive().getAsString()))
                    return true;
                if (resultElement.isJsonArray()) {
                    for (JsonElement element : resultElement.getAsJsonArray()) {
                        if (element.isJsonObject() && isItemHidden(getResultItemId(element.getAsJsonObject())))
                            return true;
                        if (element.isJsonPrimitive() && isItemHidden(element.getAsString())) return true;
                    }
                }
            }

            for (String key : List.of("input", "reagent", "ingredients", "key")) {
                if (jsonObject.has(key)) {
                    if (checkJsonForHiddenItem(jsonObject.get(key))) return true;
                }
            }
        } catch (Exception ignored) {
        }
        return false;
    }

    private static boolean checkJsonForHiddenItem(JsonElement element) {
        if (element == null) return false;
        if (element.isJsonPrimitive() && element.getAsJsonPrimitive().isString()) {
            return isItemHidden(element.getAsString());
        }
        if (element.isJsonArray()) {
            for (JsonElement e : element.getAsJsonArray()) {
                if (checkJsonForHiddenItem(e)) return true;
            }
        }
        if (element.isJsonObject()) {
            JsonObject obj = element.getAsJsonObject();
            if (obj.has("item") && obj.get("item").isJsonPrimitive() && isItemHidden(obj.get("item").getAsString()))
                return true;
            if (obj.has("id") && obj.get("id").isJsonPrimitive() && isItemHidden(obj.get("id").getAsString()))
                return true;
            for (Map.Entry<String, JsonElement> entry : obj.entrySet()) {
                if (checkJsonForHiddenItem(entry.getValue())) return true;
            }
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
        Identifier location = Identifier.tryParse(itemId);
        if (location == null) return false;
        Item item = CompatUtil.getItem(location);
        return ReliableRecipesAPI.isItemHidden(item.getDefaultInstance());
    }

    /**
     * Registers the repair rules ({@code prevent_repair}, {@code set_repair_material}) from the config.
     */
    public static void applyGlobalRules() {
        ReliableRecipesAPI.clearRepairBlockers();
        ReliableRecipesAPI.clearCustomRepairMaterials();
        List<RecipeRule> rules = cachedRules != null ? cachedRules : RecipeConfigIO.loadRules();

        for (RecipeRule rule : rules) {
            if (rule.getAction() == RecipeRule.Action.PREVENT_REPAIR) {
                //? if >=1.21.2 {
                for (Item item : BuiltInRegistries.ITEM) {
                    if (rule.getTargetInput().test(item.getDefaultInstance())) {
                        DataComponentMap newMap = DataComponentMap.builder()
                                .addAll(item.components())
                                .set(DataComponents.REPAIRABLE, null)
                                .build();
                        ((HolderReferenceAccessor) item.builtInRegistryHolder()).setComponents(newMap);
                    }
                }
                //?}
                ReliableRecipesAPI.registerRepairBlocker(stack -> rule.getTargetInput().test(stack));
            } else if (rule.getAction() == RecipeRule.Action.SET_REPAIR_MATERIAL) {
                //? if >=1.21.2 {
                Repairable repairable = rule.getNewInput().isEmpty() ? null : new Repairable(HolderSet.direct(rule.getNewInput().items().toList()));
                //?}
                for (Item item : BuiltInRegistries.ITEM) {
                    if (rule.getTargetInput().test(item.getDefaultInstance())) {
                        //? if >=1.21.2 {
                        if (repairable != null) {
                            DataComponentMap newMap = DataComponentMap.builder()
                                    .addAll(item.components())
                                    .set(DataComponents.REPAIRABLE, repairable)
                                    .build();
                            ((HolderReferenceAccessor) item.builtInRegistryHolder()).setComponents(newMap);
                        }
                        //?}
                        ReliableRecipesAPI.registerCustomRepairMaterial(item, rule.getNewInput());
                    }
                }
            }
        }
    }

    //? if <1.21.2 {
    /*public static void apply() {
        reset();
        applyGlobalRules();
    }
    *///?}

    public static void reset() {
        RecipeUndoCache.clear();
        ReliableRecipesAPI.clearRepairBlockers();
        ReliableRecipesAPI.clearCustomRepairMaterials();
        cachedRules = null;
        currentItemTags = null;
    }

    //? if >=1.21.2 {
    public static void apply(RecipeManager manager, HolderLookup.Provider registries) {
        reset();

        MODIFYING_JSON.set(true);
        try {
            cachedRules = new ArrayList<>(RecipeConfigIO.loadRules());
            applyGlobalRules();

            Map<String, String> globalReplacements = ReliableRecipesAPI.getReplacements();
            boolean needsJson = !cachedRules.isEmpty() || !globalReplacements.isEmpty() || ReliableRecipesAPI.hasItemHidingCapabilities();

            RecipeManagerAccessor managerAccessor = (RecipeManagerAccessor) manager;
            RecipeMap currentMap = managerAccessor.reliableRecipes$getRecipeMap();
            List<RecipeHolder<?>> validRecipes = new ArrayList<>();

            RegistryOps<JsonElement> ops = registries.createSerializationContext(JsonOps.INSTANCE);
            int removedCount = 0;
            int replacedCount = 0;

            for (RecipeHolder<?> recipeHolder : currentMap.values()) {
                RecipeHolder<?> processed;
                if (shouldHideRecipe(recipeHolder)) {
                    processed = null;
                } else if (!needsJson) {
                    processed = recipeHolder;
                } else {
                    processed = processRecipe(recipeHolder, cachedRules, globalReplacements, ops);
                }

                if (processed == null) {
                    RecipeUndoCache.put(recipeHolder);
                    removedCount++;
                } else {
                    if (processed != recipeHolder) replacedCount++;
                    validRecipes.add(processed);
                }
            }

            Map<Identifier, JsonElement> customRecipes = RecipeConfigIO.loadCustomRecipes();
            for (Map.Entry<Identifier, JsonElement> entry : customRecipes.entrySet()) {
                Identifier id = entry.getKey();
                if (!entry.getValue().isJsonObject()) {
                    Constants.LOG.error("Custom recipe {} is not a JSON object", id);
                    continue;
                }
                JsonObject json = entry.getValue().getAsJsonObject().deepCopy();
                try {
                    if (!processCustomRecipeJson(json, globalReplacements)) continue;

                    Optional<Recipe<?>> parsed = RECIPE_CODEC.parse(ops, json).result();
                    if (parsed.isPresent()) {
                        ResourceKey<Recipe<?>> key = ResourceKey.create(Registries.RECIPE, id);
                        validRecipes.add(new RecipeHolder<>(key, parsed.get()));
                    } else {
                        Constants.LOG.error("Failed to parse custom recipe: {}", id);
                    }
                } catch (Exception e) {
                    Constants.LOG.error("Error loading custom recipe: {}", id, e);
                }
            }

            if (!customRecipes.isEmpty()) {
                Constants.LOG.info("Loaded {} custom recipe(s) from reliable_recipes", customRecipes.size());
            }

            if (removedCount > 0 || replacedCount > 0) {
                Constants.LOG.info("RecipeModifier removed {} and replaced contents in {} recipes.", removedCount, replacedCount);
            }

            managerAccessor.reliableRecipes$setRecipeMap(createRecipeMap(validRecipes));
            //? if <=26.2 {
            BrewingRecipeManager.reload(manager);
            //?}
        } finally {
            cachedRules = null;
            MODIFYING_JSON.set(false);
        }
    }

    public static RecipeHolder<?> processRecipe(RecipeHolder<?> recipeHolder, List<RecipeRule> rules, Map<String, String> globalReplacements, RegistryOps<JsonElement> ops) {
        try {
            Optional<JsonElement> encodeResult = RECIPE_CODEC.encodeStart(ops, recipeHolder.value()).result();
            if (encodeResult.isEmpty() || !encodeResult.get().isJsonObject()) {
                return recipeHolder;
            }

            JsonObject json = encodeResult.get().getAsJsonObject();
            JsonObject original = json.deepCopy();
            addSyntheticKeys(json, recipeHolder);

            boolean keep = processRecipeJson(CompatUtil.recipeId(recipeHolder), json, rules, globalReplacements);
            json.remove(RecipeRuleParser.SYNTHETIC_TYPE_KEY);
            json.remove(RecipeRuleParser.SYNTHETIC_RESULTS_KEY);

            if (!keep) return null;
            if (json.equals(original)) return recipeHolder;

            Optional<Recipe<?>> decodeResult = RECIPE_CODEC.parse(ops, json).result();
            if (decodeResult.isPresent()) {
                return new RecipeHolder<>(recipeHolder.id(), decodeResult.get());
            }
            Constants.LOG.error("Failed to decode mutated recipe: {}", CompatUtil.recipeId(recipeHolder));
            Constants.LOG.debug("Mutated JSON was: {}", json);
        } catch (Exception e) {
            Constants.LOG.error("Failed to process recipe modifications for: {}", CompatUtil.recipeId(recipeHolder), e);
        }
        return recipeHolder;
    }

    private static void addSyntheticKeys(JsonObject json, RecipeHolder<?> holder) {
        Identifier typeId = BuiltInRegistries.RECIPE_TYPE.getKey(holder.value().getType());
        if (typeId != null) {
            json.addProperty(RecipeRuleParser.SYNTHETIC_TYPE_KEY, typeId.toString());
        }

        if (!json.has("result") && !json.has("results") && !json.has("output")) {
            JsonArray results = new JsonArray();
            try {
                for (ItemStack stack : ReliableRecipesAPI.getRecipeResults(holder.value())) {
                    if (!stack.isEmpty()) {
                        results.add(BuiltInRegistries.ITEM.getKey(stack.getItem()).toString());
                    }
                }
            } catch (Exception ignored) {
            }
            if (!results.isEmpty()) {
                json.add(RecipeRuleParser.SYNTHETIC_RESULTS_KEY, results);
            }
        }
    }

    private static boolean shouldHideRecipe(RecipeHolder<?> holder) {
        if (!ReliableRecipesAPI.hasItemHidingCapabilities()) return false;
        try {
            List<ItemStack> outputs = ReliableRecipesAPI.getRecipeResults(holder.value());
            for (ItemStack stack : outputs) {
                if (!stack.isEmpty() && ReliableRecipesAPI.isItemHidden(stack)) {
                    return true;
                }
            }
        } catch (Exception ignored) {
        }
        return false;
    }

    public static RecipeMap createRecipeMap(Iterable<RecipeHolder<?>> recipes) {
        //? if <=26.2 {
        return RecipeMap.create(recipes);
        //?} else {
        /*return RecipeMap.create(new RecipeHolderLookup(recipes));
        *///?}
    }
    //?}

    //? if >26.2 {
    /*private static class RecipeHolderLookup implements HolderLookup<Recipe<?>> {
        private final List<Holder.Reference<Recipe<?>>> list = new ArrayList<>();
        private final Map<ResourceKey<Recipe<?>>, Holder.Reference<Recipe<?>>> map = new HashMap<>();

        public RecipeHolderLookup(Iterable<RecipeHolder<?>> recipes) {
            HolderOwner<Recipe<?>> owner = new HolderOwner<>() {};
            for (RecipeHolder<?> holder : recipes) {
                Holder.Reference<Recipe<?>> ref = new StandaloneReference<>(owner, holder.id(), (Recipe<?>) holder.value());
                list.add(ref);
                map.put(holder.id(), ref);
            }
        }

        @Override
        public Stream<Holder.Reference<Recipe<?>>> listElements() {
            return list.stream();
        }

        @Override
        public Stream<HolderSet.Named<Recipe<?>>> listTags() {
            return Stream.empty();
        }

        @Override
        public Optional<Holder.Reference<Recipe<?>>> get(ResourceKey<Recipe<?>> key) {
            return Optional.ofNullable(map.get(key));
        }

        @Override
        public Optional<HolderSet.Named<Recipe<?>>> get(TagKey<Recipe<?>> tag) {
            return Optional.empty();
        }
    }

    private static class StandaloneReference<T> extends Holder.Reference<T> {
        public StandaloneReference(HolderOwner<T> owner, ResourceKey<T> key, T value) {
            super(Type.STAND_ALONE, owner, key, value);
        }
    }
    *///?}
}
