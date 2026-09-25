package com.evandev.reliable_recipes.recipe;

//? if <=26.2 {
/*import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class BrewingRecipe implements Recipe<SingleRecipeInput> {
    public static final Codec<List<Identifier>> POTIONS_CODEC = Codec.either(
            Identifier.CODEC.listOf(),
            Identifier.CODEC
    ).xmap(
            either -> either.map(l -> l, List::of),
            list -> list.size() == 1 ? Either.right(list.getFirst()) : Either.left(list)
    );

    private static Optional<Ingredient> createIngredientFromLocation(Identifier loc) {
        String path = loc.toString();
        if (path.startsWith("#")) {
            Identifier tagId = Identifier.parse(path.substring(1));
            return BuiltInRegistries.ITEM.get(TagKey.create(Registries.ITEM, tagId)).map(Ingredient::of);
        }
        return BuiltInRegistries.ITEM.get(loc)
                .map(Holder.Reference::value)
                .filter(item -> item != Items.AIR)
                .map(Ingredient::of);
    }

    private static Optional<Ingredient> createIngredientFromLocations(List<Identifier> list) {
        List<Item> items = new ArrayList<>();
        for (Identifier loc : list) {
            BuiltInRegistries.ITEM.get(loc)
                    .map(Holder.Reference::value)
                    .filter(item -> item != Items.AIR)
                    .ifPresent(items::add);
        }
        return items.isEmpty() ? Optional.empty() : Optional.of(Ingredient.of(items.toArray(ItemLike[]::new)));
    }

    public static final Codec<Ingredient> BASE_INGREDIENT_CODEC = Codec.either(
            Ingredient.CODEC,
            Codec.either(
                    Identifier.CODEC,
                    Identifier.CODEC.listOf()
            )
    ).flatXmap(
            either -> either.map(
                    DataResult::success,
                    stringOrList -> stringOrList.map(
                            loc -> createIngredientFromLocation(loc)
                                    .map(DataResult::success)
                                    .orElseGet(() -> DataResult.error(() -> "Unknown item or tag: " + loc)),
                            list -> createIngredientFromLocations(list)
                                    .map(DataResult::success)
                                    .orElseGet(() -> DataResult.error(() -> "Unknown items in list"))
                    )
            ),
            ing -> DataResult.success(Either.left(ing))
    );

    public static final Codec<Ingredient> ITEM_WRAPPED_INGREDIENT_CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    BASE_INGREDIENT_CODEC.optionalFieldOf("ingredient").forGetter(Optional::of),
                    BASE_INGREDIENT_CODEC.optionalFieldOf("item").forGetter(Optional::of)
            ).apply(instance, (ing1, ing2) -> ing1.or(() -> ing2).orElse(null))
    );

    public static final Codec<Ingredient> FLEXIBLE_INGREDIENT_CODEC = Codec.either(
            BASE_INGREDIENT_CODEC,
            ITEM_WRAPPED_INGREDIENT_CODEC
    ).xmap(
            either -> either.map(ing -> ing, ing -> ing),
            Either::left
    );

    public static final MapCodec<BrewingRecipe> MAP_CODEC = RecordCodecBuilder.mapCodec(
            i -> i.group(
                    BrewingInputMatcher.CODEC.fieldOf("input").forGetter(o -> o.input),
                    FLEXIBLE_INGREDIENT_CODEC.fieldOf("reagent").forGetter(o -> o.reagent),
                    ItemStack.CODEC.fieldOf("output").forGetter(o -> o.output)
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

    public static final RecipeSerializer<BrewingRecipe> SERIALIZER = new RecipeSerializer<>(MAP_CODEC, STREAM_CODEC);

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
    public @NotNull ItemStack assemble(@NotNull SingleRecipeInput inputContainer) {
        return this.output.copy();
    }

    @Override
    public boolean showNotification() {
        return false;
    }

    @Override
    public @NotNull String group() {
        return "";
    }

    @Override
    public @NotNull PlacementInfo placementInfo() {
        return PlacementInfo.createFromOptionals(List.of(input.ingredient(), Optional.of(reagent)));
    }

    @Override
    public @NotNull RecipeBookCategory recipeBookCategory() {
        return RecipeBookCategories.CRAFTING_MISC;
    }

    @Override
    public @NotNull RecipeSerializer<BrewingRecipe> getSerializer() {
        return SERIALIZER;
    }

    @Override
    public @NotNull RecipeType<BrewingRecipe> getType() {
        return TYPE;
    }

    public record BrewingInputMatcher(Optional<Ingredient> ingredient, Optional<List<Identifier>> potionContents) {
        public static final Codec<Optional<List<Identifier>>> POTION_CONTENTS_OBJECT_CODEC = RecordCodecBuilder.create(instance ->
                instance.group(
                        POTIONS_CODEC.optionalFieldOf("potions").forGetter(opt -> opt),
                        POTIONS_CODEC.optionalFieldOf("potion").forGetter(opt -> opt)
                ).apply(instance, (potions1, potions2) -> potions1.isPresent() ? potions1 : potions2)
        );

        public static final Codec<Optional<List<Identifier>>> FLEXIBLE_POTION_CONTENTS_CODEC = Codec.either(
                POTION_CONTENTS_OBJECT_CODEC,
                POTIONS_CODEC
        ).xmap(
                either -> either.map(opt -> opt, Optional::of),
                opt -> opt.map(Either::<Optional<List<Identifier>>, List<Identifier>>right).orElseGet(() -> Either.left(Optional.empty()))
        );

        public static final Codec<BrewingInputMatcher> OBJECT_CODEC = RecordCodecBuilder.create(instance ->
                instance.group(
                        FLEXIBLE_INGREDIENT_CODEC.optionalFieldOf("ingredient").forGetter(BrewingInputMatcher::ingredient),
                        FLEXIBLE_INGREDIENT_CODEC.optionalFieldOf("item").forGetter(b -> Optional.empty()),
                        FLEXIBLE_POTION_CONTENTS_CODEC.optionalFieldOf("potion_contents", Optional.empty()).forGetter(BrewingInputMatcher::potionContents),
                        POTIONS_CODEC.optionalFieldOf("potion").forGetter(b -> Optional.empty()),
                        POTIONS_CODEC.optionalFieldOf("potions").forGetter(b -> Optional.empty())
                ).apply(instance, (ing1, ing2, potionContents, potion1, potion2) -> {
                    Optional<Ingredient> ing = ing1.or(() -> ing2);
                    Optional<List<Identifier>> pot = potionContents.or(() -> potion1).or(() -> potion2);
                    return new BrewingInputMatcher(ing, pot);
                })
        );

        public static final Codec<BrewingInputMatcher> CODEC = Codec.either(
                OBJECT_CODEC,
                FLEXIBLE_INGREDIENT_CODEC
        ).flatXmap(
                either -> either.map(
                        matcher -> {
                            if (matcher.ingredient().isEmpty() && matcher.potionContents().isEmpty()) {
                                return DataResult.error(() -> "Neither ingredient/item nor potion_contents found in input object");
                            }
                            return DataResult.success(matcher);
                        },
                        ing -> DataResult.success(new BrewingInputMatcher(Optional.of(ing), Optional.empty()))
                ),
                matcher -> {
                    if (matcher.potionContents().isPresent()) {
                        return DataResult.success(Either.left(matcher));
                    } else if (matcher.ingredient().isPresent()) {
                        return DataResult.success(Either.right(matcher.ingredient().get()));
                    } else {
                        return DataResult.error(() -> "Empty BrewingInputMatcher");
                    }
                }
        );

        public static final StreamCodec<RegistryFriendlyByteBuf, BrewingInputMatcher> STREAM_CODEC = StreamCodec.of(
                (buf, matcher) -> {
                    buf.writeBoolean(matcher.ingredient.isPresent());
                    matcher.ingredient.ifPresent(ing -> Ingredient.CONTENTS_STREAM_CODEC.encode(buf, ing));
                    buf.writeBoolean(matcher.potionContents.isPresent());
                    if (matcher.potionContents.isPresent()) {
                        List<Identifier> list = matcher.potionContents.get();
                        buf.writeVarInt(list.size());
                        for (Identifier id : list) {
                            buf.writeIdentifier(id);
                        }
                    }
                },
                buf -> {
                    boolean hasIng = buf.readBoolean();
                    Optional<Ingredient> ingredient = hasIng ? Optional.of(Ingredient.CONTENTS_STREAM_CODEC.decode(buf)) : Optional.empty();
                    boolean hasPotionContents = buf.readBoolean();
                    Optional<List<Identifier>> potionContents = Optional.empty();
                    if (hasPotionContents) {
                        int size = buf.readVarInt();
                        List<Identifier> list = new ArrayList<>(size);
                        for (int i = 0; i < size; i++) {
                            list.add(buf.readIdentifier());
                        }
                        potionContents = Optional.of(list);
                    }
                    return new BrewingInputMatcher(ingredient, potionContents);
                }
        );

        public boolean matches(ItemStack stack) {
            if (stack.isEmpty()) return false;
            if (ingredient.isPresent() && !ingredient.get().test(stack)) {
                return false;
            }
            if (potionContents.isPresent() && !potionContents.get().isEmpty()) {
                PotionContents contents = stack.get(DataComponents.POTION_CONTENTS);
                if (contents == null) return false;
                Optional<Holder<Potion>> potionHolder = contents.potion();
                if (potionHolder.isEmpty()) return false;
                Optional<ResourceKey<Potion>> key = potionHolder.get().unwrapKey();
                if (key.isEmpty()) return false;
                Identifier potionId = key.get().identifier();
                return potionContents.get().contains(potionId);
            }
            return true;
        }
    }
}
*///?} else {
public class BrewingRecipe {}
//?}
