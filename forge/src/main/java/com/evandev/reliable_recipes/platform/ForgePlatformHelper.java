package com.evandev.reliable_recipes.platform;

import com.evandev.reliable_recipes.compat.ItemObliteratorCompat;
import com.evandev.reliable_recipes.network.DeleteRecipePacket;
import com.evandev.reliable_recipes.network.PacketHandler;
import com.evandev.reliable_recipes.platform.services.IPlatformHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.loading.FMLLoader;
import net.minecraftforge.fml.loading.FMLPaths;

import java.nio.file.Path;

public class ForgePlatformHelper implements IPlatformHelper {
    private final boolean hasItemObliterator = ModList.get().isLoaded("item_obliterator");

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
        PacketHandler.INSTANCE.sendToServer(new DeleteRecipePacket(recipeId));
    }
}