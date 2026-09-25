package com.evandev.reliable_recipes.neoforge.client;

//? if neoforge {
/*import com.evandev.reliable_recipes.Constants;
import com.evandev.reliable_recipes.client.RrvInteractions;
import com.evandev.reliable_recipes.networking.ClientboundAddRecipePayload;
import com.evandev.reliable_recipes.networking.ClientboundRemoveRecipePayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public class ClientPayloadHandler {

    public static void handleRemove(final ClientboundRemoveRecipePayload payload, final IPayloadContext context) {
        context.enqueueWork(() -> {
            Constants.LOG.info("Received recipe removal notification for: {}", payload.recipeKey().identifier());
            RrvInteractions.closeRecipeViewScreen();
        });
    }

    public static void handleAdd(final ClientboundAddRecipePayload payload, final IPayloadContext context) {
        context.enqueueWork(() -> {
            Constants.LOG.info("Received recipe restoration notification for: {}", payload.recipeHolder().id().identifier());
            RrvInteractions.closeRecipeViewScreen();
        });
    }
}
*///?}

