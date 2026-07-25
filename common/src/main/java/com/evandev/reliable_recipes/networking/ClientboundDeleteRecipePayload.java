package com.evandev.reliable_recipes.networking;

import com.evandev.reliable_recipes.Constants;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

public record ClientboundDeleteRecipePayload(ResourceLocation recipeId) implements CustomPacketPayload {
    public static final Type<ClientboundDeleteRecipePayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "client_delete_recipe"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ClientboundDeleteRecipePayload> STREAM_CODEC = StreamCodec.composite(
            ResourceLocation.STREAM_CODEC,
            ClientboundDeleteRecipePayload::recipeId,
            ClientboundDeleteRecipePayload::new
    );

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
