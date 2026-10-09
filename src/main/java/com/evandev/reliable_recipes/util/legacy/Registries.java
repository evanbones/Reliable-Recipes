package com.evandev.reliable_recipes.util.legacy;

//? if <1.19.3 {
/*import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.Block;

public final class Registries {
    public static final ResourceKey<Registry<Item>> ITEM = Registry.ITEM_REGISTRY;
    public static final ResourceKey<Registry<Block>> BLOCK = Registry.BLOCK_REGISTRY;
    public static final ResourceKey<Registry<Potion>> POTION = Registry.POTION_REGISTRY;
    public static final ResourceKey<Registry<RecipeSerializer<?>>> RECIPE_SERIALIZER = Registry.RECIPE_SERIALIZER_REGISTRY;
    public static final ResourceKey<Registry<RecipeType<?>>> RECIPE_TYPE = Registry.RECIPE_TYPE_REGISTRY;

    private Registries() {
    }
}
*///?}
