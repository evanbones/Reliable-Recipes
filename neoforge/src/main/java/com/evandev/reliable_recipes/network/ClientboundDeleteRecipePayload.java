package com.evandev.reliable_recipes.network;

import com.evandev.reliable_recipes.Constants;
import com.evandev.reliable_recipes.config.ModConfig;
import com.evandev.reliable_recipes.recipe.RecipeModifier;
import dev.emi.emi.runtime.EmiReloadManager;
import io.netty.buffer.ByteBuf;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

public record ClientboundDeleteRecipePayload(ResourceLocation recipeId) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<ClientboundDeleteRecipePayload> TYPE = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "client_delete_recipe"));

    public static final StreamCodec<ByteBuf, ClientboundDeleteRecipePayload> CODEC = StreamCodec.composite(
            ResourceLocation.STREAM_CODEC, ClientboundDeleteRecipePayload::recipeId,
            ClientboundDeleteRecipePayload::new
    );

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(ClientboundDeleteRecipePayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc.getConnection() != null) {
                boolean removed = RecipeModifier.removeRecipe(mc.getConnection().getRecipeManager(), payload.recipeId());

                if (removed) {
                    ModConfig config = ModConfig.get();

                    if (config.reloadEmi) {
                        EmiReloadManager.reload();
                    }

                    if (config.showChatMessages && mc.player != null) {
                        mc.player.sendSystemMessage(Component.literal("Reliable Recipes: Deleted " + payload.recipeId() + " ")
                                .append(Component.literal("[UNDO]")
                                        .withStyle(style -> style
                                                .withColor(ChatFormatting.RED)
                                                .withBold(true)
                                                .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/reliable_recipes_undo " + payload.recipeId()))
                                        )));
                    }

                    if (config.showToast) {
                        SystemToast.add(mc.getToasts(),
                                SystemToast.SystemToastId.PERIODIC_NOTIFICATION,
                                Component.literal("Recipe Deleted"),
                                Component.literal(payload.recipeId().toString()));
                    }
                }
            }
        });
    }
}