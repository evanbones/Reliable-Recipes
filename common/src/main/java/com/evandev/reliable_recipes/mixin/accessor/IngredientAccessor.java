package com.evandev.reliable_recipes.mixin.accessor;

import net.minecraft.world.item.crafting.Ingredient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Ingredient.class)
public interface IngredientAccessor {
    @Accessor("values")
    Ingredient.Value[] getValues();

    @Accessor("values")
    @Mutable
    void setValues(Ingredient.Value[] values);
}