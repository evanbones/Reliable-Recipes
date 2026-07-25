package com.evandev.reliable_recipes.recipe;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class BrewingRecipe implements Recipe<SingleRecipeInput> {
    public static final Codec<List<ResourceLocation>> POTIONS_CODEC = Codec.either(
            ResourceLocation.CODEC.listOf(),
            ResourceLocation.CODEC
    ).xmap(
            either -> either.map(l -> l, List::of),
            list -> list.size() == 1 ? Either.right(list.getFirst()) : Either.left(list)
    );

    public static final Codec<Ingredient> FLEXIBLE_INGREDIENT_CODEC = Codec.either(
            Ingredient.CODEC,
            Codec.either(
                    ResourceLocation.CODEC,
                    ResourceLocation.CODEC.listOf()
            )
    ).xmap(
            either -> either.map(
                    ing -> ing,
                    stringOrList -> stringOrList.map(
                            BrewingRecipe::createIngredientFromLocation,
                            BrewingRecipe::createIngredientFromLocations
                    )
            ),
            Either::left
    );
    public static final MapCodec<BrewingRecipe> MAP_CODEC = RecordCodecBuilder.mapCodec(
            i -> i.group(
                    BrewingInputMatcher.CODEC.fieldOf("input").forGetter((BrewingRecipe o) -> o.input),
                    FLEXIBLE_INGREDIENT_CODEC.fieldOf("reagent").forGetter((BrewingRecipe o) -> o.reagent),
                    ItemStack.STRICT_CODEC.fieldOf("output").forGetter((BrewingRecipe o) -> o.output)
            ).apply(i, BrewingRecipe::new)
    );
    public static final StreamCodec<RegistryFriendlyByteBuf, BrewingRecipe> STREAM_CODEC = StreamCodec.of(
            (buf, recipe) -> {
                BrewingInputMatcher.STREAM_CODEC.encode(buf, recipe.input);
                Ingredient.CONTENTS_STREAM_CODEC.encode(buf, recipe.reagent);
                ItemStack.STREAM_CODEC.encode(buf, recipe.output);
            },
            buf -> {
                BrewingInputMatcher input = BrewingInputMatcher.STREAM_CODEC.decode(buf);
                Ingredient reagent = Ingredient.CONTENTS_STREAM_CODEC.decode(buf);
                ItemStack output = ItemStack.STREAM_CODEC.decode(buf);
                return new BrewingRecipe(input, reagent, output);
            }
    );
    public static final RecipeSerializer<BrewingRecipe> SERIALIZER = new RecipeSerializer<>() {
        @Override
        public @NotNull MapCodec<BrewingRecipe> codec() {
            return MAP_CODEC;
        }

        @Override
        public @NotNull StreamCodec<RegistryFriendlyByteBuf, BrewingRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    };
    public static final RecipeType<BrewingRecipe> TYPE = new RecipeType<>() {
        @Override
        public String toString() {
            return "minecraft:brewing";
        }
    };
    private final BrewingInputMatcher input;
    private final Ingredient reagent;
    private final ItemStack output;

    public BrewingRecipe(BrewingInputMatcher input, Ingredient reagent, ItemStack output) {
        this.input = input;
        this.reagent = reagent;
        this.output = output;
    }

    private static Ingredient createIngredientFromLocation(ResourceLocation loc) {
        String path = loc.toString();
        if (path.startsWith("#")) {
            return Ingredient.of(TagKey.create(Registries.ITEM, ResourceLocation.parse(path.substring(1))));
        }
        Item item = BuiltInRegistries.ITEM.get(loc);
        return item != Items.AIR ? Ingredient.of(item) : Ingredient.EMPTY;
    }

    private static Ingredient createIngredientFromLocations(List<ResourceLocation> list) {
        List<ItemStack> stacks = new ArrayList<>();
        for (ResourceLocation loc : list) {
            Item item = BuiltInRegistries.ITEM.get(loc);
            if (item != Items.AIR) {
                stacks.add(new ItemStack(item));
            }
        }
        return stacks.isEmpty() ? Ingredient.EMPTY : Ingredient.of(stacks.stream());
    }

    public BrewingInputMatcher getInputMatcher() {
        return input;
    }

    public Ingredient getReagent() {
        return reagent;
    }

    public ItemStack getOutput() {
        return output;
    }

    public boolean matches(ItemStack inputStack, ItemStack reagentStack) {
        return this.input.matches(inputStack) && this.reagent.test(reagentStack);
    }

    @Override
    public boolean matches(@NotNull SingleRecipeInput inputContainer, @NotNull Level level) {
        return this.input.matches(inputContainer.item());
    }

    @Override
    public @NotNull ItemStack assemble(@NotNull SingleRecipeInput inputContainer, HolderLookup.@NotNull Provider registries) {
        return this.output.copy();
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return true;
    }

    @Override
    public @NotNull ItemStack getResultItem(HolderLookup.@NotNull Provider registries) {
        return this.output;
    }

    @Override
    public @NotNull NonNullList<Ingredient> getIngredients() {
        NonNullList<Ingredient> ingredients = NonNullList.create();
        ingredients.add(this.input.ingredient());
        ingredients.add(this.reagent);
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

    public record BrewingInputMatcher(Ingredient ingredient, Optional<List<ResourceLocation>> potionContents) {
        public static final Codec<Optional<List<ResourceLocation>>> POTION_CONTENTS_OBJECT_CODEC = RecordCodecBuilder.create(instance ->
                instance.group(
                        POTIONS_CODEC.optionalFieldOf("potions").forGetter(opt -> opt),
                        POTIONS_CODEC.optionalFieldOf("potion").forGetter(opt -> opt)
                ).apply(instance, (potions1, potions2) -> potions1.isPresent() ? potions1 : potions2)
        );

        public static final Codec<BrewingInputMatcher> CODEC = RecordCodecBuilder.create(instance ->
                instance.group(
                        FLEXIBLE_INGREDIENT_CODEC.optionalFieldOf("ingredient", Ingredient.EMPTY).forGetter(BrewingInputMatcher::ingredient),
                        FLEXIBLE_INGREDIENT_CODEC.optionalFieldOf("item", Ingredient.EMPTY).forGetter(b -> Ingredient.EMPTY),
                        POTION_CONTENTS_OBJECT_CODEC.optionalFieldOf("potion_contents", Optional.empty()).forGetter(BrewingInputMatcher::potionContents)
                ).apply(instance, (ing1, ing2, potionContents) -> {
                    Ingredient ing = !ing1.isEmpty() ? ing1 : ing2;
                    return new BrewingInputMatcher(ing, potionContents);
                })
        );

        public static final StreamCodec<RegistryFriendlyByteBuf, BrewingInputMatcher> STREAM_CODEC = StreamCodec.of(
                (buf, matcher) -> {
                    Ingredient.CONTENTS_STREAM_CODEC.encode(buf, matcher.ingredient);
                    buf.writeBoolean(matcher.potionContents.isPresent());
                    if (matcher.potionContents.isPresent()) {
                        List<ResourceLocation> list = matcher.potionContents.get();
                        buf.writeVarInt(list.size());
                        for (ResourceLocation rl : list) {
                            buf.writeResourceLocation(rl);
                        }
                    }
                },
                buf -> {
                    Ingredient ingredient = Ingredient.CONTENTS_STREAM_CODEC.decode(buf);
                    boolean hasPotionContents = buf.readBoolean();
                    Optional<List<ResourceLocation>> potionContents = Optional.empty();
                    if (hasPotionContents) {
                        int size = buf.readVarInt();
                        List<ResourceLocation> list = new ArrayList<>(size);
                        for (int i = 0; i < size; i++) {
                            list.add(buf.readResourceLocation());
                        }
                        potionContents = Optional.of(list);
                    }
                    return new BrewingInputMatcher(ingredient, potionContents);
                }
        );

        public boolean matches(ItemStack stack) {
            if (stack.isEmpty()) return false;
            if (!ingredient.isEmpty() && !ingredient.test(stack)) {
                return false;
            }
            if (potionContents.isPresent() && !potionContents.get().isEmpty()) {
                PotionContents contents = stack.get(DataComponents.POTION_CONTENTS);
                if (contents == null) return false;
                Optional<Holder<Potion>> potionHolder = contents.potion();
                if (potionHolder.isEmpty()) return false;
                Optional<ResourceKey<Potion>> key = potionHolder.get().unwrapKey();
                if (key.isEmpty()) return false;
                ResourceLocation potionId = key.get().location();
                return potionContents.get().contains(potionId);
            }
            return true;
        }
    }
}
