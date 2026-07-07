package com.evandev.reliable_recipes.recipe;

import com.evandev.reliable_recipes.Constants;
import com.evandev.reliable_recipes.api.ReliableRecipesAPI;
import com.evandev.reliable_recipes.config.RecipeConfigIO;
import com.evandev.reliable_recipes.mixin.accessor.HolderReferenceAccessor;
import com.evandev.reliable_recipes.mixin.accessor.RecipeManagerAccessor;
import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.HolderSet;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeMap;
import net.minecraft.world.item.enchantment.Repairable;

import java.util.*;

public class RecipeModifier {

    private static final Map<ResourceKey<Recipe<?>>, RecipeHolder<?>> DELETED_RECIPES_CACHE = new HashMap<>();
    private static RecipeManager lastAppliedManager = null;

    public static void applyGlobalRules() {
        ReliableRecipesAPI.clearRepairBlockers();
        ReliableRecipesAPI.clearCustomRepairMaterials();

        List<RecipeRule> rules = new ArrayList<>(RecipeConfigIO.loadRules());
        for (RecipeRule rule : rules) {

            if (rule.getAction() == RecipeRule.Action.PREVENT_REPAIR) {
                rule.getTargetInput().ifPresent(target -> {
                    for (Item item : BuiltInRegistries.ITEM) {
                        if (target.test(item.getDefaultInstance())) {
                            DataComponentMap oldMap = item.components();
                            DataComponentMap newMap = DataComponentMap.builder()
                                    .addAll(oldMap)
                                    .set(DataComponents.REPAIRABLE, null)
                                    .build();

                            ((HolderReferenceAccessor) item.builtInRegistryHolder()).setComponents(newMap);
                        }
                    }
                });

                ReliableRecipesAPI.registerRepairBlocker(stack ->
                        rule.getTargetInput().map(ing -> ing.test(stack)).orElse(false)
                );
            } else if (rule.getAction() == RecipeRule.Action.SET_REPAIR_MATERIAL) {
                rule.getTargetInput().ifPresent(target -> rule.getNewInput().ifPresent(material -> {
                    HolderSet<Item> materialHolderSet = HolderSet.direct(material.items().toList());
                    Repairable newRepairableComponent = new Repairable(materialHolderSet);

                    for (Item item : BuiltInRegistries.ITEM) {
                        if (target.test(item.getDefaultInstance())) {
                            DataComponentMap oldMap = item.components();
                            DataComponentMap newMap = DataComponentMap.builder()
                                    .addAll(oldMap)
                                    .set(DataComponents.REPAIRABLE, newRepairableComponent)
                                    .build();

                            ((HolderReferenceAccessor) item.builtInRegistryHolder()).setComponents(newMap);

                            ReliableRecipesAPI.registerCustomRepairMaterial(item, material);
                        }
                    }
                }));
            }
        }
    }

    public static void apply(RecipeManager manager, HolderLookup.Provider registries) {
        applyGlobalRules();

        if (manager == lastAppliedManager) return;
        lastAppliedManager = manager;

        List<RecipeRule> rules = new ArrayList<>(RecipeConfigIO.loadRules());

        RecipeManagerAccessor managerAccessor = (RecipeManagerAccessor) manager;
        RecipeMap currentMap = managerAccessor.reliableRecipes$getRecipeMap();
        List<RecipeHolder<?>> validRecipes = new ArrayList<>();

        RegistryOps<JsonElement> ops = registries.createSerializationContext(JsonOps.INSTANCE);
        int replacedCount = 0;

        for (RecipeHolder<?> recipeHolder : currentMap.values()) {
            boolean shouldRemove = false;
            Recipe<?> recipe = recipeHolder.value();

            try {
                Map<String, JsonElement> inputReplacements = new HashMap<>();
                Map<String, JsonElement> outputReplacements = new HashMap<>();

                for (RecipeRule rule : rules) {
                    if (rule.test(recipeHolder)) {
                        if (rule.getAction() == RecipeRule.Action.REMOVE) {
                            shouldRemove = true;
                        } else if (rule.getAction() == RecipeRule.Action.REPLACE_INPUT) {
                            if (rule.getReplaceTargetStr() != null && rule.getReplaceWithEl() != null && !rule.getReplaceTargetStr().isEmpty()) {
                                inputReplacements.put(rule.getReplaceTargetStr(), rule.getReplaceWithEl());
                            }
                        } else if (rule.getAction() == RecipeRule.Action.REPLACE_OUTPUT) {
                            if (rule.getReplaceTargetStr() != null && rule.getReplaceWithEl() != null && !rule.getReplaceTargetStr().isEmpty()) {
                                outputReplacements.put(rule.getReplaceTargetStr(), rule.getReplaceWithEl());
                            }
                        }
                    }
                }

                if (!shouldRemove && shouldHideRecipe(recipeHolder)) {
                    shouldRemove = true;
                }

                if (!shouldRemove && (!inputReplacements.isEmpty() || !outputReplacements.isEmpty())) {
                    Optional<JsonElement> encodeResult = Recipe.CODEC.encodeStart(ops, recipe).result();
                    if (encodeResult.isPresent()) {
                        JsonElement json = encodeResult.get();

                        if (RecipeJsonMutator.mutateRecipe(json, inputReplacements, outputReplacements)) {
                            Optional<Recipe<?>> decodeResult = Recipe.CODEC.parse(ops, json).result();
                            if (decodeResult.isPresent()) {
                                recipe = decodeResult.get();
                                recipeHolder = new RecipeHolder<>(recipeHolder.id(), recipe);
                                replacedCount++;
                            } else {
                                Constants.LOG.error("Failed to decode mutated recipe: {}", recipeHolder.id().identifier());
                                Constants.LOG.debug("Mutated JSON was: {}", json);
                            }
                        }
                    }
                }

                if (!shouldRemove) {
                    validRecipes.add(recipeHolder);
                } else {
                    DELETED_RECIPES_CACHE.put(recipeHolder.id(), recipeHolder);
                }

            } catch (Exception e) {
                Constants.LOG.error("Failed to process recipe modifications for: {}", recipeHolder.id().identifier(), e);
                validRecipes.add(recipeHolder);
            }
        }

        if (!DELETED_RECIPES_CACHE.isEmpty() || replacedCount > 0) {
            Constants.LOG.info("RecipeModifier removed {} and replaced contents in {} recipes.", DELETED_RECIPES_CACHE.size(), replacedCount);
        }

        managerAccessor.reliableRecipes$setRecipeMap(RecipeMap.create(validRecipes));
    }

    public static boolean removeRecipe(RecipeManager manager, ResourceKey<Recipe<?>> recipeKey) {
        RecipeManagerAccessor managerAccessor = (RecipeManagerAccessor) manager;
        RecipeMap currentMap = managerAccessor.reliableRecipes$getRecipeMap();

        RecipeHolder<?> recipe = currentMap.byKey(recipeKey);
        if (recipe != null) {
            DELETED_RECIPES_CACHE.put(recipeKey, recipe);
            List<RecipeHolder<?>> updatedRecipes = new ArrayList<>();
            for (RecipeHolder<?> holder : currentMap.values()) {
                if (!holder.id().equals(recipeKey)) {
                    updatedRecipes.add(holder);
                }
            }
            managerAccessor.reliableRecipes$setRecipeMap(RecipeMap.create(updatedRecipes));
            return true;
        }
        return false;
    }

    public static boolean restoreRecipe(RecipeManager manager, ResourceKey<Recipe<?>> recipeKey) {
        RecipeHolder<?> recipe = DELETED_RECIPES_CACHE.remove(recipeKey);
        if (recipe == null) return false;

        RecipeManagerAccessor managerAccessor = (RecipeManagerAccessor) manager;
        RecipeMap currentMap = managerAccessor.reliableRecipes$getRecipeMap();

        List<RecipeHolder<?>> updatedRecipes = new ArrayList<>(currentMap.values());
        updatedRecipes.add(recipe);

        managerAccessor.reliableRecipes$setRecipeMap(RecipeMap.create(updatedRecipes));
        return true;
    }

    private static boolean shouldHideRecipe(RecipeHolder<?> holder) {
        try {
            List<ItemStack> outputs = ReliableRecipesAPI.getRecipeResults(holder.value());
            for (ItemStack stack : outputs) {
                if (!stack.isEmpty() && ReliableRecipesAPI.isItemHidden(stack)) {
                    return true;
                }
            }
        } catch (Exception ignored) {
        }
        return false;
    }
}