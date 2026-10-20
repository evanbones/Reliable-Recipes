package com.evandev.reliable_recipes.recipe;

//? if <=26.2 {
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
//? if neoforge {
/*import net.minecraft.core.registries.Registries;
import net.neoforged.neoforge.registries.RegisterEvent;
*///?}
//? if forge {
/*import net.minecraft.core.registries.Registries;
import net.minecraftforge.registries.RegisterEvent;
*///?}
//?}

public class BrewingRegistration {
    public static void registerFabric() {
        //? if <=26.2 {
        Identifier brewingId = Identifier.withDefaultNamespace("brewing");
        if (!BuiltInRegistries.RECIPE_SERIALIZER.containsKey(brewingId))
            Registry.register(BuiltInRegistries.RECIPE_SERIALIZER, brewingId, BrewingRecipe.SERIALIZER);
        if (!BuiltInRegistries.RECIPE_TYPE.containsKey(brewingId))
            Registry.register(BuiltInRegistries.RECIPE_TYPE, brewingId, BrewingRecipe.TYPE);
        //?}
        //? if <1.21.2 {
        /*Identifier transmuteId = Identifier.withDefaultNamespace("crafting_transmute");
        if (!BuiltInRegistries.RECIPE_SERIALIZER.containsKey(transmuteId))
            Registry.register(BuiltInRegistries.RECIPE_SERIALIZER, transmuteId, TransmuteRecipe.SERIALIZER);
        if (!BuiltInRegistries.RECIPE_TYPE.containsKey(transmuteId))
            Registry.register(BuiltInRegistries.RECIPE_TYPE, transmuteId, TransmuteRecipe.TYPE);
        *///?}
    }

    //? if <=26.2 && neoforge {
    /*public static void registerNeoForge(RegisterEvent event) {
        Identifier brewingId = Identifier.withDefaultNamespace("brewing");
        if (!BuiltInRegistries.RECIPE_SERIALIZER.containsKey(brewingId))
            event.register(Registries.RECIPE_SERIALIZER, brewingId, () -> BrewingRecipe.SERIALIZER);
        if (!BuiltInRegistries.RECIPE_TYPE.containsKey(brewingId))
            event.register(Registries.RECIPE_TYPE, brewingId, () -> BrewingRecipe.TYPE);
        //? if <1.21.2 {
        /^Identifier transmuteId = Identifier.withDefaultNamespace("crafting_transmute");
        if (!BuiltInRegistries.RECIPE_SERIALIZER.containsKey(transmuteId))
            event.register(Registries.RECIPE_SERIALIZER, transmuteId, () -> TransmuteRecipe.SERIALIZER);
        if (!BuiltInRegistries.RECIPE_TYPE.containsKey(transmuteId))
            event.register(Registries.RECIPE_TYPE, transmuteId, () -> TransmuteRecipe.TYPE);
        ^///?}
    }
    *///?}
    //? if forge {
    /*public static void registerForge(RegisterEvent event) {
        Identifier brewingId = Identifier.withDefaultNamespace("brewing");
        if (!BuiltInRegistries.RECIPE_SERIALIZER.containsKey(brewingId))
            event.register(Registries.RECIPE_SERIALIZER, brewingId, () -> BrewingRecipe.SERIALIZER);
        if (!BuiltInRegistries.RECIPE_TYPE.containsKey(brewingId))
            event.register(Registries.RECIPE_TYPE, brewingId, () -> BrewingRecipe.TYPE);
        Identifier transmuteId = Identifier.withDefaultNamespace("crafting_transmute");
        if (!BuiltInRegistries.RECIPE_SERIALIZER.containsKey(transmuteId))
            event.register(Registries.RECIPE_SERIALIZER, transmuteId, () -> TransmuteRecipe.SERIALIZER);
        if (!BuiltInRegistries.RECIPE_TYPE.containsKey(transmuteId))
            event.register(Registries.RECIPE_TYPE, transmuteId, () -> TransmuteRecipe.TYPE);
    }
    *///?}
}
