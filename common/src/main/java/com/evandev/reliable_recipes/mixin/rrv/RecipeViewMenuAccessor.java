package com.evandev.reliable_recipes.mixin.rrv;

import cc.cassian.rrv.api.recipe.ReliableClientRecipe;
import cc.cassian.rrv.common.recipe.inventory.RecipeViewMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.util.List;

@Mixin(RecipeViewMenu.class)
public interface RecipeViewMenuAccessor {

    @Invoker("guiOffsetLeft")
    int reliableRecipes$guiOffsetLeft();

    @Invoker("guiOffsetTop")
    int reliableRecipes$guiOffsetTop(int displayIndex);

    @Invoker("getCurrentDisplay")
    List<ReliableClientRecipe> reliableRecipes$getCurrentDisplay();
}