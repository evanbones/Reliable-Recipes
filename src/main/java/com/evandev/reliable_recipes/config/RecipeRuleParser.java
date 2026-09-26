package com.evandev.reliable_recipes.config;

import com.evandev.reliable_recipes.Constants;
import com.evandev.reliable_recipes.recipe.RecipeModifier;
import com.evandev.reliable_recipes.recipe.RecipeRule;
import com.evandev.reliable_recipes.tag.TagRule;
import com.evandev.reliable_recipes.util.CompatUtil;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.mojang.serialization.JsonOps;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
//? if <1.21.2 {
/*import com.google.gson.JsonArray;
import net.minecraft.world.item.ItemStack;
*///?}

import java.util.*;
import java.util.function.BiPredicate;
import java.util.function.Predicate;
import java.util.regex.Pattern;

public class RecipeRuleParser {
    public static final String SYNTHETIC_TYPE_KEY = "reliable_recipes:recipe_type";
    public static final String SYNTHETIC_RESULTS_KEY = "reliable_recipes:results";

    private static final Set<String> IGNORED_KEYS = Set.of(
            "action", "target", "replacement", "material", "items", "tags", "tag", "filter"
    );

    private static final Set<String> OUTPUT_KEYS = Set.of("result", "output", "results");
    private static final Set<String> NON_INPUT_KEYS = Set.of("result", "output", "results", SYNTHETIC_TYPE_KEY, SYNTHETIC_RESULTS_KEY);

    public static RecipeRule parseRule(JsonObject mod) {
        if (!mod.has("action")) {
            return null;
        }
        String actionStr = mod.get("action").getAsString();

        if (actionStr.equals("add") || actionStr.equals("add_recipe")) {
            return null;
        }

        if (Set.of("remove_all_tags", "remove_tag", "remove_from_tag", "clear_tag").contains(actionStr)) {
            return null;
        }

        if (actionStr.equals("prevent_repair")) {
            Ingredient target = parseIngredient(mod.get("target"));
            return new RecipeRule(RecipeRule.Action.PREVENT_REPAIR, (id, r) -> false, target, CompatUtil.emptyIngredient());
        }

        if (actionStr.equals("set_repair_material")) {
            Ingredient target = parseIngredient(mod.get("target"));
            JsonElement matEl = mod.has("material") ? mod.get("material") : mod.get("replacement");
            Ingredient material = parseIngredient(matEl);
            return new RecipeRule(RecipeRule.Action.SET_REPAIR_MATERIAL, (id, r) -> false, target, material);
        }

        JsonElement filterEl = mod.has("filter") ? mod.get("filter") : mod;
        if (actionStr.equals("remove_output") && filterEl.isJsonObject()) {
            JsonObject filterObj = filterEl.getAsJsonObject();
            if (filterObj.has("target") && !filterObj.has("output") && !filterObj.has("result") && !filterObj.has("results")) {
                JsonObject adjusted = filterObj.deepCopy();
                adjusted.add("output", adjusted.get("target"));
                filterEl = adjusted;
            }
        }
        BiPredicate<Identifier, JsonObject> filter = parseFilter(filterEl);

        return switch (actionStr) {
            case "remove", "remove_recipe", "remove_output" -> new RecipeRule(RecipeRule.Action.REMOVE, filter);
            case "replace_input" -> {
                List<String> rawTargets = extractStrings(mod.get("target"));
                yield new RecipeRule(RecipeRule.Action.REPLACE_INPUT, filter, rawTargets, mod.get("replacement"));
            }
            case "replace_output" -> {
                List<String> rawTargets = mod.has("target") ? extractStrings(mod.get("target")) : List.of();
                JsonElement replacement = mod.has("replacement") ? mod.get("replacement") :
                        (mod.has("output") ? mod.get("output") : mod.get("result"));
                yield new RecipeRule(RecipeRule.Action.REPLACE_OUTPUT, filter, rawTargets, replacement);
            }
            default -> {
                Constants.LOG.warn("Unknown recipe action: {}", actionStr);
                yield null;
            }
        };
    }

    public static TagRule parseTagRule(JsonObject mod) {
        if (!mod.has("action")) {
            return null;
        }
        String actionStr = mod.get("action").getAsString();

        if (Set.of("remove", "remove_recipe", "remove_output", "replace_input", "replace_output", "prevent_repair", "set_repair_material", "add", "add_recipe").contains(actionStr)) {
            return null;
        }

        final Predicate<String> itemStringMatcher;
        JsonElement itemEl = mod.has("id") ? mod.get("id") : mod.get("items");
        if (itemEl != null) {
            itemStringMatcher = getStringMatcher(itemEl, false);
        } else {
            itemStringMatcher = s -> false;
        }
        Predicate<Identifier> itemMatcher = rl -> itemStringMatcher.test(rl.toString());

        final Predicate<String> tagStringMatcher;
        JsonElement tagEl = mod.has("tags") ? mod.get("tags") : mod.get("tag");
        if (tagEl != null) {
            tagStringMatcher = getStringMatcher(tagEl, false);
        } else {
            tagStringMatcher = s -> false;
        }
        Predicate<Identifier> tagMatcher = rl -> tagStringMatcher.test(rl.toString());

        return switch (actionStr) {
            case "remove_all_tags", "remove_tag" ->
                    new TagRule(TagRule.Action.REMOVE_ALL_TAGS, itemMatcher, rl -> false);
            case "remove_from_tag" -> new TagRule(TagRule.Action.REMOVE_FROM_TAG, itemMatcher, tagMatcher);
            case "clear_tag" -> new TagRule(TagRule.Action.CLEAR_TAG, rl -> false, tagMatcher);
            default -> throw new IllegalArgumentException("Unknown tag action: " + actionStr);
        };
    }

    private static BiPredicate<Identifier, JsonObject> parseFilter(JsonElement json) {
        if (json.isJsonObject()) {
            JsonObject obj = json.getAsJsonObject();
            BiPredicate<Identifier, JsonObject> combined = (id, recipe) -> true;

            for (String rawKey : obj.keySet()) {
                if (IGNORED_KEYS.contains(rawKey)) continue;

                String key = rawKey.trim().replaceAll(":$", "").trim();
                if (IGNORED_KEYS.contains(key)) continue;

                JsonElement criterion = obj.get(rawKey);
                BiPredicate<Identifier, JsonObject> check = switch (key) {
                    case "not" -> {
                        BiPredicate<Identifier, JsonObject> inner = parseFilter(criterion);
                        yield (id, recipe) -> !inner.test(id, recipe);
                    }
                    case "or" -> {
                        BiPredicate<Identifier, JsonObject> p = (id, recipe) -> false;
                        for (JsonElement e : criterion.getAsJsonArray()) {
                            BiPredicate<Identifier, JsonObject> inner = parseFilter(e);
                            BiPredicate<Identifier, JsonObject> currentP = p;
                            p = (id, recipe) -> currentP.test(id, recipe) || inner.test(id, recipe);
                        }
                        yield p;
                    }
                    case "and" -> {
                        BiPredicate<Identifier, JsonObject> p = (id, recipe) -> true;
                        for (JsonElement e : criterion.getAsJsonArray()) {
                            BiPredicate<Identifier, JsonObject> inner = parseFilter(e);
                            BiPredicate<Identifier, JsonObject> currentP = p;
                            p = (id, recipe) -> currentP.test(id, recipe) && inner.test(id, recipe);
                        }
                        yield p;
                    }
                    case "type" -> {
                        Predicate<String> m = getStringMatcher(criterion, false);
                        yield (id, recipe) -> matchesType(recipe, "type", m) || matchesType(recipe, SYNTHETIC_TYPE_KEY, m);
                    }
                    case "mod" -> {
                        Predicate<String> m = getStringMatcher(criterion, false);
                        yield (id, recipe) -> m.test(id.getNamespace());
                    }
                    case "id", "pattern", "patterns" -> {
                        Predicate<String> m = getStringMatcher(criterion, false);
                        yield (id, recipe) -> m.test(id.toString()) || m.test(id.getPath());
                    }
                    case "input", "reagent", "ingredient", "ingredients" -> {
                        Predicate<String> matcher = getStringMatcher(criterion, false);
                        yield (id, recipe) -> {
                            if (recipe.has(key) && jsonContainsValue(recipe.get(key), matcher)) return true;
                            return jsonContainsValueExcluding(recipe, matcher, NON_INPUT_KEYS);
                        };
                    }
                    case "output", "result", "results" -> {
                        Predicate<String> matcher = getStringMatcher(criterion, true);
                        yield (id, recipe) -> {
                            JsonElement res = recipe.has("result") ? recipe.get("result") :
                                    (recipe.has("results") ? recipe.get("results") :
                                            (recipe.has("output") ? recipe.get("output") : recipe.get(SYNTHETIC_RESULTS_KEY)));
                            return jsonContainsValue(res, matcher);
                        };
                    }
                    default -> {
                        Constants.LOG.warn("Unrecognized filter key '{}' in recipe rule JSON.", rawKey);
                        yield (id, recipe) -> false;
                    }
                };
                BiPredicate<Identifier, JsonObject> finalCombined = combined;
                combined = (id, recipe) -> finalCombined.test(id, recipe) && check.test(id, recipe);
            }
            return combined;
        }
        return (id, recipe) -> true;
    }

    private static boolean matchesType(JsonObject recipe, String key, Predicate<String> m) {
        if (!recipe.has(key) || !recipe.get(key).isJsonPrimitive()) return false;
        String typeStr = recipe.get(key).getAsString();
        if (m.test(typeStr)) return true;
        Identifier loc = Identifier.tryParse(typeStr);
        return loc != null && (m.test(loc.toString()) || m.test(loc.getPath()));
    }

    private static boolean jsonContainsValue(JsonElement element, Predicate<String> matcher) {
        return jsonContainsValueExcluding(element, matcher, Set.of());
    }

    private static boolean jsonContainsValueExcluding(JsonElement element, Predicate<String> matcher, Set<String> excludedKeys) {
        if (element == null) return false;
        if (element.isJsonPrimitive() && element.getAsJsonPrimitive().isString()) {
            return matcher.test(element.getAsString());
        }
        if (element.isJsonArray()) {
            for (JsonElement e : element.getAsJsonArray()) {
                if (jsonContainsValueExcluding(e, matcher, excludedKeys)) return true;
            }
        }
        if (element.isJsonObject()) {
            JsonObject obj = element.getAsJsonObject();
            if (obj.has("item") && obj.get("item").isJsonPrimitive() && matcher.test(obj.get("item").getAsString()))
                return true;
            if (obj.has("id") && obj.get("id").isJsonPrimitive() && matcher.test(obj.get("id").getAsString()))
                return true;
            if (obj.has("tag") && obj.get("tag").isJsonPrimitive() && matcher.test("#" + obj.get("tag").getAsString()))
                return true;

            for (Map.Entry<String, JsonElement> entry : obj.entrySet()) {
                if (excludedKeys.contains(entry.getKey())) continue;
                if (jsonContainsValueExcluding(entry.getValue(), matcher, excludedKeys)) return true;
            }
        }
        return false;
    }

    private static Predicate<String> getStringMatcher(JsonElement element, boolean autoExpandTags) {
        if (element.isJsonArray()) {
            Predicate<String> p = s -> false;
            for (JsonElement e : element.getAsJsonArray()) p = p.or(getStringMatcher(e, autoExpandTags));
            return p;
        }

        if (element.isJsonObject()) {
            JsonObject obj = element.getAsJsonObject();
            if (obj.has("tag")) {
                String tag = obj.get("tag").getAsString();
                if (!tag.startsWith("#") && !tag.startsWith("+#")) tag = "#" + tag;

                boolean expand = autoExpandTags;
                if (obj.has("expand")) {
                    expand = obj.get("expand").getAsBoolean();
                }
                if (expand && !tag.startsWith("+#")) {
                    tag = tag.replaceFirst("^#", "+#");
                }
                return getStringMatcher(new JsonPrimitive(tag), autoExpandTags);
            }
            return s -> false;
        }

        String str = element.getAsString();
        if (str.startsWith("/") && str.endsWith("/") && str.length() > 2) {
            try {
                Pattern pattern = Pattern.compile(str.substring(1, str.length() - 1));
                return s -> pattern.matcher(s).matches();
            } catch (Exception e) {
                Constants.LOG.warn("Invalid regex pattern in filter: {}", str);
                return s -> false;
            }
        }

        boolean expand = autoExpandTags;
        String matchStr = str;

        if (str.startsWith("+#")) {
            expand = true;
            matchStr = "#" + str.substring(2);
        }

        if (matchStr.startsWith("#")) {
            String tagPath = matchStr.substring(1);
            final boolean doExpand = expand;
            final String finalMatchStr = matchStr;

            return s -> {
                if (s.equals(finalMatchStr)) return true;
                if (s.equals(tagPath)) return true;

                if (doExpand && !s.startsWith("#")) {
                    try {
                        Identifier itemId = Identifier.tryParse(s);
                        if (itemId != null) {
                            Item item = CompatUtil.getItem(itemId);
                            if (item != Items.AIR) {
                                Identifier tagId = Identifier.tryParse(tagPath);
                                if (tagId != null) {
                                    return RecipeModifier.isItemInTag(item, tagId);
                                }
                            }
                        }
                    } catch (Exception ignored) {
                    }
                }
                return false;
            };
        }

        return str::equals;
    }

    private static List<String> extractStrings(JsonElement element) {
        List<String> list = new ArrayList<>();
        if (element == null) return list;
        if (element.isJsonPrimitive()) {
            list.add(element.getAsString());
        } else if (element.isJsonArray()) {
            for (JsonElement e : element.getAsJsonArray()) {
                if (e.isJsonPrimitive()) list.add(e.getAsString());
            }
        }
        return list;
    }

    public static Ingredient parseIngredient(JsonElement json) {
        if (json == null || json.isJsonNull()) return CompatUtil.emptyIngredient();

        if (json.isJsonPrimitive() && json.getAsJsonPrimitive().isString()) {
            return parseIngredientString(json.getAsString());
        }

        if (json.isJsonArray()) {
            List<Ingredient> list = new ArrayList<>();
            json.getAsJsonArray().forEach(e -> list.add(parseIngredient(e)));
            return mergeIngredients(list);
        }

        return Ingredient.CODEC.parse(JsonOps.INSTANCE, json).result().orElseGet(() -> {
            if (json.isJsonObject()) {
                JsonObject obj = json.getAsJsonObject();
                if (obj.has("id")) {
                    return parseIngredientString(obj.get("id").getAsString());
                }
                if (obj.has("item")) {
                    return parseIngredientString(obj.get("item").getAsString());
                }
                if (obj.has("tag")) {
                    String tag = obj.get("tag").getAsString();
                    return parseIngredientString(tag.startsWith("#") ? tag : "#" + tag);
                }
            }
            return CompatUtil.emptyIngredient();
        });
    }

    public static Ingredient mergeIngredients(Collection<Ingredient> ingredients) {
        if (ingredients == null || ingredients.isEmpty()) return CompatUtil.emptyIngredient();
        if (ingredients.size() == 1) return ingredients.iterator().next();

        //? if <1.21.2 {
        /*JsonArray array = new JsonArray();
        Set<JsonElement> seen = new HashSet<>();
        for (Ingredient ing : ingredients) {
            if (ing == null || ing.isEmpty()) continue;
            Ingredient.CODEC.encodeStart(JsonOps.INSTANCE, ing).result().ifPresent(json -> {
                if (json.isJsonArray()) {
                    for (JsonElement elem : json.getAsJsonArray()) {
                        if (seen.add(elem)) {
                            array.add(elem);
                        }
                    }
                } else {
                    if (seen.add(json)) {
                        array.add(json);
                    }
                }
            });
        }

        if (array.isEmpty()) return Ingredient.EMPTY;
        return Ingredient.CODEC.parse(JsonOps.INSTANCE, array).result().orElseGet(() -> {
            List<ItemStack> stacks = new ArrayList<>();
            for (Ingredient ing : ingredients) {
                if (ing != null && !ing.isEmpty()) {
                    stacks.addAll(Arrays.asList(ing.getItems()));
                }
            }
            return stacks.isEmpty() ? Ingredient.EMPTY : Ingredient.of(stacks.stream());
        });
        *///?} else {
        Set<Item> items = new LinkedHashSet<>();
        for (Ingredient ing : ingredients) {
            if (ing != null && !ing.isEmpty()) {
                items.addAll(CompatUtil.ingredientItems(ing));
            }
        }
        return CompatUtil.ingredientOf(items);
        //?}
    }

    public static Ingredient parseIngredientString(String str) {
        if (str == null || str.isBlank()) return CompatUtil.emptyIngredient();
        str = str.trim();

        if (str.startsWith("/") && str.endsWith("/") && str.length() > 2) {
            try {
                Pattern pattern = Pattern.compile(str.substring(1, str.length() - 1));
                List<Item> matchingItems = new ArrayList<>();

                for (Item item : BuiltInRegistries.ITEM) {
                    Identifier key = BuiltInRegistries.ITEM.getKey(item);
                    if (pattern.matcher(key.toString()).matches() || pattern.matcher(key.getPath()).matches()) {
                        matchingItems.add(item);
                    }
                }

                return CompatUtil.ingredientOf(matchingItems);
            } catch (Exception e) {
                Constants.LOG.warn("Invalid regex pattern in ingredient: {}", str);
                return CompatUtil.emptyIngredient();
            }
        }

        if (str.startsWith("#") || str.startsWith("tag:")) {
            String tagPath = str.startsWith("#") ? str.substring(1) : str.substring(4);
            Identifier tagLoc = Identifier.tryParse(tagPath);
            return tagLoc != null ? CompatUtil.ingredientOf(TagKey.create(Registries.ITEM, tagLoc)) : CompatUtil.emptyIngredient();
        }

        if (str.startsWith("item:")) {
            String itemPath = str.substring(5);
            Identifier itemLoc = Identifier.tryParse(itemPath);
            if (itemLoc != null) {
                Item item = CompatUtil.getItem(itemLoc);
                if (item != Items.AIR) {
                    return CompatUtil.ingredientOf(List.of(item));
                }
            }
            return CompatUtil.emptyIngredient();
        }

        Identifier loc = Identifier.tryParse(str);
        if (loc != null) {
            if (BuiltInRegistries.ITEM.containsKey(loc)) {
                Item item = CompatUtil.getItem(loc);
                if (item != Items.AIR) {
                    return CompatUtil.ingredientOf(List.of(item));
                }
            }
            return CompatUtil.ingredientOf(TagKey.create(Registries.ITEM, loc));
        }

        return CompatUtil.emptyIngredient();
    }
}
