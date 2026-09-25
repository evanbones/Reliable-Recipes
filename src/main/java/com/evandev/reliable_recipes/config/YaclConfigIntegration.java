package com.evandev.reliable_recipes.config;

import dev.isxander.yacl3.api.*;
import dev.isxander.yacl3.api.controller.TickBoxControllerBuilder;
import dev.isxander.yacl3.api.controller.StringControllerBuilder;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class YaclConfigIntegration {

    public static Screen createScreen(Screen parent) {
        ModConfig config = ModConfig.get();

        YetAnotherConfigLib.Builder builder = YetAnotherConfigLib.createBuilder()
                .title(Component.literal("Reliable Recipes Config"))
                .save(ModConfig::save);

        ConfigCategory.Builder generalCategory = ConfigCategory.createBuilder()
                .name(Component.literal("General"));

        generalCategory.option(createBoolOption("show_toast", true, () -> config.showToast, val -> config.showToast = val));
        generalCategory.option(createBoolOption("show_chat_messages", true, () -> config.showChatMessages, val -> config.showChatMessages = val));
        generalCategory.option(createBoolOption("dev_mode", false, () -> config.devMode, val -> config.devMode = val));
        generalCategory.option(createBoolOption("reload_rrv", true, () -> config.reloadRrv, val -> config.reloadRrv = val));

        generalCategory.group(ListOption.<String>createBuilder(String.class)
                .name(Component.translatable("config.reliable_recipes.ignored_tags"))
                .description(OptionDescription.of(Component.translatable("config.reliable_recipes.ignored_tags.tooltip")))
                .binding(
                        new ArrayList<>(List.of("c:hidden_from_recipe_viewers")),
                        () -> config.ignoredTags,
                        newValue -> config.ignoredTags = newValue
                )
                .controller(StringControllerBuilder::create)
                .initial("")
                .build());

        return builder
                .category(generalCategory.build())
                .build()
                .generateScreen(parent);
    }

    private static Option<Boolean> createBoolOption(String name, boolean defaultValue, Supplier<Boolean> getter, Consumer<Boolean> setter) {
        return Option.<Boolean>createBuilder()
                .name(Component.translatable("config.reliable_recipes." + name))
                .description(OptionDescription.of(Component.translatable("config.reliable_recipes." + name + ".tooltip")))
                .binding(defaultValue, getter, setter)
                .controller(TickBoxControllerBuilder::create)
                .build();
    }
}
