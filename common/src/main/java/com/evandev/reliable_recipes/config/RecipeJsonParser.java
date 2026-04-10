package com.evandev.reliable_recipes.config;

import com.evandev.reliable_recipes.Constants;
import com.evandev.reliable_recipes.api.ReliableRecipesAPI;
import com.evandev.reliable_recipes.recipe.RecipeRule;
import com.evandev.reliable_recipes.recipe.TagRule;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.ItemLike;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
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

        Predicate<RecipeHolder<?>> filter = mod.has("filter") ? parseFilter(mod.get("filter")) : parseFilter(mod);

        return switch (actionStr) {
            case "remove", "remove_recipe" -> new RecipeRule(RecipeRule.Action.REMOVE, filter);
            case "prevent_repair" -> {
                Optional<Ingredient> target = parseIngredient(mod.get("target"));
                yield new RecipeRule(RecipeRule.Action.PREVENT_REPAIR, r -> false, target);
            }
            case "replace_input" -> {
                String target = mod.get("target").getAsString();
                String replace = mod.get("replacement").getAsString();
                yield new RecipeRule(RecipeRule.Action.REPLACE_INPUT, filter, target, replace);
            }
            case "replace_output" -> {
                String replace = mod.get("replacement").getAsString();
                String target = mod.has("target") ? mod.get("target").getAsString() : "";
                if (target.isEmpty() && mod.has("filter") && mod.getAsJsonObject("filter").has("id")) {
                    target = mod.getAsJsonObject("filter").get("id").getAsString();
                }
                yield new RecipeRule(RecipeRule.Action.REPLACE_OUTPUT, filter, target, replace);
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

        List<Identifier> items = new ArrayList<>();
        JsonElement itemEl = mod.has("id") ? mod.get("id") : mod.get("items");
        if (itemEl != null) {
            if (itemEl.isJsonArray())
                itemEl.getAsJsonArray().forEach(e -> items.add(Identifier.parse(e.getAsString())));
            else items.add(Identifier.parse(itemEl.getAsString()));
        }

        List<Identifier> tags = new ArrayList<>();
        if (mod.has("tags")) {
            mod.get("tags").getAsJsonArray().forEach(e -> tags.add(Identifier.parse(e.getAsString())));
        } else if (mod.has("tag")) {
            tags.add(Identifier.parse(mod.get("tag").getAsString()));
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
                            Identifier serializerId = BuiltInRegistries.RECIPE_SERIALIZER.getKey(r.value().getSerializer());
                            if (serializerId != null && m.test(serializerId.toString())) {
                                return true;
                            }

                            Identifier typeId = BuiltInRegistries.RECIPE_TYPE.getKey(r.value().getType());
                            return typeId != null && m.test(typeId.toString());
                        };
                    }
                    case "mod" -> {
                        Predicate<String> m = getStringMatcher(criterion);
                        yield r -> m.test(r.id().identifier().getNamespace());
                    }
                    case "id", "pattern", "patterns" -> {
                        Predicate<String> m = getStringMatcher(criterion);
                        yield r -> m.test(r.id().identifier().toString());
                    }
                    case "input" -> {
                        Predicate<String> matcher = getStringMatcher(criterion);
                        yield r -> r.value().placementInfo().ingredients().stream().anyMatch(ing -> {
                            return ing.items().anyMatch(holder -> {
                                Identifier id = BuiltInRegistries.ITEM.getKey(holder.value());
                                return matcher.test(id.toString());
                            });
                        });
                    }
                    case "output" -> {
                        Predicate<String> m = getStringMatcher(criterion);
                        yield r -> {
                            try {
                                List<ItemStack> outputs = ReliableRecipesAPI.getRecipeResults(r.value());
                                if (outputs.isEmpty()) return false;
                                return outputs.stream().anyMatch(out -> {
                                    Identifier id = BuiltInRegistries.ITEM.getKey(out.getItem());
                                    return m.test(id.toString());
                                });
                            } catch (Exception e) {
                                Constants.LOG.error("Failed to parse output for filter on recipe {}: {}", r.id().identifier(), e.getMessage());
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

    private static Optional<Ingredient> parseIngredient(JsonElement json) {
        if (json == null) return Optional.empty();
        if (json.isJsonArray()) {
            List<ItemLike> list = new ArrayList<>();
            json.getAsJsonArray().forEach(e -> {
                parseIngredientString(e.getAsString()).ifPresent(ing -> {
                    ing.items().forEach(holder -> list.add(holder.value()));
                });
            });
            return list.isEmpty() ? Optional.empty() : Optional.of(Ingredient.of(list.stream()));
        }
        return parseIngredientString(json.getAsString());
    }

    private static Optional<Ingredient> parseIngredientString(String str) {
        if (str.startsWith("#")) {
            TagKey<Item> tagKey = TagKey.create(Registries.ITEM, Identifier.parse(str.substring(1)));
            var tagOpt = BuiltInRegistries.ITEM.get(tagKey);
            return tagOpt.map(Ingredient::of);
        }

        Item item = BuiltInRegistries.ITEM.get(Identifier.parse(str)).map(Holder.Reference::value).orElse(Items.AIR);
        return item != Items.AIR ? Optional.of(Ingredient.of(item)) : Optional.empty();
    }
}