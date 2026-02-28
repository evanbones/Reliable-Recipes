package com.evandev.reliable_recipes.mixin.minecraft;

import com.evandev.reliable_recipes.api.ReliableRecipesAPI;
import net.minecraft.world.Container;
import net.minecraft.world.inventory.ItemCombinerMenu;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemCombinerMenu.class)
public abstract class ItemCombinerMenuMixin {

    @Shadow
    @Final
    protected ResultContainer resultSlots;
    @Shadow
    @Final
    protected Container inputSlots;

    @Inject(method = "slotsChanged", at = @At("RETURN"))
    private void reliableRecipes$blockHiddenCombiner(Container container, CallbackInfo ci) {
        if (container == this.inputSlots) {

            ItemStack result = this.resultSlots.getItem(0);
            if (!result.isEmpty() && (ReliableRecipesAPI.isItemHidden(result))) {
                this.resultSlots.setItem(0, ItemStack.EMPTY);
                return;
            }

            for (int i = 0; i < this.inputSlots.getContainerSize(); i++) {
                ItemStack input = this.inputSlots.getItem(i);
                if (!input.isEmpty() && (ReliableRecipesAPI.isItemHidden(input))) {
                    this.resultSlots.setItem(0, ItemStack.EMPTY);
                    return;
                }
            }
        }
    }
}