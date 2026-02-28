package com.evandev.reliable_recipes.compat.emi;

import com.evandev.reliable_recipes.api.ReliableRecipesAPI;
import dev.emi.emi.api.EmiEntrypoint;
import dev.emi.emi.api.EmiPlugin;
import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import net.minecraft.world.item.ItemStack;

@EmiEntrypoint
public class ReliableRecipesEmiPlugin implements EmiPlugin {

    @Override
    public void register(EmiRegistry registry) {
        registry.removeRecipes(recipe -> {
            for (EmiStack stack : recipe.getOutputs()) {
                if (isHidden(stack)) return true;
            }

            for (EmiIngredient ingredient : recipe.getInputs()) {
                for (EmiStack stack : ingredient.getEmiStacks()) {
                    if (isHidden(stack)) return true;
                }
            }

            return false;
        });
    }

    private boolean isHidden(EmiStack emiStack) {
        if (emiStack.isEmpty()) return false;
        ItemStack stack = emiStack.getItemStack();
        if (stack == null || stack.isEmpty()) return false;

        return ReliableRecipesAPI.isItemHidden(stack);
    }
}