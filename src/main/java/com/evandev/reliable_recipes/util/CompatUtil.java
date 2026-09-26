package com.evandev.reliable_recipes.util;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
//? if >=1.21.2 {
import com.mojang.datafixers.util.Either;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderOwner;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.util.RandomSource;
//?}

import java.util.*;
import java.util.stream.Stream;

public final class CompatUtil {

    public static Item getItem(Identifier id) {
        //? if <1.21.2 {
        /*return BuiltInRegistries.ITEM.get(id);
        *///?} else {
        return BuiltInRegistries.ITEM.get(id).map(Holder.Reference::value).orElse(Items.AIR);
        //?}
    }

    public static Identifier keyId(ResourceKey<?> key) {
        //? if <1.21.11 {
        /*return key.location();
        *///?} else {
        return key.identifier();
        //?}
    }

    public static Identifier recipeId(RecipeHolder<?> holder) {
        //? if <1.21.2 {
        /*return holder.id();
        *///?} else {
        return keyId(holder.id());
        //?}
    }

    public static Ingredient emptyIngredient() {
        //? if <1.21.2 {
        /*return Ingredient.EMPTY;
        *///?} else {
        return EMPTY_INGREDIENT;
        //?}
    }

    public static Ingredient ingredientOf(Collection<Item> items) {
        List<Item> nonAir = items.stream().filter(item -> item != Items.AIR).distinct().toList();
        if (nonAir.isEmpty()) return emptyIngredient();
        //? if <1.21.2 {
        /*return Ingredient.of(nonAir.stream().map(ItemStack::new));
        *///?} else {
        return Ingredient.of(nonAir.stream());
        //?}
    }

    public static Ingredient ingredientOf(TagKey<Item> tag) {
        //? if <1.21.2 {
        /*return Ingredient.of(tag);
        *///?} else {
        return BuiltInRegistries.ITEM.get(tag).map(Ingredient::of).orElseGet(CompatUtil::emptyIngredient);
        //?}
    }

    public static List<Item> ingredientItems(Ingredient ingredient) {
        List<Item> items = new ArrayList<>();
        //? if <1.21.2 {
        /*for (ItemStack stack : ingredient.getItems()) {
            items.add(stack.getItem());
        }
        *///?} else {
        ingredient.items().forEach(holder -> items.add(holder.value()));
        //?}
        return items;
    }

    //? if >=1.21.2 {
    private static final Ingredient EMPTY_INGREDIENT = Ingredient.of(new EmptyTagHolderSet());

    private static final class EmptyTagHolderSet implements HolderSet<Item> {
        private static final TagKey<Item> KEY = TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath("reliable_recipes", "empty"));

        @Override
        public Stream<Holder<Item>> stream() {
            return Stream.empty();
        }

        @Override
        public int size() {
            return 0;
        }

        @Override
        public boolean isBound() {
            return true;
        }

        @Override
        public Either<TagKey<Item>, List<Holder<Item>>> unwrap() {
            return Either.left(KEY);
        }

        @Override
        public Optional<Holder<Item>> getRandomElement(RandomSource random) {
            return Optional.empty();
        }

        @Override
        public Holder<Item> get(int index) {
            throw new IndexOutOfBoundsException(index);
        }

        @Override
        public boolean contains(Holder<Item> value) {
            return false;
        }

        @Override
        public boolean canSerializeIn(HolderOwner<Item> owner) {
            return false;
        }

        @Override
        public Optional<TagKey<Item>> unwrapKey() {
            return Optional.of(KEY);
        }

        @Override
        public Iterator<Holder<Item>> iterator() {
            return Collections.emptyIterator();
        }
    }
    //?}
}
