package com.evandev.reliable_recipes.client;

import com.evandev.reliable_recipes.config.ModConfig;
import dev.isxander.yacl3.api.*;
import dev.isxander.yacl3.api.controller.StringControllerBuilder;
import dev.isxander.yacl3.api.controller.TickBoxControllerBuilder;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class ModConfigScreen {
    public static Screen createScreen(Screen parent) {
        ModConfig.load();

        YetAnotherConfigLib.Builder builder = YetAnotherConfigLib.createBuilder()
                .title(Component.translatable("config.reliable_recipes.title"))
                .save(ModConfig::save);

        ConfigCategory.Builder general = ConfigCategory.createBuilder()
                .name(Component.translatable("config.reliable_recipes.category.general"))
                .group(ListOption.<String>createBuilder()
                        .name(Component.translatable("config.reliable_recipes.option.ignored_tags"))
                        .description(OptionDescription.of(Component.translatable("config.reliable_recipes.option.ignored_tags.tooltip")))
                        .binding(new ArrayList<>(List.of("c:hidden_from_recipe_viewers")), () -> ModConfig.get().ignoredTags, val -> ModConfig.get().ignoredTags = new ArrayList<>(val))
                        .controller(StringControllerBuilder::create)
                        .initial("")
                        .build());

        ConfigCategory.Builder emi = ConfigCategory.createBuilder()
                .name(Component.translatable("config.reliable_recipes.category.emi"))
                .option(createBoolOption("enable_emi_removal", false, () -> ModConfig.get().enableEmiRemoval, val -> ModConfig.get().enableEmiRemoval = val))
                .option(createBoolOption("show_toast", true, () -> ModConfig.get().showToast, val -> ModConfig.get().showToast = val))
                .option(createBoolOption("show_chat_messages", true, () -> ModConfig.get().showChatMessages, val -> ModConfig.get().showChatMessages = val))
                .option(createBoolOption("reload_emi", false, () -> ModConfig.get().reloadEmi, val -> ModConfig.get().reloadEmi = val));

        return builder
                .category(general.build())
                .category(emi.build())
                .build()
                .generateScreen(parent);
    }

    private static Option<Boolean> createBoolOption(String name, boolean defaultValue, Supplier<Boolean> getter, Consumer<Boolean> setter) {
        return Option.<Boolean>createBuilder()
                .name(Component.translatable("config.reliable_recipes.option." + name))
                .description(OptionDescription.of(Component.translatable("config.reliable_recipes.option." + name + ".tooltip")))
                .binding(defaultValue, getter, setter)
                .controller(TickBoxControllerBuilder::create)
                .build();
    }
}
