package com.evandev.reliable_recipes.mixin.minecraft;

import com.evandev.reliable_recipes.compat.ReliableRemoverCompat;
import com.evandev.reliable_recipes.platform.Services;
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
    Container repairSlots;
    @Shadow
    @Final
    private Container resultSlots;

    @Inject(method = "createResult", at = @At("RETURN"))
    private void reliableRecipes$blockHiddenGrindstone(CallbackInfo ci) {
        ItemStack result = this.resultSlots.getItem(0);
        if (!result.isEmpty() && (Services.PLATFORM.isItemHidden(result) || ReliableRemoverCompat.isHidden(result))) {
            this.resultSlots.setItem(0, ItemStack.EMPTY);
            return;
        }

        for (int i = 0; i < this.repairSlots.getContainerSize(); i++) {
            ItemStack input = this.repairSlots.getItem(i);
            if (!input.isEmpty() && (Services.PLATFORM.isItemHidden(input) || ReliableRemoverCompat.isHidden(input))) {
                this.resultSlots.setItem(0, ItemStack.EMPTY);
                return;
            }
        }
    }
}