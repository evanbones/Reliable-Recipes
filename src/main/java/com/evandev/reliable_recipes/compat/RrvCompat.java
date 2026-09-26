package com.evandev.reliable_recipes.compat;

//? if >=1.21.2 {
import cc.cassian.rrv.common.recipe.ServerRecipeManager;
//?}

public class RrvCompat {

    /**
     * Rebuilds RRV's internal recipe cache and broadcasts the updated
     * recipes to all connected clients.
     */
    public static void syncRecipesToAllClients() {
        //? if >=1.21.2 {
        ServerRecipeManager.INSTANCE.reload();
        //?}
    }
}
