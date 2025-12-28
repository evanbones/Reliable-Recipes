package com.evandev.reliable_recipes.client;

import com.evandev.reliable_recipes.recipe.RecipeModifier;
import dev.emi.emi.runtime.EmiReloadManager;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.resources.ResourceLocation;

public class ReliableRecipesModClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        ClientPlayNetworking.registerGlobalReceiver(new ResourceLocation("reliable_recipes", "client_delete_recipe"),
                (client, handler, buf, responseSender) -> {
                    ResourceLocation recipeId = buf.readResourceLocation();

                    client.execute(() -> {
                        if (client.getConnection() != null) {
                            boolean removed = RecipeModifier.removeRecipe(client.getConnection().getRecipeManager(), recipeId);
                            if (removed) {
                                EmiReloadManager.reload();
                            }
                        }
                    });
                });
    }
}