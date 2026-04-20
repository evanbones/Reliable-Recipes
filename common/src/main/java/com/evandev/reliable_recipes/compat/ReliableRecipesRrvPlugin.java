package com.evandev.reliable_recipes.compat;

import cc.cassian.rrv.api.ReliableRecipeViewerClientPlugin;
import cc.cassian.rrv.api.recipe.ItemView;
import com.evandev.reliable_recipes.api.ReliableRecipesAPI;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;

import java.util.ArrayList;
import java.util.List;

public class ReliableRecipesRrvPlugin implements ReliableRecipeViewerClientPlugin {

    @Override
    public void onIntegrationInitialize() {
        ItemView.addClientReloadCallback(() -> {
            List<Item> hiddenItems = new ArrayList<>();
            for (Item item : BuiltInRegistries.ITEM) {
                if (ReliableRecipesAPI.isItemHidden(item.getDefaultInstance())) {
                    hiddenItems.add(item);
                }
            }
            if (!hiddenItems.isEmpty()) {
                ItemView.excludeItems(hiddenItems.toArray(new Item[0]));
            }
        });
    }
}