package com.evandev.reliable_recipes;

import com.evandev.reliable_recipes.command.UndoCommand;
import com.evandev.reliable_recipes.config.RecipeConfigIO;
import com.evandev.reliable_recipes.recipe.RecipeModifier;
import com.evandev.reliable_recipes.recipe.TagModifier;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.FriendlyByteBuf;
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

        ServerPlayNetworking.registerGlobalReceiver(new ResourceLocation("reliable_recipes", "delete_recipe"),
                (server, player, handler, buf, responseSender) -> {
                    ResourceLocation id = buf.readResourceLocation();
                    server.execute(() -> {
                        if (player.hasPermissions(2)) {
                            RecipeConfigIO.addRemovalRule(id.toString());

                            boolean removed = RecipeModifier.removeRecipe(server.getRecipeManager(), id);

                            if (removed) {
                                Constants.LOG.info(String.valueOf(Component.translatable("toast.reliable_recipes.deleted", id)));

                                FriendlyByteBuf packetBuf = PacketByteBufs.create();
                                packetBuf.writeResourceLocation(id);

                                ResourceLocation packetId = new ResourceLocation("reliable_recipes", "client_delete_recipe");

                                server.getPlayerList().getPlayers().forEach(p ->
                                        ServerPlayNetworking.send(p, packetId, packetBuf)
                                );
                            } else {
                                player.sendSystemMessage(Component.translatable("toast.reliable_recipes.could_not_find_recipe", id));
                            }
                        } else {
                            player.sendSystemMessage(Component.translatable("toast.reliable_recipes.permission_denied"));
                        }
                    });
                });
    }
}