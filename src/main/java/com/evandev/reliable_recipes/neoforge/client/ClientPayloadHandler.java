package com.evandev.reliable_recipes.neoforge.client;

//? if neoforge {
/*import com.evandev.reliable_recipes.client.ClientRecipeSync;
import com.evandev.reliable_recipes.networking.ClientboundAddRecipePayload;
import com.evandev.reliable_recipes.networking.ClientboundRemoveRecipePayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public class ClientPayloadHandler {

    public static void handleRemove(final ClientboundRemoveRecipePayload payload, final IPayloadContext context) {
        context.enqueueWork(() -> ClientRecipeSync.onRecipeRemoved(payload.recipeKey()));
    }

    public static void handleAdd(final ClientboundAddRecipePayload payload, final IPayloadContext context) {
        context.enqueueWork(() -> ClientRecipeSync.onRecipeAdded(payload.recipeHolder()));
    }
}
*///?}

