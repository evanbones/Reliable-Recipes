package com.evandev.reliable_recipes.platform;

import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;

import java.nio.file.Path;
import java.util.Optional;
import java.util.function.Predicate;
import com.google.gson.JsonElement;

//? if fabric {
import com.evandev.reliable_recipes.networking.ClientboundAddRecipePayload;
import com.evandev.reliable_recipes.networking.ClientboundRemoveRecipePayload;
import com.evandev.reliable_recipes.networking.ClientboundSyncConfigPayload;
import com.evandev.reliable_recipes.networking.DeleteRecipePayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.loader.api.FabricLoader;
//?}
//? if forge {
/*import com.evandev.reliable_recipes.forge.ForgeNetworking;
import com.evandev.reliable_recipes.networking.ClientboundAddRecipePayload;
import com.evandev.reliable_recipes.networking.ClientboundRemoveRecipePayload;
import com.evandev.reliable_recipes.networking.ClientboundSyncConfigPayload;
import com.evandev.reliable_recipes.networking.DeleteRecipePayload;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.loading.FMLLoader;
import net.minecraftforge.fml.loading.FMLPaths;
*///?}
//? if neoforge {
/*import com.evandev.reliable_recipes.networking.ClientboundAddRecipePayload;
import com.evandev.reliable_recipes.networking.ClientboundRemoveRecipePayload;
import com.evandev.reliable_recipes.networking.ClientboundSyncConfigPayload;
import com.evandev.reliable_recipes.networking.DeleteRecipePayload;
import net.neoforged.fml.ModList;
import net.neoforged.fml.loading.FMLLoader;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.network.PacketDistributor;
*///?}
//? if neoforge && >=1.21.2 {
/*import net.neoforged.neoforge.client.network.ClientPacketDistributor;
*///?}
//? if fabric && <1.21 {
/*import net.fabricmc.fabric.api.resource.conditions.v1.ResourceConditions;
*///?} else if fabric {
import net.fabricmc.fabric.api.resource.conditions.v1.ResourceCondition;
import net.fabricmc.fabric.api.resource.conditions.v1.ResourceConditions;
//?}
//? if neoforge {
/*import net.neoforged.neoforge.common.conditions.ConditionalOps;
import net.neoforged.neoforge.common.conditions.ICondition;
*///?}
//? if fabric && >=1.21 && <1.21.2 {
/*import com.evandev.reliable_recipes.mixin.accessor.RecipeManagerAccessor;
*///?} else if neoforge && <1.21.2 {
/*import com.evandev.reliable_recipes.mixin.accessor.ContextAwareReloadListenerAccessor;
*///?}
//? if >=1.21 {
import net.minecraft.core.HolderLookup;
//?}
//? if fabric && >26.2 {
/*import net.minecraft.core.HolderGetter;
*///?}
//? if <1.21.2 {
/*import net.minecraft.world.item.crafting.RecipeManager;
*///?} else {
import net.minecraft.core.Registry;
import net.minecraft.resources.RegistryOps;
import net.minecraft.server.ReloadableServerResources;
//?}
//? if <1.21 {
/*import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
*///?}

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

    /**
     * Checks the loader's load conditions ({@code fabric:load_conditions} or {@code neoforge:conditions}) on recipes
     * added from reliable_recipes/. Forge checks its own conditions when the recipes are parsed.
     **/
    //? if <1.21.2 {
    /*
    public Predicate<JsonObject> recipeConditions(RecipeManager manager) {
        //? if fabric && <1.21 {
        /^return ResourceConditions::objectMatchesConditions;
        ^///?} else if fabric {
        /^HolderLookup.Provider registries = ((RecipeManagerAccessor) manager).reliableRecipes$getRegistries();
        return json -> !json.has(ResourceConditions.CONDITIONS_KEY) || ResourceCondition.CONDITION_CODEC
                .parse(JsonOps.INSTANCE, json.get(ResourceConditions.CONDITIONS_KEY)).getOrThrow().test(registries);
        ^///?} else if neoforge {
        /^ConditionalOps<JsonElement> ops = ((ContextAwareReloadListenerAccessor) manager).reliableRecipes$makeConditionalOps();
        return json -> ICondition.conditionsMatched(ops, json);
        ^///?} else {
        /^return json -> true;
        ^///?}
    }
    *///?} else {
    public Predicate<JsonObject> recipeConditions(ReloadableServerResources resources, HolderLookup.Provider registries) {
        //? if fabric {
        RegistryOps.RegistryInfoLookup lookup = new RegistryOps.RegistryInfoLookup() {
            //? if <=26.2 {
            @Override
            public <T> Optional<RegistryOps.RegistryInfo<T>> lookup(ResourceKey<? extends Registry<? extends T>> key) {
                return registries.lookup(key).map(RegistryOps.RegistryInfo::fromRegistryLookup);
            }
            //?} else {
            /*@Override
            public <T> Optional<HolderGetter<T>> lookup(ResourceKey<? extends Registry<? extends T>> key) {
                return registries.lookup(key).map(registry -> registry);
            }
            *///?}
        };
        return json -> !json.has(ResourceConditions.CONDITIONS_KEY) || ResourceCondition.CONDITION_CODEC
                .parse(JsonOps.INSTANCE, json.get(ResourceConditions.CONDITIONS_KEY)).getOrThrow().test(lookup);
        //?} else {
        /*ConditionalOps<JsonElement> ops = new ConditionalOps<>(registries.createSerializationContext(JsonOps.INSTANCE), resources.getConditionContext());
        return json -> ICondition.conditionsMatched(ops, json);
        *///?}
    }
    //?}

    /**
     * Whether the connected server has Reliable Recipes and can receive {@link DeleteRecipePayload} (client-only)
     */
    public boolean canSendToServer() {
        //? if fabric && <1.21 {
        /*return ClientPlayNetworking.canSend(DeleteRecipePayload.TYPE.id());
         *///?} else if fabric {
        return ClientPlayNetworking.canSend(DeleteRecipePayload.TYPE);
        //?}
        //? if forge {
        /*return ForgeNetworking.isServerPresent();
         *///?}
        //? if neoforge {
        /*var connection = net.minecraft.client.Minecraft.getInstance().getConnection();
        return connection != null && connection.hasChannel(DeleteRecipePayload.TYPE);
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

    /**
     * @return false if the player doesn't have Reliable Recipes and the packet wasn't sent
     */
    public boolean sendDeleteRecipePacketToPlayer(ServerPlayer player, ResourceKey<Recipe<?>> recipeKey) {
        //? if fabric && <1.21 {
        /*if (!ServerPlayNetworking.canSend(player, ClientboundRemoveRecipePayload.TYPE.id())) return false;
        ServerPlayNetworking.send(player, ClientboundRemoveRecipePayload.TYPE.id(), encode(ClientboundRemoveRecipePayload.STREAM_CODEC, new ClientboundRemoveRecipePayload(recipeKey)));
        *///?} else if fabric {
        if (!ServerPlayNetworking.canSend(player, ClientboundRemoveRecipePayload.TYPE)) return false;
        ServerPlayNetworking.send(player, new ClientboundRemoveRecipePayload(recipeKey));
        //?}
        //? if forge {
        /*if (!ForgeNetworking.isPresent(player)) return false;
        ForgeNetworking.sendToPlayer(player, new ClientboundRemoveRecipePayload(recipeKey));
        *///?}
        //? if neoforge {
        /*if (!player.connection.hasChannel(ClientboundRemoveRecipePayload.TYPE)) return false;
        PacketDistributor.sendToPlayer(player, new ClientboundRemoveRecipePayload(recipeKey));
        *///?}
        return true;
    }

    public void sendSyncConfigPacketToPlayer(ServerPlayer player, ClientboundSyncConfigPayload payload) {
        //? if fabric && <1.21 {
        /*if (!ServerPlayNetworking.canSend(player, ClientboundSyncConfigPayload.TYPE.id())) return;
        ServerPlayNetworking.send(player, ClientboundSyncConfigPayload.TYPE.id(), encode(ClientboundSyncConfigPayload.STREAM_CODEC, payload));
        *///?} else if fabric {
        if (!ServerPlayNetworking.canSend(player, ClientboundSyncConfigPayload.TYPE)) return;
        ServerPlayNetworking.send(player, payload);
        //?}
        //? if forge {
        /*ForgeNetworking.sendSyncToPlayer(player, payload);
         *///?}
        //? if neoforge {
        /*if (!player.connection.hasChannel(ClientboundSyncConfigPayload.TYPE)) return;
        PacketDistributor.sendToPlayer(player, payload);
        *///?}
    }

    public void sendAddRecipePacketToPlayer(ServerPlayer player, RecipeHolder<?> recipeHolder) {
        //? if fabric && <1.21 {
        /*if (!ServerPlayNetworking.canSend(player, ClientboundAddRecipePayload.TYPE.id())) return;
        ServerPlayNetworking.send(player, ClientboundAddRecipePayload.TYPE.id(), encode(ClientboundAddRecipePayload.STREAM_CODEC, new ClientboundAddRecipePayload(recipeHolder)));
        *///?} else if fabric {
        if (!ServerPlayNetworking.canSend(player, ClientboundAddRecipePayload.TYPE)) return;
        ServerPlayNetworking.send(player, new ClientboundAddRecipePayload(recipeHolder));
        //?}
        //? if forge {
        /*ForgeNetworking.sendToPlayer(player, new ClientboundAddRecipePayload(recipeHolder));
         *///?}
        //? if neoforge {
        /*if (!player.connection.hasChannel(ClientboundAddRecipePayload.TYPE)) return;
        PacketDistributor.sendToPlayer(player, new ClientboundAddRecipePayload(recipeHolder));
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
