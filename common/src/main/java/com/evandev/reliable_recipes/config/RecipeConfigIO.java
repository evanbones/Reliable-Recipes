package com.evandev.reliable_recipes.config;

import com.evandev.reliable_recipes.Constants;
import com.evandev.reliable_recipes.platform.Services;
import com.evandev.reliable_recipes.recipe.RecipeRule;
import com.evandev.reliable_recipes.recipe.TagRule;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class RecipeConfigIO {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_PATH = Services.PLATFORM.getConfigDirectory().resolve("reliable-recipes.json");

    public static List<RecipeRule> loadRules() {
        List<RecipeRule> rules = new ArrayList<>();
        JsonObject config = loadJson();

        if (config == null || !config.has("modifications")) return rules;

        for (JsonElement element : config.getAsJsonArray("modifications")) {
            try {
                if (!element.isJsonObject()) continue;
                RecipeRule rule = RecipeJsonParser.parseRule(element.getAsJsonObject());
                if (rule != null) rules.add(rule);
            } catch (Exception e) {
                Constants.LOG.error("Failed to parse recipe rule: {}", element, e);
            }
        }
        return rules;
    }

    public static List<TagRule> loadTagRules() {
        List<TagRule> rules = new ArrayList<>();
        JsonObject config = loadJson();

        if (config == null || !config.has("tag_modifications")) return rules;

        for (JsonElement element : config.getAsJsonArray("tag_modifications")) {
            try {
                if (!element.isJsonObject()) continue;
                rules.add(RecipeJsonParser.parseTagRule(element.getAsJsonObject()));
            } catch (Exception e) {
                Constants.LOG.error("Failed to parse tag rule: {}", element, e);
            }
        }
        return rules;
    }

    private static JsonObject loadJson() {
        if (!CONFIG_PATH.toFile().exists()) createDefault();
        try (FileReader reader = new FileReader(CONFIG_PATH.toFile())) {
            return GSON.fromJson(reader, JsonObject.class);
        } catch (Exception e) {
            Constants.LOG.error("Failed to load recipe config JSON", e);
            return null;
        }
    }

    private static void createDefault() {
        JsonObject root = new JsonObject();
        JsonArray modifications = new JsonArray();
        JsonObject removeExample = new JsonObject();
        removeExample.addProperty("action", "remove");
        JsonObject filter = new JsonObject();
        filter.addProperty("output", "minecraft:stone_pickaxe");
        removeExample.add("filter", filter);
        modifications.add(removeExample);
        root.add("modifications", modifications);

        JsonArray tagModifications = new JsonArray();
        JsonObject tagRemoveExample = new JsonObject();
        tagRemoveExample.addProperty("action", "remove_all_tags");
        JsonArray items = new JsonArray();
        items.add("minecraft:wooden_hoe");
        tagRemoveExample.add("items", items);
        tagModifications.add(tagRemoveExample);
        root.add("tag_modifications", tagModifications);

        try (FileWriter writer = new FileWriter(CONFIG_PATH.toFile())) {
            GSON.toJson(root, writer);
        } catch (IOException e) {
            Constants.LOG.error("Failed to create default recipe config: {}", CONFIG_PATH, e);
        }
    }
}