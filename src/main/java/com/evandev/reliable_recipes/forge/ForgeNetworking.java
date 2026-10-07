package com.evandev.reliable_recipes.forge;

//? if forge {
/*import com.evandev.reliable_recipes.Constants;
import com.evandev.reliable_recipes.client.ClientRecipeSync;
import com.evandev.reliable_recipes.networking.ClientboundAddRecipePayload;
import com.evandev.reliable_recipes.networking.ClientboundRemoveRecipePayload;
import com.evandev.reliable_recipes.networking.ClientboundSyncConfigPayload;
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
    private static final SimpleChannel SYNC_CHANNEL = NetworkRegistry.newSimpleChannel(
            Identifier.fromNamespaceAndPath(Constants.MOD_ID, "sync"),
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
        register(SYNC_CHANNEL, 0, ClientboundSyncConfigPayload.class, ClientboundSyncConfigPayload.STREAM_CODEC, NetworkDirection.PLAY_TO_CLIENT,
                (payload, context) -> ClientRecipeSync.onConfigSynced(payload.channels()));
    }

    private static <T> void register(Class<T> type, StreamCodec<? super RegistryFriendlyByteBuf, T> codec, NetworkDirection direction, BiConsumer<T, NetworkEvent.Context> handler) {
        register(CHANNEL, nextId++, type, codec, direction, handler);
    }

    private static <T> void register(SimpleChannel channel, int id, Class<T> type, StreamCodec<? super RegistryFriendlyByteBuf, T> codec, NetworkDirection direction, BiConsumer<T, NetworkEvent.Context> handler) {
        channel.messageBuilder(type, id, direction)
                .encoder((payload, buf) -> codec.encode(new RegistryFriendlyByteBuf(buf), payload))
                .decoder(buf -> codec.decode(new RegistryFriendlyByteBuf(buf)))
                .consumerMainThread((payload, context) -> handler.accept(payload, context.get()))
                .add();
    }

    public static void sendToServer(Object payload) {
        CHANNEL.sendToServer(payload);
    }

    public static boolean isServerPresent() {
        var connection = net.minecraft.client.Minecraft.getInstance().getConnection();
        return connection != null && CHANNEL.isRemotePresent(connection.getConnection());
    }

    public static boolean isPresent(ServerPlayer player) {
        return CHANNEL.isRemotePresent(player.connection.connection);
    }

    public static void sendToPlayer(ServerPlayer player, Object payload) {
        if (isPresent(player)) {
            CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), payload);
        }
    }

    public static void sendSyncToPlayer(ServerPlayer player, ClientboundSyncConfigPayload payload) {
        if (SYNC_CHANNEL.isRemotePresent(player.connection.connection)) {
            SYNC_CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), payload);
        }
    }
}
*///?}
