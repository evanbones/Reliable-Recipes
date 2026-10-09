package com.evandev.reliable_recipes.recipe;

//? if <1.21 {
/*import com.google.gson.JsonObject;
import net.minecraft.advancements.critereon.MinMaxBounds;
import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.Identifier;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.level.Level;

public class TransmuteRecipe implements CraftingRecipe {
    public static final MinMaxBounds.Ints DEFAULT_MATERIAL_COUNT = MinMaxBounds.Ints.exactly(1);
    
    public static final RecipeSerializer<TransmuteRecipe> SERIALIZER = new RecipeSerializer<>() {
        @Override
        public TransmuteRecipe fromJson(Identifier id, JsonObject json) {
            String group = GsonHelper.getAsString(json, "group", "");
            
            CraftingBookCategory category = CraftingBookCategory.MISC;
            if (json.has("category")) {
                String catStr = GsonHelper.getAsString(json, "category").toUpperCase();
                for (CraftingBookCategory val : CraftingBookCategory.values()) {
                    if (val.name().equals(catStr)) {
                        category = val;
                        break;
                    }
                }
            }
            
            Ingredient input = Ingredient.fromJson(json.get("input"));
            Ingredient material = Ingredient.fromJson(json.get("material"));
            MinMaxBounds.Ints materialCount = json.has("material_count") ? MinMaxBounds.Ints.fromJson(json.get("material_count")) : DEFAULT_MATERIAL_COUNT;
            ItemStack result = ShapedRecipe.itemStackFromJson(GsonHelper.getAsJsonObject(json, "result"));
            boolean addMaterialCountToResult = GsonHelper.getAsBoolean(json, "add_material_count_to_result", false);
            return new TransmuteRecipe(id, group, category, input, material, materialCount, result, addMaterialCountToResult);
        }

        @Override
        public TransmuteRecipe fromNetwork(Identifier id, FriendlyByteBuf buf) {
            String group = buf.readUtf();
            CraftingBookCategory category = buf.readEnum(CraftingBookCategory.class);
            Ingredient input = Ingredient.fromNetwork(buf);
            Ingredient material = Ingredient.fromNetwork(buf);
            
            Integer min = buf.readBoolean() ? buf.readVarInt() : null;
            Integer max = buf.readBoolean() ? buf.readVarInt() : null;
            MinMaxBounds.Ints materialCount = createIntBounds(min, max);
            
            ItemStack result = buf.readItem();
            boolean addMaterialCountToResult = buf.readBoolean();
            return new TransmuteRecipe(id, group, category, input, material, materialCount, result, addMaterialCountToResult);
        }

        @Override
        public void toNetwork(FriendlyByteBuf buf, TransmuteRecipe recipe) {
            buf.writeUtf(recipe.group);
            buf.writeEnum(recipe.category);
            recipe.input.toNetwork(buf);
            recipe.material.toNetwork(buf);
            
            buf.writeBoolean(recipe.materialCount.getMin() != null);
            if (recipe.materialCount.getMin() != null) buf.writeVarInt(recipe.materialCount.getMin());
            buf.writeBoolean(recipe.materialCount.getMax() != null);
            if (recipe.materialCount.getMax() != null) buf.writeVarInt(recipe.materialCount.getMax());
            
            buf.writeItem(recipe.result);
            buf.writeBoolean(recipe.addMaterialCountToResult);
        }
    };

    public static MinMaxBounds.Ints createIntBounds(Integer min, Integer max) {
        if (min == null && max == null) {
            return MinMaxBounds.Ints.ANY;
        } else if (min == null) {
            return MinMaxBounds.Ints.atMost(max);
        } else if (max == null) {
            return MinMaxBounds.Ints.atLeast(min);
        } else {
            return MinMaxBounds.Ints.between(min, max);
        }
    }

    public static final RecipeType<TransmuteRecipe> TYPE = new RecipeType<>() {
        @Override
        public String toString() {
            return "minecraft:crafting_transmute";
        }
    };
    
    private static final int MIN_MATERIAL_COUNT = 1;
    private static final int MAX_MATERIAL_COUNT = 8;
    
    private final Identifier id;
    private final String group;
    private final CraftingBookCategory category;
    private final Ingredient input;
    private final Ingredient material;
    private final MinMaxBounds.Ints materialCount;
    private final ItemStack result;
    private final boolean addMaterialCountToResult;

    public TransmuteRecipe(
            final Identifier id,
            final String group,
            final CraftingBookCategory category,
            final Ingredient input,
            final Ingredient material,
            final MinMaxBounds.Ints materialCount,
            final ItemStack result,
            final boolean addMaterialCountToResult
    ) {
        this.id = id;
        this.group = group;
        this.category = category;
        this.input = input;
        this.material = material;
        this.materialCount = materialCount;
        this.result = result;
        this.addMaterialCountToResult = addMaterialCountToResult;
    }

    public static ItemStack createWithOriginalNbt(final ItemStack target, final ItemStack input) {
        return createWithOriginalNbt(target, input, 0);
    }

    public static ItemStack createWithOriginalNbt(final ItemStack target, final ItemStack input, final int extraCount) {
        ItemStack result = target.copy();
        result.setCount(target.getCount() + extraCount);
        if (input.hasTag()) {
            CompoundTag inputTag = input.getTag().copy();
            if (result.hasTag()) {
                CompoundTag merged = result.getTag().copy();
                merged.merge(inputTag);
                result.setTag(merged);
            } else {
                result.setTag(inputTag);
            }
        }
        return result;
    }

    private int computeResultSize(final int materialCount) {
        return this.addMaterialCountToResult ? materialCount + this.result.getCount() : this.result.getCount();
    }

    private ItemStack computeResult(final ItemStack inputIngredient, final int materialCount) {
        return createWithOriginalNbt(this.result, inputIngredient, materialCount);
    }

    private int getIngredientCount(CraftingContainer input) {
        int count = 0;
        for (int i = 0; i < input.getContainerSize(); i++) {
            if (!input.getItem(i).isEmpty()) {
                count++;
            }
        }
        return count;
    }

    @Override
    public boolean matches(final CraftingContainer input, final Level level) {
        int minMaterialCount = this.minMaterialCount();
        int maxMaterialCount = this.maxMaterialCount();
        int ingredientCount = this.getIngredientCount(input);
        if (ingredientCount >= minMaterialCount + 1 && ingredientCount <= maxMaterialCount + 1) {
            ItemStack foundInput = null;
            int materialCount = 0;

            for (int slot = 0; slot < input.getContainerSize(); slot++) {
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

    //? if <1.19.4 {
    /^@Override
    public ItemStack assemble(final CraftingContainer input) {
        return this.assemble(input, null);
    }

    public ItemStack assemble(final CraftingContainer input, RegistryAccess registries) {
    ^///?} else {
    @Override
    public ItemStack assemble(final CraftingContainer input, RegistryAccess registries) {
    //?}
        if (this.addMaterialCountToResult) {
            int materialCount = 0;
            ItemStack inputIngredient = ItemStack.EMPTY;

            for (int slot = 0; slot < input.getContainerSize(); slot++) {
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
            for (int slotx = 0; slotx < input.getContainerSize(); slotx++) {
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

    //? if <1.19.4 {
    /^@Override
    public ItemStack getResultItem() {
        return this.result;
    }
    ^///?} else {
    @Override
    public ItemStack getResultItem(RegistryAccess registries) {
        return this.result;
    }
    //?}

    @Override
    public NonNullList<Ingredient> getIngredients() {
        int maxMaterialCount = this.maxMaterialCount();
        NonNullList<Ingredient> ingredients = NonNullList.withSize(1 + maxMaterialCount, Ingredient.EMPTY);
        ingredients.set(0, this.input);
        for (int i = 1; i <= maxMaterialCount; i++) {
            ingredients.set(i, this.material);
        }
        return ingredients;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return SERIALIZER;
    }

    @Override
    public RecipeType<?> getType() {
        return RecipeType.CRAFTING;
    }

    //? if >=1.19.3 {
    @Override
    //?}
    public CraftingBookCategory category() {
        return this.category;
    }

    @Override
    public String getGroup() {
        return this.group;
    }

    @Override
    public Identifier getId() {
        return this.id;
    }

    private int minMaterialCount() {
        return this.materialCount.getMin() != null ? this.materialCount.getMin() : MIN_MATERIAL_COUNT;
    }

    private int maxMaterialCount() {
        return this.materialCount.getMax() != null ? this.materialCount.getMax() : MAX_MATERIAL_COUNT;
    }
}
*///?} else if <1.21.2 {
/*import com.mojang.serialization.Codec;
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
        return RecipeType.CRAFTING;
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
*///?} else {
public class TransmuteRecipe {}
//?}
