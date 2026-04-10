package com.evandev.reliable_recipes.networking;

import com.evandev.reliable_recipes.Constants;
import com.evandev.reliable_recipes.config.RecipeConfigIO;
import com.evandev.reliable_recipes.platform.Services;
import com.evandev.reliable_recipes.recipe.RecipeModifier;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permission;
import net.minecraft.server.permissions.PermissionLevel;
import net.minecraft.world.item.crafting.Recipe;
import org.jetbrains.annotations.NotNull;

public record DeleteRecipePayload(ResourceKey<Recipe<?>> recipeKey) implements CustomPacketPayload {
    public static final Type<DeleteRecipePayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath(Constants.MOD_ID, "delete_recipe"));

    public static final StreamCodec<RegistryFriendlyByteBuf, DeleteRecipePayload> STREAM_CODEC = StreamCodec.composite(
            ResourceKey.streamCodec(Registries.RECIPE),
            DeleteRecipePayload::recipeKey,
            DeleteRecipePayload::new
    );

    public static void handle(ResourceKey<Recipe<?>> key, MinecraftServer server, ServerPlayer player) {
        if (player.permissions().hasPermission(new Permission.HasCommandLevel(PermissionLevel.GAMEMASTERS))) {
            RecipeConfigIO.addRemovalRule(key.identifier().toString());

            boolean removed = RecipeModifier.removeRecipe(server.getRecipeManager(), key);

            if (removed) {
                Constants.LOG.info("Runtime deletion of recipe: {}", key.identifier());

                server.getPlayerList().getPlayers().forEach(p ->
                        Services.PLATFORM.sendDeleteRecipePacketToPlayer(p, key)
                );
            } else {
                player.sendSystemMessage(Component.translatable("toast.reliable_recipes.could_not_find_recipe", key.identifier()));
            }
        } else {
            player.sendSystemMessage(Component.translatable("toast.reliable_recipes.permission_denied"));
        }
    }

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}