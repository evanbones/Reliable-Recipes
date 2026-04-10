package com.evandev.reliable_recipes.client;

import cc.cassian.rrv.api.recipe.ReliableClientRecipe;
import cc.cassian.rrv.client.ReliableRecipeViewerClient;
import com.evandev.reliable_recipes.platform.Services;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.world.item.crafting.Recipe;

import java.lang.reflect.Method;

public class RrvInteractions {
    public static boolean requestDeletion(ReliableClientRecipe recipe) {
        if (recipe == null) return false;

        Identifier id = getIdentifier(recipe);
        if (id == null) return false;

        ResourceKey<Recipe<?>> recipeKey = ResourceKey.create(Registries.RECIPE, id);

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return false;

        if (!mc.player.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER)) {
            mc.player.sendSystemMessage(Component.translatable("toast.reliable_recipes.permission_denied"));
            return false;
        }

        if (!ReliableRecipeViewerClient.isCheatmodeActive()) {
            mc.player.sendSystemMessage(Component.translatable("toast.reliable_recipes.rrv_cheat_mode"));
            return false;
        }

        Services.PLATFORM.sendDeleteRecipePacket(recipeKey);
        return true;
    }

    private static Identifier getIdentifier(ReliableClientRecipe recipe) {
        try {
            Method getIdMethod = recipe.getClass().getMethod("getId");
            Object idObj = getIdMethod.invoke(recipe);
            if (idObj instanceof Identifier id) {
                return id;
            }
        } catch (Exception e) {
            try {
                Method idMethod = recipe.getClass().getMethod("id");
                Object idObj = idMethod.invoke(recipe);
                if (idObj instanceof Identifier id) {
                    return id;
                }
            } catch (Exception ignored) {
            }
        }
        return null;
    }
}