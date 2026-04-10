package com.evandev.reliable_recipes.networking;

import com.evandev.reliable_recipes.Constants;
import com.evandev.reliable_recipes.api.ReliableRecipesAPI;
import com.evandev.reliable_recipes.config.RecipeConfigIO;
import com.evandev.reliable_recipes.platform.Services;
import com.evandev.reliable_recipes.recipe.RecipeModifier;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public record DeleteRecipeByOutputPayload(ItemStack output) implements CustomPacketPayload {
    public static final Type<DeleteRecipeByOutputPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath(Constants.MOD_ID, "delete_recipe_by_output"));

    public static final StreamCodec<RegistryFriendlyByteBuf, DeleteRecipeByOutputPayload> STREAM_CODEC = StreamCodec.composite(
            ItemStack.STREAM_CODEC,
            DeleteRecipeByOutputPayload::output,
            DeleteRecipeByOutputPayload::new
    );

    public static void handle(ItemStack output, MinecraftServer server, ServerPlayer player) {
        if (!player.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER)) {
            player.sendSystemMessage(Component.translatable("toast.reliable_recipes.permission_denied"));
            return;
        }

        RecipeManager manager = server.getRecipeManager();
        List<ResourceKey<Recipe<?>>> keysToRemove = new ArrayList<>();

        for (RecipeHolder<?> holder : manager.getRecipes()) {
            List<ItemStack> results = ReliableRecipesAPI.getRecipeResults(holder.value());
            for (ItemStack result : results) {
                if (!result.isEmpty() && result.is(output.getItem())) {
                    keysToRemove.add(holder.id());
                    break;
                }
            }
        }

        if (keysToRemove.isEmpty()) {
            player.sendSystemMessage(Component.literal("§cCould not find any loaded recipes outputting: " + output.getHoverName().getString()));
            return;
        }

        for (ResourceKey<Recipe<?>> key : keysToRemove) {
            RecipeConfigIO.addRemovalRule(key.identifier().toString());
            RecipeModifier.removeRecipe(manager, key);
        }

        server.getPlayerList().getPlayers().forEach(p ->
                Services.PLATFORM.sendDeleteRecipePacketToPlayer(p, keysToRemove.getFirst())
        );

        Constants.LOG.info("Runtime deletion of {} recipes outputting: {}", keysToRemove.size(), BuiltInRegistries.ITEM.getKey(output.getItem()));
        player.sendSystemMessage(Component.literal("§aSuccessfully deleted " + keysToRemove.size() + " recipes for " + output.getHoverName().getString()));
    }

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}