package com.evandev.reliable_recipes.client;

import com.evandev.reliable_recipes.platform.Services;
import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.config.EmiConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.network.chat.Component;

public class EmiInteractions {
    public static boolean requestDeletion(EmiRecipe recipe) {
        if (recipe == null || recipe.getId() == null) return false;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return false;

        if (!mc.player.hasPermissions(2)) {
            Minecraft.getInstance().getToasts().addToast(SystemToast.multiline(
                    Minecraft.getInstance(),
                    SystemToast.SystemToastIds.TUTORIAL_HINT,
                    Component.literal("Reliable Recipes"),
                    Component.literal("You need OP to delete recipes.")
            ));
            return false;
        }

        if (!EmiConfig.devMode) {
            Minecraft.getInstance().getToasts().addToast(SystemToast.multiline(
                    Minecraft.getInstance(),
                    SystemToast.SystemToastIds.TUTORIAL_HINT,
                    Component.literal("Reliable Recipes"),
                    Component.literal("Enable EMI dev mode to delete recipes.")
            ));
            return false;
        }

        Services.PLATFORM.sendDeleteRecipePacket(recipe.getId());
        return true;
    }
}