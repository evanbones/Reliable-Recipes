package com.evandev.reliable_recipes.util.legacy;

//? if <1.21 {
/*import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.game.ClientboundUpdateRecipesPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.crafting.Recipe;

public record RecipeHolder<T extends Recipe<?>>(Identifier id, T value) {
    public static final StreamCodec<FriendlyByteBuf, RecipeHolder<?>> STREAM_CODEC = StreamCodec.of(
            (buf, holder) -> ClientboundUpdateRecipesPacket.toNetwork(buf, holder.value()),
            buf -> of(ClientboundUpdateRecipesPacket.fromNetwork(buf))
    );

    public static <T extends Recipe<?>> RecipeHolder<T> of(T recipe) {
        return new RecipeHolder<>(recipe.getId(), recipe);
    }
}
*///?}
