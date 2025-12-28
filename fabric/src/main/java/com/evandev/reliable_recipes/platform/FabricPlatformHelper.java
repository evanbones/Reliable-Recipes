package com.evandev.reliable_recipes.platform;

import com.evandev.reliable_recipes.compat.ItemObliteratorCompat;
import com.evandev.reliable_recipes.networking.DeleteRecipePayload;
import com.evandev.reliable_recipes.platform.services.IPlatformHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.nio.file.Path;

public class FabricPlatformHelper implements IPlatformHelper {
    private final boolean hasItemObliterator = FabricLoader.getInstance().isModLoaded("item_obliterator");

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
    public boolean hasItemHidingCapabilities() {
        return hasItemObliterator;
    }

    @Override
    public boolean isItemHidden(ItemStack stack) {
        if (hasItemObliterator) {
            return ItemObliteratorCompat.shouldHide(stack);
        }
        return false;
    }

    @Override
    public void sendDeleteRecipePacket(ResourceLocation recipeId) {
        ClientPlayNetworking.send(new DeleteRecipePayload(recipeId));
    }
}