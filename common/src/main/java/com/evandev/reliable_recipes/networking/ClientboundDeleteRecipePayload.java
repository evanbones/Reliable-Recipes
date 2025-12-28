package com.evandev.reliable_recipes.networking;

import com.evandev.reliable_recipes.Constants;
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
import net.minecraft.resources.ResourceLocation;

public record ClientboundDeleteRecipePayload(ResourceLocation recipeId) implements CustomPacketPayload {
	public static final Type<ClientboundDeleteRecipePayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "client_delete_recipe"));
	public static final StreamCodec<RegistryFriendlyByteBuf, ClientboundDeleteRecipePayload> STREAM_CODEC = StreamCodec.composite(
			ResourceLocation.STREAM_CODEC,
			ClientboundDeleteRecipePayload::recipeId,
			ClientboundDeleteRecipePayload::new
	);

	public static void handle(ResourceLocation recipeId, Minecraft client) {
		client.execute(() -> {
			if (client.getConnection() != null) {
				boolean removed = RecipeModifier.removeRecipe(client.getConnection().getRecipeManager(), recipeId);
				if (removed) {
					handleFeedback(recipeId, client);
				}
			}
		});
	}


	private static void handleFeedback(ResourceLocation recipeId, Minecraft client) {
		ModConfig config = ModConfig.get();

		if (config.reloadEmi) {
			EmiReloadManager.reload();
		}

		if (config.showChatMessages) {
			if (client.player != null) {
				client.player.sendSystemMessage(Component.literal("Reliable Recipes: Deleted " + recipeId + " ")
						.append(Component.literal("[UNDO]")
								.withStyle(style -> style
										.withColor(ChatFormatting.RED)
										.withBold(true)
										.withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/reliable_recipes_undo " + recipeId))
								)));
			}
		}

//        if (config.showToast) {
//            SystemToast.add(Minecraft.getInstance().getToasts(),
//                    SystemToast.SystemToastIds.TUTORIAL_HINT,
//                    Component.literal("Recipe Deleted"),
//                    Component.literal(recipeId.toString()));
//        }
	}


	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
