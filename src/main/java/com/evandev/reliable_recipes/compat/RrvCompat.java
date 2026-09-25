package com.evandev.reliable_recipes.compat;

import cc.cassian.rrv.common.recipe.ServerRecipeManager;

public class RrvCompat {

    /**
     * Rebuilds RRV's internal recipe cache and broadcasts the updated
     * recipes to all connected clients.
     */
    public static void syncRecipesToAllClients() {
        ServerRecipeManager.INSTANCE.reload();
    }
}