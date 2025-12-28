package com.evandev.reliable_recipes.config;

import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class ClothConfigIntegration {

    public static Screen createScreen(Screen parent) {
        ModConfig config = ModConfig.get();

        ConfigBuilder builder = ConfigBuilder.create()
                .setParentScreen(parent)
                .setTitle(Component.literal("Reliable Recipes Config"));

        builder.setSavingRunnable(ModConfig::save);

        ConfigCategory general = builder.getOrCreateCategory(Component.literal("General"));
        ConfigEntryBuilder entryBuilder = builder.entryBuilder();

        // TODO: extract these into translation keys
        general.addEntry(entryBuilder.startBooleanToggle(Component.literal("Show Toast"), config.showToast)
                .setDefaultValue(true)
                .setTooltip(Component.literal("Show a toast after removing a recipe through EMI"))
                .setSaveConsumer(newValue -> config.showToast = newValue)
                .build());

        general.addEntry(entryBuilder.startBooleanToggle(Component.literal("Show Chat Messages"), config.showChatMessages)
                .setDefaultValue(true)
                .setTooltip(Component.literal("Show feedback in chat after removing a recipe through EMI"))
                .setSaveConsumer(newValue -> config.showChatMessages = newValue)
                .build());

        general.addEntry(entryBuilder.startBooleanToggle(Component.literal("Reload EMI After Removal"), config.reloadEmi)
                .setDefaultValue(true)
                .setTooltip(Component.literal("Reload EMI immediately after recipe removal"))
                .setSaveConsumer(newValue -> config.reloadEmi = newValue)
                .build());

        return builder.build();
    }
}