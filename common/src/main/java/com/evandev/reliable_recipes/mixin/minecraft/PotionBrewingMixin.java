package com.evandev.reliable_recipes.mixin.minecraft;

import com.evandev.reliable_recipes.recipe.BrewingRecipeManager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.alchemy.PotionBrewing;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;

@Mixin(PotionBrewing.class)
public class PotionBrewingMixin {

    @Inject(method = "hasMix", at = @At("HEAD"), cancellable = true)
    private void reliableRecipes$customHasMix(ItemStack container, ItemStack ingredient, CallbackInfoReturnable<Boolean> cir) {
        if (BrewingRecipeManager.isSuppressingOverrides()) {
            return;
        }

        if (BrewingRecipeManager.hasMix(container, ingredient)) {
            cir.setReturnValue(true);
            return;
        }

        Optional<ItemStack> replacedTarget = BrewingRecipeManager.getReplacedReagent(ingredient);
        if (replacedTarget.isPresent()) {
            PotionBrewing self = (PotionBrewing) (Object) this;
            BrewingRecipeManager.withVanillaBehavior(() -> {
                if (self.hasMix(container, replacedTarget.get())) {
                    cir.setReturnValue(true);
                }
            });
        }
    }

    @Inject(method = "hasMix", at = @At("RETURN"), cancellable = true)
    private void reliableRecipes$filterVanillaHasMix(ItemStack container, ItemStack ingredient, CallbackInfoReturnable<Boolean> cir) {
        if (BrewingRecipeManager.isSuppressingOverrides()) {
            return;
        }

        if (Boolean.TRUE.equals(cir.getReturnValue())) {
            PotionBrewing self = (PotionBrewing) (Object) this;
            ItemStack output = self.mix(ingredient, container);
            if (BrewingRecipeManager.isBrewingRecipeRemoved(container, ingredient, output)) {
                cir.setReturnValue(false);
            }
        }
    }

    @Inject(method = "mix", at = @At("HEAD"), cancellable = true)
    private void reliableRecipes$customMix(ItemStack ingredient, ItemStack container, CallbackInfoReturnable<ItemStack> cir) {
        if (BrewingRecipeManager.hasMix(container, ingredient)) {
            cir.setReturnValue(BrewingRecipeManager.mix(ingredient, container));
            return;
        }

        if (BrewingRecipeManager.isSuppressingOverrides()) {
            return;
        }

        Optional<ItemStack> replacedTarget = BrewingRecipeManager.getReplacedReagent(ingredient);
        if (replacedTarget.isPresent()) {
            PotionBrewing self = (PotionBrewing) (Object) this;
            BrewingRecipeManager.withVanillaBehavior(() -> {
                ItemStack result = self.mix(replacedTarget.get(), container);
                if (!result.isEmpty() && !ItemStack.isSameItemSameComponents(result, container)) {
                    cir.setReturnValue(result);
                }
            });
        }
    }

    @Inject(method = "isIngredient", at = @At("HEAD"), cancellable = true, require = 0)
    private void reliableRecipes$customIsIngredient(ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        if (BrewingRecipeManager.isReagent(stack)) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "isContainer", at = @At("HEAD"), cancellable = true, require = 0)
    private void reliableRecipes$customIsContainer(ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        if (BrewingRecipeManager.isInput(stack)) {
            cir.setReturnValue(true);
        }
    }
}
