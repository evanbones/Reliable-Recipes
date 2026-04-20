package com.evandev.reliable_recipes.networking;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.crafting.RecipeHolder;
import org.jetbrains.annotations.NotNull;

public record ClientboundAddRecipePayload(RecipeHolder<?> recipeHolder) implements CustomPacketPayload {
    public static final Type<ClientboundAddRecipePayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath("reliable_recipes", "add_recipe"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ClientboundAddRecipePayload> STREAM_CODEC = StreamCodec.composite(
            RecipeHolder.STREAM_CODEC,
            ClientboundAddRecipePayload::recipeHolder,
            ClientboundAddRecipePayload::new
    );

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}