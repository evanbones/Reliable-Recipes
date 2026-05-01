package com.evandev.reliable_recipes.mixin.minecraft;

import com.evandev.reliable_recipes.api.ReliableRecipesAPI;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ItemStack.class)
public abstract class ItemStackMixin {

    @Inject(method = "isValidRepairItem", at = @At("HEAD"), cancellable = true)
    private void reliableRecipes$overrideRepairableCheck(ItemStack repairItem, CallbackInfoReturnable<Boolean> cir) {
        ItemStack stack = (ItemStack) (Object) this;

        if (ReliableRecipesAPI.isRepairBlocked(stack)) {
            cir.setReturnValue(false);
            return;
        }

        Ingredient customMaterial = ReliableRecipesAPI.getCustomRepairMaterial(stack.getItem());
        if (customMaterial != null && !customMaterial.isEmpty()) {
            cir.setReturnValue(customMaterial.test(repairItem));
        }
    }
}