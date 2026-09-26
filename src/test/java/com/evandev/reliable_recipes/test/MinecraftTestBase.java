package com.evandev.reliable_recipes.test;

import com.evandev.reliable_recipes.config.RecipeConfigIO;
import com.evandev.reliable_recipes.platform.Services;
import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
//? if >=1.21.2 {
import net.minecraft.core.component.DataComponentInitializers;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.registries.VanillaRegistries;
//?}

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public abstract class MinecraftTestBase {
    private static boolean bootstrapped = false;

    @BeforeAll
    public static synchronized void setupMinecraft() {
        if (!bootstrapped) {
            SharedConstants.tryDetectVersion();
            Bootstrap.bootStrap();
            //? if >=1.21.2 {
            //? if <=26.2 {
            BuiltInRegistries.DATA_COMPONENT_INITIALIZERS.build(VanillaRegistries.createLookup())
                    .forEach(DataComponentInitializers.PendingComponents::apply);
            //?} else {
            /*BuiltInRegistries.DATA_COMPONENT_INITIALIZERS.build(VanillaRegistries.createWorldLookup())
                    .forEach(DataComponentInitializers.PendingComponents::apply);
            *///?}
            //?}
            bootstrapped = true;
        }
    }

    @BeforeEach
    public void resetState() throws IOException {
        RecipeConfigIO.invalidateCache();
        Path dir = Path.of("build", "tmp", "test-config");
        Files.createDirectories(dir);
        Services.configDirectoryOverride = dir;
    }
}
