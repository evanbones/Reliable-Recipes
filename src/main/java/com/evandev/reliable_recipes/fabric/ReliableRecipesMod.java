package com.evandev.reliable_recipes.fabric;

//? if fabric {

import com.evandev.reliable_recipes.Constants;
import com.evandev.reliable_recipes.command.UndoCommand;
import com.evandev.reliable_recipes.config.ConfigSync;
import com.evandev.reliable_recipes.config.RecipeConfigIO;
import com.evandev.reliable_recipes.config.ModConfig;
import com.evandev.reliable_recipes.networking.ClientboundAddRecipePayload;
import com.evandev.reliable_recipes.networking.ClientboundRemoveRecipePayload;
import com.evandev.reliable_recipes.networking.ClientboundSyncConfigPayload;
import com.evandev.reliable_recipes.networking.DeleteRecipePayload;
import com.evandev.reliable_recipes.recipe.BrewingRegistration;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
//? if <1.21 {
/*import net.fabricmc.fabric.api.networking.v1.S2CPlayChannelEvents;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.TickTask;
*///?} else {
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
//?}

public class ReliableRecipesMod implements ModInitializer {

    @Override
    public void onInitialize() {
        ModConfig.load();

        BrewingRegistration.registerFabric();
        ConfigSync.register(Constants.MOD_ID, RecipeConfigIO::createSyncSnapshot, RecipeConfigIO::invalidateCache);

        ServerLifecycleEvents.SYNC_DATA_PACK_CONTENTS.register((player, joined) -> ConfigSync.sendTo(player));
        //? if <1.21 {
        /*S2CPlayChannelEvents.REGISTER.register((handler, sender, server, channels) -> {
            if (channels.contains(ClientboundSyncConfigPayload.TYPE.id())) {
                server.tell(new TickTask(server.getTickCount(), () -> {
                    if (!handler.player.hasDisconnected()) ConfigSync.sendTo(handler.player);
                }));
            }
        });
        *///?}

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
        PayloadTypeRegistry.playS2C().register(ClientboundSyncConfigPayload.TYPE, ClientboundSyncConfigPayload.STREAM_CODEC);
        *///?} else {
        PayloadTypeRegistry.serverboundPlay().register(DeleteRecipePayload.TYPE, DeleteRecipePayload.STREAM_CODEC);
        PayloadTypeRegistry.clientboundPlay().register(ClientboundRemoveRecipePayload.TYPE, ClientboundRemoveRecipePayload.STREAM_CODEC);
        PayloadTypeRegistry.clientboundPlay().register(ClientboundAddRecipePayload.TYPE, ClientboundAddRecipePayload.STREAM_CODEC);
        PayloadTypeRegistry.clientboundPlay().register(ClientboundSyncConfigPayload.TYPE, ClientboundSyncConfigPayload.STREAM_CODEC);
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
