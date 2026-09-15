package com.evandev.reliable_recipes.test;

import com.evandev.reliable_recipes.platform.services.IPlatformHelper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class TestPlatformHelper implements IPlatformHelper {
    public static Path customConfigDir = null;

    @Override
    public String getPlatformName() {
        return "JUnit";
    }

    @Override
    public boolean isModLoaded(String modId) {
        return false;
    }

    @Override
    public boolean isDevelopmentEnvironment() {
        return true;
    }

    @Override
    public Path getConfigDirectory() {
        if (customConfigDir != null) {
            return customConfigDir;
        }
        try {
            Path dir = Path.of("build", "tmp", "test-config");
            Files.createDirectories(dir);
            return dir;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
