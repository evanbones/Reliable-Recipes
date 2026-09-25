package com.evandev.reliable_recipes.platform;

//? if fabric {
import com.evandev.reliable_recipes.networking.ClientboundAddRecipePayload;
import com.evandev.reliable_recipes.networking.ClientboundRemoveRecipePayload;
import com.evandev.reliable_recipes.networking.DeleteRecipePayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.loader.api.FabricLoader;
//?}
//? if neoforge {
/*import com.evandev.reliable_recipes.networking.ClientboundAddRecipePayload;
import com.evandev.reliable_recipes.networking.ClientboundRemoveRecipePayload;
import com.evandev.reliable_recipes.networking.DeleteRecipePayload;
import net.neoforged.fml.ModList;
import net.neoforged.fml.loading.FMLLoader;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.neoforged.neoforge.network.PacketDistributor;
*///?}
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;

import java.nio.file.Path;

public class Services {
    public static final Services PLATFORM = new Services();

    public String getPlatformName() {
        //? if fabric {
        return "Fabric";
        //?}
        //? if neoforge {
        /*return "NeoForge";
        *///?}
    }

    public boolean isModLoaded(String modId) {
        //? if fabric {
        return FabricLoader.getInstance().isModLoaded(modId);
        //?}
        //? if neoforge {
        /*return ModList.get().isLoaded(modId);
        *///?}
    }

    public boolean isDevelopmentEnvironment() {
        //? if fabric {
        return FabricLoader.getInstance().isDevelopmentEnvironment();
        //?}
        //? if neoforge {
        /*return !FMLLoader.getCurrent().isProduction();
        *///?}
    }

    public Path getConfigDirectory() {
        //? if fabric {
        return FabricLoader.getInstance().getConfigDir();
        //?}
        //? if neoforge {
        /*return FMLPaths.CONFIGDIR.get();
        *///?}
    }

    public void sendDeleteRecipePacket(ResourceKey<Recipe<?>> recipeKey) {
        //? if fabric {
        ClientPlayNetworking.send(new DeleteRecipePayload(recipeKey));
        //?}
        //? if neoforge {
        /*ClientPacketDistributor.sendToServer(new DeleteRecipePayload(recipeKey));
        *///?}
    }

    public void sendDeleteRecipePacketToPlayer(ServerPlayer player, ResourceKey<Recipe<?>> recipeKey) {
        //? if fabric {
        ServerPlayNetworking.send(player, new ClientboundRemoveRecipePayload(recipeKey));
        //?}
        //? if neoforge {
        /*PacketDistributor.sendToPlayer(player, new ClientboundRemoveRecipePayload(recipeKey));
        *///?}
    }

    public void sendAddRecipePacketToPlayer(ServerPlayer player, RecipeHolder<?> recipeHolder) {
        //? if fabric {
        ServerPlayNetworking.send(player, new ClientboundAddRecipePayload(recipeHolder));
        //?}
        //? if neoforge {
        /*PacketDistributor.sendToPlayer(player, new ClientboundAddRecipePayload(recipeHolder));
        *///?}
    }
}