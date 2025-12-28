package com.evandev.reliable_recipes.network;

import com.evandev.reliable_recipes.config.RecipeConfigIO;
import com.evandev.reliable_recipes.recipe.RecipeModifier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundUpdateRecipesPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class DeleteRecipePacket {
    private final ResourceLocation recipeId;

    public DeleteRecipePacket(ResourceLocation recipeId) {
        this.recipeId = recipeId;
    }

    public static void encode(DeleteRecipePacket msg, FriendlyByteBuf buf) {
        buf.writeResourceLocation(msg.recipeId);
    }

    public static DeleteRecipePacket decode(FriendlyByteBuf buf) {
        return new DeleteRecipePacket(buf.readResourceLocation());
    }

    public static void handle(DeleteRecipePacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player != null) {
                if (player.hasPermissions(2)) {
                    RecipeConfigIO.addRemovalRule(msg.recipeId.toString());

                    MinecraftServer server = player.getServer();
                    if (server != null) {
                        boolean removed = RecipeModifier.removeRecipe(server.getRecipeManager(), msg.recipeId);

                        if (removed) {
                            server.getPlayerList().getPlayers().forEach(p ->
                                    p.connection.send(new ClientboundUpdateRecipesPacket(server.getRecipeManager().getRecipes()))
                            );
                            player.sendSystemMessage(Component.literal("Reliable Recipes: Deleted " + msg.recipeId));
                        } else {
                            player.sendSystemMessage(Component.literal("Reliable Recipes: Could not find recipe " + msg.recipeId));
                        }
                    }
                } else {
                    player.sendSystemMessage(Component.literal("Reliable Recipes: You need OP to delete recipes."));
                }
            }
        });
        ctx.get().setPacketHandled(true);
    }
}