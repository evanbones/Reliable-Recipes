package com.evandev.reliable_recipes.config;

import com.evandev.reliable_recipes.Constants;
import com.evandev.reliable_recipes.platform.Services;
import com.evandev.reliable_recipes.recipe.RecipeRule;
import com.evandev.reliable_recipes.recipe.TagRule;
import com.google.gson.*;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Iterator;
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

    public static void addRemovalRule(String recipeId) {
        Path generatedPath = CONFIG_DIR.resolve("generated_removals.json");
        File file = generatedPath.toFile();
        JsonObject root;

        // Load existing or create new root
        if (file.exists()) {
            try (FileReader reader = new FileReader(file)) {
                root = JsonParser.parseReader(reader).getAsJsonObject();
            } catch (Exception e) {
                Constants.LOG.error("Failed to read generated config", e);
                root = new JsonObject();
            }
        } else {
            root = new JsonObject();
        }

        if (!root.has("recipe_modifications")) {
            root.add("recipe_modifications", new JsonArray());
        }
        JsonArray modifications = root.getAsJsonArray("recipe_modifications");

        // Find an existing bulk remove rule or create one
        JsonObject bulkRemoveRule = null;
        for (JsonElement e : modifications) {
            if (e.isJsonObject()) {
                JsonObject obj = e.getAsJsonObject();
                if ("remove".equals(obj.get("action").getAsString()) && obj.has("filter")) {
                    bulkRemoveRule = obj;
                    break;
                }
            }
        }

        if (bulkRemoveRule == null) {
            bulkRemoveRule = new JsonObject();
            bulkRemoveRule.addProperty("action", "remove");
            bulkRemoveRule.add("filter", new JsonObject());
            modifications.add(bulkRemoveRule);
        }

        JsonObject filter = bulkRemoveRule.getAsJsonObject("filter");
        JsonArray ids;

        // Convert single "id" to array if necessary, or get existing array
        if (filter.has("id")) {
            JsonElement existingId = filter.get("id");
            if (existingId.isJsonArray()) {
                ids = existingId.getAsJsonArray();
            } else {
                ids = new JsonArray();
                ids.add(existingId);
                filter.add("id", ids);
            }
        } else {
            ids = new JsonArray();
            filter.add("id", ids);
        }

        // Add the new ID if it's not already there
        boolean exists = false;
        for (JsonElement e : ids) {
            if (e.getAsString().equals(recipeId)) {
                exists = true;
                break;
            }
        }

        if (!exists) {
            ids.add(recipeId);
            try (FileWriter writer = new FileWriter(file)) {
                GSON.toJson(root, writer);
            } catch (IOException e) {
                Constants.LOG.error("Failed to save generated config", e);
            }
        }
    }

    public static void removeRemovalRule(String recipeId) {
        Path generatedPath = CONFIG_DIR.resolve("generated_removals.json");
        File file = generatedPath.toFile();
        if (!file.exists()) return;

        try (FileReader reader = new FileReader(file)) {
            JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
            boolean changed = false;

            if (root.has("recipe_modifications")) {
                JsonArray mods = root.getAsJsonArray("recipe_modifications");
                Iterator<JsonElement> modIterator = mods.iterator();

                while (modIterator.hasNext()) {
                    JsonElement m = modIterator.next();
                    if (!m.isJsonObject()) continue;

                    JsonObject rule = m.getAsJsonObject();
                    if ("remove".equals(rule.get("action").getAsString()) && rule.has("filter")) {
                        JsonObject filter = rule.getAsJsonObject("filter");
                        if (filter.has("id")) {
                            JsonElement idEl = filter.get("id");

                            if (idEl.isJsonArray()) {
                                // Handle array removal
                                JsonArray ids = idEl.getAsJsonArray();
                                Iterator<JsonElement> idIterator = ids.iterator();
                                while (idIterator.hasNext()) {
                                    if (idIterator.next().getAsString().equals(recipeId)) {
                                        idIterator.remove();
                                        changed = true;
                                    }
                                }
                                // Clean up empty rules
                                if (ids.isEmpty()) {
                                    modIterator.remove();
                                }
                            } else if (idEl.getAsString().equals(recipeId)) {
                                // Handle single string removal
                                modIterator.remove();
                                changed = true;
                            }
                        }
                    }
                }
            }

            if (changed) {
                try (FileWriter writer = new FileWriter(file)) {
                    GSON.toJson(root, writer);
                }
            }
        } catch (Exception e) {
            Constants.LOG.error("Failed to update generated config", e);
        }
    }
}