package com.evandev.reliable_recipes.client;

import com.evandev.reliable_recipes.config.ModConfig;
import com.evandev.reliable_recipes.networking.DeleteRecipePayload;
import com.evandev.reliable_recipes.recipe.RecipeModifier;
import dev.emi.emi.runtime.EmiReloadManager;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public class ReliableRecipesModClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        ClientPlayNetworking.registerGlobalReceiver(DeleteRecipePayload.TYPE,
                (payload, context) -> {
                    ResourceLocation recipeId = payload.recipeId();
                    var client = context.client();

                    client.execute(() -> {
                        if (client.getConnection() != null) {
                            boolean removed = RecipeModifier.removeRecipe(client.getConnection().getRecipeManager(), recipeId);
                            if (removed) {
                                handleFeedback(recipeId);
                            }
                        }
                    });
                });
    }

    private void handleFeedback(ResourceLocation recipeId) {
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

//        if (config.showToast) {
//            SystemToast.add(Minecraft.getInstance().getToasts(),
//                    SystemToast.SystemToastIds.TUTORIAL_HINT,
//                    Component.literal("Recipe Deleted"),
//                    Component.literal(recipeId.toString()));
//        }
    }
}