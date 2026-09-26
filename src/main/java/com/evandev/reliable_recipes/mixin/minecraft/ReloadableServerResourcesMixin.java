package com.evandev.reliable_recipes.mixin.minecraft;

import com.evandev.reliable_recipes.recipe.RecipeModifier;
import com.evandev.reliable_recipes.tag.TagModifier;
import net.minecraft.server.ReloadableServerResources;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
//? if >=1.21.2 {
import com.evandev.reliable_recipes.compat.RrvCompat;
import com.evandev.reliable_recipes.config.RecipeConfigIO;
import com.evandev.reliable_recipes.platform.Services;
import net.minecraft.server.ReloadableServerRegistries;
import net.minecraft.world.item.crafting.RecipeManager;
import org.spongepowered.asm.mixin.Shadow;
//?}

@Mixin(ReloadableServerResources.class)
public abstract class ReloadableServerResourcesMixin {

    //? if <1.21.2 {
    /*
    @Inject(
            method = "updateRegistryTags()V",
            at = @At("RETURN")
    )
    private void reliableRecipes$onTagsLoaded(CallbackInfo ci) {
        TagModifier.apply();
        RecipeModifier.apply();
    }
    *///?} else {
    @Shadow
    public abstract RecipeManager getRecipeManager();

    @Shadow
    public abstract ReloadableServerRegistries.Holder fullRegistries();

    @Inject(
            method = "updateComponentsAndStaticRegistryTags()V",
            at = @At("RETURN")
    )
    private void reliableRecipes$onTagsLoaded(CallbackInfo ci) {
        RecipeConfigIO.invalidateCache();
        TagModifier.apply();
        RecipeModifier.apply(this.getRecipeManager(), this.fullRegistries().lookup());

        if (Services.PLATFORM.isModLoaded("rrv")) {
            RrvCompat.syncRecipesToAllClients();
        }
    }
    //?}
}
