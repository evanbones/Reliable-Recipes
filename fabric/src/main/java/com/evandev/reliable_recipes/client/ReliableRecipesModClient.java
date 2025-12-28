package com.evandev.reliable_recipes.client;

import com.evandev.reliable_recipes.config.ModConfig;
import com.evandev.reliable_recipes.recipe.RecipeModifier;
import dev.emi.emi.runtime.EmiReloadManager;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

public class ReliableRecipesModClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        ClientPlayNetworking.registerGlobalReceiver(new ResourceLocation("reliable_recipes", "client_delete_recipe"),
                (client, handler, buf, responseSender) -> {
                    ResourceLocation recipeId = buf.readResourceLocation();

                    client.execute(() -> {
                        if (client.getConnection() != null) {
                            ItemStack outputIcon = ItemStack.EMPTY;

                            var recipe = client.getConnection().getRecipeManager().byKey(recipeId).orElse(null);
                            if (recipe != null && client.level != null) {
                                outputIcon = recipe.getResultItem(client.level.registryAccess());
                            }

                            boolean removed = RecipeModifier.removeRecipe(client.getConnection().getRecipeManager(), recipeId);
                            if (removed) {
                                handleFeedback(recipeId, outputIcon);
                            }
                        }
                    });
                });

        ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
            ScreenEvents.afterRender(screen).register((sharedScreen, guiGraphics, mouseX, mouseY, tickDelta) -> {
                DeletionToastOverlay.render(guiGraphics);
            });
        });

    }

    private static void handleFeedback(ResourceLocation recipeId, ItemStack outputIcon) {
        ModConfig config = ModConfig.get();

        if (config.reloadEmi) {
            EmiReloadManager.reload();
        }

        if (config.showChatMessages) {
            if (Minecraft.getInstance().player != null) {
                Minecraft.getInstance().player.sendSystemMessage(Component.literal("Reliable Recipes: Deleted " + recipeId + " ")
                        .append(Component.literal("[UNDO]")
                                .withStyle(style -> style
                                        .withColor(ChatFormatting.RED)
                                        .withBold(true)
                                        .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/reliable_recipes_undo " + recipeId))
                                )));
            }
        }

        if (config.showToast) {
            DeletionToastOverlay.show(Component.literal(recipeId.getPath()), outputIcon);
        }
    }
}