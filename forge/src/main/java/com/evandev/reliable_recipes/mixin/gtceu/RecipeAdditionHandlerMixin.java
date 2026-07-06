package com.evandev.reliable_recipes.mixin.gtceu;

import com.evandev.reliable_recipes.recipe.RecipeModifier;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;
import com.gregtechceu.gtceu.api.recipe.GTRecipeSerializer;
import com.gregtechceu.gtceu.api.recipe.lookup.RecipeAdditionHandler;
import com.gregtechceu.gtceu.api.recipe.lookup.StagingRecipeDB;
import com.gregtechceu.gtceu.api.registry.GTRegistries;
import com.mojang.serialization.JsonOps;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.minecraft.resources.RegistryOps;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

@Mixin(value = RecipeAdditionHandler.class, remap = false)
public abstract class RecipeAdditionHandlerMixin {

    @Final
    @Shadow
    private StagingRecipeDB stagingDB;

    @Shadow
    private boolean isStaging;

    @Inject(method = "completeStaging", at = @At("HEAD"), remap = false)
    private void reliable_recipes$onCompleteStaging(CallbackInfo ci) {
        if (!this.isStaging) return;
        ObjectOpenHashSet<GTRecipe> recipes = ((StagingRecipeDBAccessor) (Object) this.stagingDB).getRecipes();
        if (recipes == null || recipes.isEmpty()) return;

        var ops = RegistryOps.create(JsonOps.INSTANCE, GTRegistries.builtinRegistry());
        List<GTRecipe> toRemove = new ArrayList<>();
        Map<GTRecipe, GTRecipe> replacements = new IdentityHashMap<>();

        for (GTRecipe recipe : recipes) {
            try {
                JsonElement jsonEl = GTRecipeSerializer.CODEC.encodeStart(ops, recipe).getOrThrow(false, msg -> {
                });
                if (jsonEl.isJsonObject()) {
                    JsonObject json = jsonEl.getAsJsonObject();
                    JsonObject modified = RecipeModifier.modifySingleRecipeJson(recipe.getId(), json);
                    if (modified == null) {
                        toRemove.add(recipe);
                    } else if (!modified.equals(json)) {
                        GTRecipe newRecipe = GTRecipeSerializer.SERIALIZER.fromJson(recipe.getId(), modified);
                        replacements.put(recipe, newRecipe);
                    }
                }
            } catch (Exception ignored) {
            }
        }

        for (GTRecipe recipe : toRemove) {
            recipes.remove(recipe);
        }

        for (Map.Entry<GTRecipe, GTRecipe> entry : replacements.entrySet()) {
            recipes.remove(entry.getKey());
            recipes.add(entry.getValue());
        }
    }
}
