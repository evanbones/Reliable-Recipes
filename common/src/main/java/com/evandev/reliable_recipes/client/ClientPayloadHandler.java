package com.evandev.reliable_recipes.client;

import com.evandev.reliable_recipes.config.ModConfig;
import com.evandev.reliable_recipes.recipe.RecipeUndoCache;
import dev.emi.emi.runtime.EmiReloadManager;
import dev.emi.emi.screen.RecipeScreen;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

public class ClientPayloadHandler {

    public static void handleDeleteRecipe(ResourceLocation recipeId) {
        Minecraft client = Minecraft.getInstance();
        client.execute(() -> {
            if (client.getConnection() != null) {
                ItemStack outputIcon = ItemStack.EMPTY;
                var recipe = client.getConnection().getRecipeManager().byKey(recipeId).orElse(null);
                if (recipe != null && client.level != null) {
                    outputIcon = recipe.value().getResultItem(client.level.registryAccess());
                }

                boolean removed = RecipeUndoCache.removeRecipe(client.getConnection().getRecipeManager(), recipeId);
                if (removed) {
                    handleFeedback(recipeId, client, outputIcon);
                }
            }
        });
    }

    private static void handleFeedback(ResourceLocation recipeId, Minecraft client, ItemStack outputIcon) {
        ModConfig config = ModConfig.get();

        if (config.reloadEmi) {
            EmiReloadManager.reload();
            if (client.screen instanceof RecipeScreen) {
                client.screen.onClose();
            }
        }

        if (config.showChatMessages && client.player != null) {
            client.player.sendSystemMessage(Component.translatable("toast.reliable_recipes.deleted", recipeId.toString())
                    .append(Component.translatable("toast.reliable_recipes.undo")
                            .withStyle(style -> style
                                    .withColor(ChatFormatting.RED)
                                    .withBold(true)
                                    .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/rrecipes_undo " + recipeId))
                            )));
        }

        if (config.showToast) {
            SharedToastOverlay.show(Component.literal("Recipe Deleted"), Component.literal(recipeId.getPath()), outputIcon);
        }
    }
}
