package com.evandev.reliable_recipes.network;

import com.evandev.reliable_recipes.recipe.RecipeModifier;
import dev.emi.emi.runtime.EmiReloadManager;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class ClientboundDeleteRecipePacket {
    private final ResourceLocation recipeId;

    public ClientboundDeleteRecipePacket(ResourceLocation recipeId) {
        this.recipeId = recipeId;
    }

    public static void encode(ClientboundDeleteRecipePacket msg, FriendlyByteBuf buf) {
        buf.writeResourceLocation(msg.recipeId);
    }

    public static ClientboundDeleteRecipePacket decode(FriendlyByteBuf buf) {
        return new ClientboundDeleteRecipePacket(buf.readResourceLocation());
    }

    public static void handle(ClientboundDeleteRecipePacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc.getConnection() != null) {
                boolean removed = RecipeModifier.removeRecipe(mc.getConnection().getRecipeManager(), msg.recipeId);

                if (removed) {
                    EmiReloadManager.reload();
                }
            }
        });
        ctx.get().setPacketHandled(true);
    }
}