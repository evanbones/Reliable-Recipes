package com.evandev.reliable_recipes;

import com.evandev.reliable_recipes.recipe.RecipeModifier;
import com.evandev.reliable_recipes.recipe.TagModifier;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.network.protocol.game.ClientboundUpdateRecipesPacket;

public class ReliableRecipesMod implements ModInitializer {

    @Override
    public void onInitialize() {
        CommonClass.init();

        ServerLifecycleEvents.SYNC_DATA_PACK_CONTENTS.register((player, joined) -> {
            // TODO: Re-sync recipes to player? SERVER_STARTED should handle the modification though
        });

        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            TagModifier.apply();
            RecipeModifier.apply(server.getRecipeManager());

            // Sync recipes to all players
            server.getPlayerList().getPlayers().forEach(player ->
                    player.connection.send(new ClientboundUpdateRecipesPacket(server.getRecipeManager().getRecipes()))
            );
        });

        ServerLifecycleEvents.END_DATA_PACK_RELOAD.register((server, resourceManager, success) -> {
            if (success) {
                TagModifier.apply();
                RecipeModifier.apply(server.getRecipeManager());
                server.getPlayerList().getPlayers().forEach(player ->
                        player.connection.send(new ClientboundUpdateRecipesPacket(server.getRecipeManager().getRecipes()))
                );
            }
        });
    }
}