package com.evandev.reliable_recipes.config;

import com.evandev.reliable_recipes.Constants;
import com.evandev.reliable_recipes.platform.Services;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ModConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final File CONFIG_FILE = Services.PLATFORM.getConfigDirectory().resolve("reliable_recipes.json").toFile();

    private static ModConfig INSTANCE;

    public boolean enableEmiRemoval = false;
    public boolean showToast = true;
    public boolean showChatMessages = true;
    public boolean reloadEmi = false;
    public List<String> ignoredTags = new ArrayList<>(List.of("c:hidden_from_recipe_viewers"));

    public static ModConfig get() {
        if (INSTANCE == null) {
            load();
        }
        return INSTANCE;
    }

    public static void load() {
        if (CONFIG_FILE.exists()) {
            try (FileReader reader = new FileReader(CONFIG_FILE)) {
                INSTANCE = GSON.fromJson(reader, ModConfig.class);
                if (INSTANCE == null) {
                    INSTANCE = new ModConfig();
                    save();
                } else if (INSTANCE.ignoredTags == null) {
                    INSTANCE.ignoredTags = new ArrayList<>(List.of("c:hidden_from_recipe_viewers"));
                } else {
                    INSTANCE.ignoredTags = new ArrayList<>(INSTANCE.ignoredTags);
                }
            } catch (Exception e) {
                Constants.LOG.error("Failed to load reliable_recipes.json", e);
                INSTANCE = new ModConfig();
                save();
            }
        } else {
            INSTANCE = new ModConfig();
            save();
        }
    }

    public static void save() {
        try (FileWriter writer = new FileWriter(CONFIG_FILE)) {
            GSON.toJson(INSTANCE, writer);
        } catch (IOException e) {
            Constants.LOG.error("Failed to save reliable_recipes.json", e);
        }
    }
}