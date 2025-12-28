package com.evandev.reliable_recipes.network;

import com.evandev.reliable_recipes.Constants;
import com.evandev.reliable_recipes.config.RecipeConfigIO;
import com.evandev.reliable_recipes.recipe.RecipeModifier;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record DeleteRecipePayload(ResourceLocation recipeId) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<DeleteRecipePayload> TYPE = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "delete_recipe"));

    public static final StreamCodec<ByteBuf, DeleteRecipePayload> CODEC = StreamCodec.composite(
            ResourceLocation.STREAM_CODEC, DeleteRecipePayload::recipeId,
            DeleteRecipePayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(DeleteRecipePayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                if (player.hasPermissions(2)) {
                    RecipeConfigIO.addRemovalRule(payload.recipeId().toString());

                    MinecraftServer server = player.getServer();
                    if (server != null) {
                        boolean removed = RecipeModifier.removeRecipe(server.getRecipeManager(), payload.recipeId());

                        if (removed) {
                            Constants.LOG.info("Runtime deletion of recipe: {}", payload.recipeId());

                            // Send update to all clients
                            ClientboundDeleteRecipePayload clientPayload = new ClientboundDeleteRecipePayload(payload.recipeId());
                            PacketDistributor.sendToAllPlayers(clientPayload);

                        } else {
                            player.sendSystemMessage(Component.literal("Reliable Recipes: Could not find recipe " + payload.recipeId()));
                        }
                    }
                } else {
                    player.sendSystemMessage(Component.literal("Reliable Recipes: You need OP to delete recipes."));
                }
            }
        });
    }
}