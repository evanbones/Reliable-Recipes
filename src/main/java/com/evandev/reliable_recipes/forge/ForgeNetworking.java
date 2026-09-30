package com.evandev.reliable_recipes.forge;

//? if forge {
/*import com.evandev.reliable_recipes.Constants;
import com.evandev.reliable_recipes.client.ClientRecipeSync;
import com.evandev.reliable_recipes.networking.ClientboundAddRecipePayload;
import com.evandev.reliable_recipes.networking.ClientboundRemoveRecipePayload;
import com.evandev.reliable_recipes.networking.DeleteRecipePayload;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.function.BiConsumer;

public class ForgeNetworking {
    private static final String PROTOCOL_VERSION = "1";
    private static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            Identifier.fromNamespaceAndPath(Constants.MOD_ID, "main"),
            () -> PROTOCOL_VERSION,
            NetworkRegistry.acceptMissingOr(PROTOCOL_VERSION),
            NetworkRegistry.acceptMissingOr(PROTOCOL_VERSION)
    );
    private static int nextId = 0;

    public static void register() {
        register(DeleteRecipePayload.class, DeleteRecipePayload.STREAM_CODEC, NetworkDirection.PLAY_TO_SERVER, (payload, context) -> {
            ServerPlayer player = context.getSender();
            if (player != null) {
                DeleteRecipePayload.handle(payload.recipeKey(), player.getServer(), player);
            }
        });
        register(ClientboundRemoveRecipePayload.class, ClientboundRemoveRecipePayload.STREAM_CODEC, NetworkDirection.PLAY_TO_CLIENT,
                (payload, context) -> ClientRecipeSync.onRecipeRemoved(payload.recipeKey()));
        register(ClientboundAddRecipePayload.class, ClientboundAddRecipePayload.STREAM_CODEC, NetworkDirection.PLAY_TO_CLIENT,
                (payload, context) -> ClientRecipeSync.onRecipeAdded(payload.recipeHolder()));
    }

    private static <T> void register(Class<T> type, StreamCodec<? super RegistryFriendlyByteBuf, T> codec, NetworkDirection direction, BiConsumer<T, NetworkEvent.Context> handler) {
        CHANNEL.messageBuilder(type, nextId++, direction)
                .encoder((payload, buf) -> codec.encode(new RegistryFriendlyByteBuf(buf), payload))
                .decoder(buf -> codec.decode(new RegistryFriendlyByteBuf(buf)))
                .consumerMainThread((payload, context) -> handler.accept(payload, context.get()))
                .add();
    }

    public static void sendToServer(Object payload) {
        CHANNEL.sendToServer(payload);
    }

    public static void sendToPlayer(ServerPlayer player, Object payload) {
        if (CHANNEL.isRemotePresent(player.connection.connection)) {
            CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), payload);
        }
    }
}
*///?}
