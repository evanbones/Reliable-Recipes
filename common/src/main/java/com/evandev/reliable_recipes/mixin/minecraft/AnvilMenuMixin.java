package com.evandev.reliable_recipes.mixin.minecraft;

import com.evandev.reliable_recipes.api.ReliableRecipesAPI;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.ItemCombinerMenu;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AnvilMenu.class)
public abstract class AnvilMenuMixin extends ItemCombinerMenu {

    public AnvilMenuMixin() {
        super(null, 0, null, null);
    }

    @Inject(method = "createResult", at = @At("RETURN"))
    private void reliableRecipes$blockAnvilRepairs(CallbackInfo ci) {
        ItemStack result = this.resultSlots.getItem(0);
        if (!result.isEmpty() && ReliableRecipesAPI.isItemHidden(result)) {
            this.resultSlots.setItem(0, ItemStack.EMPTY);
            return;
        }

        for (int i = 0; i < this.inputSlots.getContainerSize(); i++) {
            ItemStack input = this.inputSlots.getItem(i);
            if (!input.isEmpty() && ReliableRecipesAPI.isItemHidden(input)) {
                this.resultSlots.setItem(0, ItemStack.EMPTY);
                return;
            }
        }

        ItemStack input1 = this.inputSlots.getItem(0);
        ItemStack input2 = this.inputSlots.getItem(1);

        if (!input1.isEmpty() && !input2.isEmpty() && ReliableRecipesAPI.isRepairBlocked(input1)) {
            boolean isRepairAttempt = ItemStack.isSameItem(input1, input2) || input1.getItem().isValidRepairItem(input1, input2);

            if (isRepairAttempt) {
                this.resultSlots.setItem(0, ItemStack.EMPTY);
            }
        }
    }
}