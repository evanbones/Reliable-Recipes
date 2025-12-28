package com.evandev.reliable_recipes.client;

import com.evandev.reliable_recipes.networking.ClientboundDeleteRecipePayload;
import com.evandev.reliable_recipes.networking.DeleteRecipePayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

public class ReliableRecipesModClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        ClientPlayNetworking.registerGlobalReceiver(ClientboundDeleteRecipePayload.TYPE,
                (payload, context) -> ClientboundDeleteRecipePayload.handle(payload.recipeId(), context.client()));
    }

}