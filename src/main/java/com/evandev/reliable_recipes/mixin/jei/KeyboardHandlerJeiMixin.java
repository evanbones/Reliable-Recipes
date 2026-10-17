package com.evandev.reliable_recipes.mixin.jei;

import com.evandev.reliable_recipes.compat.jei.JeiInteractions;
import com.mojang.blaze3d.platform.InputConstants;
import com.moulberry.mixinconstraints.annotations.IfModLoaded;
import net.minecraft.client.KeyboardHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
//? if >=1.21.2 {
import net.minecraft.client.input.KeyEvent;
//?}

@IfModLoaded("jei")
@Mixin(KeyboardHandler.class)
public abstract class KeyboardHandlerJeiMixin {

    //? if >=1.21.2 {
    @Inject(method = "keyPress", at = @At("HEAD"), cancellable = true)
    private void reliableRecipes$onJeiKeyPressed(long handle, int action, KeyEvent event, CallbackInfo ci) {
        if (action == 1 && event.key() == InputConstants.KEY_DELETE && JeiInteractions.tryDeleteHoveredRecipe()) {
            ci.cancel();
        }
    }
    //?} else {
    /*@Inject(method = "keyPress", at = @At("HEAD"), cancellable = true)
    private void reliableRecipes$onJeiKeyPressed(long window, int key, int scanCode, int action, int modifiers, CallbackInfo ci) {
        if (action == 1 && key == InputConstants.KEY_DELETE && JeiInteractions.tryDeleteHoveredRecipe()) {
            ci.cancel();
        }
    }
    *///?}
}
