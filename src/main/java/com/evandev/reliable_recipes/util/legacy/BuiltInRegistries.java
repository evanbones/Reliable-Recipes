package com.evandev.reliable_recipes.util.legacy;

//? if <1.19.3 {
/*import net.minecraft.core.DefaultedRegistry;
import net.minecraft.core.Registry;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.Block;

public final class BuiltInRegistries {
    public static final DefaultedRegistry<Item> ITEM = Registry.ITEM;
    public static final DefaultedRegistry<Block> BLOCK = Registry.BLOCK;
    public static final DefaultedRegistry<Potion> POTION = Registry.POTION;
    public static final Registry<RecipeSerializer<?>> RECIPE_SERIALIZER = Registry.RECIPE_SERIALIZER;
    public static final Registry<RecipeType<?>> RECIPE_TYPE = Registry.RECIPE_TYPE;

    private BuiltInRegistries() {
    }
}
*///?}
