package com.evandev.reliable_recipes.mixin.minecraft;

import com.evandev.reliable_recipes.Constants;
import com.evandev.reliable_recipes.api.ReliableRecipesAPI;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;
import java.util.function.Supplier;

//? if <1.21.2 {
/*import com.evandev.reliable_recipes.util.CompatUtil;
*///?}
//? if <1.21 {
/*import net.minecraft.world.Container;
*///?} else {
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeInput;
//?}

@Mixin(RecipeManager.class)
public class RecipeManagerResultMixin {

    //? if <1.21 {
    /*@Inject(
            method = "getRecipeFor(Lnet/minecraft/world/item/crafting/RecipeType;Lnet/minecraft/world/Container;Lnet/minecraft/world/level/Level;)Ljava/util/Optional;",
            at = @At("RETURN"),
            cancellable = true
    )
    private <C extends Container, T extends Recipe<C>> void reliableRecipes$skipHiddenResult(RecipeType<T> type, C container, Level level, CallbackInfoReturnable<Optional<T>> cir) {
        Optional<T> match = cir.getReturnValue();
        //? if <1.19.4 {
        /^if (match.isPresent() && reliableRecipes$isResultHidden(match.get(), () -> match.get().assemble(container))) {
        ^///?} else {
        if (match.isPresent() && reliableRecipes$isResultHidden(match.get(), () -> match.get().assemble(container, level.registryAccess()))) {
        //?}
            cir.setReturnValue(Optional.empty());
        }
    }
    *///?} else if <1.21.2 {
    /*@Inject(
            method = "getRecipeFor(Lnet/minecraft/world/item/crafting/RecipeType;Lnet/minecraft/world/item/crafting/RecipeInput;Lnet/minecraft/world/level/Level;Lnet/minecraft/world/item/crafting/RecipeHolder;)Ljava/util/Optional;",
            at = @At("RETURN"),
            cancellable = true
    )
    private <I extends RecipeInput, T extends Recipe<I>> void reliableRecipes$skipHiddenResult(RecipeType<T> type, I input, Level level, RecipeHolder<T> lastRecipe, CallbackInfoReturnable<Optional<RecipeHolder<T>>> cir) {
        Optional<RecipeHolder<T>> match = cir.getReturnValue();
        if (match.isPresent() && reliableRecipes$isResultHidden(match.get().value(), () -> match.get().value().assemble(input, level.registryAccess()))) {
            cir.setReturnValue(Optional.empty());
        }
    }
    *///?} else {
    @Inject(
            method = "getRecipeFor(Lnet/minecraft/world/item/crafting/RecipeType;Lnet/minecraft/world/item/crafting/RecipeInput;Lnet/minecraft/world/level/Level;)Ljava/util/Optional;",
            at = @At("RETURN"),
            cancellable = true
    )
    private <I extends RecipeInput, T extends Recipe<I>> void reliableRecipes$skipHiddenResult(RecipeType<T> type, I input, Level level, CallbackInfoReturnable<Optional<RecipeHolder<T>>> cir) {
        Optional<RecipeHolder<T>> match = cir.getReturnValue();
        if (match.isPresent() && reliableRecipes$isResultHidden(match.get().value(), () -> match.get().value().assemble(input))) {
            cir.setReturnValue(Optional.empty());
        }
    }

    @Inject(
            method = "getRecipeFor(Lnet/minecraft/world/item/crafting/RecipeType;Lnet/minecraft/world/item/crafting/RecipeInput;Lnet/minecraft/world/level/Level;Lnet/minecraft/world/item/crafting/RecipeHolder;)Ljava/util/Optional;",
            at = @At("RETURN"),
            cancellable = true
    )
    private <I extends RecipeInput, T extends Recipe<I>> void reliableRecipes$skipHiddenHintedResult(RecipeType<T> type, I input, Level level, RecipeHolder<T> recipeHint, CallbackInfoReturnable<Optional<RecipeHolder<T>>> cir) {
        Optional<RecipeHolder<T>> match = cir.getReturnValue();
        if (match.isPresent() && reliableRecipes$isResultHidden(match.get().value(), () -> match.get().value().assemble(input))) {
            cir.setReturnValue(Optional.empty());
        }
    }
    //?}

    @Unique
    private static boolean reliableRecipes$isResultHidden(Recipe<?> recipe, Supplier<ItemStack> result) {
        if (!ReliableRecipesAPI.hasItemHidingCapabilities()) return false;
        //? if <1.21.2 {
        /*if (!recipe.isSpecial() && !CompatUtil.resultItem(recipe, null).isEmpty()) return false;
        *///?} else {
        if (!recipe.isSpecial()) return false;
        //?}
        try {
            ItemStack stack = result.get();
            return !stack.isEmpty() && ReliableRecipesAPI.isItemHidden(stack);
        } catch (Exception e) {
            Constants.LOG.debug("Failed to assemble a matched recipe while checking for hidden results", e);
            return false;
        }
    }
}
