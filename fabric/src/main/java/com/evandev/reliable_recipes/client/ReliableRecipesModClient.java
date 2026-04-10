package com.evandev.reliable_recipes.client;

import com.evandev.reliable_recipes.networking.ClientboundAddRecipePayload;
import com.evandev.reliable_recipes.networking.ClientboundRemoveRecipePayload;
import com.evandev.reliable_recipes.recipe.RecipeModifier;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;

public class ReliableRecipesModClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        ClientPlayNetworking.registerGlobalReceiver(ClientboundRemoveRecipePayload.TYPE,
                (payload, context) -> {
                    context.client().execute(() -> {
                        if (context.client().getConnection() != null) {
                            // TODO: Trigger RRV/JEI cache removal
                            // ex ReliableRecipesRRVPlugin.removeRecipe(payload.recipeKey());
                        }
                    });
                });

        ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
            ScreenEvents.afterExtract(screen).register((sharedScreen, guiGraphics, mouseX, mouseY, tickDelta) -> {
                SharedToastOverlay.extract(guiGraphics);
            });
        });

        ClientPlayNetworking.registerGlobalReceiver(ClientboundAddRecipePayload.TYPE,
                (payload, context) -> {
                    context.client().execute(() -> {
                        if (context.client().getConnection() != null) {
                            // TODO: Trigger RRV/JEI cache addition
                            // ex ReliableRecipesRRVPlugin.addRecipe(payload.recipeHolder());
                        }
                    });
                });
    }
}