package com.evandev.reliable_recipes.client;

import com.evandev.reliable_recipes.Constants;
import com.evandev.reliable_recipes.config.ModConfig;
import com.evandev.reliable_recipes.util.CompatUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
//? if <1.21.2 {
/*import com.evandev.reliable_recipes.compat.emi.EmiInteractions;
import com.evandev.reliable_recipes.platform.Services;
import com.evandev.reliable_recipes.recipe.RecipeUndoCache;
import net.minecraft.world.item.crafting.RecipeManager;
*///?}

public class ClientRecipeSync {

    public static void onRecipeRemoved(ResourceKey<Recipe<?>> recipeKey) {
        Identifier recipeId = CompatUtil.keyId(recipeKey);
        Constants.LOG.info("Received recipe removal notification for: {}", recipeId);

        //? if <1.21.2 {
        /*
        Minecraft client = Minecraft.getInstance();
        if (client.getConnection() == null) return;

        RecipeManager recipeManager = client.getConnection().getRecipeManager();
        ItemStack icon = ItemStack.EMPTY;
        var recipe = recipeManager.byKey(recipeId).orElse(null);
        if (recipe != null && client.level != null) {
            icon = recipe.value().getResultItem(client.level.registryAccess());
        }

        if (RecipeUndoCache.removeRecipe(recipeManager, recipeKey) == null) return;

        if (ModConfig.get().reloadRrv && Services.PLATFORM.isModLoaded("emi")) {
            EmiInteractions.reload();
        }
        *///?} else {
        ItemStack icon = RrvInteractions.onRecipeRemoved(recipeKey);
        //?}

        showRemovalFeedback(recipeId, icon);
    }

    public static void onRecipeAdded(RecipeHolder<?> recipeHolder) {
        Constants.LOG.info("Received recipe restoration notification for: {}", CompatUtil.recipeId(recipeHolder));

        //? if >=1.21.2 {
        RrvInteractions.onRecipeAdded(recipeHolder);
        //?}
    }

    private static void showRemovalFeedback(Identifier recipeId, ItemStack icon) {
        Minecraft client = Minecraft.getInstance();
        ModConfig config = ModConfig.get();

        if (config.showChatMessages && client.player != null) {
            String command = "/rrecipes_undo " + recipeId;
            Component hover = Component.translatable("commands.reliable_recipes.undo.hover");
            MutableComponent undoText = Component.translatable("toast.reliable_recipes.undo")
                    .withStyle(style -> style
                            .withColor(ChatFormatting.RED)
                            .withBold(true)
                            //? if <1.21.2 {
                            /*.withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, command))
                            .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, hover))
                            *///?} else {
                            .withClickEvent(new ClickEvent.RunCommand(command))
                            .withHoverEvent(new HoverEvent.ShowText(hover))
                            //?}
                    );

            client.player.sendSystemMessage(Component.translatable("toast.reliable_recipes.deleted", recipeId.toString()).append(undoText));
        }

        if (config.showToast) {
            SharedToastOverlay.show(Component.translatable("toast.reliable_recipes.deleted_title"), Component.literal(recipeId.getPath()), icon);
        }
    }
}
