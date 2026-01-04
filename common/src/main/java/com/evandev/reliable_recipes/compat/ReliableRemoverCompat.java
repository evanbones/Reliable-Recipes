package com.evandev.reliable_recipes.compat;

import com.evandev.reliable_recipes.platform.Services;
import net.minecraft.world.item.ItemStack;

public class ReliableRemoverCompat {

    public static boolean isLoaded() {
        return Services.PLATFORM.isModLoaded("reliable_remover");
    }

    public static boolean isHidden(ItemStack stack) {
        if (isLoaded()) {
            return Handler.isHidden(stack);
        }
        return false;
    }

    private static class Handler {
        static boolean isHidden(ItemStack stack) {
            return com.evandev.reliable_remover.config.RuleManager.isHidden(stack);
        }
    }
}