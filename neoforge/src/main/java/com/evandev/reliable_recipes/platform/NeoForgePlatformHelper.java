package com.evandev.reliable_recipes.platform;

import com.evandev.reliable_recipes.networking.ClientboundDeleteRecipePayload;
import com.evandev.reliable_recipes.networking.DeleteRecipePayload;
import com.evandev.reliable_recipes.platform.services.IPlatformHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.fml.ModList;
import net.neoforged.fml.loading.FMLLoader;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.network.PacketDistributor;

import java.nio.file.Path;

public class NeoForgePlatformHelper implements IPlatformHelper {
    @Override
    public String getPlatformName() {
        return "NeoForge";
    }

    @Override
    public boolean isModLoaded(String modId) {
        return ModList.get().isLoaded(modId);
    }

    @Override
    public boolean isDevelopmentEnvironment() {
        return !FMLLoader.isProduction();
    }

    @Override
    public Path getConfigDirectory() {
        return FMLPaths.CONFIGDIR.get();
    }

    @Override
    public void sendDeleteRecipePacket(ResourceLocation recipeId) {
        PacketDistributor.sendToServer(new DeleteRecipePayload(recipeId));
    }

    @Override
    public void sendDeleteRecipePacketToPlayer(ServerPlayer player, ResourceLocation recipeId) {
        PacketDistributor.sendToPlayer(player, new ClientboundDeleteRecipePayload(recipeId));
    }
}