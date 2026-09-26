package com.evandev.reliable_recipes.client;

//? if >=1.21.2 {
import cc.cassian.rrv.api.recipe.ReliableClientRecipe;
import cc.cassian.rrv.client.ReliableRecipeViewerClient;
import cc.cassian.rrv.client.recipe.ClientRecipeCache;
import cc.cassian.rrv.common.overlay.itemlist.view.ItemFilters;
import cc.cassian.rrv.common.overlay.itemlist.view.ItemViewOverlay;
import cc.cassian.rrv.common.recipe.inventory.SlotContent;
import com.evandev.reliable_recipes.api.ReliableRecipesAPI;
import com.evandev.reliable_recipes.config.ModConfig;
import com.evandev.reliable_recipes.platform.Services;
import com.evandev.reliable_recipes.recipe.RecipeModifier;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeMap;

import java.util.ArrayList;
import java.util.List;

public class RrvInteractions {
    public static boolean requestDeletion(ReliableClientRecipe recipe) {
        if (recipe == null) return false;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return false;

        if (!mc.player.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER)) {
            mc.player.sendSystemMessage(Component.translatable("toast.reliable_recipes.permission_denied"));
            return false;
        }

        if (!ModConfig.get().devMode) {
            mc.player.sendSystemMessage(Component.translatable("toast.reliable_recipes.dev_mode"));
            return false;
        }

        Identifier recipeId = recipe.getId();
        if (recipeId != null) {
            Services.PLATFORM.sendDeleteRecipePacket(ResourceKey.create(Registries.RECIPE, recipeId));
            return true;
        }

        // Fallback for recipes that fail to provide an ID
        List<SlotContent> results = recipe.getResults();
        if (results == null || results.isEmpty() || results.getFirst().getValidContents().isEmpty()) {
            mc.player.sendSystemMessage(Component.translatable("chat.reliable_recipes.cannot_determine_recipe"));
            return false;
        }

        mc.player.sendSystemMessage(Component.translatable("chat.reliable_recipes.no_id_dynamically"));
        return false;
    }

    public static void closeRecipeViewScreen() {
        Minecraft mc = Minecraft.getInstance();
        //? if <=26.1 {
        /*if (mc.screen != null && mc.screen.getClass().getName().contains("RecipeViewScreen")) {
            mc.screen.onClose();
        }
        *///?} else {
        if (mc.gui.screen() != null && mc.gui.screen().getClass().getName().contains("RecipeViewScreen")) {
            mc.gui.screen().onClose();
        }
        //?}
    }

    /**
     * Removes the recipe from RRV's client-side recipes.
     *
     * @return the removed recipe's output, for display in the removal toast, or an empty stack.
     */
    public static ItemStack onRecipeRemoved(ResourceKey<Recipe<?>> recipeKey) {
        closeRecipeViewScreen();

        if (!Services.PLATFORM.isModLoaded("rrv")) return ItemStack.EMPTY;

        ItemStack icon = ItemStack.EMPTY;
        RecipeMap currentMap = ReliableRecipeViewerClient.LOCAL_RECIPES;
        if (currentMap != null) {
            RecipeHolder<?> removed = currentMap.byKey(recipeKey);
            if (removed != null) {
                List<ItemStack> results = ReliableRecipesAPI.getRecipeResults(removed.value());
                if (!results.isEmpty()) icon = results.getFirst();
            }
        }

        if (!ModConfig.get().reloadRrv) return icon;

        if (currentMap != null) {
            List<RecipeHolder<?>> updated = new ArrayList<>();
            for (RecipeHolder<?> holder : currentMap.values()) {
                if (!holder.id().equals(recipeKey)) {
                    updated.add(holder);
                }
            }
            ReliableRecipeViewerClient.LOCAL_RECIPES = RecipeModifier.createRecipeMap(updated);
        }

        ClientRecipeCache.INSTANCE.buildRecipeCache(true);
        ItemFilters.clearCaches(true);
        ItemViewOverlay.INSTANCE.updateDisplayedItems();
        return icon;
    }

    public static void onRecipeAdded(RecipeHolder<?> recipeHolder) {
        closeRecipeViewScreen();

        if (!Services.PLATFORM.isModLoaded("rrv") || !ModConfig.get().reloadRrv) return;

        RecipeMap currentMap = ReliableRecipeViewerClient.LOCAL_RECIPES;
        if (currentMap != null) {
            List<RecipeHolder<?>> updated = new ArrayList<>(currentMap.values());
            updated.removeIf(holder -> holder.id().equals(recipeHolder.id()));
            updated.add(recipeHolder);
            ReliableRecipeViewerClient.LOCAL_RECIPES = RecipeModifier.createRecipeMap(updated);
        }

        ClientRecipeCache.INSTANCE.buildRecipeCache(true);
        ItemFilters.clearCaches(true);
        ItemViewOverlay.INSTANCE.updateDisplayedItems();
    }
}
//?} else {
/*public class RrvInteractions {}
*///?}
