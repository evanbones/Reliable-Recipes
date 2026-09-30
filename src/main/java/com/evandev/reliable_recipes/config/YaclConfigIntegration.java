package com.evandev.reliable_recipes.config;

import dev.isxander.yacl3.api.*;
import dev.isxander.yacl3.api.controller.StringControllerBuilder;
import dev.isxander.yacl3.api.controller.TickBoxControllerBuilder;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.function.Consumer;
import java.util.function.Supplier;

public class YaclConfigIntegration {

    public static Screen createScreen(Screen parent) {
        ModConfig config = ModConfig.get();
        ModConfig defaults = new ModConfig();

        YetAnotherConfigLib.Builder builder = YetAnotherConfigLib.createBuilder()
                .title(Component.translatable("config.reliable_recipes.title"))
                .save(ModConfig::save);

        ConfigCategory.Builder generalCategory = ConfigCategory.createBuilder()
                .name(Component.translatable("config.reliable_recipes.general"));

        generalCategory.option(createBoolOption("show_toast", defaults.showToast, () -> config.showToast, val -> config.showToast = val));
        generalCategory.option(createBoolOption("show_chat_messages", defaults.showChatMessages, () -> config.showChatMessages, val -> config.showChatMessages = val));
        generalCategory.option(createBoolOption("dev_mode", defaults.devMode, () -> config.devMode, val -> config.devMode = val));
        generalCategory.option(createBoolOption("reload_rrv", defaults.reloadRrv, () -> config.reloadRrv, val -> config.reloadRrv = val));

        generalCategory.group(ListOption.<String>createBuilder(String.class)
                .name(Component.translatable("config.reliable_recipes.ignored_tags"))
                .description(OptionDescription.of(Component.translatable("config.reliable_recipes.ignored_tags.tooltip")))
                .binding(
                        defaults.ignoredTags,
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
