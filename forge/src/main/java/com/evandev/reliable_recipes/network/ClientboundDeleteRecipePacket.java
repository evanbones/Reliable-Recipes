package com.evandev.reliable_recipes.network;

import com.evandev.reliable_recipes.config.ModConfig;
import com.evandev.reliable_recipes.recipe.RecipeModifier;
import dev.emi.emi.runtime.EmiReloadManager;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
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
                    ModConfig config = ModConfig.get();

                    if (config.reloadEmi) {
                        EmiReloadManager.reload();
                    }

                    if (config.showChatMessages && mc.player != null) {
                        mc.player.sendSystemMessage(Component.literal("Reliable Recipes: Deleted " + msg.recipeId + " ")
                                .append(Component.literal("[UNDO]")
                                        .withStyle(style -> style
                                                .withColor(ChatFormatting.RED)
                                                .withBold(true)
                                                .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/reliable_recipes_undo " + msg.recipeId))
                                        )));
                    }

                    if (config.showToast) {
                        SystemToast.add(mc.getToasts(),
                                SystemToast.SystemToastIds.TUTORIAL_HINT,
                                Component.literal("Recipe Deleted"),
                                Component.literal(msg.recipeId.toString()));
                    }
                }
            }
        });
        ctx.get().setPacketHandled(true);
    }
}