package com.evandev.reliable_recipes.client;

import com.evandev.reliable_recipes.platform.Services;
import dev.emi.emi.api.recipe.EmiRecipe;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

public class EmiInteractions {
    public static boolean requestDeletion(EmiRecipe recipe) {
        if (recipe == null || recipe.getId() == null) return false;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return false;

        if (!mc.player.hasPermissions(2)) {
            mc.player.displayClientMessage(Component.literal("You need OP to delete recipes."), true);
            return false;
        }

        Services.PLATFORM.sendDeleteRecipePacket(recipe.getId());

        mc.player.displayClientMessage(Component.literal("Requested deletion of: " + recipe.getId()), true);
        return true;
    }
}