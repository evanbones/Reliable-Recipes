package com.evandev.reliable_recipes.recipe;

//? if <1.21 {
/*import com.evandev.reliable_recipes.Constants;
import com.evandev.reliable_recipes.config.RecipeRuleParser;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.TagParser;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.Identifier;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.Container;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class BrewingRecipe implements Recipe<Container> {

    public static final RecipeType<BrewingRecipe> TYPE = new RecipeType<>() {
        @Override
        public String toString() {
            return "minecraft:brewing";
        }
    };

    public static final RecipeSerializer<BrewingRecipe> SERIALIZER = new RecipeSerializer<>() {
        @Override
        public BrewingRecipe fromJson(Identifier id, JsonObject json) {
            BrewingInputMatcher input = parseInputMatcher(json.get("input"));
            Ingredient reagent = RecipeRuleParser.parseIngredient(json.get("reagent"));
            ItemStack output = parseOutputStack(json.get("output"));
            return new BrewingRecipe(id, input, reagent, output);
        }

        @Override
        public BrewingRecipe fromNetwork(Identifier id, FriendlyByteBuf buf) {
            BrewingInputMatcher input = BrewingInputMatcher.fromNetwork(buf);
            Ingredient reagent = Ingredient.fromNetwork(buf);
            ItemStack output = buf.readItem();
            return new BrewingRecipe(id, input, reagent, output);
        }

        @Override
        public void toNetwork(FriendlyByteBuf buf, BrewingRecipe recipe) {
            recipe.input.toNetwork(buf);
            recipe.reagent.toNetwork(buf);
            buf.writeItem(recipe.output);
        }
    };

    private final Identifier id;
    private final BrewingInputMatcher input;
    private final Ingredient reagent;
    private final ItemStack output;

    public BrewingRecipe(Identifier id, BrewingInputMatcher input, Ingredient reagent, ItemStack output) {
        this.id = id != null ? id : Identifier.fromNamespaceAndPath(Constants.MOD_ID, "brewing_" + Math.abs(input.hashCode() ^ reagent.hashCode() ^ output.hashCode()));
        this.input = input;
        this.reagent = reagent;
        this.output = output;
    }

    public BrewingRecipe(BrewingInputMatcher input, Ingredient reagent, ItemStack output) {
        this(null, input, reagent, output);
    }

    private static BrewingInputMatcher parseInputMatcher(JsonElement json) {
        if (json == null || json.isJsonNull()) {
            return new BrewingInputMatcher(Ingredient.EMPTY, Optional.empty());
        }

        if (json.isJsonPrimitive()) {
            Ingredient ing = RecipeRuleParser.parseIngredientString(json.getAsString());
            return new BrewingInputMatcher(ing, Optional.empty());
        }

        if (json.isJsonObject()) {
            JsonObject obj = json.getAsJsonObject();
            Ingredient ing = Ingredient.EMPTY;

            if (obj.has("ingredient")) {
                ing = RecipeRuleParser.parseIngredient(obj.get("ingredient"));
            } else if (obj.has("item")) {
                ing = RecipeRuleParser.parseIngredient(obj.get("item"));
            } else if (obj.has("tag")) {
                ing = RecipeRuleParser.parseIngredientString("#" + obj.get("tag").getAsString());
            }

            Optional<List<Identifier>> potionContents = parsePotionLocations(obj);
            return new BrewingInputMatcher(ing, potionContents);
        }

        return new BrewingInputMatcher(RecipeRuleParser.parseIngredient(json), Optional.empty());
    }

    private static Optional<List<Identifier>> parsePotionLocations(JsonObject obj) {
        List<Identifier> locations = new ArrayList<>();

        if (obj.has("potion_contents")) {
            JsonElement pc = obj.get("potion_contents");
            if (pc.isJsonPrimitive()) {
                Identifier loc = Identifier.tryParse(pc.getAsString());
                if (loc != null) locations.add(loc);
            } else if (pc.isJsonObject()) {
                JsonObject pcObj = pc.getAsJsonObject();
                if (pcObj.has("potions") && pcObj.get("potions").isJsonArray()) {
                    for (JsonElement el : pcObj.getAsJsonArray("potions")) {
                        if (el.isJsonPrimitive()) {
                            Identifier loc = Identifier.tryParse(el.getAsString());
                            if (loc != null) locations.add(loc);
                        }
                    }
                } else if (pcObj.has("potion") && pcObj.get("potion").isJsonPrimitive()) {
                    Identifier loc = Identifier.tryParse(pcObj.get("potion").getAsString());
                    if (loc != null) locations.add(loc);
                } else if (pcObj.has("potions") && pcObj.get("potions").isJsonPrimitive()) {
                    Identifier loc = Identifier.tryParse(pcObj.get("potions").getAsString());
                    if (loc != null) locations.add(loc);
                }
            }
        } else if (obj.has("potion") && obj.get("potion").isJsonPrimitive()) {
            Identifier loc = Identifier.tryParse(obj.get("potion").getAsString());
            if (loc != null) locations.add(loc);
        } else if (obj.has("potions") && obj.get("potions").isJsonArray()) {
            for (JsonElement el : obj.getAsJsonArray("potions")) {
                if (el.isJsonPrimitive()) {
                    Identifier loc = Identifier.tryParse(el.getAsString());
                    if (loc != null) locations.add(loc);
                }
            }
        }

        return locations.isEmpty() ? Optional.empty() : Optional.of(locations);
    }

    private static ItemStack parseOutputStack(JsonElement json) {
        if (json == null || json.isJsonNull()) {
            return ItemStack.EMPTY;
        }

        if (json.isJsonPrimitive()) {
            Item item = BuiltInRegistries.ITEM.get(Identifier.tryParse(json.getAsString()));
            return item != Items.AIR ? new ItemStack(item) : ItemStack.EMPTY;
        }

        if (json.isJsonObject()) {
            JsonObject obj = json.getAsJsonObject();
            String itemId = null;
            if (obj.has("id") && obj.get("id").isJsonPrimitive()) {
                itemId = obj.get("id").getAsString();
            } else if (obj.has("item") && obj.get("item").isJsonPrimitive()) {
                itemId = obj.get("item").getAsString();
            }

            Item item = itemId != null ? BuiltInRegistries.ITEM.get(Identifier.tryParse(itemId)) : Items.AIR;
            int count = GsonHelper.getAsInt(obj, "count", 1);
            ItemStack stack = item != Items.AIR ? new ItemStack(item, count) : ItemStack.EMPTY;

            String potionId = null;
            if (obj.has("components") && obj.get("components").isJsonObject()) {
                JsonObject comp = obj.getAsJsonObject("components");
                if (comp.has("minecraft:potion_contents") && comp.get("minecraft:potion_contents").isJsonObject()) {
                    JsonObject pc = comp.getAsJsonObject("minecraft:potion_contents");
                    if (pc.has("potion") && pc.get("potion").isJsonPrimitive()) {
                        potionId = pc.get("potion").getAsString();
                    }
                }
            }
            if (potionId == null && obj.has("potion") && obj.get("potion").isJsonPrimitive()) {
                potionId = obj.get("potion").getAsString();
            }

            if (potionId != null && !stack.isEmpty()) {
                Identifier pLoc = Identifier.tryParse(potionId);
                if (pLoc != null && BuiltInRegistries.POTION.containsKey(pLoc)) {
                    Potion potion = BuiltInRegistries.POTION.get(pLoc);
                    PotionUtils.setPotion(stack, potion);
                }
            }

            if (obj.has("nbt") && !stack.isEmpty()) {
                try {
                    if (obj.get("nbt").isJsonObject()) {
                        stack.setTag(TagParser.parseTag(obj.get("nbt").toString()));
                    } else if (obj.get("nbt").isJsonPrimitive()) {
                        stack.setTag(TagParser.parseTag(obj.get("nbt").getAsString()));
                    }
                } catch (CommandSyntaxException ignored) {
                }
            }

            return stack;
        }

        return ItemStack.EMPTY;
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
    public boolean matches(Container container, Level level) {
        return this.input.matches(container.getItem(0));
    }

    //? if <1.19.4 {
    /^@Override
    public ItemStack assemble(Container container) {
        return this.output.copy();
    }
    ^///?} else {
    @Override
    public ItemStack assemble(Container container, RegistryAccess registries) {
        return this.output.copy();
    }
    //?}

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return true;
    }

    //? if <1.19.4 {
    /^@Override
    public ItemStack getResultItem() {
        return this.output;
    }
    ^///?} else {
    @Override
    public ItemStack getResultItem(RegistryAccess registries) {
        return this.output;
    }
    //?}

    @Override
    public NonNullList<Ingredient> getIngredients() {
        NonNullList<Ingredient> ingredients = NonNullList.create();
        ingredients.add(this.input.ingredient());
        ingredients.add(this.reagent);
        return ingredients;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return SERIALIZER;
    }

    @Override
    public RecipeType<?> getType() {
        return TYPE;
    }

    @Override
    public Identifier getId() {
        return this.id;
    }

    public record BrewingInputMatcher(Ingredient ingredient, Optional<List<Identifier>> potionContents) {
        public static BrewingInputMatcher fromNetwork(FriendlyByteBuf buf) {
            Ingredient ing = Ingredient.fromNetwork(buf);
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
            return new BrewingInputMatcher(ing, potionContents);
        }

        public boolean matches(ItemStack stack) {
            if (stack == null || stack.isEmpty()) return false;
            if (!ingredient.isEmpty() && !ingredient.test(stack)) {
                return false;
            }
            if (potionContents.isPresent() && !potionContents.get().isEmpty()) {
                Potion potion = PotionUtils.getPotion(stack);
                Identifier potionId = BuiltInRegistries.POTION.getKey(potion);
                return potionContents.get().contains(potionId);
            }
            return true;
        }

        public void toNetwork(FriendlyByteBuf buf) {
            ingredient.toNetwork(buf);
            buf.writeBoolean(potionContents.isPresent());
            if (potionContents.isPresent()) {
                List<Identifier> list = potionContents.get();
                buf.writeVarInt(list.size());
                for (Identifier rl : list) {
                    buf.writeIdentifier(rl);
                }
            }
        }
    }
}
*///?} else if <1.21.2 {
/*import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
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
import net.minecraft.resources.Identifier;
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
    public static final Codec<List<Identifier>> POTIONS_CODEC = Codec.either(
            Identifier.CODEC.listOf(),
            Identifier.CODEC
    ).xmap(
            either -> either.map(l -> l, List::of),
            list -> list.size() == 1 ? Either.right(list.getFirst()) : Either.left(list)
    );

    public static final Codec<Ingredient> BASE_INGREDIENT_CODEC = Codec.either(
            Ingredient.CODEC,
            Codec.either(
                    Identifier.CODEC,
                    Identifier.CODEC.listOf()
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

    public static final Codec<Ingredient> ITEM_WRAPPED_INGREDIENT_CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    BASE_INGREDIENT_CODEC.optionalFieldOf("ingredient", Ingredient.EMPTY).forGetter(ing -> ing),
                    BASE_INGREDIENT_CODEC.optionalFieldOf("item", Ingredient.EMPTY).forGetter(ing -> ing)
            ).apply(instance, (ing1, ing2) -> !ing1.isEmpty() ? ing1 : ing2)
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
                    BrewingInputMatcher.CODEC.fieldOf("input").forGetter((BrewingRecipe o) -> o.input),
                    FLEXIBLE_INGREDIENT_CODEC.fieldOf("reagent").forGetter((BrewingRecipe o) -> o.reagent),
                    ItemStack.STRICT_CODEC.fieldOf("output").forGetter((BrewingRecipe o) -> o.output)
            ).apply(i, BrewingRecipe::new)
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

    private static Ingredient createIngredientFromLocation(Identifier loc) {
        String path = loc.toString();
        if (path.startsWith("#")) {
            return Ingredient.of(TagKey.create(Registries.ITEM, Identifier.parse(path.substring(1))));
        }
        Item item = BuiltInRegistries.ITEM.get(loc);
        return item != Items.AIR ? Ingredient.of(item) : Ingredient.EMPTY;
    }

    private static Ingredient createIngredientFromLocations(List<Identifier> list) {
        List<ItemStack> stacks = new ArrayList<>();
        for (Identifier loc : list) {
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

    public record BrewingInputMatcher(Ingredient ingredient, Optional<List<Identifier>> potionContents) {
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
                        FLEXIBLE_INGREDIENT_CODEC.optionalFieldOf("ingredient", Ingredient.EMPTY).forGetter(BrewingInputMatcher::ingredient),
                        FLEXIBLE_INGREDIENT_CODEC.optionalFieldOf("item", Ingredient.EMPTY).forGetter(b -> Ingredient.EMPTY),
                        FLEXIBLE_POTION_CONTENTS_CODEC.optionalFieldOf("potion_contents", Optional.empty()).forGetter(BrewingInputMatcher::potionContents)
                ).apply(instance, (ing1, ing2, potionContents) -> {
                    Ingredient ing = !ing1.isEmpty() ? ing1 : ing2;
                    return new BrewingInputMatcher(ing, potionContents);
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
                        ing -> DataResult.success(new BrewingInputMatcher(ing, Optional.empty()))
                ),
                matcher -> DataResult.success(matcher.potionContents().isPresent() ? Either.left(matcher) : Either.right(matcher.ingredient()))
        );

        public static final StreamCodec<RegistryFriendlyByteBuf, BrewingInputMatcher> STREAM_CODEC = StreamCodec.of(
                (buf, matcher) -> {
                    Ingredient.CONTENTS_STREAM_CODEC.encode(buf, matcher.ingredient);
                    buf.writeBoolean(matcher.potionContents.isPresent());
                    if (matcher.potionContents.isPresent()) {
                        List<Identifier> list = matcher.potionContents.get();
                        buf.writeVarInt(list.size());
                        for (Identifier rl : list) {
                            buf.writeIdentifier(rl);
                        }
                    }
                },
                buf -> {
                    Ingredient ingredient = Ingredient.CONTENTS_STREAM_CODEC.decode(buf);
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
                Identifier potionId = key.get().location();
                return potionContents.get().contains(potionId);
            }
            return true;
        }
    }
}
*///?} else if <=26.2 {
import com.mojang.datafixers.util.Either;
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
//?} else {
/*public class BrewingRecipe {}
*///?}
