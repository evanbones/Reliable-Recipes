package com.evandev.reliable_recipes.recipe;

//? if <=26.2 {
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
//? if neoforge {
/*import net.minecraft.core.registries.Registries;
import net.neoforged.neoforge.registries.RegisterEvent;
*///?}
//?}

public class BrewingRegistration {
    public static void registerFabric() {
        //? if <=26.2 {
        Registry.register(BuiltInRegistries.RECIPE_SERIALIZER, Identifier.withDefaultNamespace("brewing"), BrewingRecipe.SERIALIZER);
        Registry.register(BuiltInRegistries.RECIPE_TYPE, Identifier.withDefaultNamespace("brewing"), BrewingRecipe.TYPE);
        //?}
        //? if <1.21.2 {
        /*Registry.register(BuiltInRegistries.RECIPE_SERIALIZER, Identifier.withDefaultNamespace("crafting_transmute"), TransmuteRecipe.SERIALIZER);
        Registry.register(BuiltInRegistries.RECIPE_TYPE, Identifier.withDefaultNamespace("crafting_transmute"), TransmuteRecipe.TYPE);
        *///?}
    }

    //? if <=26.2 && neoforge {
    /*public static void registerNeoForge(RegisterEvent event) {
        event.register(Registries.RECIPE_SERIALIZER, Identifier.withDefaultNamespace("brewing"), () -> BrewingRecipe.SERIALIZER);
        event.register(Registries.RECIPE_TYPE, Identifier.withDefaultNamespace("brewing"), () -> BrewingRecipe.TYPE);
        //? if <1.21.2 {
        /^event.register(Registries.RECIPE_SERIALIZER, Identifier.withDefaultNamespace("crafting_transmute"), () -> TransmuteRecipe.SERIALIZER);
        event.register(Registries.RECIPE_TYPE, Identifier.withDefaultNamespace("crafting_transmute"), () -> TransmuteRecipe.TYPE);
        ^///?}
    }
    *///?}
    //? if >26.2 || fabric {
    public static void registerNeoForge(Object event) {
    }
    //?}
}
