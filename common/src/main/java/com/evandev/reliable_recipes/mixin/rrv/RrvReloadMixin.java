package com.evandev.reliable_recipes.mixin.rrv;

import cc.cassian.rrv.common.recipe.inventory.RecipeViewScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundUpdateRecipesPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPacketListener.class)
public class RrvReloadMixin {

    @Inject(method = "handleUpdateRecipes", at = @At("RETURN"))
    private void reliableRecipes$onRecipesUpdated(ClientboundUpdateRecipesPacket packet, CallbackInfo ci) {
        reliableRecipes$scheduleReload();
    }

    @Unique
    private void reliableRecipes$scheduleReload() {
        Minecraft client = Minecraft.getInstance();
        if (client.screen instanceof RecipeViewScreen) {
            client.screen.onClose();
        }
    }
}