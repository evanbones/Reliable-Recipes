package com.evandev.reliable_recipes.mixin.minecraft;

import com.evandev.reliable_recipes.api.ReliableRecipesAPI;
import net.minecraft.world.Container;
import net.minecraft.world.inventory.GrindstoneMenu;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GrindstoneMenu.class)
public abstract class GrindstoneMenuMixin {

    @Shadow
    @Final
    private Container repairSlots;
    @Shadow
    @Final
    private Container resultSlots;

    @Inject(method = "createResult", at = @At("RETURN"))
    private void reliableRecipes$blockHiddenGrindstone(CallbackInfo ci) {
        ItemStack result = this.resultSlots.getItem(0);
        if (!result.isEmpty() && (ReliableRecipesAPI.isItemHidden(result))) {
            this.resultSlots.setItem(0, ItemStack.EMPTY);
            return;
        }

        for (int i = 0; i < this.repairSlots.getContainerSize(); i++) {
            ItemStack input = this.repairSlots.getItem(i);
            if (!input.isEmpty() && (ReliableRecipesAPI.isItemHidden(input))) {
                this.resultSlots.setItem(0, ItemStack.EMPTY);
                return;
            }
        }

        ItemStack input1 = this.repairSlots.getItem(0);
        ItemStack input2 = this.repairSlots.getItem(1);
        if (!input1.isEmpty() && !input2.isEmpty()) {
            if (ReliableRecipesAPI.isRepairBlocked(input1) || ReliableRecipesAPI.isRepairBlocked(input2)) {
                this.resultSlots.setItem(0, ItemStack.EMPTY);
            }
        }
    }
}