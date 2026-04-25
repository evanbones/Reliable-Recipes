package com.evandev.reliable_recipes.compat;

import cc.cassian.rrv.api.ReliableRecipeViewerClientPlugin;
import cc.cassian.rrv.api.recipe.ItemView;
import com.evandev.reliable_recipes.api.ReliableRecipesAPI;
import net.minecraft.core.registries.BuiltInRegistries;

public class ReliableRecipesRrvPlugin implements ReliableRecipeViewerClientPlugin {

    @Override
    public void onIntegrationInitialize() {
        ItemView.addClientReloadCallback(() -> {
            BuiltInRegistries.ITEM.stream()
                    .filter(item -> ReliableRecipesAPI.isItemHidden(item.getDefaultInstance()))
                    .forEach(ItemView::excludeItem);
        });
    }
}