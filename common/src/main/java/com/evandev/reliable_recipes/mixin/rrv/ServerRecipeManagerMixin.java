package com.evandev.reliable_recipes.mixin.rrv;

import cc.cassian.rrv.api.recipe.ReliableServerRecipeType;
import cc.cassian.rrv.common.builtin.anvil.AnvilCombiningServerRecipe;
import cc.cassian.rrv.common.recipe.ServerRecipeManager;
import cc.cassian.rrv.common.recipe.ServerRecipeManager.ServerRecipeEntry;
import cc.cassian.rrv.common.recipe.inventory.SlotContent;
import com.evandev.reliable_recipes.api.ReliableRecipesAPI;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.HashMap;
import java.util.List;

@Mixin(value = ServerRecipeManager.class, remap = false)
public class ServerRecipeManagerMixin {

    @Shadow
    @Final
    private static HashMap<ReliableServerRecipeType<?>, List<ServerRecipeEntry>> PRESENT_RECIPES;

    @Inject(method = "reloadRecipes", at = @At("RETURN"))
    private void reliableRecipes$filterBlockedRecipes(CallbackInfo ci) {
        List<ServerRecipeEntry> anvilRecipes = PRESENT_RECIPES.get(AnvilCombiningServerRecipe.TYPE);

        if (anvilRecipes != null) {
            anvilRecipes.removeIf(entry -> {
                if (entry.recipe() instanceof AnvilCombiningServerRecipe anvilRecipe) {
                    SlotContent result = anvilRecipe.getResult();
                    if (result != null) {
                        for (ItemStack stack : result.getValidContents()) {
                            if (ReliableRecipesAPI.isRepairBlocked(stack)) {
                                return true;
                            }
                        }
                    }
                }
                return false;
            });
        }
    }
}