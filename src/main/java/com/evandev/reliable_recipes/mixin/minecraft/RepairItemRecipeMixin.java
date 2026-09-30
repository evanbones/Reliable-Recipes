package com.evandev.reliable_recipes.mixin.minecraft;

import com.evandev.reliable_recipes.api.ReliableRecipesAPI;
import net.minecraft.world.item.ItemStack;
//? if <1.21 {
/*import net.minecraft.world.inventory.CraftingContainer;
*///?} else {
import net.minecraft.world.item.crafting.CraftingInput;
//?}
import net.minecraft.world.item.crafting.RepairItemRecipe;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(RepairItemRecipe.class)
public class RepairItemRecipeMixin {

    //? if <1.21 {
    /*@Inject(
            method = "matches(Lnet/minecraft/world/inventory/CraftingContainer;Lnet/minecraft/world/level/Level;)Z",
            at = @At("RETURN"),
            cancellable = true
    )
    private void reliableRecipes$blockSpecificRepair(CraftingContainer input, Level level, CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValueZ()) {
            for (int i = 0; i < input.getContainerSize(); i++) {
    *///?} else {
    @Inject(
            method = "matches(Lnet/minecraft/world/item/crafting/CraftingInput;Lnet/minecraft/world/level/Level;)Z",
            at = @At("RETURN"),
            cancellable = true
    )
    private void reliableRecipes$blockSpecificRepair(CraftingInput input, Level level, CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValueZ()) {
            for (int i = 0; i < input.size(); i++) {
    //?}
                ItemStack stack = input.getItem(i);
                if (!stack.isEmpty() && ReliableRecipesAPI.isRepairBlocked(stack)) {
                    cir.setReturnValue(false);
                    return;
                }
            }
        }
    }
}