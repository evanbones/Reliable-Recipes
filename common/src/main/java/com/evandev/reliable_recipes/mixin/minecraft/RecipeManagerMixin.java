package com.evandev.reliable_recipes.mixin.minecraft;

import com.evandev.reliable_recipes.api.ReliableRecipesAPI;
import com.evandev.reliable_recipes.recipe.RecipeModifier;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.HashMap;
import java.util.Map;

@Mixin(RecipeManager.class)
public class RecipeManagerMixin {

    @Inject(
            method = "apply(Ljava/util/Map;Lnet/minecraft/server/packs/resources/ResourceManager;Lnet/minecraft/util/profiling/ProfilerFiller;)V",
            at = @At("HEAD")
    )
    private void reliableRecipes$filterJsonAndReset(Map<Identifier, JsonElement> map, ResourceManager resourceManager, ProfilerFiller profiler, CallbackInfo ci) {
        RecipeModifier.reset();

        if (!ReliableRecipesAPI.hasItemHidingCapabilities()) return;

        Map<Identifier, JsonElement> filteredMap = new HashMap<>();

        for (Map.Entry<Identifier, JsonElement> entry : map.entrySet()) {
            Identifier id = entry.getKey();
            JsonElement jsonElement = entry.getValue();

            if (reliable_recipes$shouldDisableJson(jsonElement)) {
                continue;
            }

            filteredMap.put(id, jsonElement);
        }

        map.clear();
        map.putAll(filteredMap);
    }

    @Unique
    private boolean reliable_recipes$shouldDisableJson(JsonElement jsonElement) {
        try {
            if (!jsonElement.isJsonObject()) return false;
            JsonObject jsonObject = jsonElement.getAsJsonObject();
            JsonElement resultElement = null;

            if (jsonObject.has("result")) resultElement = jsonObject.get("result");
            else if (jsonObject.has("output")) resultElement = jsonObject.get("output");

            if (resultElement == null) return false;

            if (resultElement.isJsonObject()) {
                return reliable_recipes$isItemHidden(reliable_recipes$getResultItemId(resultElement.getAsJsonObject()));
            } else if (resultElement.isJsonPrimitive() && resultElement.getAsJsonPrimitive().isString()) {
                return reliable_recipes$isItemHidden(resultElement.getAsString());
            } else if (resultElement.isJsonArray()) {
                for (JsonElement element : resultElement.getAsJsonArray()) {
                    if (element.isJsonObject() && reliable_recipes$isItemHidden(reliable_recipes$getResultItemId(element.getAsJsonObject())))
                        return true;
                    if (element.isJsonPrimitive() && reliable_recipes$isItemHidden(element.getAsString())) return true;
                }
            }
        } catch (Exception ignored) {
        }
        return false;
    }

    @Unique
    private String reliable_recipes$getResultItemId(JsonObject resultObject) {
        if (resultObject.has("item")) return GsonHelper.getAsString(resultObject, "item");
        if (resultObject.has("id")) return GsonHelper.getAsString(resultObject, "id");
        if (resultObject.has("result")) return GsonHelper.getAsString(resultObject, "result");
        if (resultObject.has("output")) return GsonHelper.getAsString(resultObject, "output");
        return null;
    }

    @Unique
    private boolean reliable_recipes$isItemHidden(String itemId) {
        if (itemId == null || itemId.isEmpty()) return false;
        Identifier location = Identifier.tryParse(itemId);
        if (location == null) return false;
        Item item = BuiltInRegistries.ITEM.get(location);
        if (item == Items.AIR) return false;

        return ReliableRecipesAPI.isItemHidden(item.getDefaultInstance());
    }
}