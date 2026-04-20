package com.evandev.reliable_recipes.client;

import com.evandev.reliable_recipes.Constants;
import com.evandev.reliable_recipes.networking.ClientboundAddRecipePayload;
import com.evandev.reliable_recipes.networking.ClientboundRemoveRecipePayload;
import net.minecraft.client.Minecraft;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public class ClientPayloadHandler {

    public static void handleRemove(final ClientboundRemoveRecipePayload payload, final IPayloadContext context) {
        context.enqueueWork(() -> {
            Constants.LOG.info("Received recipe removal notification for: {}", payload.recipeKey().identifier());

            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.screen != null && minecraft.screen.getClass().getName().contains("RecipeViewScreen")) {
                minecraft.screen.onClose();
            }
        });
    }

    public static void handleAdd(final ClientboundAddRecipePayload payload, final IPayloadContext context) {
        context.enqueueWork(() -> {
            Constants.LOG.info("Received recipe restoration notification for: {}", payload.recipeHolder().id());

            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.screen != null && minecraft.screen.getClass().getName().contains("RecipeViewScreen")) {
                minecraft.screen.onClose();
            }
        });
    }
}