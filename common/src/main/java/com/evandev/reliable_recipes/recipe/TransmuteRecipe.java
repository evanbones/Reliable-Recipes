package com.evandev.reliable_recipes.recipe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.advancements.critereon.MinMaxBounds.Ints;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

public class TransmuteRecipe implements CraftingRecipe {
    public static final Ints DEFAULT_MATERIAL_COUNT = Ints.exactly(1);
    public static final Codec<Ints> MATERIAL_COUNT_BOUNDS = Ints.CODEC;
    public static final MapCodec<TransmuteRecipe> MAP_CODEC = RecordCodecBuilder.mapCodec(
            i -> i.group(
                            Codec.STRING.optionalFieldOf("group", "").forGetter((TransmuteRecipe o) -> o.group),
                            CraftingBookCategory.CODEC.optionalFieldOf("category", CraftingBookCategory.MISC).forGetter((TransmuteRecipe o) -> o.category),
                            Ingredient.CODEC.fieldOf("input").forGetter((TransmuteRecipe o) -> o.input),
                            Ingredient.CODEC.fieldOf("material").forGetter((TransmuteRecipe o) -> o.material),
                            MATERIAL_COUNT_BOUNDS.optionalFieldOf("material_count", DEFAULT_MATERIAL_COUNT).forGetter((TransmuteRecipe o) -> o.materialCount),
                            ItemStack.STRICT_CODEC.fieldOf("result").forGetter((TransmuteRecipe o) -> o.result),
                            Codec.BOOL.optionalFieldOf("add_material_count_to_result", false).forGetter((TransmuteRecipe o) -> o.addMaterialCountToResult)
                    )
                    .apply(i, TransmuteRecipe::new)
    );
    public static final RecipeSerializer<TransmuteRecipe> SERIALIZER = new RecipeSerializer<>() {
        @Override
        public @NotNull MapCodec<TransmuteRecipe> codec() {
            return MAP_CODEC;
        }

        @Override
        public @NotNull StreamCodec<RegistryFriendlyByteBuf, TransmuteRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    };
    public static final StreamCodec<RegistryFriendlyByteBuf, TransmuteRecipe> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public @NotNull TransmuteRecipe decode(@NotNull RegistryFriendlyByteBuf buf) {
            String group = ByteBufCodecs.STRING_UTF8.decode(buf);
            CraftingBookCategory category = CraftingBookCategory.STREAM_CODEC.decode(buf);
            Ingredient input = Ingredient.CONTENTS_STREAM_CODEC.decode(buf);
            Ingredient material = Ingredient.CONTENTS_STREAM_CODEC.decode(buf);
            Ints materialCount = ByteBufCodecs.fromCodec(Ints.CODEC).decode(buf);
            ItemStack result = ItemStack.STREAM_CODEC.decode(buf);
            boolean addMaterialCountToResult = ByteBufCodecs.BOOL.decode(buf);
            return new TransmuteRecipe(group, category, input, material, materialCount, result, addMaterialCountToResult);
        }

        @Override
        public void encode(@NotNull RegistryFriendlyByteBuf buf, @NotNull TransmuteRecipe recipe) {
            ByteBufCodecs.STRING_UTF8.encode(buf, recipe.group);
            CraftingBookCategory.STREAM_CODEC.encode(buf, recipe.category);
            Ingredient.CONTENTS_STREAM_CODEC.encode(buf, recipe.input);
            Ingredient.CONTENTS_STREAM_CODEC.encode(buf, recipe.material);
            ByteBufCodecs.fromCodec(Ints.CODEC).encode(buf, recipe.materialCount);
            ItemStack.STREAM_CODEC.encode(buf, recipe.result);
            ByteBufCodecs.BOOL.encode(buf, recipe.addMaterialCountToResult);
        }
    };
    public static final RecipeType<TransmuteRecipe> TYPE = new RecipeType<>() {
        @Override
        public String toString() {
            return "minecraft:crafting_transmute";
        }
    };
    private static final int MIN_MATERIAL_COUNT = 1;
    private static final int MAX_MATERIAL_COUNT = 8;
    private final String group;
    private final CraftingBookCategory category;
    private final Ingredient input;
    private final Ingredient material;
    private final Ints materialCount;
    private final ItemStack result;
    private final boolean addMaterialCountToResult;

    public TransmuteRecipe(
            final String group,
            final CraftingBookCategory category,
            final Ingredient input,
            final Ingredient material,
            final Ints materialCount,
            final ItemStack result,
            final boolean addMaterialCountToResult
    ) {
        this.group = group;
        this.category = category;
        this.input = input;
        this.material = material;
        this.materialCount = materialCount;
        this.result = result;
        this.addMaterialCountToResult = addMaterialCountToResult;
    }

    public static ItemStack createWithOriginalComponents(final ItemStack target, final ItemStack input) {
        return createWithOriginalComponents(target, input, 0);
    }

    public static ItemStack createWithOriginalComponents(final ItemStack target, final ItemStack input, final int extraCount) {
        ItemStack result = target.copy();
        result.setCount(target.getCount() + extraCount);
        result.applyComponents(input.getComponentsPatch());
        return result;
    }

    private int computeResultSize(final int materialCount) {
        return this.addMaterialCountToResult ? materialCount + this.result.getCount() : this.result.getCount();
    }

    private ItemStack computeResult(final ItemStack inputIngredient, final int materialCount) {
        return createWithOriginalComponents(this.result, inputIngredient, materialCount);
    }

    private int getIngredientCount(CraftingInput input) {
        int count = 0;
        for (int i = 0; i < input.size(); i++) {
            if (!input.getItem(i).isEmpty()) {
                count++;
            }
        }
        return count;
    }

    @Override
    public boolean matches(final @NotNull CraftingInput input, final @NotNull Level level) {
        int minMaterialCount = this.minMaterialCount();
        int maxMaterialCount = this.maxMaterialCount();
        int ingredientCount = this.getIngredientCount(input);
        if (ingredientCount >= minMaterialCount + 1 && ingredientCount <= maxMaterialCount + 1) {
            ItemStack foundInput = null;
            int materialCount = 0;

            for (int slot = 0; slot < input.size(); slot++) {
                ItemStack stack = input.getItem(slot);
                if (!stack.isEmpty()) {
                    if (this.input.test(stack)) {
                        if (foundInput != null) {
                            return false;
                        }

                        foundInput = stack;
                    } else {
                        if (!this.material.test(stack)) {
                            return false;
                        }

                        if (++materialCount > maxMaterialCount) {
                            return false;
                        }
                    }
                }
            }

            if (foundInput != null && !foundInput.isEmpty() && this.materialCount.matches(materialCount)) {
                int resultCount = this.computeResultSize(materialCount);
                if (resultCount != 1) {
                    return true;
                } else {
                    ItemStack result = this.computeResult(foundInput, 0);
                    return !result.isEmpty() && !ItemStack.isSameItemSameComponents(foundInput, result);
                }
            } else {
                return false;
            }
        } else {
            return false;
        }
    }

    @Override
    public @NotNull ItemStack assemble(final @NotNull CraftingInput input, HolderLookup.@NotNull Provider registries) {
        if (this.addMaterialCountToResult) {
            int materialCount = 0;
            ItemStack inputIngredient = ItemStack.EMPTY;

            for (int slot = 0; slot < input.size(); slot++) {
                ItemStack itemStack = input.getItem(slot);
                if (!itemStack.isEmpty()) {
                    if (this.input.test(itemStack)) {
                        inputIngredient = itemStack;
                    } else if (this.material.test(itemStack)) {
                        materialCount++;
                    }
                }
            }

            return this.computeResult(inputIngredient, materialCount);
        } else {
            for (int slotx = 0; slotx < input.size(); slotx++) {
                ItemStack itemStack = input.getItem(slotx);
                if (!itemStack.isEmpty() && this.input.test(itemStack)) {
                    return this.computeResult(itemStack, 0);
                }
            }

            return ItemStack.EMPTY;
        }
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= this.minMaterialCount() + 1;
    }

    @Override
    public @NotNull ItemStack getResultItem(HolderLookup.@NotNull Provider registries) {
        return this.result;
    }

    @Override
    public @NotNull NonNullList<Ingredient> getIngredients() {
        int maxMaterialCount = this.maxMaterialCount();
        NonNullList<Ingredient> ingredients = NonNullList.withSize(1 + maxMaterialCount, Ingredient.EMPTY);
        ingredients.set(0, this.input);
        for (int i = 1; i <= maxMaterialCount; i++) {
            ingredients.set(i, this.material);
        }
        return ingredients;
    }

    @Override
    public @NotNull RecipeSerializer<?> getSerializer() {
        return SERIALIZER;
    }

    @Override
    public @NotNull RecipeType<?> getType() {
        return TYPE;
    }

    @Override
    public @NotNull CraftingBookCategory category() {
        return this.category;
    }

    @Override
    public @NotNull String getGroup() {
        return this.group;
    }

    private int minMaterialCount() {
        return this.materialCount.min().orElse(MIN_MATERIAL_COUNT);
    }

    private int maxMaterialCount() {
        return this.materialCount.max().orElse(MAX_MATERIAL_COUNT);
    }
}
