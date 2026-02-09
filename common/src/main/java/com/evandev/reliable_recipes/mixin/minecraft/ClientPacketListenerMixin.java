package com.evandev.reliable_recipes.mixin.minecraft;

import com.evandev.reliable_recipes.recipe.TagModifier;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.common.ClientboundUpdateTagsPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPacketListener.class)
public class ClientPacketListenerMixin {

    @Inject(method = "handleUpdateTags", at = @At("RETURN"))
    private void reliableRecipes$onTagsUpdated(ClientboundUpdateTagsPacket packet, CallbackInfo ci) {
        TagModifier.apply();
    }
}