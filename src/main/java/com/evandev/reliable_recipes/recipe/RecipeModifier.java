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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
//? if <1.21 {
/^import net.minecraft.tags.TagManager;
^///?}
*///?} else {
import com.evandev.reliable_recipes.mixin.accessor.HolderReferenceAccessor;
import com.evandev.reliable_recipes.mixin.accessor.RecipeManagerAccessor;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
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
import java.util.function.Predicate;

public class RecipeModifier {
    private static List<RecipeRule> cachedRules = null;
    private static Map<Identifier, Set<Item>> currentItemTags = null;

    //? if >=1.21.2 && <=26.2 {
    private static final Codec<Recipe<?>> RECIPE_CODEC = Recipe.CODEC;
    //?} else if >26.2 {
    /*private static final Codec<Recipe<?>> RECIPE_CODEC = Recipe.DIRECT_CODEC;
     *///?}

    //? if <1.21.2 {
    /*public static void modifyRecipesJson(Map<Identifier, JsonElement> map, ResourceManager resourceManager) {
        if (resourceManager != null) {
            try {
                //? if <1.21 {
                /^TagLoader<Item> tagLoader = new TagLoader<>(BuiltInRegistries.ITEM::getOptional, TagManager.getTagDir(Registries.ITEM));
                ^///?} else {
                TagLoader<Item> tagLoader = new TagLoader<>(BuiltInRegistries.ITEM::getOptional, Registries.tagsDirPath(Registries.ITEM));
                //?}
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

    //? if <1.21.2 {
    /*/^*
     * Applies the configured rules to a map of raw recipe JSON (as found in data packs),
     * removing, mutating, and adding recipes in place.
     ^/
    public static void modifyRecipesJson(Map<Identifier, JsonElement> map) {
        cachedRules = new ArrayList<>(RecipeConfigIO.loadRules());
        warnUnmatchedIds(cachedRules, map.keySet());
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
            if (processRecipeJson(id, recipeJson, cachedRules, globalReplacements, false)) {
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
            if (processCustomRecipeJson(recipeJson, globalReplacements, false)) {
                newMap.put(id, recipeJson);
            }
        }

        if (!customRecipes.isEmpty()) {
            Constants.LOG.info("Loaded {} custom recipe(s) from reliable_recipes", customRecipes.size());
        }

        map.clear();
        map.putAll(newMap);
    }
    *///?}

    /**
     * Runs the configured rules against a single recipe's JSON, mutating it in place.
     *
     * @return false if the recipe should be removed.
     */
    private static boolean processRecipeJson(Identifier id, JsonObject recipeJson, List<RecipeRule> rules, Map<String, String> globalReplacements, boolean checkInputs) {
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

        return processCustomRecipeJson(recipeJson, globalReplacements, checkInputs);
    }

    //? if forge {
    /*/^*
     * Runs the configured rules against a single recipe's JSON, for mods that load recipes outside the recipe manager.
     *
     * @return the mutated JSON, or null if the recipe should be removed.
     ^/
    public static JsonObject modifySingleRecipeJson(Identifier id, JsonObject recipeJson) {
        List<RecipeRule> rules = cachedRules != null ? cachedRules : RecipeConfigIO.loadRules();
        return processRecipeJson(id, recipeJson, rules, ReliableRecipesAPI.getReplacements(), true) ? recipeJson : null;
    }

    *///?}

    /**
     * Applies API replacements and hidden-item checks, which apply to custom recipes as well.
     *
     * @return false if the recipe should be removed.
     */
    private static boolean processCustomRecipeJson(JsonObject recipeJson, Map<String, String> globalReplacements, boolean checkInputs) {
        for (Map.Entry<String, String> rep : globalReplacements.entrySet()) {
            applyGlobalReplacement(recipeJson, rep.getKey(), rep.getValue());
        }

        return !ReliableRecipesAPI.hasItemHidingCapabilities() || !shouldHideRecipeJson(recipeJson, RecipeModifier::isItemHidden, checkInputs);
    }

    /**
     * Warns about rules whose {@code id} filter names a recipe that doesn't exist, which usually means the item id was used by mistake.
     */
    private static void warnUnmatchedIds(List<RecipeRule> rules, Collection<Identifier> recipeIds) {
        Set<String> known = new HashSet<>();
        for (Identifier id : recipeIds) {
            known.add(id.toString());
            known.add(id.getPath());
        }
        for (RecipeRule rule : rules) {
            for (String id : rule.getLiteralIds()) {
                if (!known.contains(id)) {
                    Constants.LOG.warn("A {} rule filters on recipe id '{}', but no loaded recipe has that id. To match recipes by the item they make, use 'target' or 'output' instead of 'id'.",
                            rule.getAction().name().toLowerCase(Locale.ROOT), id);
                }
            }
        }
    }

    private static void applyRuleReplacement(JsonObject recipeJson, RecipeRule rule) {
        RecipeJsonMutator.applyRule(recipeJson, rule, RecipeJsonMutator.NATIVE_FORMAT);
    }

    private static void applyGlobalReplacement(JsonObject recipeJson, String from, String to) {
        Map<String, JsonElement> replacements = Map.of(from, new JsonPrimitive(to));
        RecipeJsonMutator.mutateRecipe(recipeJson, replacements, replacements, RecipeJsonMutator.NATIVE_FORMAT);
    }

    public static boolean shouldHideRecipeJson(JsonObject jsonObject, Predicate<String> isItemHidden, boolean checkInputs) {
        try {
            JsonElement resultElement = jsonObject.has("result") ? jsonObject.get("result") :
                    (jsonObject.has("results") ? jsonObject.get("results") :
                            (jsonObject.has("output") ? jsonObject.get("output") : null));
            if (resultElement != null) {
                if (resultElement.isJsonObject() && isItemHidden.test(getResultItemId(resultElement.getAsJsonObject())))
                    return true;
                if (resultElement.isJsonPrimitive() && isItemHidden.test(resultElement.getAsJsonPrimitive().getAsString()))
                    return true;
                if (resultElement.isJsonArray()) {
                    for (JsonElement element : resultElement.getAsJsonArray()) {
                        if (element.isJsonObject() && isItemHidden.test(getResultItemId(element.getAsJsonObject())))
                            return true;
                        if (element.isJsonPrimitive() && isItemHidden.test(element.getAsString())) return true;
                    }
                }
            }

            if (!checkInputs) return false;

            if (jsonObject.has("key") && jsonObject.get("key").isJsonObject()) {
                for (Map.Entry<String, JsonElement> slot : jsonObject.getAsJsonObject("key").entrySet()) {
                    if (isIngredientHidden(slot.getValue(), isItemHidden)) return true;
                }
            }

            if (jsonObject.has("ingredients")) {
                JsonElement ingredients = jsonObject.get("ingredients");
                if (ingredients.isJsonArray()) {
                    for (JsonElement slot : ingredients.getAsJsonArray()) {
                        if (isIngredientHidden(slot, isItemHidden)) return true;
                    }
                } else if (isIngredientHidden(ingredients, isItemHidden)) {
                    return true;
                }
            }

            for (String key : List.of("ingredient", "input", "reagent")) {
                if (jsonObject.has(key) && isIngredientHidden(jsonObject.get(key), isItemHidden)) return true;
            }
        } catch (Exception ignored) {
        }
        return false;
    }

    private static boolean isIngredientHidden(JsonElement element, Predicate<String> isItemHidden) {
        if (element == null || element.isJsonNull()) return false;
        if (element.isJsonPrimitive()) {
            if (!element.getAsJsonPrimitive().isString()) return false;
            String id = element.getAsString();
            return !id.startsWith("#") && isItemHidden.test(id);
        }
        if (element.isJsonArray()) {
            JsonArray options = element.getAsJsonArray();
            if (options.isEmpty()) return false;
            for (JsonElement option : options) {
                if (!isIngredientHidden(option, isItemHidden)) return false;
            }
            return true;
        }
        if (element.isJsonObject()) {
            JsonObject obj = element.getAsJsonObject();
            if (obj.has("tag")) return false;
            if (obj.has("item")) return isIngredientHidden(obj.get("item"), isItemHidden);
            if (obj.has("items")) return isIngredientHidden(obj.get("items"), isItemHidden);
            if (obj.has("ingredient")) return isIngredientHidden(obj.get("ingredient"), isItemHidden);
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
    /*public static void apply(RecipeManager manager) {
        reset();
        applyGlobalRules();
        hideRecipesWithHiddenInputs(manager);
        BrewingRecipeManager.reload(manager);
    }

    private static void hideRecipesWithHiddenInputs(RecipeManager manager) {
        if (!ReliableRecipesAPI.hasItemHidingCapabilities()) return;

        List<RecipeHolder<?>> recipes = RecipeUndoCache.getRecipes(manager);
        int originalCount = recipes.size();
        recipes.removeIf(holder -> {
            try {
                if (!hasHiddenIngredient(holder.value().getIngredients(), ReliableRecipesAPI::isItemHidden)) return false;
            } catch (Exception e) {
                return false;
            }
            RecipeUndoCache.put(holder);
            return true;
        });

        if (recipes.size() < originalCount) {
            RecipeUndoCache.setRecipes(manager, recipes);
            Constants.LOG.info("RecipeModifier removed {} recipes with hidden inputs.", originalCount - recipes.size());
        }
    }

    public static boolean hasHiddenIngredient(List<Ingredient> ingredients, Predicate<ItemStack> isItemHidden) {
        return ingredients.stream().map(Ingredient::getItems)
                .anyMatch(options -> options.length > 0 && Arrays.stream(options).allMatch(isItemHidden));
    }

    public static void applyClient() {
        resetRules();
        applyGlobalRules();
    }
    *///?}

    public static void reset() {
        RecipeUndoCache.clear();
        resetRules();
    }

    private static void resetRules() {
        ReliableRecipesAPI.clearRepairBlockers();
        ReliableRecipesAPI.clearCustomRepairMaterials();
        cachedRules = null;
        currentItemTags = null;
    }

    //? if >=1.21.2 {
    public static void apply(RecipeManager manager, HolderLookup.Provider registries) {
        reset();

        try {
            cachedRules = new ArrayList<>(RecipeConfigIO.loadRules());
            applyGlobalRules();

            Map<String, String> globalReplacements = ReliableRecipesAPI.getReplacements();
            boolean needsJson = !cachedRules.isEmpty() || !globalReplacements.isEmpty() || ReliableRecipesAPI.hasItemHidingCapabilities();

            RecipeManagerAccessor managerAccessor = (RecipeManagerAccessor) manager;
            RecipeMap currentMap = managerAccessor.reliableRecipes$getRecipeMap();
            List<RecipeHolder<?>> validRecipes = new ArrayList<>();

            List<Identifier> recipeIds = new ArrayList<>();
            for (RecipeHolder<?> recipeHolder : currentMap.values()) {
                recipeIds.add(CompatUtil.recipeId(recipeHolder));
            }
            warnUnmatchedIds(cachedRules, recipeIds);

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
                    if (!processCustomRecipeJson(json, globalReplacements, true)) continue;

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
        }
    }

    public static RecipeHolder<?> processRecipe(RecipeHolder<?> recipeHolder, List<RecipeRule> rules, Map<String, String> globalReplacements, RegistryOps<JsonElement> ops) {
        try {
            DataResult<JsonElement> encoded = RECIPE_CODEC.encodeStart(ops, recipeHolder.value());
            Optional<JsonElement> encodeResult = encoded.result();
            if (encodeResult.isEmpty() || !encodeResult.get().isJsonObject()) {
                Constants.LOG.debug("Skipping recipe {}, it could not be encoded to a JSON object: {}", CompatUtil.recipeId(recipeHolder),
                        encoded.error().map(DataResult.Error::message).orElse("encoded to " + encodeResult.map(JsonElement::toString).orElse("nothing")));
                return recipeHolder;
            }

            JsonObject json = encodeResult.get().getAsJsonObject();
            JsonObject original = json.deepCopy();
            addSyntheticKeys(json, recipeHolder);

            boolean keep = processRecipeJson(CompatUtil.recipeId(recipeHolder), json, rules, globalReplacements, true);
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
