package com.evandev.reliable_recipes.config;

import com.evandev.reliable_recipes.Constants;
import com.evandev.reliable_recipes.recipe.RecipeModifier;
import com.evandev.reliable_recipes.recipe.RecipeRule;
import com.evandev.reliable_recipes.recipe.TagRule;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;

import java.util.*;
import java.util.function.BiPredicate;
import java.util.function.Predicate;
import java.util.regex.Pattern;

public class RecipeJsonParser {

    private static final Set<String> IGNORED_KEYS = Set.of(
            "action", "target", "replacement", "items", "tags", "tag", "filter"
    );

    public static RecipeRule parseRule(JsonObject mod) {
        String actionStr = mod.has("action") ? mod.get("action").getAsString() : "unknown";

        if (Set.of("remove_all_tags", "remove_tag", "remove_from_tag", "clear_tag").contains(actionStr)) {
            return null;
        }

        if (actionStr.equals("prevent_repair")) {
            Ingredient target = parseIngredient(mod.get("target"));
            return new RecipeRule(RecipeRule.Action.PREVENT_REPAIR, (id, r) -> false, target, Ingredient.EMPTY);
        }

        BiPredicate<ResourceLocation, JsonObject> filter = mod.has("filter") ? parseFilter(mod.get("filter")) : parseFilter(mod);

        return switch (actionStr) {
            case "remove", "remove_recipe" -> new RecipeRule(RecipeRule.Action.REMOVE, filter);
            case "replace_input" -> {
                List<String> rawTargets = extractStrings(mod.get("target"));
                yield new RecipeRule(RecipeRule.Action.REPLACE_INPUT, filter, rawTargets, mod.get("replacement"));
            }
            case "replace_output" -> {
                List<String> rawTargets = mod.has("target") ? extractStrings(mod.get("target")) : List.of();
                yield new RecipeRule(RecipeRule.Action.REPLACE_OUTPUT, filter, rawTargets, mod.get("replacement"));
            }
            case "set_repair_material" -> {
                Ingredient target = parseIngredient(mod.get("target"));
                JsonElement matEl = mod.has("material") ? mod.get("material") : mod.get("replacement");
                Ingredient material = parseIngredient(matEl);
                yield new RecipeRule(RecipeRule.Action.SET_REPAIR_MATERIAL, (id, r) -> false, target, material);
            }
            default -> {
                Constants.LOG.warn("Unknown recipe action: {}", actionStr);
                yield null;
            }
        };
    }

    public static TagRule parseTagRule(JsonObject mod) {
        String actionStr = mod.has("action") ? mod.get("action").getAsString() : "unknown";

        if (Set.of("remove", "remove_recipe", "replace_input", "replace_output", "prevent_repair", "set_repair_material").contains(actionStr)) {
            return null;
        }

        final Predicate<String> itemStringMatcher;
        JsonElement itemEl = mod.has("id") ? mod.get("id") : mod.get("items");
        if (itemEl != null) {
            itemStringMatcher = getStringMatcher(itemEl, false);
        } else {
            itemStringMatcher = s -> false;
        }
        Predicate<ResourceLocation> itemMatcher = rl -> itemStringMatcher.test(rl.toString());

        final Predicate<String> tagStringMatcher;
        JsonElement tagEl = mod.has("tags") ? mod.get("tags") : mod.get("tag");
        if (tagEl != null) {
            tagStringMatcher = getStringMatcher(tagEl, false);
        } else {
            tagStringMatcher = s -> false;
        }
        Predicate<ResourceLocation> tagMatcher = rl -> tagStringMatcher.test(rl.toString());

        return switch (actionStr) {
            case "remove_all_tags", "remove_tag" ->
                    new TagRule(TagRule.Action.REMOVE_ALL_TAGS, itemMatcher, rl -> false);
            case "remove_from_tag" -> new TagRule(TagRule.Action.REMOVE_FROM_TAG, itemMatcher, tagMatcher);
            case "clear_tag" -> new TagRule(TagRule.Action.CLEAR_TAG, rl -> false, tagMatcher);
            default -> throw new IllegalArgumentException("Unknown tag action: " + actionStr);
        };
    }

    private static BiPredicate<ResourceLocation, JsonObject> parseFilter(JsonElement json) {
        if (json.isJsonObject()) {
            JsonObject obj = json.getAsJsonObject();
            BiPredicate<ResourceLocation, JsonObject> combined = (id, recipe) -> true;

            for (String key : obj.keySet()) {
                if (IGNORED_KEYS.contains(key)) continue;

                JsonElement criterion = obj.get(key);
                BiPredicate<ResourceLocation, JsonObject> check = switch (key) {
                    case "not" -> {
                        BiPredicate<ResourceLocation, JsonObject> inner = parseFilter(criterion);
                        yield (id, recipe) -> !inner.test(id, recipe);
                    }
                    case "or" -> {
                        BiPredicate<ResourceLocation, JsonObject> p = (id, recipe) -> false;
                        for (JsonElement e : criterion.getAsJsonArray()) {
                            BiPredicate<ResourceLocation, JsonObject> inner = parseFilter(e);
                            BiPredicate<ResourceLocation, JsonObject> currentP = p;
                            p = (id, recipe) -> currentP.test(id, recipe) || inner.test(id, recipe);
                        }
                        yield p;
                    }
                    case "and" -> {
                        BiPredicate<ResourceLocation, JsonObject> p = (id, recipe) -> true;
                        for (JsonElement e : criterion.getAsJsonArray()) {
                            BiPredicate<ResourceLocation, JsonObject> inner = parseFilter(e);
                            BiPredicate<ResourceLocation, JsonObject> currentP = p;
                            p = (id, recipe) -> currentP.test(id, recipe) && inner.test(id, recipe);
                        }
                        yield p;
                    }
                    case "type" -> {
                        Predicate<String> m = getStringMatcher(criterion, false);
                        yield (id, recipe) -> recipe.has("type") && m.test(recipe.get("type").getAsString());
                    }
                    case "mod" -> {
                        Predicate<String> m = getStringMatcher(criterion, false);
                        yield (id, recipe) -> m.test(id.getNamespace());
                    }
                    case "id", "pattern", "patterns" -> {
                        Predicate<String> m = getStringMatcher(criterion, false);
                        yield (id, recipe) -> m.test(id.toString());
                    }
                    case "input" -> {
                        Predicate<String> matcher = getStringMatcher(criterion, false);
                        yield (id, recipe) -> jsonContainsValue(recipe, matcher);
                    }
                    case "output" -> {
                        Predicate<String> matcher = getStringMatcher(criterion, true);
                        yield (id, recipe) -> {
                            JsonElement res = recipe.has("result") ? recipe.get("result") :
                                    (recipe.has("results") ? recipe.get("results") :
                                            (recipe.has("output") ? recipe.get("output") :
                                                    (recipe.has("outputs") ? recipe.get("outputs") : null)));
                            return jsonContainsValue(res, matcher);
                        };
                    }
                    default -> (id, recipe) -> true;
                };
                BiPredicate<ResourceLocation, JsonObject> finalCombined = combined;
                combined = (id, recipe) -> finalCombined.test(id, recipe) && check.test(id, recipe);
            }
            return combined;
        }
        return (id, recipe) -> true;
    }

    private static boolean jsonContainsValue(JsonElement element, Predicate<String> matcher) {
        if (element == null) return false;
        if (element.isJsonPrimitive() && element.getAsJsonPrimitive().isString()) {
            return matcher.test(element.getAsString());
        }
        if (element.isJsonArray()) {
            for (JsonElement e : element.getAsJsonArray()) {
                if (jsonContainsValue(e, matcher)) return true;
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
                if (jsonContainsValue(entry.getValue(), matcher)) return true;
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
                return getStringMatcher(new com.google.gson.JsonPrimitive(tag), autoExpandTags);
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
                        ResourceLocation itemId = ResourceLocation.tryParse(s);
                        if (itemId != null) {
                            Item item = BuiltInRegistries.ITEM.get(itemId);
                            if (item != Items.AIR) {
                                ResourceLocation tagId = ResourceLocation.tryParse(tagPath);
                                if (tagId != null) {
                                    return RecipeModifier.isItemInTag(item, tagId);
                                }
                            }
                        }
                    } catch (Exception e) {
                        // Ignore malformed ResourceLocations during tag expansion
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

    private static Ingredient parseIngredient(JsonElement json) {
        if (json == null) return Ingredient.EMPTY;
        if (json.isJsonArray()) {
            List<Ingredient> list = new ArrayList<>();
            json.getAsJsonArray().forEach(e -> list.add(parseIngredientString(e.getAsString())));
            return mergeIngredients(list);
        }
        return parseIngredientString(json.getAsString());
    }

    private static Ingredient mergeIngredients(List<Ingredient> ingredients) {
        if (ingredients.isEmpty()) return Ingredient.EMPTY;
        if (ingredients.size() == 1) return ingredients.get(0);

        List<ItemStack> allStacks = new ArrayList<>();
        for (Ingredient ing : ingredients) {
            allStacks.addAll(Arrays.asList(ing.getItems()));
        }
        return Ingredient.of(allStacks.toArray(new ItemStack[0]));
    }

    private static Ingredient parseIngredientString(String str) {
        if (str.startsWith("#")) {
            return Ingredient.of(TagKey.create(Registries.ITEM, new ResourceLocation(str.substring(1))));
        }

        if (str.startsWith("/") && str.endsWith("/") && str.length() > 2) {
            try {
                Pattern pattern = Pattern.compile(str.substring(1, str.length() - 1));
                List<ItemStack> matchingItems = new ArrayList<>();

                for (Item item : BuiltInRegistries.ITEM) {
                    ResourceLocation key = BuiltInRegistries.ITEM.getKey(item);
                    if (pattern.matcher(key.toString()).matches()) {
                        matchingItems.add(new ItemStack(item));
                    }
                }

                return matchingItems.isEmpty() ? Ingredient.EMPTY : Ingredient.of(matchingItems.stream());
            } catch (Exception e) {
                Constants.LOG.warn("Invalid regex pattern in ingredient: {}", str);
                return Ingredient.EMPTY;
            }
        }

        Item item = BuiltInRegistries.ITEM.get(new ResourceLocation(str));
        return item != Items.AIR ? Ingredient.of(item) : Ingredient.EMPTY;
    }
}