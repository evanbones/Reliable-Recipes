package com.evandev.reliable_recipes.mixin.minecraft;

import com.evandev.reliable_recipes.api.ReliableRecipesAPI;
import com.moulberry.mixinconstraints.annotations.IfMinecraftVersion;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@IfMinecraftVersion(maxVersion = "1.21.1", maxInclusive = true)
@Mixin(targets = {
        "net.minecraft.world.item.Item",
        "net.minecraft.world.item.TieredItem",
        "net.minecraft.world.item.ArmorItem",
        "net.minecraft.world.item.ShieldItem",
        "net.minecraft.world.item.ElytraItem"
})
public class ItemMixin {

    @Inject(method = "isValidRepairItem", at = @At("HEAD"), cancellable = true)
    private void reliableRecipes$customRepairMaterial(ItemStack stack, ItemStack repairCandidate, CallbackInfoReturnable<Boolean> cir) {
        Ingredient customMaterial = ReliableRecipesAPI.getCustomRepairMaterial(stack.getItem());
        if (customMaterial != null && !customMaterial.isEmpty()) {
            cir.setReturnValue(customMaterial.test(repairCandidate));
        }
    }
}
