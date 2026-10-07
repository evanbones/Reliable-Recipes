package com.evandev.reliable_recipes.config;

import com.evandev.reliable_recipes.Constants;
import com.evandev.reliable_recipes.networking.ClientboundSyncConfigPayload;
import com.evandev.reliable_recipes.platform.Services;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Stream;

/**
 * Sends the server's rule files to clients, so recipe viewers and other client-side checks use the same rules as the
 * server.
 */
public class ConfigSync {

    private static final int MAX_TOTAL_CHARS = 900_000; // this is to prevent overly large packets
    private static final Map<String, Channel> CHANNELS = new LinkedHashMap<>();
    private static volatile Map<String, List<SyncedFile>> remoteFiles = Map.of();

    public static synchronized void register(String id, Supplier<List<SyncedFile>> snapshot, Runnable onClientChange) {
        CHANNELS.put(id, new Channel(snapshot, onClientChange));
    }

    /**
     * @return the rule files the server sent for this channel, or null to read rules from disk.
     */
    public static @Nullable List<SyncedFile> getRemoteFiles(String id) {
        return remoteFiles.get(id);
    }

    /**
     * Reads every JSON file in a directory.
     */
    public static List<SyncedFile> readDirectory(Path dir) {
        List<SyncedFile> files = new ArrayList<>();
        if (!Files.isDirectory(dir)) return files;

        try (Stream<Path> stream = Files.walk(dir)) {
            stream.filter(path -> Files.isRegularFile(path) && path.toString().endsWith(".json"))
                    .forEach(path -> {
                        try {
                            String relPath = dir.relativize(path).toString().replace('\\', '/');
                            files.add(new SyncedFile(relPath, Files.readString(path, StandardCharsets.UTF_8)));
                        } catch (IOException e) {
                            Constants.LOG.error("Failed to read config file for syncing: {}", path, e);
                        }
                    });
        } catch (IOException e) {
            Constants.LOG.error("Failed to walk config directory for syncing: {}", dir, e);
        }
        return files;
    }

    public static void sendTo(ServerPlayer player) {
        ClientboundSyncConfigPayload payload = createPayload();
        if (payload != null) {
            Services.PLATFORM.sendSyncConfigPacketToPlayer(player, payload);
        }
    }

    public static void sendToAll(MinecraftServer server) {
        ClientboundSyncConfigPayload payload = createPayload();
        if (payload != null) {
            server.getPlayerList().getPlayers().forEach(p -> Services.PLATFORM.sendSyncConfigPacketToPlayer(p, payload));
        }
    }

    private static synchronized @Nullable ClientboundSyncConfigPayload createPayload() {
        Map<String, List<SyncedFile>> channels = new LinkedHashMap<>();
        long totalChars = 0;
        for (Map.Entry<String, Channel> entry : CHANNELS.entrySet()) {
            List<SyncedFile> files = sorted(entry.getValue().snapshot().get());
            for (SyncedFile file : files) {
                totalChars += file.path().length() + file.content().length();
            }
            channels.put(entry.getKey(), files);
        }

        if (totalChars > MAX_TOTAL_CHARS) {
            Constants.LOG.error("Rule files are too large to sync to clients ({} characters, limit {}). Clients will use their own rule files.", totalChars, MAX_TOTAL_CHARS);
            return null;
        }
        return new ClientboundSyncConfigPayload(channels);
    }

    /**
     * Uses the rule files the server sent. Client only.
     *
     * @return whether the rules in effect changed for any channel.
     */
    public static boolean applyRemote(Map<String, List<SyncedFile>> received) {
        Map<String, List<SyncedFile>> previous = remoteFiles;
        Map<String, List<SyncedFile>> next = new HashMap<>();
        received.forEach((id, files) -> next.put(id, sorted(files)));
        remoteFiles = Map.copyOf(next);
        return notifyChanged(previous, remoteFiles);
    }

    /**
     * Goes back to reading rule files from disk, for example after leaving a server. Client only.
     *
     * @return whether the rules in effect changed for any channel.
     */
    public static boolean clearRemote() {
        Map<String, List<SyncedFile>> previous = remoteFiles;
        if (previous.isEmpty()) return false;

        remoteFiles = Map.of();
        return notifyChanged(previous, remoteFiles);
    }

    private static boolean notifyChanged(Map<String, List<SyncedFile>> before, Map<String, List<SyncedFile>> after) {
        List<Channel> channels;
        List<String> ids;
        synchronized (ConfigSync.class) {
            ids = new ArrayList<>(CHANNELS.keySet());
            channels = new ArrayList<>(CHANNELS.values());
        }

        boolean anyChanged = false;
        for (int i = 0; i < ids.size(); i++) {
            String id = ids.get(i);
            Channel channel = channels.get(i);
            if (Objects.equals(before.get(id), after.get(id))) continue;

            List<SyncedFile> local = null;
            List<SyncedFile> beforeFiles = before.get(id);
            List<SyncedFile> afterFiles = after.get(id);
            if (beforeFiles == null || afterFiles == null) {
                local = sorted(channel.snapshot().get());
            }
            if ((beforeFiles == null ? local : beforeFiles).equals(afterFiles == null ? local : afterFiles)) continue;

            try {
                channel.onClientChange().run();
            } catch (Exception e) {
                Constants.LOG.error("Failed to apply synced config for {}", id, e);
            }
            anyChanged = true;
        }
        return anyChanged;
    }

    private static List<SyncedFile> sorted(List<SyncedFile> files) {
        List<SyncedFile> copy = new ArrayList<>(files);
        copy.sort(Comparator.comparing(SyncedFile::path).thenComparing(SyncedFile::content));
        return List.copyOf(copy);
    }

    public record SyncedFile(String path, String content) {
    }

    private record Channel(Supplier<List<SyncedFile>> snapshot, Runnable onClientChange) {
    }
}
