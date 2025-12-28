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

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class RecipeConfigIO {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_DIR = Services.PLATFORM.getConfigDirectory().resolve("reliable_recipes");

    public static List<RecipeRule> loadRules() {
        List<RecipeRule> rules = new ArrayList<>();
        List<JsonObject> configs = loadAllConfigs();

        for (JsonObject config : configs) {
            if (!config.has("recipe_modifications")) continue;

            for (JsonElement element : config.getAsJsonArray("recipe_modifications")) {
                try {
                    if (!element.isJsonObject()) continue;
                    RecipeRule rule = RecipeJsonParser.parseRule(element.getAsJsonObject());
                    if (rule != null) rules.add(rule);
                } catch (Exception e) {
                    Constants.LOG.error("Failed to parse recipe rule: {}", element, e);
                }
            }
        }
        return rules;
    }

    public static List<TagRule> loadTagRules() {
        List<TagRule> rules = new ArrayList<>();
        List<JsonObject> configs = loadAllConfigs();

        for (JsonObject config : configs) {
            if (!config.has("tag_modifications")) continue;

            for (JsonElement element : config.getAsJsonArray("tag_modifications")) {
                try {
                    if (!element.isJsonObject()) continue;
                    rules.add(RecipeJsonParser.parseTagRule(element.getAsJsonObject()));
                } catch (Exception e) {
                    Constants.LOG.error("Failed to parse tag rule: {}", element, e);
                }
            }
        }
        return rules;
    }

    private static List<JsonObject> loadAllConfigs() {
        List<JsonObject> loadedConfigs = new ArrayList<>();
        File dir = CONFIG_DIR.toFile();

        if (!dir.exists()) {
            if (dir.mkdirs()) {
                createDefault(CONFIG_DIR.resolve("example.json"));
            } else {
                Constants.LOG.error("Could not create config directory: {}", CONFIG_DIR);
            }
        }

        File[] files = dir.listFiles((d, name) -> name.endsWith(".json"));
        if (files == null) return loadedConfigs;

        for (File file : files) {
            try (FileReader reader = new FileReader(file)) {
                JsonObject json = GSON.fromJson(reader, JsonObject.class);
                if (json != null) {
                    loadedConfigs.add(json);
                }
            } catch (Exception e) {
                Constants.LOG.error("Failed to load recipe config file: {}", file.getName(), e);
            }
        }

        return loadedConfigs;
    }

    private static void createDefault(Path path) {
        JsonObject root = new JsonObject();

        // Recipe Modifications
        JsonArray recipeMods = new JsonArray();
        JsonObject removeExample = new JsonObject();
        removeExample.addProperty("action", "remove");
        JsonObject filter = new JsonObject();
        filter.addProperty("output", "minecraft:stone_pickaxe");
        removeExample.add("filter", filter);
        recipeMods.add(removeExample);
        root.add("recipe_modifications", recipeMods);

        // Tag Modifications
        JsonArray tagMods = new JsonArray();
        JsonObject tagRemoveExample = new JsonObject();
        tagRemoveExample.addProperty("action", "remove_all_tags");
        JsonArray items = new JsonArray();
        items.add("minecraft:wooden_hoe");
        tagRemoveExample.add("items", items);
        tagMods.add(tagRemoveExample);
        root.add("tag_modifications", tagMods);

        try (FileWriter writer = new FileWriter(path.toFile())) {
            GSON.toJson(root, writer);
        } catch (IOException e) {
            Constants.LOG.error("Failed to create default recipe config: {}", path, e);
        }
    }
}