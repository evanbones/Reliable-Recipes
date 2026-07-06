package com.evandev.reliable_recipes.mixin.minecraft;

import com.evandev.reliable_recipes.recipe.RecipeModifier;
import com.evandev.reliable_recipes.recipe.TagModifier;
import net.minecraft.core.RegistryAccess;
import net.minecraft.server.ReloadableServerResources;
import net.minecraft.world.item.crafting.RecipeManager;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ReloadableServerResources.class)
public class ReloadableServerResourcesMixin {

    @Inject(
            method = "updateRegistryTags(Lnet/minecraft/core/RegistryAccess;)V",
            at = @At("RETURN")
    )
    private void reliableRecipes$onTagsLoaded(RegistryAccess registryAccess, CallbackInfo ci) {
        TagModifier.apply();
        RecipeModifier.apply();
    }
}