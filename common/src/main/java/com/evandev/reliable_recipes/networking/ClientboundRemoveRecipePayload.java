package com.evandev.reliable_recipes.networking;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.crafting.Recipe;
import org.jspecify.annotations.NonNull;

public record ClientboundRemoveRecipePayload(ResourceKey<Recipe<?>> recipeKey) implements CustomPacketPayload {

    public static final Type<ClientboundRemoveRecipePayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath("reliable_recipes", "remove_recipe"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ClientboundRemoveRecipePayload> STREAM_CODEC = StreamCodec.composite(
            ResourceKey.streamCodec(Registries.RECIPE),
            ClientboundRemoveRecipePayload::recipeKey,
            ClientboundRemoveRecipePayload::new
    );

    @Override
    public @NonNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}