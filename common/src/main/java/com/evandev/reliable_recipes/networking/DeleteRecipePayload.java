package com.evandev.reliable_recipes.networking;

import com.evandev.reliable_recipes.Constants;
import com.evandev.reliable_recipes.config.RecipeConfigIO;
import com.evandev.reliable_recipes.platform.Services;
import com.evandev.reliable_recipes.recipe.RecipeModifier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

public record DeleteRecipePayload(ResourceLocation recipeId) implements CustomPacketPayload {
	public static final Type<DeleteRecipePayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "delete_recipe"));
	public static final StreamCodec<RegistryFriendlyByteBuf, DeleteRecipePayload> STREAM_CODEC = StreamCodec.composite(
			ResourceLocation.STREAM_CODEC,
			DeleteRecipePayload::recipeId,
			DeleteRecipePayload::new
	);

	public static void handle(ResourceLocation id, MinecraftServer server, ServerPlayer player) {
		{
			if (player.hasPermissions(2)) {
				RecipeConfigIO.addRemovalRule(id.toString());

				boolean removed = RecipeModifier.removeRecipe(server.getRecipeManager(), id);

				if (removed) {
					Constants.LOG.info("Runtime deletion of recipe: {}", id);

					server.getPlayerList().getPlayers().forEach(p ->
							Services.PLATFORM.sendDeleteRecipePacketToPlayer(p, id)
					);
				} else {
					player.sendSystemMessage(Component.translatable("toast.reliable_recipes.could_not_find_recipe", id));
				}
			} else {
				player.sendSystemMessage(Component.translatable("toast.reliable_recipes.permission_denied"));
			}
		}
	}

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
