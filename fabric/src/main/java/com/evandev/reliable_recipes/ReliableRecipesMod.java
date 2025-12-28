package com.evandev.reliable_recipes;

import com.evandev.reliable_recipes.config.RecipeConfigIO;
import com.evandev.reliable_recipes.recipe.RecipeModifier;
import com.evandev.reliable_recipes.recipe.TagModifier;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundUpdateRecipesPacket;
import net.minecraft.resources.ResourceLocation;

public class ReliableRecipesMod implements ModInitializer {

    @Override
    public void onInitialize() {
        CommonClass.init();

        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            TagModifier.apply();

            // Sync recipes to all players
            server.getPlayerList().getPlayers().forEach(player ->
                    player.connection.send(new ClientboundUpdateRecipesPacket(server.getRecipeManager().getRecipes()))
            );
        });

        ServerLifecycleEvents.END_DATA_PACK_RELOAD.register((server, resourceManager, success) -> {
            if (success) {
                TagModifier.apply();
                server.getPlayerList().getPlayers().forEach(player ->
                        player.connection.send(new ClientboundUpdateRecipesPacket(server.getRecipeManager().getRecipes()))
                );
            }
        });

        ServerPlayNetworking.registerGlobalReceiver(new ResourceLocation("reliable_recipes", "delete_recipe"),
                (server, player, handler, buf, responseSender) -> {
                    ResourceLocation id = buf.readResourceLocation();
                    server.execute(() -> {
                        if (player.hasPermissions(2)) {
                            RecipeConfigIO.addRemovalRule(id.toString());
                            Minecraft.getInstance().getToasts().addToast(SystemToast.multiline(
                                    Minecraft.getInstance(),
                                    SystemToast.SystemToastIds.TUTORIAL_HINT,
                                    Component.literal("Reliable Recipes"),
                                    Component.literal("Recipe deleted. /reload to apply.")
                            ));
                        }
                    });
                });
    }
}