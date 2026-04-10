package com.evandev.reliable_recipes.platform;

import com.evandev.reliable_recipes.networking.ClientboundAddRecipePayload;
import com.evandev.reliable_recipes.networking.ClientboundRemoveRecipePayload;
import com.evandev.reliable_recipes.networking.DeleteRecipePayload;
import com.evandev.reliable_recipes.platform.services.IPlatformHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.neoforged.fml.ModList;
import net.neoforged.fml.loading.FMLLoader;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
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
        return !FMLLoader.getCurrent().isProduction();
    }

    @Override
    public Path getConfigDirectory() {
        return FMLPaths.CONFIGDIR.get();
    }

    @Override
    public void sendDeleteRecipePacket(ResourceKey<Recipe<?>> recipeKey) {
        ClientPacketDistributor.sendToServer(new DeleteRecipePayload(recipeKey));
    }

    @Override
    public void sendDeleteRecipePacketToPlayer(ServerPlayer player, ResourceKey<Recipe<?>> recipeKey) {
        PacketDistributor.sendToPlayer(player, new ClientboundRemoveRecipePayload(recipeKey));
    }

    @Override
    public void sendAddRecipePacketToPlayer(ServerPlayer player, RecipeHolder<?> recipeHolder) {
        PacketDistributor.sendToPlayer(player, new ClientboundAddRecipePayload(recipeHolder));
    }
}