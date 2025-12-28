package com.evandev.reliable_recipes.network;

import com.evandev.reliable_recipes.config.RecipeConfigIO;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
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
            if (player != null && player.hasPermissions(2)) { // OP check
                RecipeConfigIO.addRemovalRule(msg.recipeId.toString());

                Minecraft.getInstance().getToasts().addToast(SystemToast.multiline(
                        Minecraft.getInstance(),
                        SystemToast.SystemToastIds.TUTORIAL_HINT,
                        Component.literal("Reliable Recipes"),
                        Component.literal("Recipe deleted. /reload to apply.")
                ));
            }
        });
        ctx.get().setPacketHandled(true);
    }
}