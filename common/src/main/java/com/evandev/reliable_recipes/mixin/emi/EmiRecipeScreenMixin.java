package com.evandev.reliable_recipes.mixin.emi;

import com.evandev.reliable_recipes.client.EmiInteractions;
import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.widget.Widget;
import dev.emi.emi.screen.RecipeScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(RecipeScreen.class)
public abstract class EmiRecipeScreenMixin extends Screen {

    @Shadow(remap = false)
    private List currentPage;

    protected EmiRecipeScreenMixin(Component title) {
        super(title);
    }

    @Inject(method = "keyPressed", at = @At("HEAD"), cancellable = true)
    private void reliableRecipes$onKeyPressed(int keyCode, int scanCode, int modifiers, CallbackInfoReturnable<Boolean> cir) {

        if (keyCode == 261)  { // Delete key
            double mouseX = 0;
            double mouseY = 0;
            if (this.minecraft != null) {
                mouseX = this.minecraft.mouseHandler.xpos() * (double)this.minecraft.getWindow().getGuiScaledWidth() / (double)this.minecraft.getWindow().getScreenWidth();
                mouseY = this.minecraft.mouseHandler.ypos() * (double)this.minecraft.getWindow().getGuiScaledHeight() / (double)this.minecraft.getWindow().getScreenHeight();
            }

            EmiRecipe hoveredRecipe = reliableRecipes$getHoveredRecipe(mouseX, mouseY);

            if (hoveredRecipe != null) {
                if (EmiInteractions.requestDeletion(hoveredRecipe)) {
                    cir.setReturnValue(true);
                }
            }
        }
    }

    @Unique
    @SuppressWarnings("unchecked")
    private EmiRecipe reliableRecipes$getHoveredRecipe(double mouseX, double mouseY) {
        if (this.currentPage == null) return null;

        // Iterate over the list of WidgetGroups (treated as Object because the class is hidden)
        for (Object group : this.currentPage) {
            try {
                Class<?> groupClass = group.getClass();

                // 'WidgetGroup' has methods x() and y() and fields 'widgets' and 'recipe'
                // We use reflection to access them.
                int groupX = (int) groupClass.getMethod("x").invoke(group);
                int groupY = (int) groupClass.getMethod("y").invoke(group);

                // Calculate mouse position relative to the group
                int relX = (int) mouseX - groupX;
                int relY = (int) mouseY - groupY;

                // Check widgets inside this group
                List<Widget> widgets = (List<Widget>) groupClass.getField("widgets").get(group);
                for (Widget widget : widgets) {
                    if (widget.getBounds().contains(relX, relY)) {
                        // We found the widget under the mouse!
                        // Now grab the recipe from the group.
                        return (EmiRecipe) groupClass.getField("recipe").get(group);
                    }
                }
            } catch (Exception ignored) {
                // If structure changes, fail silently
            }
        }
        return null;
    }
}