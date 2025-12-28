package com.evandev.reliable_recipes.networking;

import com.evandev.reliable_recipes.Constants;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

public record DeleteRecipePayload(ResourceLocation recipeId) implements CustomPacketPayload {
	public static final Type<DeleteRecipePayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "delete_recipe"));
	public static final StreamCodec<RegistryFriendlyByteBuf, DeleteRecipePayload> STREAM_CODEC = StreamCodec.composite(
			ResourceLocation.STREAM_CODEC,
			DeleteRecipePayload::recipeId,
			DeleteRecipePayload::new
	);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
