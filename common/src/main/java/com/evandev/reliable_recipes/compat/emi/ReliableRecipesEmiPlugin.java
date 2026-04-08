package com.evandev.reliable_recipes.compat.emi;

import com.evandev.reliable_recipes.api.ReliableRecipesAPI;
import dev.emi.emi.api.EmiEntrypoint;
import dev.emi.emi.api.EmiPlugin;
import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

import java.util.List;

@EmiEntrypoint
public class ReliableRecipesEmiPlugin implements EmiPlugin {

    @Override
    public void register(EmiRegistry registry) {
        registry.removeRecipes(recipe -> {
            boolean isRepairRecipe = isRepairCategory(recipe.getCategory().getId());

            boolean hasAnyRealOutput = false;
            boolean hasValidOutput = false;

            for (EmiStack stack : recipe.getOutputs()) {
                boolean isReturnedTool = false;

                for (EmiIngredient input : recipe.getInputs()) {
                    if (input.getEmiStacks().stream().anyMatch(s -> ItemStack.isSameItem(s.getItemStack(), stack.getItemStack()))) {
                        isReturnedTool = true;
                        break;
                    }
                }

                if (!isReturnedTool) {
                    for (EmiIngredient catalyst : recipe.getCatalysts()) {
                        if (catalyst.getEmiStacks().stream().anyMatch(s -> ItemStack.isSameItem(s.getItemStack(), stack.getItemStack()))) {
                            isReturnedTool = true;
                            break;
                        }
                    }
                }

                if (!isReturnedTool) {
                    hasAnyRealOutput = true;
                    if (!isHidden(stack) && !(isRepairRecipe && isRepairBlocked(stack))) {
                        hasValidOutput = true;
                    }
                }
            }

            if (hasAnyRealOutput && !hasValidOutput) {
                return true;
            }

            for (EmiIngredient ingredient : recipe.getInputs()) {
                List<EmiStack> stacks = ingredient.getEmiStacks();
                if (stacks != null && !stacks.isEmpty()) {
                    boolean allHidden = true;
                    boolean allRepairBlocked = true;

                    for (EmiStack stack : stacks) {
                        if (!isHidden(stack)) {
                            allHidden = false;
                        }
                        if (!isRepairRecipe || !isRepairBlocked(stack)) {
                            allRepairBlocked = false;
                        }
                    }

                    if (allHidden) return true;
                    if (isRepairRecipe && allRepairBlocked) return true;
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

    private boolean isRepairBlocked(EmiStack emiStack) {
        if (emiStack.isEmpty()) return false;
        ItemStack stack = emiStack.getItemStack();
        if (stack == null || stack.isEmpty()) return false;

        return ReliableRecipesAPI.isRepairBlocked(stack);
    }

    private boolean isRepairCategory(Identifier categoryId) {
        if (categoryId == null) return false;
        String path = categoryId.getPath();

        return path.contains("anvil") || path.contains("grindstone") || path.contains("repair");
    }
}