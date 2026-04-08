package com.evandev.reliable_recipes;

import com.evandev.reliable_recipes.command.UndoCommand;
import com.evandev.reliable_recipes.networking.ClientboundDeleteRecipePayload;
import com.evandev.reliable_recipes.networking.DeleteRecipePayload;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.resources.Identifier;

public class ReliableRecipesMod implements ModInitializer {

    @Override
    public void onInitialize() {
        CommonClass.init();

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                UndoCommand.register(dispatcher)
        );

        PayloadTypeRegistry.playS2C().register(ClientboundDeleteRecipePayload.TYPE, ClientboundDeleteRecipePayload.STREAM_CODEC);
        PayloadTypeRegistry.playC2S().register(DeleteRecipePayload.TYPE, DeleteRecipePayload.STREAM_CODEC);

        ServerPlayNetworking.registerGlobalReceiver(DeleteRecipePayload.TYPE,
                (payload, context) -> {
                    Identifier id = payload.recipeId();
                    var server = context.server();
                    var player = context.player();
                    server.execute(() -> DeleteRecipePayload.handle(id, server, player));
                });
    }
}