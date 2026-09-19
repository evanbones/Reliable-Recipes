package com.evandev.reliable_recipes;

import com.evandev.reliable_recipes.command.UndoCommand;
import com.evandev.reliable_recipes.config.RecipeConfigIO;
import com.evandev.reliable_recipes.platform.Services;
import com.evandev.reliable_recipes.recipe.BrewingRecipe;
import com.evandev.reliable_recipes.recipe.RecipeUndoCache;
import com.evandev.reliable_recipes.recipe.TransmuteRecipe;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public class ReliableRecipesMod implements ModInitializer {

    @Override
    public void onInitialize() {
        CommonClass.init();

        Registry.register(BuiltInRegistries.RECIPE_SERIALIZER, new ResourceLocation("crafting_transmute"), TransmuteRecipe.SERIALIZER);
        Registry.register(BuiltInRegistries.RECIPE_TYPE, new ResourceLocation("crafting_transmute"), TransmuteRecipe.TYPE);

        Registry.register(BuiltInRegistries.RECIPE_SERIALIZER, new ResourceLocation("brewing"), BrewingRecipe.SERIALIZER);
        Registry.register(BuiltInRegistries.RECIPE_TYPE, new ResourceLocation("brewing"), BrewingRecipe.TYPE);

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                UndoCommand.register(dispatcher)
        );

        ServerPlayNetworking.registerGlobalReceiver(new ResourceLocation("reliable_recipes", "delete_recipe"),
                (server, player, handler, buf, responseSender) -> {
                    ResourceLocation id = buf.readResourceLocation();
                    server.execute(() -> {
                        if (player.hasPermissions(2)) {
                            RecipeConfigIO.addRemovalRule(id.toString());
                            boolean removed = RecipeUndoCache.removeRecipe(server.getRecipeManager(), id);

                            if (removed) {
                                Constants.LOG.info("Runtime deletion of recipe: {}", id);
                                server.getPlayerList().getPlayers().forEach(p ->
                                        Services.PLATFORM.sendDeleteRecipePacketToPlayer(p, id)
                                );
                            } else {
                                player.sendSystemMessage(Component.translatable("toast.reliable_recipes.could_not_find_recipe", id));
                            }
                        } else {
                            player.sendSystemMessage(Component.translatable("toast.reliable_recipes.permission_denied"));
                        }
                    });
                });
    }
}