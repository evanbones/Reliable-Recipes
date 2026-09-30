package com.evandev.reliable_recipes.fabric;

//? if fabric {

import com.evandev.reliable_recipes.command.UndoCommand;
import com.evandev.reliable_recipes.config.ModConfig;
import com.evandev.reliable_recipes.networking.ClientboundAddRecipePayload;
import com.evandev.reliable_recipes.networking.ClientboundRemoveRecipePayload;
import com.evandev.reliable_recipes.networking.DeleteRecipePayload;
import com.evandev.reliable_recipes.recipe.BrewingRegistration;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
//? if <1.21 {
/*import net.minecraft.network.RegistryFriendlyByteBuf;
*///?} else {
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
//?}

public class ReliableRecipesMod implements ModInitializer {

    @Override
    public void onInitialize() {
        ModConfig.load();

        BrewingRegistration.registerFabric();

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                UndoCommand.register(dispatcher)
        );

        //? if <1.21 {
        /*ServerPlayNetworking.registerGlobalReceiver(DeleteRecipePayload.TYPE.id(),
                (server, player, handler, buf, responseSender) -> {
                    DeleteRecipePayload payload = DeleteRecipePayload.STREAM_CODEC.decode(new RegistryFriendlyByteBuf(buf));
                    server.execute(() -> DeleteRecipePayload.handle(payload.recipeKey(), server, player));
                });
        *///?} else if <1.21.2 {
        /*PayloadTypeRegistry.playC2S().register(DeleteRecipePayload.TYPE, DeleteRecipePayload.STREAM_CODEC);
        PayloadTypeRegistry.playS2C().register(ClientboundRemoveRecipePayload.TYPE, ClientboundRemoveRecipePayload.STREAM_CODEC);
        PayloadTypeRegistry.playS2C().register(ClientboundAddRecipePayload.TYPE, ClientboundAddRecipePayload.STREAM_CODEC);
        *///?} else {
        PayloadTypeRegistry.serverboundPlay().register(DeleteRecipePayload.TYPE, DeleteRecipePayload.STREAM_CODEC);
        PayloadTypeRegistry.clientboundPlay().register(ClientboundRemoveRecipePayload.TYPE, ClientboundRemoveRecipePayload.STREAM_CODEC);
        PayloadTypeRegistry.clientboundPlay().register(ClientboundAddRecipePayload.TYPE, ClientboundAddRecipePayload.STREAM_CODEC);
        //?}

        //? if >=1.21 {
        ServerPlayNetworking.registerGlobalReceiver(DeleteRecipePayload.TYPE,
                (payload, context) -> {
                    var server = context.server();
                    var player = context.player();
                    server.execute(() -> DeleteRecipePayload.handle(payload.recipeKey(), server, player));
                });
        //?}
    }
}
//?}
