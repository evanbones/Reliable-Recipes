package com.evandev.reliable_recipes.config;

import com.evandev.reliable_recipes.Constants;
import com.evandev.reliable_recipes.recipe.RecipeRule;
import com.evandev.reliable_recipes.recipe.TagRule;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
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
            return new RecipeRule(RecipeRule.Action.PREVENT_REPAIR, r -> false, target, Ingredient.EMPTY);
        }

        Predicate<RecipeHolder<?>> filter = mod.has("filter") ? parseFilter(mod.get("filter")) : parseFilter(mod);

        return switch (actionStr) {
            case "remove", "remove_recipe" -> new RecipeRule(RecipeRule.Action.REMOVE, filter);
            case "replace_input" -> {
                Ingredient target = parseIngredient(mod.get("target"));
                Ingredient replace = parseIngredient(mod.get("replacement"));
                yield new RecipeRule(RecipeRule.Action.REPLACE_INPUT, filter, target, replace);
            }
            case "replace_output" -> {
                String idStr = mod.get("replacement").getAsString();
                Item item = BuiltInRegistries.ITEM.get(ResourceLocation.parse(idStr));
                if (item == Items.AIR) {
                    Constants.LOG.warn("Skipping rule: Invalid replacement item '{}'.", idStr);
                    yield null;
                }
                yield new RecipeRule(RecipeRule.Action.REPLACE_OUTPUT, filter, new ItemStack(item));
            }
            default -> {
                Constants.LOG.warn("Unknown recipe action: {}", actionStr);
                yield null;
            }
        };
    }

    public static TagRule parseTagRule(JsonObject mod) {
        String actionStr = mod.has("action") ? mod.get("action").getAsString() : "unknown";

        if (Set.of("remove", "remove_recipe", "replace_input", "replace_output", "prevent_repair").contains(actionStr)) {
            return null;
        }

        List<ResourceLocation> items = new ArrayList<>();
        JsonElement itemEl = mod.has("id") ? mod.get("id") : mod.get("items");
        if (itemEl != null) {
            if (itemEl.isJsonArray())
                itemEl.getAsJsonArray().forEach(e -> items.add(ResourceLocation.parse(e.getAsString())));
            else items.add(ResourceLocation.parse(itemEl.getAsString()));
        }

        List<ResourceLocation> tags = new ArrayList<>();
        if (mod.has("tags")) {
            mod.get("tags").getAsJsonArray().forEach(e -> tags.add(ResourceLocation.parse(e.getAsString())));
        } else if (mod.has("tag")) {
            tags.add(ResourceLocation.parse(mod.get("tag").getAsString()));
        }

        return switch (actionStr) {
            case "remove_all_tags", "remove_tag" -> new TagRule(TagRule.Action.REMOVE_ALL_TAGS, items, null);
            case "remove_from_tag" -> new TagRule(TagRule.Action.REMOVE_FROM_TAG, items, tags);
            case "clear_tag" -> new TagRule(TagRule.Action.CLEAR_TAG, null, tags);
            default -> throw new IllegalArgumentException("Unknown tag action: " + actionStr);
        };
    }

    private static Predicate<RecipeHolder<?>> parseFilter(JsonElement json) {
        if (json.isJsonObject()) {
            JsonObject obj = json.getAsJsonObject();

            Predicate<RecipeHolder<?>> combined = r -> true;
            for (String key : obj.keySet()) {
                if (IGNORED_KEYS.contains(key)) continue;

                JsonElement criterion = obj.get(key);
                Predicate<RecipeHolder<?>> check = switch (key) {
                    case "not" -> parseFilter(criterion).negate();
                    case "or" -> {
                        Predicate<RecipeHolder<?>> p = r -> false;
                        for (JsonElement e : criterion.getAsJsonArray()) p = p.or(parseFilter(e));
                        yield p;
                    }
                    case "and" -> {
                        Predicate<RecipeHolder<?>> p = r -> true;
                        for (JsonElement e : criterion.getAsJsonArray()) p = p.and(parseFilter(e));
                        yield p;
                    }
                    case "type" -> {
                        Predicate<String> m = getStringMatcher(criterion);
                        yield r -> {
                            ResourceLocation typeId = BuiltInRegistries.RECIPE_TYPE.getKey(r.value().getType());
                            return typeId != null && m.test(typeId.toString());
                        };
                    }
                    case "mod" -> {
                        Predicate<String> m = getStringMatcher(criterion);
                        yield r -> m.test(r.id().getNamespace());
                    }
                    case "id", "pattern", "patterns" -> {
                        Predicate<String> m = getStringMatcher(criterion);
                        yield r -> m.test(r.id().toString());
                    }
                    case "input" -> {
                        Predicate<ItemStack> matcher = getItemStackMatcher(criterion);
                        yield r -> r.value().getIngredients().stream().anyMatch(ing -> {
                            for (ItemStack stack : ing.getItems()) {
                                if (matcher.test(stack)) return true;
                            }
                            return false;
                        });
                    }
                    case "output" -> {
                        Predicate<ItemStack> m = getItemStackMatcher(criterion);
                        yield r -> {
                            try {
                                ItemStack out = r.value().getResultItem(RegistryAccess.EMPTY);
                                if (out.isEmpty()) return false;
                                return m.test(out);
                            } catch (Exception e) {
                                return false;
                            }
                        };
                    }
                    default -> r -> true;
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

    private static Predicate<ItemStack> getItemStackMatcher(JsonElement element) {
        if (element.isJsonArray()) {
            Predicate<ItemStack> p = s -> false;
            for (JsonElement e : element.getAsJsonArray()) p = p.or(getItemStackMatcher(e));
            return p;
        }
        String str = element.getAsString();

        if (str.startsWith("#")) {
            TagKey<Item> tagKey = TagKey.create(Registries.ITEM, ResourceLocation.parse(str.substring(1)));
            return stack -> stack.is(tagKey);
        } else if (str.startsWith("/") && str.endsWith("/") && str.length() > 2) {
            try {
                Pattern pattern = Pattern.compile(str.substring(1, str.length() - 1));
                return stack -> {
                    ResourceLocation key = BuiltInRegistries.ITEM.getKey(stack.getItem());
                    return pattern.matcher(key.toString()).matches();
                };
            } catch (Exception e) {
                Constants.LOG.warn("Invalid regex pattern in filter: {}", str);
                return s -> false;
            }
        }
        return stack -> {
            ResourceLocation key = BuiltInRegistries.ITEM.getKey(stack.getItem());
            return str.equals(key.toString());
        };
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
        if (ingredients.size() == 1) return ingredients.getFirst();

        List<ItemStack> allStacks = new ArrayList<>();
        for (Ingredient ing : ingredients) {
            allStacks.addAll(Arrays.asList(ing.getItems()));
        }
        return Ingredient.of(allStacks.toArray(new ItemStack[0]));
    }

    private static Ingredient parseIngredientString(String str) {
        if (str.startsWith("#")) {
            return Ingredient.of(TagKey.create(Registries.ITEM, ResourceLocation.parse(str.substring(1))));
        }
        Item item = BuiltInRegistries.ITEM.get(ResourceLocation.parse(str));
        return item != Items.AIR ? Ingredient.of(item) : Ingredient.EMPTY;
    }
}