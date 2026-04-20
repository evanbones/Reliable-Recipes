package com.evandev.reliable_recipes.client;

import com.evandev.reliable_recipes.Constants;
import com.evandev.reliable_recipes.networking.ClientboundAddRecipePayload;
import com.evandev.reliable_recipes.networking.ClientboundRemoveRecipePayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;

public class ReliableRecipesModClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
            ScreenEvents.afterExtract(screen).register((sharedScreen, guiGraphics, mouseX, mouseY, tickDelta) -> {
                SharedToastOverlay.extract(guiGraphics);
            });
        });

        ClientPlayNetworking.registerGlobalReceiver(ClientboundRemoveRecipePayload.TYPE, (payload, context) -> {
            context.client().execute(() -> {
                Constants.LOG.info("Received recipe removal notification for: {}", payload.recipeKey().identifier());

                if (context.client().screen != null && context.client().screen.getClass().getName().contains("RecipeViewScreen")) {
                    context.client().screen.onClose();
                }
            });
        });

        ClientPlayNetworking.registerGlobalReceiver(ClientboundAddRecipePayload.TYPE, (payload, context) -> {
            context.client().execute(() -> {
                Constants.LOG.info("Received recipe restoration notification for: {}", payload.recipeHolder().id().identifier());

                if (context.client().screen != null && context.client().screen.getClass().getName().contains("RecipeViewScreen")) {
                    context.client().screen.onClose();
                }
            });
        });
    }
}