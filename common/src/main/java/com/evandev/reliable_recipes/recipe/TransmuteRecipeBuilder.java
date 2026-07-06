package com.evandev.reliable_recipes.recipe;

import com.google.gson.JsonObject;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.CriterionTriggerInstance;
import net.minecraft.advancements.RequirementsStrategy;
import net.minecraft.advancements.critereon.MinMaxBounds;
import net.minecraft.advancements.critereon.RecipeUnlockedTrigger;
import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraft.data.recipes.RecipeBuilder;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.core.registries.BuiltInRegistries;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;

public class TransmuteRecipeBuilder implements RecipeBuilder {
    private final RecipeCategory category;
    private final ItemStack result;
    private final Ingredient input;
    private final Ingredient material;
    private final Map<String, CriterionTriggerInstance> criteria = new LinkedHashMap<>();
    private String group;
    private MinMaxBounds.Ints materialCount = TransmuteRecipe.DEFAULT_MATERIAL_COUNT;
    private boolean addMaterialCountToOutput;

    private TransmuteRecipeBuilder(final RecipeCategory category, final ItemStack result, final Ingredient input, final Ingredient material) {
        this.category = category;
        this.result = result;
        this.input = input;
        this.material = material;
    }

    public static TransmuteRecipeBuilder transmute(final RecipeCategory category, final Ingredient input, final Ingredient material, final Item result) {
        return transmute(category, input, material, new ItemStack(result));
    }

    public static TransmuteRecipeBuilder transmute(
            final RecipeCategory category, final Ingredient input, final Ingredient material, final ItemStack result
    ) {
        return new TransmuteRecipeBuilder(category, result, input, material);
    }

    private static CraftingBookCategory determineBookCategory(RecipeCategory category) {
        return switch (category) {
            case BUILDING_BLOCKS, DECORATIONS, TRANSPORTATION -> CraftingBookCategory.BUILDING;
            case REDSTONE -> CraftingBookCategory.REDSTONE;
            case TOOLS, COMBAT -> CraftingBookCategory.EQUIPMENT;
            default -> CraftingBookCategory.MISC;
        };
    }

    public TransmuteRecipeBuilder unlockedBy(final String name, final CriterionTriggerInstance criterion) {
        this.criteria.put(name, criterion);
        return this;
    }

    public TransmuteRecipeBuilder group(final String group) {
        this.group = group;
        return this;
    }

    public TransmuteRecipeBuilder addMaterialCountToOutput() {
        this.addMaterialCountToOutput = true;
        return this;
    }

    public TransmuteRecipeBuilder setMaterialCount(final MinMaxBounds.Ints materialCount) {
        this.materialCount = materialCount;
        return this;
    }

    @Override
    public Item getResult() {
        return this.result.getItem();
    }

    public ResourceLocation defaultId() {
        return RecipeBuilder.getDefaultRecipeId(this.getResult());
    }

    @Override
    public void save(final Consumer<FinishedRecipe> consumer, final ResourceLocation id) {
        if (this.criteria.isEmpty()) {
            throw new IllegalStateException("No way of obtaining recipe " + id);
        }
        CraftingBookCategory bookCategory = determineBookCategory(this.category);
        
        consumer.accept(new FinishedRecipe() {
            @Override
            public void serializeRecipeData(JsonObject json) {
                if (group != null && !group.isEmpty()) {
                    json.addProperty("group", group);
                }
                json.addProperty("category", bookCategory.getSerializedName());
                json.add("input", input.toJson());
                json.add("material", material.toJson());
                json.add("material_count", materialCount.serializeToJson());
                
                JsonObject resultJson = new JsonObject();
                resultJson.addProperty("item", BuiltInRegistries.ITEM.getKey(result.getItem()).toString());
                if (result.getCount() > 1) {
                    resultJson.addProperty("count", result.getCount());
                }
                if (result.hasTag()) {
                    resultJson.addProperty("nbt", result.getTag().toString());
                }
                json.add("result", resultJson);
                
                json.addProperty("add_material_count_to_result", addMaterialCountToOutput);
            }

            @Override
            public ResourceLocation getId() {
                return id;
            }

            @Override
            public RecipeSerializer<?> getType() {
                return TransmuteRecipe.SERIALIZER;
            }

            @Override
            public JsonObject serializeAdvancement() {
                Advancement.Builder advancementBuilder = Advancement.Builder.advancement()
                        .addCriterion("has_the_recipe", RecipeUnlockedTrigger.unlocked(id))
                        .rewards(net.minecraft.advancements.AdvancementRewards.Builder.recipe(id))
                        .requirements(RequirementsStrategy.OR);
                criteria.forEach(advancementBuilder::addCriterion);
                return advancementBuilder.serializeToJson();
            }

            @Override
            public ResourceLocation getAdvancementId() {
                return id.withPrefix("recipes/" + category.getFolderName() + "/");
            }
        });
    }
}
