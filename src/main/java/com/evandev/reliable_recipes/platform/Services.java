package com.evandev.reliable_recipes.platform;

//? if fabric {
import com.evandev.reliable_recipes.networking.ClientboundAddRecipePayload;
import com.evandev.reliable_recipes.networking.ClientboundRemoveRecipePayload;
import com.evandev.reliable_recipes.networking.DeleteRecipePayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.loader.api.FabricLoader;
//?}
//? if forge {
/*import com.evandev.reliable_recipes.forge.ForgeNetworking;
import com.evandev.reliable_recipes.networking.ClientboundAddRecipePayload;
import com.evandev.reliable_recipes.networking.ClientboundRemoveRecipePayload;
import com.evandev.reliable_recipes.networking.DeleteRecipePayload;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.loading.FMLLoader;
import net.minecraftforge.fml.loading.FMLPaths;
*///?}
//? if neoforge {
/*import com.evandev.reliable_recipes.networking.ClientboundAddRecipePayload;
import com.evandev.reliable_recipes.networking.ClientboundRemoveRecipePayload;
import com.evandev.reliable_recipes.networking.DeleteRecipePayload;
import net.neoforged.fml.ModList;
import net.neoforged.fml.loading.FMLLoader;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.network.PacketDistributor;
*///?}
//? if neoforge && >=1.21.2 {
/*import net.neoforged.neoforge.client.network.ClientPacketDistributor;
*///?}
//? if <1.21 {
/*import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
*///?}
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;

import java.nio.file.Path;

public class Services {
    public static final Services PLATFORM = new Services();

    public static Path configDirectoryOverride = null;

    public boolean isModLoaded(String modId) {
        //? if fabric {
        return FabricLoader.getInstance().isModLoaded(modId);
        //?}
        //? if forge || neoforge {
        /*return ModList.get().isLoaded(modId);
        *///?}
    }

    public boolean isDevelopmentEnvironment() {
        //? if fabric {
        return FabricLoader.getInstance().isDevelopmentEnvironment();
        //?}
        //? if (forge || neoforge) && <1.21.2 {
        /*return !FMLLoader.isProduction();
        *///?} else if neoforge {
        /*return !FMLLoader.getCurrent().isProduction();
        *///?}
    }

    public Path getConfigDirectory() {
        if (configDirectoryOverride != null) {
            return configDirectoryOverride;
        }
        //? if fabric {
        return FabricLoader.getInstance().getConfigDir();
        //?}
        //? if forge || neoforge {
        /*return FMLPaths.CONFIGDIR.get();
        *///?}
    }

    public void sendDeleteRecipePacket(ResourceKey<Recipe<?>> recipeKey) {
        //? if fabric && <1.21 {
        /*ClientPlayNetworking.send(DeleteRecipePayload.TYPE.id(), encode(DeleteRecipePayload.STREAM_CODEC, new DeleteRecipePayload(recipeKey)));
        *///?} else if fabric {
        ClientPlayNetworking.send(new DeleteRecipePayload(recipeKey));
        //?}
        //? if forge {
        /*ForgeNetworking.sendToServer(new DeleteRecipePayload(recipeKey));
        *///?}
        //? if neoforge && <1.21.2 {
        /*PacketDistributor.sendToServer(new DeleteRecipePayload(recipeKey));
        *///?} else if neoforge {
        /*ClientPacketDistributor.sendToServer(new DeleteRecipePayload(recipeKey));
        *///?}
    }

    public void sendDeleteRecipePacketToPlayer(ServerPlayer player, ResourceKey<Recipe<?>> recipeKey) {
        //? if fabric && <1.21 {
        /*ServerPlayNetworking.send(player, ClientboundRemoveRecipePayload.TYPE.id(), encode(ClientboundRemoveRecipePayload.STREAM_CODEC, new ClientboundRemoveRecipePayload(recipeKey)));
        *///?} else if fabric {
        ServerPlayNetworking.send(player, new ClientboundRemoveRecipePayload(recipeKey));
        //?}
        //? if forge {
        /*ForgeNetworking.sendToPlayer(player, new ClientboundRemoveRecipePayload(recipeKey));
        *///?}
        //? if neoforge {
        /*PacketDistributor.sendToPlayer(player, new ClientboundRemoveRecipePayload(recipeKey));
        *///?}
    }

    public void sendAddRecipePacketToPlayer(ServerPlayer player, RecipeHolder<?> recipeHolder) {
        //? if fabric && <1.21 {
        /*ServerPlayNetworking.send(player, ClientboundAddRecipePayload.TYPE.id(), encode(ClientboundAddRecipePayload.STREAM_CODEC, new ClientboundAddRecipePayload(recipeHolder)));
        *///?} else if fabric {
        ServerPlayNetworking.send(player, new ClientboundAddRecipePayload(recipeHolder));
        //?}
        //? if forge {
        /*ForgeNetworking.sendToPlayer(player, new ClientboundAddRecipePayload(recipeHolder));
        *///?}
        //? if neoforge {
        /*PacketDistributor.sendToPlayer(player, new ClientboundAddRecipePayload(recipeHolder));
        *///?}
    }

    //? if <1.21 {
    /*public static <T> FriendlyByteBuf encode(StreamCodec<? super RegistryFriendlyByteBuf, T> codec, T value) {
        RegistryFriendlyByteBuf buf = new RegistryFriendlyByteBuf(Unpooled.buffer());
        codec.encode(buf, value);
        return buf;
    }
    *///?}
}
