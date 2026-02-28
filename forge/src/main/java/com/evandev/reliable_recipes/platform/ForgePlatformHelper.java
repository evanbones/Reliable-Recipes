package com.evandev.reliable_recipes.platform;

import com.evandev.reliable_recipes.network.DeleteRecipePacket;
import com.evandev.reliable_recipes.network.PacketHandler;
import com.evandev.reliable_recipes.platform.services.IPlatformHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.loading.FMLLoader;
import net.minecraftforge.fml.loading.FMLPaths;

import java.nio.file.Path;

public class ForgePlatformHelper implements IPlatformHelper {
    @Override
    public String getPlatformName() {
        return "Forge";
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
        PacketHandler.INSTANCE.sendToServer(new DeleteRecipePacket(recipeId));
    }
}