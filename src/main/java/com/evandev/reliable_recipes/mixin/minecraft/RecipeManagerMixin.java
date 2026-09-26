package com.evandev.reliable_recipes.mixin.minecraft;

import com.moulberry.mixinconstraints.annotations.IfMinecraftVersion;
import org.spongepowered.asm.mixin.Mixin;

//? if <1.21.2 {
/*import com.evandev.reliable_recipes.config.RecipeConfigIO;
import com.evandev.reliable_recipes.recipe.BrewingRecipeManager;
import com.evandev.reliable_recipes.recipe.RecipeModifier;
import com.google.gson.JsonElement;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.crafting.RecipeManager;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;
*///?}

@IfMinecraftVersion(maxVersion = "1.21.1")
@Mixin(targets = "net.minecraft.world.item.crafting.RecipeManager")
public class RecipeManagerMixin {

    //? if <1.21.2 {
    /*@Inject(
            method = "apply(Ljava/util/Map;Lnet/minecraft/server/packs/resources/ResourceManager;Lnet/minecraft/util/profiling/ProfilerFiller;)V",
            at = @At("HEAD")
    )
    private void reliableRecipes$filterJsonAndReset(Map<Identifier, JsonElement> object, ResourceManager resourceManager, ProfilerFiller profiler, CallbackInfo ci) {
        RecipeConfigIO.invalidateCache();
        RecipeModifier.modifyRecipesJson(object, resourceManager);
    }

    @Inject(
            method = "apply(Ljava/util/Map;Lnet/minecraft/server/packs/resources/ResourceManager;Lnet/minecraft/util/profiling/ProfilerFiller;)V",
            at = @At("RETURN")
    )
    private void reliableRecipes$onRecipesApplied(Map<Identifier, JsonElement> object, ResourceManager resourceManager, ProfilerFiller profiler, CallbackInfo ci) {
        BrewingRecipeManager.reload((RecipeManager) (Object) this);
    }
    *///?}
}
