package com.evandev.reliable_recipes.compat.emi;

import com.evandev.reliable_recipes.api.ReliableRecipesAPI;
import dev.emi.emi.api.EmiEntrypoint;
import dev.emi.emi.api.EmiPlugin;
import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;

import java.util.ArrayList;
import java.util.List;

@EmiEntrypoint
public class ReliableRecipesEmiPlugin implements EmiPlugin {

    @Override
    public void register(EmiRegistry registry) {
        registry.removeRecipes(recipe -> {
            try {
                if (recipe == null || recipe.getCategory() == null) return false;
                boolean isRepairRecipe = isRepairCategory(recipe.getCategory().getId());

                if (isRepairRecipe) {
                    List<EmiStack> outputs = recipe.getOutputs();
                    if (outputs != null && outputs.size() == 1) {
                        ItemStack outStack = outputs.get(0).getItemStack();
                        if (outStack != null && !outStack.isEmpty()) {
                            Ingredient customMat = ReliableRecipesAPI.getCustomRepairMaterial(outStack.getItem());
                            if (customMat != null && !customMat.isEmpty()) {
                                applyCustomRepairMaterial(recipe, customMat, outStack.getItem());
                            }
                        }
                    }
                }

                List<EmiStack> outputs = recipe.getOutputs();
                if (outputs == null || outputs.isEmpty()) return false;

                List<EmiIngredient> inputs = recipe.getInputs();
                List<EmiIngredient> catalysts = recipe.getCatalysts();
                List<ItemStack> allInputStacks = new ArrayList<>();

                if (inputs != null) {
                    for (EmiIngredient input : inputs) {
                        if (input == null) continue;
                        List<EmiStack> stacks = input.getEmiStacks();
                        if (stacks != null) {
                            for (EmiStack s : stacks) {
                                if (s != null && !s.isEmpty() && s.getItemStack() != null) {
                                    allInputStacks.add(s.getItemStack());
                                }
                            }
                        }
                    }
                }

                List<ItemStack> allCatalystStacks = new ArrayList<>();
                if (catalysts != null) {
                    for (EmiIngredient catalyst : catalysts) {
                        if (catalyst == null) continue;
                        List<EmiStack> stacks = catalyst.getEmiStacks();
                        if (stacks != null) {
                            for (EmiStack s : stacks) {
                                if (s != null && !s.isEmpty() && s.getItemStack() != null) {
                                    allCatalystStacks.add(s.getItemStack());
                                }
                            }
                        }
                    }
                }

                boolean hasAnyRealOutput = false;
                boolean hasValidOutput = false;

                for (EmiStack outputEmi : outputs) {
                    if (outputEmi == null || outputEmi.isEmpty()) continue;
                    ItemStack outputStack = outputEmi.getItemStack();
                    if (outputStack == null || outputStack.isEmpty()) continue;

                    boolean isReturnedTool = false;

                    // Check cached inputs
                    for (ItemStack inStack : allInputStacks) {
                        if (ItemStack.isSameItem(inStack, outputStack)) {
                            isReturnedTool = true;
                            break;
                        }
                    }

                    // Check cached catalysts
                    if (!isReturnedTool) {
                        for (ItemStack catStack : allCatalystStacks) {
                            if (ItemStack.isSameItem(catStack, outputStack)) {
                                isReturnedTool = true;
                                break;
                            }
                        }
                    }

                    if (!isReturnedTool) {
                        hasAnyRealOutput = true;
                        if (!isHidden(outputEmi) && !(isRepairRecipe && isRepairBlocked(outputEmi))) {
                            hasValidOutput = true;
                        }
                    }
                }

                if (hasAnyRealOutput && !hasValidOutput) {
                    return true;
                }

                if (inputs != null) {
                    for (EmiIngredient ingredient : inputs) {
                        if (ingredient == null) continue;
                        List<EmiStack> stacks = ingredient.getEmiStacks();
                        if (stacks != null && !stacks.isEmpty()) {
                            boolean allHidden = true;
                            boolean allRepairBlocked = true;

                            for (EmiStack stack : stacks) {
                                if (stack == null) continue;
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
                }
                return false;
            } catch (Exception e) {
                return false;
            }
        });
    }

    private void applyCustomRepairMaterial(EmiRecipe recipe, Ingredient customMat, Item toolItem) {
        EmiIngredient newEmiMat = EmiIngredient.of(customMat);
        Class<?> clazz = recipe.getClass();

        while (clazz != Object.class && clazz != null) {
            for (java.lang.reflect.Field field : clazz.getDeclaredFields()) {
                try {
                    field.setAccessible(true);
                    Object val = field.get(recipe);

                    if (val instanceof EmiIngredient emiIng) {
                        if (!isToolIngredient(emiIng, toolItem)) {
                            field.set(recipe, newEmiMat);
                        }
                    } else if (val instanceof List<?> list) {
                        if (!list.isEmpty() && list.get(0) instanceof EmiIngredient) {
                            List<EmiIngredient> newList = new ArrayList<>();
                            boolean changed = false;

                            for (Object obj : list) {
                                EmiIngredient emiIng = (EmiIngredient) obj;
                                if (!isToolIngredient(emiIng, toolItem)) {
                                    newList.add(newEmiMat);
                                    changed = true;
                                } else {
                                    newList.add(emiIng);
                                }
                            }
                            if (changed) {
                                field.set(recipe, newList);
                            }
                        }
                    }
                } catch (Exception ignored) {
                }
            }
            clazz = clazz.getSuperclass();
        }
    }

    private boolean isToolIngredient(EmiIngredient emiIng, Item toolItem) {
        for (EmiStack s : emiIng.getEmiStacks()) {
            if (s.getItemStack() != null && s.getItemStack().getItem() == toolItem) {
                return true;
            }
        }
        return false;
    }

    private boolean isHidden(EmiStack emiStack) {
        if (emiStack == null || emiStack.isEmpty()) return false;
        ItemStack stack = emiStack.getItemStack();
        if (stack == null || stack.isEmpty()) return false;
        return ReliableRecipesAPI.isItemHidden(stack);
    }

    private boolean isRepairBlocked(EmiStack emiStack) {
        if (emiStack == null || emiStack.isEmpty()) return false;
        ItemStack stack = emiStack.getItemStack();
        if (stack == null || stack.isEmpty()) return false;
        return ReliableRecipesAPI.isRepairBlocked(stack);
    }

    private boolean isRepairCategory(ResourceLocation categoryId) {
        if (categoryId == null) return false;
        String path = categoryId.getPath();
        return path.contains("anvil") || path.contains("grindstone") || path.contains("repair");
    }
}