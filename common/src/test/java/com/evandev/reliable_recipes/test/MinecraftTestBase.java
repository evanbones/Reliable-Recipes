package com.evandev.reliable_recipes.test;

import com.evandev.reliable_recipes.config.RecipeConfigIO;
import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;

public abstract class MinecraftTestBase {
    private static boolean bootstrapped = false;

    @BeforeAll
    public static synchronized void setupMinecraft() {
        if (!bootstrapped) {
            SharedConstants.tryDetectVersion();
            Bootstrap.bootStrap();
            bootstrapped = true;
        }
    }

    @BeforeEach
    public void resetState() {
        RecipeConfigIO.invalidateCache();
        TestPlatformHelper.customConfigDir = null;
    }
}
