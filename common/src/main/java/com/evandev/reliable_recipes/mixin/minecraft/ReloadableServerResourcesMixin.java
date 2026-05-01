package com.evandev.reliable_recipes.mixin.minecraft;

import com.evandev.reliable_recipes.compat.RrvCompat;
import com.evandev.reliable_recipes.platform.Services;
import com.evandev.reliable_recipes.recipe.RecipeModifier;
import com.evandev.reliable_recipes.recipe.TagModifier;
import net.minecraft.server.ReloadableServerRegistries;
import net.minecraft.server.ReloadableServerResources;
import net.minecraft.world.item.crafting.RecipeManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ReloadableServerResources.class)
public abstract class ReloadableServerResourcesMixin {

    @Shadow
    public abstract RecipeManager getRecipeManager();

    @Shadow
    public abstract ReloadableServerRegistries.Holder fullRegistries();

    @Inject(
            method = "updateComponentsAndStaticRegistryTags()V",
            at = @At("RETURN")
    )
    private void reliableRecipes$onTagsLoaded(CallbackInfo ci) {
        TagModifier.apply();
        RecipeModifier.apply(this.getRecipeManager(), this.fullRegistries().lookup());

        if (Services.PLATFORM.isModLoaded("rrv")) {
            RrvCompat.syncRecipesToAllClients();
        }
    }
}