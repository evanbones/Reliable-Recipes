package com.evandev.reliable_recipes.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.minecraft.resources.ResourceLocation;

public class ReliableRecipesModClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        ClientPlayNetworking.registerGlobalReceiver(new ResourceLocation("reliable_recipes", "client_delete_recipe"),
                (client, handler, buf, responseSender) -> {
                    ResourceLocation recipeId = buf.readResourceLocation();
                    ClientPayloadHandler.handleDeleteRecipe(recipeId);
                });

        ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
            ScreenEvents.afterRender(screen).register((sharedScreen, guiGraphics, mouseX, mouseY, tickDelta) -> {
                SharedToastOverlay.render(guiGraphics);
            });
        });
    }
}