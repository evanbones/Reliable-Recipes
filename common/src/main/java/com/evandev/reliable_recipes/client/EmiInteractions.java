package com.evandev.reliable_recipes.client;

import com.evandev.reliable_recipes.platform.Services;
import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.config.EmiConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

public class EmiInteractions {
    public static boolean requestDeletion(EmiRecipe recipe) {
        if (recipe == null || recipe.getId() == null) return false;

        Identifier id = getIdentifier(recipe);

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return false;

        if (!mc.player.hasPermissions(2)) {
            mc.player.sendSystemMessage(Component.translatable("toast.reliable_recipes.permission_denied"));
            return false;
        }

        if (!EmiConfig.devMode) {
            mc.player.sendSystemMessage(Component.translatable("toast.reliable_recipes.emi_dev_mode"));
            return false;
        }

        Services.PLATFORM.sendDeleteRecipePacket(id);
        return true;
    }

    private static Identifier getIdentifier(EmiRecipe recipe) {
        Identifier id = recipe.getId();

        // Converts "jei:/modid/path/to/recipe" -> "modid:path/to/recipe"
        if (id != null && (id.getNamespace().equals("jei") || id.getNamespace().equals("emi") || id.getNamespace().equals("toomanyrecipeviewers"))) {
            String path = id.getPath();
            if (path.startsWith("/")) {
                path = path.substring(1);
            }

            int splitIndex = path.indexOf('/');
            if (splitIndex > 0) {
                String realNamespace = path.substring(0, splitIndex);
                String realPath = path.substring(splitIndex + 1);
                try {
                    id = Identifier.fromNamespaceAndPath(realNamespace, realPath);
                } catch (Exception ignored) {
                }
            }
        }
        return id;
    }
}