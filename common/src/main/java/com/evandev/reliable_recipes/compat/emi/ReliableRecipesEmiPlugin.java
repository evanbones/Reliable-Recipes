package com.evandev.reliable_recipes.compat.emi;

import com.evandev.reliable_recipes.Constants;
import com.evandev.reliable_recipes.api.ReliableRecipesAPI;
import com.evandev.reliable_recipes.config.RecipeConfigIO;
import com.evandev.reliable_recipes.recipe.BrewingRecipe;
import com.evandev.reliable_recipes.recipe.BrewingRecipeManager;
import com.evandev.reliable_recipes.recipe.RecipeRule;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import dev.emi.emi.api.EmiEntrypoint;
import dev.emi.emi.api.EmiPlugin;
import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.recipe.EmiAnvilRecipe;
import dev.emi.emi.recipe.EmiBrewingRecipe;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionBrewing;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@EmiEntrypoint
public class ReliableRecipesEmiPlugin implements EmiPlugin {

    @Override
    public void register(EmiRegistry registry) {
        registerCustomBrewingRecipes(registry);
        registerReplacedBrewingReagents(registry);
        registerCustomAnvilRepairRecipes(registry);
        removeDisabledRecipes(registry);
    }

    private void registerCustomBrewingRecipes(EmiRegistry registry) {
        for (BrewingRecipe brewingRecipe : BrewingRecipeManager.getBrewingRecipes()) {
            try {
                EmiIngredient reagent = EmiIngredient.of(brewingRecipe.getReagent());
                EmiStack output = EmiStack.of(brewingRecipe.getOutput());

                List<EmiStack> inputStacks = new ArrayList<>();
                ItemStack[] matchingInputs = brewingRecipe.getInputMatcher().ingredient().getItems();

                for (ItemStack match : matchingInputs) {
                    ItemStack copy = match.copy();
                    if (brewingRecipe.getInputMatcher().potionContents().isPresent() && !brewingRecipe.getInputMatcher().potionContents().get().isEmpty()) {
                        ResourceLocation potionId = brewingRecipe.getInputMatcher().potionContents().get().getFirst();
                        BuiltInRegistries.POTION.getHolder(ResourceKey.create(Registries.POTION, potionId))
                                .ifPresent(potionHolder -> copy.set(DataComponents.POTION_CONTENTS, new PotionContents(Optional.of(potionHolder), Optional.empty(), List.of())));
                    }
                    inputStacks.add(EmiStack.of(copy));
                }

                if (!inputStacks.isEmpty()) {
                    for (EmiStack inputStack : inputStacks) {
                        ResourceLocation id = ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "/brewing/" + BuiltInRegistries.ITEM.getKey(output.getItemStack().getItem()).getPath() + "_" + Math.abs(brewingRecipe.hashCode()));
                        registry.addRecipe(new EmiBrewingRecipe(inputStack, reagent, output, id));
                    }
                }
            } catch (Exception ignored) {
            }
        }
    }

    private void registerCustomAnvilRepairRecipes(EmiRegistry registry) {
        for (Item item : BuiltInRegistries.ITEM) {
            Ingredient customMat = ReliableRecipesAPI.getCustomRepairMaterial(item);
            if (customMat != null && !customMat.isEmpty()) {
                ResourceLocation id = ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "/anvil_repair/" + BuiltInRegistries.ITEM.getKey(item).getPath());
                registry.addRecipe(new EmiAnvilRecipe(EmiStack.of(item), EmiIngredient.of(customMat), id));
            }
        }
    }

    private void registerReplacedBrewingReagents(EmiRegistry registry) {
        Level level = Minecraft.getInstance().level;
        if (level == null) return;
        PotionBrewing brewing = level.potionBrewing();
        List<RecipeRule> rules = RecipeConfigIO.loadRules();

        BrewingRecipeManager.withVanillaBehavior(() -> {
            int counter = 0;
            for (RecipeRule rule : rules) {
                if (rule.getAction() != RecipeRule.Action.REPLACE_INPUT) continue;

                Ingredient targetIngredient = rule.targetsIngredient();
                if (targetIngredient.isEmpty()) continue;

                Ingredient replacementIngredient = rule.replacementIngredient();
                if (replacementIngredient.isEmpty()) continue;
                EmiIngredient replacement = EmiIngredient.of(replacementIngredient);

                for (ItemStack targetStack : targetIngredient.getItems()) {
                    for (Holder.Reference<Potion> basePotion : BuiltInRegistries.POTION.holders().toList()) {
                        ItemStack containerStack = PotionContents.createItemStack(Items.POTION, basePotion);
                        if (!brewing.hasMix(containerStack, targetStack)) continue;

                        ItemStack outputStack = brewing.mix(targetStack, containerStack);
                        if (outputStack.isEmpty()) continue;

                        ResourceLocation id = ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID,
                                "/brewing_replacement/" + BuiltInRegistries.ITEM.getKey(outputStack.getItem()).getPath() + "_" + counter++);

                        registry.addRecipe(new EmiBrewingRecipe(EmiStack.of(containerStack), replacement, EmiStack.of(outputStack), id));
                    }
                }
            }
        });
    }

    private void removeDisabledRecipes(EmiRegistry registry) {
        registry.removeRecipes(this::shouldRemoveRecipe);
    }

    private boolean shouldRemoveRecipe(EmiRecipe recipe) {
        try {
            if (recipe == null || recipe.getCategory() == null) return false;

            if (matchesUserRemovalRule(recipe)) {
                return true;
            }

            boolean isRepairRecipe = isRepairCategory(recipe.getCategory().getId());
            List<EmiStack> outputs = recipe.getOutputs();
            if (outputs == null || outputs.isEmpty()) return false;

            List<EmiIngredient> inputs = recipe.getInputs();
            List<EmiIngredient> catalysts = recipe.getCatalysts();
            List<ItemStack> allInputStacks = extractItemStacks(inputs);
            List<ItemStack> allCatalystStacks = extractItemStacks(catalysts);

            if (containsReplacedInput(allInputStacks)) {
                return true;
            }

            if (hasHiddenOutput(outputs, allInputStacks, allCatalystStacks, isRepairRecipe)) {
                return true;
            }

            return hasHiddenInput(inputs, isRepairRecipe);
        } catch (Exception e) {
            return false;
        }
    }

    private boolean matchesUserRemovalRule(EmiRecipe recipe) {
        List<RecipeRule> rules = RecipeConfigIO.loadRules();
        if (rules.isEmpty()) return false;

        ResourceLocation recipeId = recipe.getId() != null ? recipe.getId() : ResourceLocation.withDefaultNamespace("emi_recipe");
        JsonObject recipeJson = createJsonFromEmiRecipe(recipe);

        for (RecipeRule rule : rules) {
            if (rule.getAction() == RecipeRule.Action.REMOVE) {
                if (rule.testJson(recipeId, recipeJson)) {
                    return true;
                }
            }
        }
        return false;
    }

    private JsonObject createJsonFromEmiRecipe(EmiRecipe recipe) {
        JsonObject json = new JsonObject();

        ResourceLocation categoryId = recipe.getCategory() != null ? recipe.getCategory().getId() : null;
        if (categoryId != null) {
            String typeStr = categoryId.toString();
            if (categoryId.getNamespace().equals("emi")) {
                typeStr = "minecraft:" + categoryId.getPath();
            }
            json.addProperty("type", typeStr);
        }

        List<EmiStack> outputs = recipe.getOutputs();
        if (outputs != null && !outputs.isEmpty()) {
            JsonArray outputArr = new JsonArray();
            for (EmiStack out : outputs) {
                if (out != null && !out.isEmpty() && out.getItemStack() != null) {
                    outputArr.add(BrewingRecipeManager.stackToJson(out.getItemStack()));
                }
            }
            if (outputArr.size() == 1) {
                json.add("result", outputArr.get(0));
                json.add("output", outputArr.get(0));
            } else if (outputArr.size() > 1) {
                json.add("results", outputArr);
                json.add("output", outputArr);
            }
        }

        List<EmiIngredient> inputs = recipe.getInputs();
        JsonArray inputsArr = new JsonArray();

        if (inputs != null) {
            for (EmiIngredient ing : inputs) {
                if (ing != null && !ing.getEmiStacks().isEmpty()) {
                    JsonArray ingArr = new JsonArray();
                    for (EmiStack s : ing.getEmiStacks()) {
                        if (s != null && !s.isEmpty() && s.getItemStack() != null) {
                            ingArr.add(BrewingRecipeManager.stackToJson(s.getItemStack()));
                        }
                    }
                    if (ingArr.size() == 1) {
                        inputsArr.add(ingArr.get(0));
                    } else if (ingArr.size() > 1) {
                        inputsArr.add(ingArr);
                    }
                }
            }
        }

        if (inputsArr.size() >= 2) {
            json.add("input", inputsArr.get(0));
            json.add("reagent", inputsArr.get(1));
        } else if (inputsArr.size() == 1) {
            json.add("input", inputsArr.get(0));
        }
        json.add("ingredients", inputsArr);

        return json;
    }

    private List<ItemStack> extractItemStacks(List<EmiIngredient> ingredients) {
        List<ItemStack> stacks = new ArrayList<>();
        if (ingredients != null) {
            for (EmiIngredient ingredient : ingredients) {
                if (ingredient == null) continue;
                List<EmiStack> emiStacks = ingredient.getEmiStacks();
                if (emiStacks != null) {
                    for (EmiStack s : emiStacks) {
                        if (s != null && !s.isEmpty() && s.getItemStack() != null) {
                            stacks.add(s.getItemStack());
                        }
                    }
                }
            }
        }
        return stacks;
    }

    private boolean containsReplacedInput(List<ItemStack> allInputStacks) {
        if (allInputStacks.isEmpty()) return false;
        for (RecipeRule rule : RecipeConfigIO.loadRules()) {
            if (rule.getAction() != RecipeRule.Action.REPLACE_INPUT) continue;
            for (ItemStack inStack : allInputStacks) {
                if (rule.targetsMatch(inStack)) return true;
            }
        }
        return false;
    }

    private boolean hasHiddenOutput(List<EmiStack> outputs, List<ItemStack> allInputStacks, List<ItemStack> allCatalystStacks, boolean isRepairRecipe) {
        boolean hasAnyRealOutput = false;
        boolean hasValidOutput = false;

        for (EmiStack outputEmi : outputs) {
            if (outputEmi == null || outputEmi.isEmpty()) continue;
            ItemStack outputStack = outputEmi.getItemStack();
            if (outputStack == null || outputStack.isEmpty()) continue;

            boolean isReturnedTool = isSameItemInList(outputStack, allInputStacks) || isSameItemInList(outputStack, allCatalystStacks);

            if (!isReturnedTool) {
                hasAnyRealOutput = true;
                if (!isHidden(outputEmi) && !(isRepairRecipe && isRepairBlocked(outputEmi))) {
                    hasValidOutput = true;
                }
            }
        }

        return hasAnyRealOutput && !hasValidOutput;
    }

    private boolean isSameItemInList(ItemStack targetStack, List<ItemStack> stackList) {
        for (ItemStack stack : stackList) {
            if (ItemStack.isSameItem(stack, targetStack)) {
                return true;
            }
        }
        return false;
    }

    private boolean hasHiddenInput(List<EmiIngredient> inputs, boolean isRepairRecipe) {
        if (inputs == null) return false;
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