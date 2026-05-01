package com.evandev.reliable_recipes.mixin.rrv;

import cc.cassian.rrv.api.recipe.ReliableClientRecipe;
import cc.cassian.rrv.client.builtin.BuiltInReliableRecipeViewerClientIntegration;
import cc.cassian.rrv.common.builtin.anvil.AnvilCombiningClientRecipe;
import cc.cassian.rrv.common.recipe.inventory.SlotContent;
import com.evandev.reliable_recipes.Constants;
import com.evandev.reliable_recipes.api.ReliableRecipesAPI;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.lang.reflect.Field;
import java.util.List;

@Mixin(BuiltInReliableRecipeViewerClientIntegration.class)
public class RrvBuiltInIntegrationMixin {
    @Inject(method = "addRepairingRecipes", at = @At("RETURN"), remap = false)
    private static void reliableRecipes$modifyRrvRepairRecipes(List<ReliableClientRecipe> recipeList, CallbackInfo ci) {
        for (int i = recipeList.size() - 1; i >= 0; i--) {
            ReliableClientRecipe recipe = recipeList.get(i);

            if (recipe instanceof AnvilCombiningClientRecipe anvilRecipe) {
                if (anvilRecipe.getId() != null && anvilRecipe.getId().getPath().contains("/anvil_repairing/")) {
                    List<ItemStack> outputs = anvilRecipe.getResults().getFirst().getValidContents();

                    if (!outputs.isEmpty()) {
                        Item tool = outputs.getFirst().getItem();

                        if (ReliableRecipesAPI.isRepairBlocked(outputs.getFirst())) {
                            recipeList.remove(i);
                            continue;
                        }

                        Ingredient customMaterial = ReliableRecipesAPI.getCustomRepairMaterial(tool);
                        if (customMaterial != null && !customMaterial.isEmpty()) {
                            try {
                                List<ItemStack> customStacks = customMaterial.items()
                                        .map(h -> new ItemStack(h.value()))
                                        .toList();

                                SlotContent newRightSlot = SlotContent.of(customStacks);

                                Field rightSlotField = AnvilCombiningClientRecipe.class.getDeclaredField("right");
                                rightSlotField.setAccessible(true);
                                rightSlotField.set(anvilRecipe, newRightSlot);
                            } catch (Exception e) {
                                Constants.LOG.error("Failed to update RRV anvil recipe", e);
                            }
                        }
                    }
                }
            } else if (recipe.getClass().getSimpleName().equals("CraftingClientRecipe")) {
                if (recipe.getId() != null && recipe.getId().getPath().contains("/repairing/")) {
                    List<ItemStack> outputs = recipe.getResults().getFirst().getValidContents();

                    if (!outputs.isEmpty() && ReliableRecipesAPI.isRepairBlocked(outputs.getFirst())) {
                        recipeList.remove(i);
                    }
                }
            }
        }
    }
}