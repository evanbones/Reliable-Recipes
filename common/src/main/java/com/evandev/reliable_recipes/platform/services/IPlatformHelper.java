package com.evandev.reliable_recipes.platform.services;

import net.minecraft.world.item.ItemStack;
import java.nio.file.Path;

public interface IPlatformHelper {

    /**
     * Gets the name of the current platform
     *
     * @return The name of the current platform.
     */
    String getPlatformName();

    /**
     * Checks if a mod with the given id is loaded.
     *
     * @param modId The mod to check if it is loaded.
     * @return True if the mod is loaded, false otherwise.
     */
    boolean isModLoaded(String modId);

    /**
     * Check if the game is currently in a development environment.
     *
     * @return True if in a development environment, false otherwise.
     */
    boolean isDevelopmentEnvironment();

    /**
     * Gets the name of the environment type as a string.
     *
     * @return The name of the environment type.
     */
    default String getEnvironmentName() {
        return isDevelopmentEnvironment() ? "development" : "production";
    }

    /**
     * Gets the configuration directory for the current platform.
     *
     * @return The path to the config directory.
     */
    Path getConfigDirectory();

    /**
     * Checks if the platform has a mod loaded that provides item hiding capabilities
     * (e.g. Item Obliterator).
     *
     * @return True if item hiding logic should be processed.
     */
    boolean hasItemHidingCapabilities();

    /**
     * Checks if an item should be hidden.
     *
     * @param stack The item stack to check.
     * @return True if the item is hidden/disabled, false otherwise.
     */
    default boolean isItemHidden(ItemStack stack) {
        return false;
    }
}