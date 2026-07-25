package com.evandev.reliable_recipes.compat.emi;

import com.evandev.reliable_recipes.config.ModConfig;
import com.evandev.reliable_recipes.platform.Services;
import dev.emi.emi.api.recipe.EmiRecipe;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public class EmiInteractions {
    public static boolean requestDeletion(EmiRecipe recipe) {
        if (recipe == null || recipe.getId() == null) return false;

        ResourceLocation id = getResourceLocation(recipe);

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return false;

        if (!mc.player.hasPermissions(2)) {
            mc.player.sendSystemMessage(Component.translatable("toast.reliable_recipes.permission_denied"));
            return false;
        }

        if (!ModConfig.get().enableEmiRemoval) {
            mc.player.sendSystemMessage(Component.translatable("toast.reliable_recipes.enable_emi_removal"));
            return false;
        }

        Services.PLATFORM.sendDeleteRecipePacket(id);
        return true;
    }

    private static ResourceLocation getResourceLocation(EmiRecipe recipe) {
        ResourceLocation id = recipe.getId();

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
                    id = ResourceLocation.fromNamespaceAndPath(realNamespace, realPath);
                } catch (Exception ignored) {
                }
            }
        }
        return id;
    }
}