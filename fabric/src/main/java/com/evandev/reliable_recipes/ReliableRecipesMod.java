package com.evandev.reliable_recipes;

import com.evandev.reliable_recipes.command.UndoCommand;
import com.evandev.reliable_recipes.networking.ClientboundAddRecipePayload;
import com.evandev.reliable_recipes.networking.ClientboundRemoveRecipePayload;
import com.evandev.reliable_recipes.networking.DeleteRecipeByOutputPayload;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.world.item.ItemStack;

public class ReliableRecipesMod implements ModInitializer {

    @Override
    public void onInitialize() {
        CommonClass.init();

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                UndoCommand.register(dispatcher)
        );

        PayloadTypeRegistry.clientboundPlay().register(ClientboundRemoveRecipePayload.TYPE, ClientboundRemoveRecipePayload.STREAM_CODEC);
        PayloadTypeRegistry.clientboundPlay().register(ClientboundAddRecipePayload.TYPE, ClientboundAddRecipePayload.STREAM_CODEC);
        PayloadTypeRegistry.serverboundPlay().register(DeleteRecipeByOutputPayload.TYPE, DeleteRecipeByOutputPayload.STREAM_CODEC);

        ServerPlayNetworking.registerGlobalReceiver(DeleteRecipeByOutputPayload.TYPE,
                (payload, context) -> {
                    ItemStack output = payload.output();
                    var server = context.server();
                    var player = context.player();
                    server.execute(() -> DeleteRecipeByOutputPayload.handle(output, server, player));
                });
    }
}