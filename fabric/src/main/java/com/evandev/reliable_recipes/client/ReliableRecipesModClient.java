package com.evandev.reliable_recipes.client;

import com.evandev.reliable_recipes.networking.ClientboundDeleteRecipePayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;

public class ReliableRecipesModClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        ClientPlayNetworking.registerGlobalReceiver(ClientboundDeleteRecipePayload.TYPE,
                (payload, context) -> ClientboundDeleteRecipePayload.handle(payload.recipeId(), context.client()));

        ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
            ScreenEvents.afterRender(screen).register((sharedScreen, guiGraphics, mouseX, mouseY, tickDelta) -> {
                DeletionToastOverlay.render(guiGraphics);
            });
        });
    }
}