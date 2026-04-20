package com.evandev.reliable_recipes.client;

import cc.cassian.rrv.api.recipe.ReliableClientRecipe;
import cc.cassian.rrv.common.recipe.inventory.SlotContent;
import com.evandev.reliable_recipes.config.ModConfig;
import com.evandev.reliable_recipes.platform.Services;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.permissions.Permissions;

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

        Identifier recipeId = recipe.getId();
        if (recipeId != null) {
            Services.PLATFORM.sendDeleteRecipePacket(ResourceKey.create(Registries.RECIPE, recipeId));
            return true;
        }

        // Fallback for recipes that fail to provide an ID
        List<SlotContent> results = recipe.getResults();
        if (results == null || results.isEmpty() || results.getFirst().getValidContents().isEmpty()) {
            mc.player.sendSystemMessage(Component.literal("§c[Reliable Recipes] Could not determine recipe ID or output."));
            return false;
        }

        return false;
    }
}