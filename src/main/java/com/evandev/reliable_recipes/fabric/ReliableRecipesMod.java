package com.evandev.reliable_recipes.fabric;

//? if fabric {

import com.evandev.reliable_recipes.command.UndoCommand;
import com.evandev.reliable_recipes.networking.ClientboundAddRecipePayload;
import com.evandev.reliable_recipes.networking.ClientboundRemoveRecipePayload;
import com.evandev.reliable_recipes.networking.DeleteRecipePayload;
import com.evandev.reliable_recipes.recipe.BrewingRegistration;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

public class ReliableRecipesMod implements ModInitializer {

    @Override
    public void onInitialize() {
        BrewingRegistration.registerFabric();

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                UndoCommand.register(dispatcher)
        );

        //? if <1.21.2 {
        /*PayloadTypeRegistry.playC2S().register(DeleteRecipePayload.TYPE, DeleteRecipePayload.STREAM_CODEC);
        PayloadTypeRegistry.playS2C().register(ClientboundRemoveRecipePayload.TYPE, ClientboundRemoveRecipePayload.STREAM_CODEC);
        PayloadTypeRegistry.playS2C().register(ClientboundAddRecipePayload.TYPE, ClientboundAddRecipePayload.STREAM_CODEC);
        *///?} else {
        PayloadTypeRegistry.serverboundPlay().register(DeleteRecipePayload.TYPE, DeleteRecipePayload.STREAM_CODEC);
        PayloadTypeRegistry.clientboundPlay().register(ClientboundRemoveRecipePayload.TYPE, ClientboundRemoveRecipePayload.STREAM_CODEC);
        PayloadTypeRegistry.clientboundPlay().register(ClientboundAddRecipePayload.TYPE, ClientboundAddRecipePayload.STREAM_CODEC);
        //?}

        ServerPlayNetworking.registerGlobalReceiver(DeleteRecipePayload.TYPE,
                (payload, context) -> {
                    var server = context.server();
                    var player = context.player();
                    server.execute(() -> DeleteRecipePayload.handle(payload.recipeKey(), server, player));
                });
    }
}
//?}
