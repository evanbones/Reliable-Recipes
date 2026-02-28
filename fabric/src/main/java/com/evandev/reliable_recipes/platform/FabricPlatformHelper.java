package com.evandev.reliable_recipes.platform;

import com.evandev.reliable_recipes.networking.ClientboundDeleteRecipePayload;
import com.evandev.reliable_recipes.networking.DeleteRecipePayload;
import com.evandev.reliable_recipes.platform.services.IPlatformHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import java.nio.file.Path;

public class FabricPlatformHelper implements IPlatformHelper {
    @Override
    public String getPlatformName() {
        return "Fabric";
    }

    @Override
    public boolean isModLoaded(String modId) {
        return FabricLoader.getInstance().isModLoaded(modId);
    }

    @Override
    public boolean isDevelopmentEnvironment() {
        return FabricLoader.getInstance().isDevelopmentEnvironment();
    }

    @Override
    public Path getConfigDirectory() {
        return FabricLoader.getInstance().getConfigDir();
    }

    @Override
    public void sendDeleteRecipePacket(ResourceLocation recipeId) {
        ClientPlayNetworking.send(new DeleteRecipePayload(recipeId));
    }

    @Override
    public void sendDeleteRecipePacketToPlayer(ServerPlayer player, ResourceLocation recipeId) {
        ServerPlayNetworking.send(player, new ClientboundDeleteRecipePayload(recipeId));
    }
}