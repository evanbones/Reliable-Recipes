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
        ConfigMigrator.migrateConfigsIfNeeded();
        List<RecipeRule> rules = new ArrayList<>();
        List<JsonElement> configs = loadAllConfigs();
        for (JsonElement config : configs) {
            if (config.isJsonArray()) {
                for (JsonElement element : config.getAsJsonArray()) {
                    if (element.isJsonObject()) {
                        RecipeRule rule = RecipeJsonParser.parseRule(element.getAsJsonObject());
                        if (rule != null) rules.add(rule);
                    }
                }
            } else if (config.isJsonObject() && config.getAsJsonObject().has("recipe_modifications")) {
                for (JsonElement element : config.getAsJsonObject().getAsJsonArray("recipe_modifications")) {
                    if (element.isJsonObject()) {
                        RecipeRule rule = RecipeJsonParser.parseRule(element.getAsJsonObject());
                        if (rule != null) rules.add(rule);
                    }
                }
            } else if (config.isJsonObject()) {
                RecipeRule rule = RecipeJsonParser.parseRule(config.getAsJsonObject());
                if (rule != null) rules.add(rule);
            }
        }
        return rules;
    }

    public static List<TagRule> loadTagRules() {
        List<TagRule> rules = new ArrayList<>();
        List<JsonElement> configs = loadAllConfigs();
        for (JsonElement config : configs) {
            if (config.isJsonArray()) {
                for (JsonElement element : config.getAsJsonArray()) {
                    if (element.isJsonObject()) {
                        TagRule rule = RecipeJsonParser.parseTagRule(element.getAsJsonObject());
                        if (rule != null) rules.add(rule);
                    }
                }
            } else if (config.isJsonObject() && config.getAsJsonObject().has("tag_modifications")) {
                for (JsonElement element : config.getAsJsonObject().getAsJsonArray("tag_modifications")) {
                    if (element.isJsonObject()) {
                        TagRule rule = RecipeJsonParser.parseTagRule(element.getAsJsonObject());
                        if (rule != null) rules.add(rule);
                    }
                }
            } else if (config.isJsonObject()) {
                TagRule rule = RecipeJsonParser.parseTagRule(config.getAsJsonObject());
                if (rule != null) rules.add(rule);
            }
        }
        return rules;
    }

    private static List<JsonElement> loadAllConfigs() {
        List<JsonElement> loadedConfigs = new ArrayList<>();
        File dir = CONFIG_DIR.toFile();

        if (!dir.exists()) {
            if (dir.mkdirs()) {
                createDefault(CONFIG_DIR.resolve("recipe_example.json.disabled"));
            } else {
                Constants.LOG.error("Could not create config directory: {}", CONFIG_DIR);
            }
        }

        File[] files = dir.listFiles((d, name) -> name.endsWith(".json"));
        if (files == null) return loadedConfigs;

        for (File file : files) {
            try (FileReader reader = new FileReader(file)) {
                JsonElement root = JsonParser.parseReader(reader);
                if (root != null) {
                    loadedConfigs.add(root);
                }
            } catch (Exception e) {
                Constants.LOG.error("Failed to load recipe config file: {}", file.getName(), e);
            }
        }

        return loadedConfigs;
    }

    private static void createDefault(Path path) {
        JsonArray root = new JsonArray();

        // Example 1: Remove recipes
        JsonObject removeExample = new JsonObject();
        removeExample.addProperty("action", "remove_recipe");
        removeExample.addProperty("mod", "examplemod");
        removeExample.addProperty("type", "minecraft:crafting_shaped");
        root.add(removeExample);

        // Example 2: Replace inputs
        JsonObject replaceExample = new JsonObject();
        replaceExample.addProperty("action", "replace_input");
        replaceExample.addProperty("target", "minecraft:stick");

        JsonArray replacement = new JsonArray();
        replacement.add("minecraft:stick");
        replacement.add("examplemod:reinforced_stick");
        replaceExample.add("replacement", replacement);
        replaceExample.addProperty("id", "examplemod:reinforced_sword");
        root.add(replaceExample);

        // Example 3: Tag modifications
        JsonObject tagRemoveExample = new JsonObject();
        tagRemoveExample.addProperty("action", "remove_from_tag");
        tagRemoveExample.addProperty("tag", "c:foods");

        JsonArray tagItems = new JsonArray();
        tagItems.add("examplemod:inedible_food");
        tagRemoveExample.add("id", tagItems);
        root.add(tagRemoveExample);

        try (FileWriter writer = new FileWriter(path.toFile())) {
            GSON.toJson(root, writer);
        } catch (IOException e) {
            Constants.LOG.error("Failed to create default recipe config: {}", path, e);
        }
    }

    public static void addRemovalRule(String recipeId) {
        Path generatedPath = CONFIG_DIR.resolve("generated_removals.json");
        File file = generatedPath.toFile();
        JsonArray root;

        if (file.exists()) {
            try (FileReader reader = new FileReader(file)) {
                JsonElement parsed = JsonParser.parseReader(reader);
                root = parsed.isJsonArray() ? parsed.getAsJsonArray() : new JsonArray();
            } catch (Exception e) {
                Constants.LOG.error("Failed to read generated config", e);
                root = new JsonArray();
            }
        } else {
            root = new JsonArray();
        }

        JsonObject bulkRemoveRule = null;
        for (JsonElement e : root) {
            if (e.isJsonObject()) {
                JsonObject obj = e.getAsJsonObject();
                String action = obj.has("action") ? obj.get("action").getAsString() : "";
                if (("remove".equals(action) || "remove_recipe".equals(action)) && !obj.has("filter")) {
                    bulkRemoveRule = obj;
                    break;
                }
            }
        }

        if (bulkRemoveRule == null) {
            bulkRemoveRule = new JsonObject();
            bulkRemoveRule.addProperty("action", "remove_recipe");
            root.add(bulkRemoveRule);
        }

        JsonArray ids;
        if (bulkRemoveRule.has("id")) {
            JsonElement existingId = bulkRemoveRule.get("id");
            if (existingId.isJsonArray()) {
                ids = existingId.getAsJsonArray();
            } else {
                ids = new JsonArray();
                ids.add(existingId);
                bulkRemoveRule.add("id", ids);
            }
        } else {
            ids = new JsonArray();
            bulkRemoveRule.add("id", ids);
        }

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
            JsonElement parsed = JsonParser.parseReader(reader);
            if (!parsed.isJsonArray()) return;

            JsonArray root = parsed.getAsJsonArray();
            boolean changed = false;
            Iterator<JsonElement> modIterator = root.iterator();

            while (modIterator.hasNext()) {
                JsonElement m = modIterator.next();
                if (!m.isJsonObject()) continue;

                JsonObject rule = m.getAsJsonObject();
                String action = rule.has("action") ? rule.get("action").getAsString() : "";
                if ("remove".equals(action) || "remove_recipe".equals(action)) {
                    JsonElement idEl = rule.has("id") ? rule.get("id") : null;

                    if (idEl == null && rule.has("filter") && rule.getAsJsonObject("filter").has("id")) {
                        idEl = rule.getAsJsonObject("filter").get("id");
                    }

                    if (idEl != null) {
                        if (idEl.isJsonArray()) {
                            JsonArray ids = idEl.getAsJsonArray();
                            Iterator<JsonElement> idIterator = ids.iterator();
                            while (idIterator.hasNext()) {
                                if (idIterator.next().getAsString().equals(recipeId)) {
                                    idIterator.remove();
                                    changed = true;
                                }
                            }
                            if (ids.isEmpty()) {
                                modIterator.remove();
                            }
                        } else if (idEl.getAsString().equals(recipeId)) {
                            modIterator.remove();
                            changed = true;
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