package com.evandev.reliable_recipes.network;

import com.evandev.reliable_recipes.config.RecipeConfigIO;
import com.evandev.reliable_recipes.recipe.RecipeModifier;
import net.minecraft.ChatFormatting;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
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
                            ClientboundDeleteRecipePacket packet = new ClientboundDeleteRecipePacket(msg.recipeId);
                            PacketHandler.INSTANCE.send(net.minecraftforge.network.PacketDistributor.ALL.noArg(), packet);

                            player.sendSystemMessage(Component.literal("Reliable Recipes: Deleted " + msg.recipeId + " ")
                                    .append(Component.literal("[UNDO]")
                                            .withStyle(style -> style
                                                    .withColor(ChatFormatting.RED)
                                                    .withBold(true)
                                                    .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/reliable_recipes_undo " + msg.recipeId))
                                            )));
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