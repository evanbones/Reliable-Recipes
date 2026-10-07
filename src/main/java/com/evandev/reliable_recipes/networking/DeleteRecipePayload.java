package com.evandev.reliable_recipes.networking;

import com.evandev.reliable_recipes.Constants;
import com.evandev.reliable_recipes.config.ConfigSync;
import com.evandev.reliable_recipes.config.ModConfig;
import com.evandev.reliable_recipes.config.RecipeConfigIO;
import com.evandev.reliable_recipes.platform.Services;
import com.evandev.reliable_recipes.recipe.RecipeUndoCache;
import com.evandev.reliable_recipes.util.CompatUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.game.ClientboundUpdateRecipesPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeManager;
import org.jetbrains.annotations.NotNull;

//? if >=1.21.2 {
import net.minecraft.server.permissions.Permission;
import net.minecraft.server.permissions.PermissionLevel;
//?}

public record DeleteRecipePayload(ResourceKey<Recipe<?>> recipeKey) implements CustomPacketPayload {
    public static final Type<DeleteRecipePayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath(Constants.MOD_ID, "delete_recipe"));

    public static final StreamCodec<RegistryFriendlyByteBuf, DeleteRecipePayload> STREAM_CODEC = StreamCodec.composite(
            CompatUtil.recipeKeyStreamCodec(),
            DeleteRecipePayload::recipeKey,
            DeleteRecipePayload::new
    );

    public static void handle(ResourceKey<Recipe<?>> key, MinecraftServer server, ServerPlayer player) {
        Identifier id = CompatUtil.keyId(key);

        //? if <1.21.2 {
        /*boolean hasPermission = player.hasPermissions(2);
         *///?} else {
        boolean hasPermission = player.permissions().hasPermission(new Permission.HasCommandLevel(PermissionLevel.GAMEMASTERS));
        //?}
        if (!hasPermission) {
            player.sendSystemMessage(Component.translatable("toast.reliable_recipes.permission_denied"));
            return;
        }

        if (!ModConfig.get().devMode) {
            player.sendSystemMessage(Component.translatable("toast.reliable_recipes.server_dev_mode"));
            return;
        }

        RecipeManager recipeManager = server.getRecipeManager();
        if (RecipeUndoCache.removeRecipe(recipeManager, key) == null) {
            player.sendSystemMessage(Component.translatable("toast.reliable_recipes.could_not_find_recipe", id.toString()));
            return;
        }

        RecipeConfigIO.addRemovalRule(id.toString());

        Constants.LOG.info("Runtime deletion of recipe: {}", id);

        //? if <1.21.2 {
        /*ClientboundUpdateRecipesPacket packet = new ClientboundUpdateRecipesPacket(recipeManager.getRecipes());
         *///?} else {
        recipeManager.finalizeRecipeLoading(server.getWorldData().enabledFeatures());
        ClientboundUpdateRecipesPacket packet = new ClientboundUpdateRecipesPacket(
                recipeManager.getSynchronizedItemProperties(),
                recipeManager.getSynchronizedStonecutterRecipes()
        );
        //?}

        server.getPlayerList().getPlayers().forEach(p -> {
            //? if <1.21.2 {
            /*
            if (!Services.PLATFORM.sendDeleteRecipePacketToPlayer(p, key)) {
                p.connection.send(packet);
            }
            *///?} else {
            p.connection.send(packet);
            Services.PLATFORM.sendDeleteRecipePacketToPlayer(p, key);
            //?}
        });

        ConfigSync.sendToAll(server);
    }

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
