package com.evandev.reliable_recipes.recipe;

import com.evandev.reliable_recipes.Constants;
import com.evandev.reliable_recipes.api.ReliableRecipesAPI;
import com.evandev.reliable_recipes.config.RecipeConfigIO;
import com.evandev.reliable_recipes.mixin.accessor.HolderReferenceAccessor;
import com.evandev.reliable_recipes.mixin.accessor.RecipeManagerAccessor;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMultimap;
import com.google.gson.JsonElement;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.HolderSet;
//? if >26.2 {
import net.minecraft.core.HolderOwner;
import net.minecraft.tags.TagKey;
//?}
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeMap;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.enchantment.Repairable;

import java.util.*;
import java.util.stream.Stream;

public class RecipeModifier {

    //? if <=26.2 {
    /*private static final Codec<Recipe<?>> RECIPE_CODEC = Recipe.CODEC;
    *///?} else {
    private static final Codec<Recipe<?>> RECIPE_CODEC = Recipe.DIRECT_CODEC;
    //?}

    private static final Map<ResourceKey<Recipe<?>>, RecipeHolder<?>> DELETED_RECIPES_CACHE = new HashMap<>();

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
        DELETED_RECIPES_CACHE.clear();

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
                            if (rule.getReplaceWithEl() != null) {
                                for (String target : rule.getReplaceTargetStrs()) {
                                    if (target != null && !target.isEmpty()) {
                                        inputReplacements.put(target, rule.getReplaceWithEl());
                                    }
                                }
                            }
                        } else if (rule.getAction() == RecipeRule.Action.REPLACE_OUTPUT) {
                            if (rule.getReplaceWithEl() != null) {
                                if (rule.getReplaceTargetStrs().isEmpty()) {
                                    outputReplacements.put("", rule.getReplaceWithEl());
                                } else {
                                    for (String target : rule.getReplaceTargetStrs()) {
                                        outputReplacements.put(target, rule.getReplaceWithEl());
                                    }
                                }
                            }
                        }
                    }
                }

                if (!shouldRemove && shouldHideRecipe(recipeHolder)) {
                    shouldRemove = true;
                }

                if (!shouldRemove && (!inputReplacements.isEmpty() || !outputReplacements.isEmpty())) {
                    Optional<JsonElement> encodeResult = RECIPE_CODEC.encodeStart(ops, recipe).result();
                    if (encodeResult.isPresent()) {
                        JsonElement json = encodeResult.get();

                        if (RecipeJsonMutator.mutateRecipe(json, inputReplacements, outputReplacements)) {
                            Optional<Recipe<?>> decodeResult = RECIPE_CODEC.parse(ops, json).result();
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

        Map<Identifier, JsonElement> customRecipes = RecipeConfigIO.loadCustomRecipes();
        for (Map.Entry<Identifier, JsonElement> entry : customRecipes.entrySet()) {
            Identifier id = entry.getKey();
            JsonElement json = entry.getValue();
            try {
                Optional<Recipe<?>> parsed = RECIPE_CODEC.parse(ops, json).result();
                if (parsed.isPresent()) {
                    ResourceKey<Recipe<?>> key = ResourceKey.create(Registries.RECIPE, id);
                    validRecipes.add(new RecipeHolder<>(key, parsed.get()));
                } else {
                    Constants.LOG.error("Failed to parse custom recipe: {}", id);
                }
            } catch (Exception e) {
                Constants.LOG.error("Error loading custom recipe: {}", id, e);
            }
        }

        if (!customRecipes.isEmpty()) {
            Constants.LOG.info("Loaded {} custom recipe(s) from reliable_recipes", customRecipes.size());
        }

        if (!DELETED_RECIPES_CACHE.isEmpty() || replacedCount > 0) {
            Constants.LOG.info("RecipeModifier removed {} and replaced contents in {} recipes.", DELETED_RECIPES_CACHE.size(), replacedCount);
        }

        managerAccessor.reliableRecipes$setRecipeMap(createRecipeMap(validRecipes));
        //? if <=26.2 {
        /*BrewingRecipeManager.reload(manager);
        *///?}
    }

    public static RecipeMap createRecipeMap(Iterable<RecipeHolder<?>> recipes) {
        //? if <=26.2 {
        /*return RecipeMap.create(recipes);
        *///?} else {
        return RecipeMap.create(new RecipeHolderLookup(recipes));
        //?}
    }

    //? if >26.2 {
    private static class RecipeHolderLookup implements HolderLookup<Recipe<?>> {
        private final List<Holder.Reference<Recipe<?>>> list = new ArrayList<>();
        private final Map<ResourceKey<Recipe<?>>, Holder.Reference<Recipe<?>>> map = new HashMap<>();

        public RecipeHolderLookup(Iterable<RecipeHolder<?>> recipes) {
            HolderOwner<Recipe<?>> owner = new HolderOwner<>() {};
            for (RecipeHolder<?> holder : recipes) {
                Holder.Reference<Recipe<?>> ref = new StandaloneReference<>(owner, holder.id(), (Recipe<?>) holder.value());
                list.add(ref);
                map.put(holder.id(), ref);
            }
        }

        @Override
        public Stream<Holder.Reference<Recipe<?>>> listElements() {
            return list.stream();
        }

        @Override
        public Stream<HolderSet.Named<Recipe<?>>> listTags() {
            return Stream.empty();
        }

        @Override
        public Optional<Holder.Reference<Recipe<?>>> get(ResourceKey<Recipe<?>> key) {
            return Optional.ofNullable(map.get(key));
        }

        @Override
        public Optional<HolderSet.Named<Recipe<?>>> get(TagKey<Recipe<?>> tag) {
            return Optional.empty();
        }
    }

    private static class StandaloneReference<T> extends Holder.Reference<T> {
        public StandaloneReference(HolderOwner<T> owner, ResourceKey<T> key, T value) {
            super(Type.STAND_ALONE, owner, key, value);
        }
    }
    //?}

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
            managerAccessor.reliableRecipes$setRecipeMap(createRecipeMap(updatedRecipes));
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

        managerAccessor.reliableRecipes$setRecipeMap(createRecipeMap(updatedRecipes));
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