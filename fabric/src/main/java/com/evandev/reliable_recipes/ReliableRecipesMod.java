package com.evandev.reliable_recipes;

import com.evandev.reliable_recipes.command.UndoCommand;
import com.evandev.reliable_recipes.networking.ClientboundDeleteRecipePayload;
import com.evandev.reliable_recipes.networking.DeleteRecipePayload;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import com.evandev.reliable_recipes.recipe.TransmuteRecipe;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.resources.ResourceLocation;

public class ReliableRecipesMod implements ModInitializer {

    @Override
    public void onInitialize() {
        CommonClass.init();

        Registry.register(BuiltInRegistries.RECIPE_SERIALIZER, ResourceLocation.withDefaultNamespace("crafting_transmute"), TransmuteRecipe.SERIALIZER);
        Registry.register(BuiltInRegistries.RECIPE_TYPE, ResourceLocation.withDefaultNamespace("crafting_transmute"), TransmuteRecipe.TYPE);

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                UndoCommand.register(dispatcher)
        );

        PayloadTypeRegistry.playS2C().register(ClientboundDeleteRecipePayload.TYPE, ClientboundDeleteRecipePayload.STREAM_CODEC);
        PayloadTypeRegistry.playC2S().register(DeleteRecipePayload.TYPE, DeleteRecipePayload.STREAM_CODEC);

        ServerPlayNetworking.registerGlobalReceiver(DeleteRecipePayload.TYPE,
                (payload, context) -> {
                    ResourceLocation id = payload.recipeId();
                    var server = context.server();
                    var player = context.player();
                    server.execute(() -> DeleteRecipePayload.handle(id, server, player));
                });
    }
}