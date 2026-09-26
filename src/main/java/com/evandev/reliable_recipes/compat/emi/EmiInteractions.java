package com.evandev.reliable_recipes.compat.emi;

//? if <1.21.2 {
/*import com.evandev.reliable_recipes.config.ModConfig;
import com.evandev.reliable_recipes.platform.Services;
import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.runtime.EmiReloadManager;
import dev.emi.emi.screen.RecipeScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;

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

        if (!ModConfig.get().devMode) {
            mc.player.sendSystemMessage(Component.translatable("toast.reliable_recipes.dev_mode"));
            return false;
        }

        Services.PLATFORM.sendDeleteRecipePacket(ResourceKey.create(Registries.RECIPE, id));
        return true;
    }

    public static void reload() {
        Minecraft client = Minecraft.getInstance();
        EmiReloadManager.reload();
        if (client.screen instanceof RecipeScreen) {
            client.screen.onClose();
        }
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
*///?} else {
public class EmiInteractions {}
//?}
