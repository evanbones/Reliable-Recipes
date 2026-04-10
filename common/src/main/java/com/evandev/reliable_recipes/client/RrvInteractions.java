package com.evandev.reliable_recipes.client;

import cc.cassian.rrv.api.recipe.ReliableClientRecipe;
import cc.cassian.rrv.client.ReliableRecipeViewerClient;
import cc.cassian.rrv.common.recipe.inventory.SlotContent;
import com.evandev.reliable_recipes.config.ModConfig;
import com.evandev.reliable_recipes.platform.Services;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public class RrvInteractions {
    public static boolean requestDeletion(ReliableClientRecipe recipe) {
        if (recipe == null) return false;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return false;

        if (!mc.player.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER)) {
            mc.player.sendSystemMessage(Component.translatable("toast.reliable_recipes.permission_denied"));
            return false;
        }

        if (!ModConfig.get().devMode) {
            mc.player.sendSystemMessage(Component.translatable("toast.reliable_recipes.dev_mode"));
            return false;
        }

        List<SlotContent> results = recipe.getResults();
        if (results == null || results.isEmpty() || results.getFirst().getValidContents().isEmpty()) {
            mc.player.sendSystemMessage(Component.literal("§c[Reliable Recipes] Could not determine recipe output."));
            return false;
        }

        ItemStack outputItem = results.getFirst().getValidContents().getFirst();

        Services.PLATFORM.sendDeleteRecipeByOutputPacket(outputItem);
        return true;
    }
}