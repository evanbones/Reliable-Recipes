package com.evandev.reliable_recipes.mixin.minecraft;

import com.evandev.reliable_recipes.recipe.TagModifier;
import net.minecraft.server.ReloadableServerResources;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ReloadableServerResources.class)
public class ReloadableServerResourcesMixin {

    @Inject(
            method = "updateComponentsAndStaticRegistryTags()V",
            at = @At("RETURN")
    )
    private void reliableRecipes$onTagsLoaded(CallbackInfo ci) {
        TagModifier.apply();
    }
}