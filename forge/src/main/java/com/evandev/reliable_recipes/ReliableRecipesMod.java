package com.evandev.reliable_recipes;

import com.evandev.reliable_recipes.recipe.RecipeModifier;
import com.evandev.reliable_recipes.recipe.TagModifier;
import net.minecraft.network.protocol.game.ClientboundUpdateRecipesPacket;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TagsUpdatedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.server.ServerLifecycleHooks;

@Mod(Constants.MOD_ID)
public class ReliableRecipesMod {

    public ReliableRecipesMod() {
        CommonClass.init();
        MinecraftForge.EVENT_BUS.register(this);
    }

    @SubscribeEvent
    public void onTagsUpdated(TagsUpdatedEvent event) {
        TagModifier.apply();

        var server = ServerLifecycleHooks.getCurrentServer();
        if (server != null) {
            RecipeModifier.apply(server.getRecipeManager());

            server.getPlayerList().getPlayers().forEach(player ->
                    player.connection.send(new ClientboundUpdateRecipesPacket(server.getRecipeManager().getRecipes()))
            );
        }
    }
}