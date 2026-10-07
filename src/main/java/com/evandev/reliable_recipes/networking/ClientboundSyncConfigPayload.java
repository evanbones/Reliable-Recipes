package com.evandev.reliable_recipes.networking;

import com.evandev.reliable_recipes.Constants;
import com.evandev.reliable_recipes.config.ConfigSync;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Carries the server's rule files for each {@link ConfigSync} channel.
 */
public record ClientboundSyncConfigPayload(
        Map<String, List<ConfigSync.SyncedFile>> channels) implements CustomPacketPayload {
    public static final Type<ClientboundSyncConfigPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath(Constants.MOD_ID, "sync_config"));

    private static final int MAX_STRING_LENGTH = 1 << 20;

    public static final StreamCodec<RegistryFriendlyByteBuf, ClientboundSyncConfigPayload> STREAM_CODEC = StreamCodec.of(
            ClientboundSyncConfigPayload::write,
            ClientboundSyncConfigPayload::read
    );

    private static void write(RegistryFriendlyByteBuf buf, ClientboundSyncConfigPayload payload) {
        buf.writeVarInt(payload.channels().size());
        for (Map.Entry<String, List<ConfigSync.SyncedFile>> channel : payload.channels().entrySet()) {
            buf.writeUtf(channel.getKey());
            buf.writeVarInt(channel.getValue().size());
            for (ConfigSync.SyncedFile file : channel.getValue()) {
                buf.writeUtf(file.path(), MAX_STRING_LENGTH);
                buf.writeUtf(file.content(), MAX_STRING_LENGTH);
            }
        }
    }

    private static ClientboundSyncConfigPayload read(RegistryFriendlyByteBuf buf) {
        int channelCount = buf.readVarInt();
        Map<String, List<ConfigSync.SyncedFile>> channels = new LinkedHashMap<>();
        for (int i = 0; i < channelCount; i++) {
            String id = buf.readUtf();
            int fileCount = buf.readVarInt();
            List<ConfigSync.SyncedFile> files = new ArrayList<>();
            for (int j = 0; j < fileCount; j++) {
                files.add(new ConfigSync.SyncedFile(buf.readUtf(MAX_STRING_LENGTH), buf.readUtf(MAX_STRING_LENGTH)));
            }
            channels.put(id, files);
        }
        return new ClientboundSyncConfigPayload(channels);
    }

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
