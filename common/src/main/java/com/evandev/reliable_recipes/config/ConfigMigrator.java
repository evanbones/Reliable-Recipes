package com.evandev.reliable_recipes.config;

import com.evandev.reliable_recipes.Constants;
import com.evandev.reliable_recipes.platform.Services;
import com.google.gson.*;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

public class ConfigMigrator {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public static void migrateConfigsIfNeeded() {
        Path configDir = Services.PLATFORM.getConfigDirectory().resolve("reliable_recipes");
        File dir = configDir.toFile();
        if (!dir.exists() || !dir.isDirectory()) return;

        File[] files = dir.listFiles((d, name) -> name.endsWith(".json"));
        if (files == null) return;

        for (File file : files) {
            try {
                migrateFile(file);
            } catch (Exception e) {
                Constants.LOG.error("Failed to migrate config file: {}", file.getName(), e);
            }
        }
    }

    private static void migrateFile(File file) throws Exception {
        JsonElement rootElement;
        try (FileReader reader = new FileReader(file)) {
            rootElement = JsonParser.parseReader(reader);
        }

        if (rootElement.isJsonArray()) {
            return;
        }

        if (!rootElement.isJsonObject()) return;
        JsonObject root = rootElement.getAsJsonObject();
        boolean changed = false;
        JsonArray newFlatRoot = new JsonArray();

        if (root.has("recipe_modifications")) {
            for (JsonElement el : root.getAsJsonArray("recipe_modifications")) {
                if (el.isJsonObject()) {
                    JsonObject mod = el.getAsJsonObject();
                    if (mod.has("filter")) {
                        JsonObject filter = mod.getAsJsonObject("filter");
                        for (Map.Entry<String, JsonElement> entry : filter.entrySet()) {
                            mod.add(entry.getKey(), entry.getValue());
                        }
                        mod.remove("filter");
                    }
                    if (mod.has("action") && mod.get("action").getAsString().equals("remove")) {
                        mod.addProperty("action", "remove_recipe");
                    }
                    newFlatRoot.add(mod);
                    changed = true;
                }
            }
        } else if (root.has("tag_modifications")) {
            for (JsonElement el : root.getAsJsonArray("tag_modifications")) {
                if (el.isJsonObject()) {
                    JsonObject mod = el.getAsJsonObject();
                    if (mod.has("action") && mod.get("action").getAsString().equals("remove_all_tags")) {
                        mod.addProperty("action", "remove_tag");
                    }
                    if (mod.has("items")) {
                        mod.add("id", mod.get("items"));
                        mod.remove("items");
                    }
                    newFlatRoot.add(mod);
                    changed = true;
                }
            }
        } else {
            newFlatRoot.add(root);
            changed = true;
        }

        if (changed) {
            File backup = new File(file.getParentFile(), file.getName() + ".bak");
            if (!backup.exists()) {
                Files.copy(file.toPath(), backup.toPath());
            }

            try (FileWriter writer = new FileWriter(file)) {
                GSON.toJson(newFlatRoot, writer);
            }
            Constants.LOG.info("Successfully migrated '{}' to the new flat array format.", file.getName());
        }
    }
}