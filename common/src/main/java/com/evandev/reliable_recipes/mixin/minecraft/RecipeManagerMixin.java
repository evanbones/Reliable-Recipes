package com.evandev.reliable_recipes.mixin.minecraft;

import com.evandev.reliable_recipes.recipe.RecipeModifier;
import net.minecraft.core.HolderLookup;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeMap;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(RecipeManager.class)
public class RecipeManagerMixin {

    @Unique
    private HolderLookup.Provider reliableRecipes$registries;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void reliableRecipes$captureRegistries(HolderLookup.Provider registries, CallbackInfo ci) {
        this.reliableRecipes$registries = registries;
    }

    @Inject(method = "apply(Lnet/minecraft/world/item/crafting/RecipeMap;Lnet/minecraft/server/packs/resources/ResourceManager;Lnet/minecraft/util/profiling/ProfilerFiller;)V", at = @At("RETURN"))
    private void reliableRecipes$filterRecipeMap(RecipeMap recipes, ResourceManager manager, ProfilerFiller profiler, CallbackInfo ci) {
        RecipeModifier.reset();
        RecipeModifier.apply((RecipeManager) (Object) this, this.reliableRecipes$registries);
    }
}