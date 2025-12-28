package com.evandev.reliable_recipes;

import com.evandev.reliable_recipes.command.UndoCommand;
import com.evandev.reliable_recipes.config.RecipeConfigIO;
import com.evandev.reliable_recipes.networking.ClientboundDeleteRecipePayload;
import com.evandev.reliable_recipes.networking.DeleteRecipePayload;
import com.evandev.reliable_recipes.recipe.RecipeModifier;
import com.evandev.reliable_recipes.recipe.TagModifier;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundUpdateRecipesPacket;
import net.minecraft.resources.ResourceLocation;

public class ReliableRecipesMod implements ModInitializer {

    @Override
    public void onInitialize() {
        CommonClass.init();

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> UndoCommand.register(dispatcher));

        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            TagModifier.apply();

            // Sync recipes to all players
            server.getPlayerList().getPlayers().forEach(player ->
                    player.connection.send(new ClientboundUpdateRecipesPacket(server.getRecipeManager().getRecipes()))
            );
        });

        ServerLifecycleEvents.END_DATA_PACK_RELOAD.register((server, resourceManager, success) -> {
            if (success) {
                TagModifier.apply();
                server.getPlayerList().getPlayers().forEach(player ->
                        player.connection.send(new ClientboundUpdateRecipesPacket(server.getRecipeManager().getRecipes()))
                );
            }
        });

        PayloadTypeRegistry.playS2C().register(ClientboundDeleteRecipePayload.TYPE, ClientboundDeleteRecipePayload.STREAM_CODEC);
        PayloadTypeRegistry.playC2S().register(DeleteRecipePayload.TYPE, DeleteRecipePayload.STREAM_CODEC);

        ServerPlayNetworking.registerGlobalReceiver(DeleteRecipePayload.TYPE,
                (payload, context) -> {
                    ResourceLocation id = payload.recipeId();
                    var server = context.server();
                    var player = context.player();
                    server.execute(() -> DeleteRecipePayload.handle(id, server, player));
                });
    }
}