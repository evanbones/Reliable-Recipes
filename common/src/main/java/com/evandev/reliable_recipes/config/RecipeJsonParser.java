package com.evandev.reliable_recipes.config;

import com.evandev.reliable_recipes.Constants;
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
import net.minecraft.world.item.crafting.Recipe;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;
import java.util.regex.Pattern;

/**
 * Handles the logic of converting JSON elements into Recipe Rules and Filters.
 * Pure logic, no file I/O.
 */
public class RecipeJsonParser {

    public static RecipeRule parseRule(JsonObject mod) {
        String actionStr = mod.has("action") ? mod.get("action").getAsString() : "unknown";

        if (actionStr.equals("prevent_repair")) {
            Ingredient target = parseIngredient(mod.get("target"));
            return new RecipeRule(RecipeRule.Action.PREVENT_REPAIR, r -> false, target, Ingredient.EMPTY);
        }

        if (!mod.has("filter")) throw new IllegalArgumentException("Missing filter");

        Predicate<Recipe<?>> filter = parseFilter(mod.get("filter"));

        return switch (actionStr) {
            case "remove" -> new RecipeRule(RecipeRule.Action.REMOVE, filter);
            case "replace_input" -> {
                Ingredient target = parseIngredient(mod.get("target"));
                Ingredient replace = parseIngredient(mod.get("replacement"));
                yield new RecipeRule(RecipeRule.Action.REPLACE_INPUT, filter, target, replace);
            }
            case "replace_output" -> {
                String idStr = mod.get("replacement").getAsString();
                Item item = BuiltInRegistries.ITEM.get(new ResourceLocation(idStr));
                if (item == Items.AIR) {
                    Constants.LOG.warn("Skipping rule: Invalid replacement item '{}'.", idStr);
                    yield null;
                }
                yield new RecipeRule(RecipeRule.Action.REPLACE_OUTPUT, filter, new ItemStack(item));
            }
            default -> throw new IllegalArgumentException("Unknown action: " + actionStr);
        };
    }

    public static TagRule parseTagRule(JsonObject mod) {
        String actionStr = mod.has("action") ? mod.get("action").getAsString() : "unknown";

        List<ResourceLocation> items = new ArrayList<>();
        if (mod.has("items")) {
            JsonElement el = mod.get("items");
            if (el.isJsonArray()) el.getAsJsonArray().forEach(e -> items.add(new ResourceLocation(e.getAsString())));
            else items.add(new ResourceLocation(el.getAsString()));
        }

        List<ResourceLocation> tags = new ArrayList<>();
        if (mod.has("tags")) {
            mod.get("tags").getAsJsonArray().forEach(e -> tags.add(new ResourceLocation(e.getAsString())));
        } else if (mod.has("tag")) {
            tags.add(new ResourceLocation(mod.get("tag").getAsString()));
        }

        return switch (actionStr) {
            case "remove_all_tags" -> new TagRule(TagRule.Action.REMOVE_ALL_TAGS, items, null);
            case "remove_from_tag" -> new TagRule(TagRule.Action.REMOVE_FROM_TAG, items, tags);
            case "clear_tag" -> new TagRule(TagRule.Action.CLEAR_TAG, null, tags);
            default -> throw new IllegalArgumentException("Unknown tag action: " + actionStr);
        };
    }

    private static Predicate<Recipe<?>> parseFilter(JsonElement json) {
        if (json.isJsonObject()) {
            JsonObject obj = json.getAsJsonObject();

            Predicate<Recipe<?>> combined = r -> true;
            for (String key : obj.keySet()) {
                JsonElement criterion = obj.get(key);
                Predicate<Recipe<?>> check = switch (key) {
                    case "not" -> parseFilter(criterion).negate();
                    case "or" -> {
                        Predicate<Recipe<?>> p = r -> false;
                        for (JsonElement e : criterion.getAsJsonArray()) p = p.or(parseFilter(e));
                        yield p;
                    }
                    case "and" -> {
                        Predicate<Recipe<?>> p = r -> true;
                        for (JsonElement e : criterion.getAsJsonArray()) p = p.and(parseFilter(e));
                        yield p;
                    }
                    case "type" -> {
                        Predicate<String> m = getStringMatcher(criterion);
                        yield r -> {
                            ResourceLocation typeId = BuiltInRegistries.RECIPE_TYPE.getKey(r.getType());
                            return typeId != null && m.test(typeId.toString());
                        };
                    }
                    case "mod" -> {
                        Predicate<String> m = getStringMatcher(criterion);
                        yield r -> m.test(r.getId().getNamespace());
                    }
                    case "id" -> {
                        Predicate<String> m = getStringMatcher(criterion);
                        yield r -> m.test(r.getId().toString());
                    }
                    case "input" -> {
                        Predicate<String> matcher = getStringMatcher(criterion);
                        yield r -> r.getIngredients().stream().anyMatch(ing -> {
                            for (ItemStack stack : ing.getItems()) {
                                ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
                                if (matcher.test(id.toString())) return true;
                            }
                            return false;
                        });
                    }
                    case "output" -> {
                        Predicate<String> m = getStringMatcher(criterion);
                        yield r -> {
                            try {
                                ItemStack out = r.getResultItem(net.minecraft.core.RegistryAccess.EMPTY);
                                if (out.isEmpty()) return false;
                                ResourceLocation id = BuiltInRegistries.ITEM.getKey(out.getItem());
                                return m.test(id.toString());
                            } catch (Exception e) {
                                return false;
                            }
                        };
                    }
                    default -> throw new IllegalArgumentException("Unknown filter key: " + key);
                };
                combined = combined.and(check);
            }
            return combined;
        }
        return r -> true;
    }

    private static Predicate<String> getStringMatcher(JsonElement element) {
        if (element.isJsonArray()) {
            Predicate<String> p = s -> false;
            for (JsonElement e : element.getAsJsonArray()) p = p.or(getStringMatcher(e));
            return p;
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
        return str::equals;
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
        Item item = BuiltInRegistries.ITEM.get(new ResourceLocation(str));
        return item != Items.AIR ? Ingredient.of(item) : Ingredient.EMPTY;
    }
}