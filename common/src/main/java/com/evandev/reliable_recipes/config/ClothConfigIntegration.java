package com.evandev.reliable_recipes.config;

import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

public class ClothConfigIntegration {

    public static Screen createScreen(Screen parent) {
        ModConfig config = ModConfig.get();

        ConfigBuilder builder = ConfigBuilder.create()
                .setParentScreen(parent)
                .setTitle(Component.literal("Reliable Recipes Config"));

        builder.setSavingRunnable(ModConfig::save);

        ConfigCategory general = builder.getOrCreateCategory(Component.literal("General"));
        ConfigEntryBuilder entryBuilder = builder.entryBuilder();

        general.addEntry(entryBuilder.startBooleanToggle(Component.translatable("config.reliable_recipes.show_toast"), config.showToast)
                .setDefaultValue(true)
                .setTooltip(Component.translatable("config.reliable_recipes.show_toast.tooltip"))
                .setSaveConsumer(newValue -> config.showToast = newValue)
                .build());

        general.addEntry(entryBuilder.startBooleanToggle(Component.translatable("config.reliable_recipes.show_chat_messages"), config.showChatMessages)
                .setDefaultValue(true)
                .setTooltip(Component.translatable("config.reliable_recipes.show_chat_messages.tooltip"))
                .setSaveConsumer(newValue -> config.showChatMessages = newValue)
                .build());

        general.addEntry(entryBuilder.startBooleanToggle(Component.translatable("config.reliable_recipes.reload_emi"), config.reloadEmi)
                .setDefaultValue(true)
                .setTooltip(Component.translatable("config.reliable_recipes.reload_emi.tooltip"))
                .setSaveConsumer(newValue -> config.reloadEmi = newValue)
                .build());

        general.addEntry(entryBuilder.startStrList(Component.translatable("config.reliable_recipes.ignored_tags"), config.ignoredTags)
                .setDefaultValue(new ArrayList<>(List.of("c:hidden_from_recipe_viewers")))
                .setTooltip(Component.translatable("config.reliable_recipes.ignored_tags.tooltip"))
                .setSaveConsumer(newValue -> config.ignoredTags = newValue)
                .build());

        return builder.build();
    }
}