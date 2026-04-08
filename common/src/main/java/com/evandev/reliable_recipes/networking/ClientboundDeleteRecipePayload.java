package com.evandev.reliable_recipes.networking;

import com.evandev.reliable_recipes.Constants;
import com.evandev.reliable_recipes.client.SharedToastOverlay;
import com.evandev.reliable_recipes.config.ModConfig;
import com.evandev.reliable_recipes.recipe.RecipeModifier;
import dev.emi.emi.runtime.EmiReloadManager;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

public record ClientboundDeleteRecipePayload(Identifier recipeId) implements CustomPacketPayload {
    public static final Type<ClientboundDeleteRecipePayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath(Constants.MOD_ID, "client_delete_recipe"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ClientboundDeleteRecipePayload> STREAM_CODEC = StreamCodec.composite(
            Identifier.STREAM_CODEC,
            ClientboundDeleteRecipePayload::recipeId,
            ClientboundDeleteRecipePayload::new
    );

    public static void handle(Identifier recipeId, Minecraft client) {
        client.execute(() -> {
            if (client.getConnection() != null) {
                ItemStack outputIcon = ItemStack.EMPTY;
                var recipe = client.getConnection().getRecipeManager().byKey(recipeId).orElse(null);
                if (recipe != null && client.level != null) {
                    outputIcon = recipe.value().getResultItem(client.level.registryAccess());
                }

                boolean removed = RecipeModifier.removeRecipe(client.getConnection().getRecipeManager(), recipeId);
                if (removed) {
                    handleFeedback(recipeId, client, outputIcon);
                }
            }
        });
    }

    private static void handleFeedback(Identifier recipeId, Minecraft client, ItemStack outputIcon) {
        ModConfig config = ModConfig.get();

        if (config.reloadEmi) {
            EmiReloadManager.reload();
        }

        if (config.showChatMessages && client.player != null) {
            client.player.sendSystemMessage(Component.translatable("toast.reliable_recipes.deleted", recipeId)
                    .append(Component.translatable("toast.reliable_recipes.undo")
                            .withStyle(style -> style
                                    .withColor(ChatFormatting.RED)
                                    .withBold(true)
                                    .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/rrecipes_undo " + recipeId))
                            )));
        }

        if (config.showToast) {
            SharedToastOverlay.show(Component.literal("Recipe Deleted"), Component.literal(recipeId.getPath()), outputIcon);
        }
    }

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
