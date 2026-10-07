package com.evandev.reliable_recipes.mixin.minecraft;

import com.evandev.reliable_recipes.client.ClientRecipeSync;
import com.evandev.reliable_recipes.recipe.RecipeModifier;
import com.evandev.reliable_recipes.tag.TagModifier;
import net.minecraft.client.multiplayer.ClientPacketListener;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

//? if <1.21 {
/*import net.minecraft.network.protocol.game.ClientboundUpdateTagsPacket;
*///?} else {
import net.minecraft.network.protocol.common.ClientboundUpdateTagsPacket;
//?}
//? if <1.21.2 {
/*import com.evandev.reliable_recipes.config.RecipeConfigIO;
import net.minecraft.network.protocol.game.ClientboundUpdateRecipesPacket;
*///?}

@Mixin(ClientPacketListener.class)
public class ClientPacketListenerMixin {

    //? if <1.21.2 {
    /*@Inject(method = "handleUpdateTags", at = @At("RETURN"))
    private void reliableRecipes$onTagsUpdated(ClientboundUpdateTagsPacket packet, CallbackInfo ci) {
        RecipeConfigIO.invalidateCache();
        if (!ClientRecipeSync.isServerAuthoritative()) {
            TagModifier.apply();
        }
    }

    @Inject(method = "handleUpdateRecipes", at = @At("RETURN"))
    private void reliableRecipes$onRecipesUpdated(ClientboundUpdateRecipesPacket packet, CallbackInfo ci) {
        RecipeConfigIO.invalidateCache();
        RecipeModifier.applyClient();
    }
    *///?} else {
    @Inject(method = "handleUpdateTags", at = @At("RETURN"))
    private void reliableRecipes$onTagsUpdated(ClientboundUpdateTagsPacket packet, CallbackInfo ci) {
        if (!ClientRecipeSync.isServerAuthoritative()) {
            TagModifier.apply();
        }
        RecipeModifier.applyGlobalRules();
    }
    //?}
}
