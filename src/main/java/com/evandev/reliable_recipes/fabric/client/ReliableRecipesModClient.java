package com.evandev.reliable_recipes.fabric.client;

//? if fabric {

import com.evandev.reliable_recipes.client.ClientRecipeSync;
import com.evandev.reliable_recipes.client.SharedToastOverlay;
import com.evandev.reliable_recipes.networking.ClientboundAddRecipePayload;
import com.evandev.reliable_recipes.networking.ClientboundRemoveRecipePayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;

public class ReliableRecipesModClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
            //? if <1.21.2 {
            /*ScreenEvents.afterRender(screen).register((sharedScreen, guiGraphics, mouseX, mouseY, tickDelta) -> {
                SharedToastOverlay.extract(guiGraphics);
            });
            *///?} else {
            ScreenEvents.afterExtract(screen).register((sharedScreen, guiGraphics, mouseX, mouseY, tickDelta) -> {
                SharedToastOverlay.extract(guiGraphics);
            });
            //?}
        });

        ClientPlayNetworking.registerGlobalReceiver(ClientboundRemoveRecipePayload.TYPE, (payload, context) -> {
            context.client().execute(() -> ClientRecipeSync.onRecipeRemoved(payload.recipeKey()));
        });

        ClientPlayNetworking.registerGlobalReceiver(ClientboundAddRecipePayload.TYPE, (payload, context) -> {
            context.client().execute(() -> ClientRecipeSync.onRecipeAdded(payload.recipeHolder()));
        });
    }
}
//?}
