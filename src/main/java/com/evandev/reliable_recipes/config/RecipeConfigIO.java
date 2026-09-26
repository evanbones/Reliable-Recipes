package com.evandev.reliable_recipes.config;

import com.evandev.reliable_recipes.Constants;
import com.evandev.reliable_recipes.platform.Services;
import com.evandev.reliable_recipes.recipe.RecipeRule;
import com.evandev.reliable_recipes.tag.TagRule;
import com.google.gson.*;

import net.minecraft.resources.Identifier;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Stream;

public class RecipeConfigIO {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    public static Path getConfigDir() {
        return Services.PLATFORM.getConfigDirectory().resolve("reliable_recipes");
    }

    public record ConfigFile(Path path, Path relativePath, JsonElement element) {
    }

    private static List<RecipeRule> cachedRules;
    private static List<TagRule> cachedTagRules;
    private static Map<Identifier, JsonElement> cachedCustomRecipes;

    public static List<RecipeRule> loadRules() {
        if (cachedRules == null) {
            cachedRules = computeRules();
        }
        return cachedRules;
    }

    public static List<TagRule> loadTagRules() {
        if (cachedTagRules == null) {
            cachedTagRules = computeTagRules();
        }
        return cachedTagRules;
    }

    public static Map<Identifier, JsonElement> loadCustomRecipes() {
        if (cachedCustomRecipes == null) {
            cachedCustomRecipes = computeCustomRecipes();
        }
        return cachedCustomRecipes;
    }

    public static void invalidateCache() {
        cachedRules = null;
        cachedTagRules = null;
        cachedCustomRecipes = null;
    }

    private static List<RecipeRule> computeRules() {
        List<RecipeRule> rules = new ArrayList<>();
        List<ConfigFile> configs = loadAllConfigFiles();
        for (ConfigFile configFile : configs) {
            JsonElement config = configFile.element();
            if (config.isJsonArray()) {
                for (JsonElement element : config.getAsJsonArray()) {
                    if (element.isJsonObject()) {
                        RecipeRule rule = RecipeRuleParser.parseRule(element.getAsJsonObject());
                        if (rule != null) rules.add(rule);
                    }
                }
            } else if (config.isJsonObject() && config.getAsJsonObject().has("recipe_modifications")) {
                for (JsonElement element : config.getAsJsonObject().getAsJsonArray("recipe_modifications")) {
                    if (element.isJsonObject()) {
                        RecipeRule rule = RecipeRuleParser.parseRule(element.getAsJsonObject());
                        if (rule != null) rules.add(rule);
                    }
                }
            } else if (config.isJsonObject()) {
                RecipeRule rule = RecipeRuleParser.parseRule(config.getAsJsonObject());
                if (rule != null) rules.add(rule);
            }
        }
        return rules;
    }

    private static List<TagRule> computeTagRules() {
        List<TagRule> rules = new ArrayList<>();
        List<ConfigFile> configs = loadAllConfigFiles();
        for (ConfigFile configFile : configs) {
            JsonElement config = configFile.element();
            if (config.isJsonArray()) {
                for (JsonElement element : config.getAsJsonArray()) {
                    if (element.isJsonObject()) {
                        TagRule rule = RecipeRuleParser.parseTagRule(element.getAsJsonObject());
                        if (rule != null) rules.add(rule);
                    }
                }
            } else if (config.isJsonObject() && config.getAsJsonObject().has("tag_modifications")) {
                for (JsonElement element : config.getAsJsonObject().getAsJsonArray("tag_modifications")) {
                    if (element.isJsonObject()) {
                        TagRule rule = RecipeRuleParser.parseTagRule(element.getAsJsonObject());
                        if (rule != null) rules.add(rule);
                    }
                }
            } else if (config.isJsonObject()) {
                TagRule rule = RecipeRuleParser.parseTagRule(config.getAsJsonObject());
                if (rule != null) rules.add(rule);
            }
        }
        return rules;
    }

    private static Map<Identifier, JsonElement> computeCustomRecipes() {
        Map<Identifier, JsonElement> customRecipes = new LinkedHashMap<>();
        List<ConfigFile> configs = loadAllConfigFiles();

        for (ConfigFile configFile : configs) {
            JsonElement root = configFile.element();
            Path relPath = configFile.relativePath();

            if (root.isJsonArray()) {
                JsonArray array = root.getAsJsonArray();
                List<JsonObject> recipeCandidates = new ArrayList<>();
                for (JsonElement elem : array) {
                    if (elem.isJsonObject()) {
                        JsonObject obj = elem.getAsJsonObject();
                        if (isRecipeElement(obj)) {
                            recipeCandidates.add(obj);
                        }
                    }
                }
                for (int i = 0; i < recipeCandidates.size(); i++) {
                    processRecipeObject(recipeCandidates.get(i), relPath, i, recipeCandidates.size(), customRecipes);
                }
            } else if (root.isJsonObject()) {
                JsonObject obj = root.getAsJsonObject();
                if (obj.has("recipes") && obj.get("recipes").isJsonArray()) {
                    JsonArray array = obj.getAsJsonArray("recipes");
                    List<JsonObject> recipeCandidates = new ArrayList<>();
                    for (JsonElement elem : array) {
                        if (elem.isJsonObject()) {
                            recipeCandidates.add(elem.getAsJsonObject());
                        }
                    }
                    for (int i = 0; i < recipeCandidates.size(); i++) {
                        processRecipeObject(recipeCandidates.get(i), relPath, i, recipeCandidates.size(), customRecipes);
                    }
                } else if (obj.has("custom_recipes") && obj.get("custom_recipes").isJsonArray()) {
                    JsonArray array = obj.getAsJsonArray("custom_recipes");
                    List<JsonObject> recipeCandidates = new ArrayList<>();
                    for (JsonElement elem : array) {
                        if (elem.isJsonObject()) {
                            recipeCandidates.add(elem.getAsJsonObject());
                        }
                    }
                    for (int i = 0; i < recipeCandidates.size(); i++) {
                        processRecipeObject(recipeCandidates.get(i), relPath, i, recipeCandidates.size(), customRecipes);
                    }
                } else if (obj.has("recipe_additions") && obj.get("recipe_additions").isJsonArray()) {
                    JsonArray array = obj.getAsJsonArray("recipe_additions");
                    List<JsonObject> recipeCandidates = new ArrayList<>();
                    for (JsonElement elem : array) {
                        if (elem.isJsonObject()) {
                            recipeCandidates.add(elem.getAsJsonObject());
                        }
                    }
                    for (int i = 0; i < recipeCandidates.size(); i++) {
                        processRecipeObject(recipeCandidates.get(i), relPath, i, recipeCandidates.size(), customRecipes);
                    }
                } else if (!obj.has("recipe_modifications") && !obj.has("tag_modifications")) {
                    if (isRecipeElement(obj)) {
                        processRecipeObject(obj, relPath, 0, 1, customRecipes);
                    }
                }
            }
        }

        return customRecipes;
    }

    private static boolean isRecipeElement(JsonObject obj) {
        if (obj.has("action")) {
            String action = obj.get("action").getAsString();
            return "add".equalsIgnoreCase(action) || "add_recipe".equalsIgnoreCase(action);
        }
        return obj.has("type") && obj.get("type").isJsonPrimitive();
    }

    private static void processRecipeObject(JsonObject rawObj, Path relPath, int index, int totalInFile, Map<Identifier, JsonElement> outputMap) {
        JsonObject recipeJson;
        if (rawObj.has("action")) {
            String action = rawObj.get("action").getAsString();
            if ("add".equalsIgnoreCase(action) || "add_recipe".equalsIgnoreCase(action)) {
                if (rawObj.has("recipe") && rawObj.get("recipe").isJsonObject()) {
                    recipeJson = rawObj.getAsJsonObject("recipe").deepCopy();
                    if (rawObj.has("id") && !recipeJson.has("id")) {
                        recipeJson.add("id", rawObj.get("id"));
                    }
                } else {
                    recipeJson = rawObj.deepCopy();
                    recipeJson.remove("action");
                }
            } else {
                return;
            }
        } else {
            recipeJson = rawObj.deepCopy();
        }

        Identifier recipeId = null;
        boolean explicitId = false;

        if (recipeJson.has("id") && recipeJson.get("id").isJsonPrimitive() && recipeJson.get("id").getAsJsonPrimitive().isString()) {
            String idStr = recipeJson.get("id").getAsString().trim();
            if (!idStr.isEmpty()) {
                if (idStr.contains(":")) {
                    recipeId = Identifier.tryParse(idStr);
                } else {
                    recipeId = Identifier.tryParse("reliable_recipes:" + idStr);
                }
                if (recipeId != null) {
                    explicitId = true;
                }
            }
        }

        if (recipeId == null) {
            String pathStr = relPath.toString().replace('\\', '/');
            if (pathStr.endsWith(".json")) {
                pathStr = pathStr.substring(0, pathStr.length() - 5);
            }
            String sanitized = pathStr.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9/._-]", "_");
            if (sanitized.startsWith("/")) sanitized = sanitized.substring(1);
            if (totalInFile > 1) {
                sanitized += "_" + index;
            }
            recipeId = Identifier.tryParse("reliable_recipes:" + sanitized);
            if (recipeId == null) {
                recipeId = Identifier.fromNamespaceAndPath("reliable_recipes", "recipe_" + outputMap.size());
            }
        }

        if (!explicitId && outputMap.containsKey(recipeId)) {
            int counter = 1;
            Identifier candidate;
            do {
                candidate = Identifier.tryParse(recipeId.toString() + "_" + counter++);
            } while (candidate != null && outputMap.containsKey(candidate));
            if (candidate != null) {
                recipeId = candidate;
            }
        }

        recipeJson.remove("id");
        recipeJson.remove("action");
        outputMap.put(recipeId, recipeJson);
    }

    private static List<ConfigFile> loadAllConfigFiles() {
        List<ConfigFile> loadedConfigs = new ArrayList<>();
        Path configDir = getConfigDir();
        File dir = configDir.toFile();

        if (!dir.exists()) {
            if (dir.mkdirs()) {
                createDefault(configDir.resolve("recipe_example.json.disabled"));
            } else {
                Constants.LOG.error("Could not create config directory: {}", configDir);
            }
        }

        List<Path> scanDirs = new ArrayList<>();
        if (Files.exists(configDir)) {
            scanDirs.add(configDir);
        }

        try {
            Path configParent = Services.PLATFORM.getConfigDirectory().getParent();
            if (configParent != null) {
                Path rootReliableRecipes = configParent.resolve("reliable_recipes");
                if (Files.exists(rootReliableRecipes) && Files.isDirectory(rootReliableRecipes) && !rootReliableRecipes.equals(configDir)) {
                    scanDirs.add(rootReliableRecipes);
                }
            }
        } catch (Exception ignored) {
        }

        for (Path rootDir : scanDirs) {
            try (Stream<Path> stream = Files.walk(rootDir)) {
                stream.filter(path -> Files.isRegularFile(path) && path.toString().endsWith(".json"))
                      .forEach(path -> {
                          try (FileReader reader = new FileReader(path.toFile())) {
                              JsonElement root = JsonParser.parseReader(reader);
                              if (root != null) {
                                  Path relPath = rootDir.relativize(path);
                                  loadedConfigs.add(new ConfigFile(path, relPath, root));
                              }
                          } catch (Exception e) {
                              Constants.LOG.error("Failed to load recipe config file: {}", path.getFileName(), e);
                          }
                      });
            } catch (IOException e) {
                Constants.LOG.error("Failed to walk recipe directory: {}", rootDir, e);
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

        // Example 4: Add custom recipe
        JsonObject addExample = new JsonObject();
        addExample.addProperty("action", "add_recipe");
        addExample.addProperty("id", "reliable_recipes:example_stick");
        JsonObject recipeDetails = new JsonObject();
        recipeDetails.addProperty("type", "minecraft:crafting_shapeless");
        JsonArray ingredients = new JsonArray();
        //? if <1.21.2 {
        /*JsonObject ing = new JsonObject();
        ing.addProperty("item", "minecraft:dirt");
        ingredients.add(ing);
        *///?} else {
        ingredients.add("minecraft:dirt");
        //?}
        recipeDetails.add("ingredients", ingredients);
        JsonObject result = new JsonObject();
        result.addProperty("id", "minecraft:stick");
        result.addProperty("count", 4);
        recipeDetails.add("result", result);
        addExample.add("recipe", recipeDetails);
        root.add(addExample);

        try (FileWriter writer = new FileWriter(path.toFile())) {
            GSON.toJson(root, writer);
        } catch (IOException e) {
            Constants.LOG.error("Failed to create default recipe config: {}", path, e);
        }
    }

    public static void addRemovalRule(String recipeId) {
        Path generatedPath = getConfigDir().resolve("generated_removals.json");
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
                invalidateCache();
            } catch (IOException e) {
                Constants.LOG.error("Failed to save generated config", e);
            }
        }
    }

    public static void removeRemovalRule(String recipeId) {
        Path generatedPath = getConfigDir().resolve("generated_removals.json");
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
                invalidateCache();
            }
        } catch (Exception e) {
            Constants.LOG.error("Failed to update generated config", e);
        }
    }
}