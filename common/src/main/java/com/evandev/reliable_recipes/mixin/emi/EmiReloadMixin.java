package com.evandev.reliable_recipes.mixin.emi;

import dev.emi.emi.screen.RecipeScreen;
import dev.emi.emi.runtime.EmiReloadManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundUpdateRecipesPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPacketListener.class)
public class EmiReloadMixin {

    @Inject(method = "handleUpdateRecipes", at = @At("RETURN"))
    private void reliableRecipes$onRecipesUpdated(ClientboundUpdateRecipesPacket packet, CallbackInfo ci) {
        EmiReloadManager.reload();

        Minecraft client = Minecraft.getInstance();
        if (client.screen instanceof RecipeScreen) {
            client.screen.onClose();
        }
    }
}