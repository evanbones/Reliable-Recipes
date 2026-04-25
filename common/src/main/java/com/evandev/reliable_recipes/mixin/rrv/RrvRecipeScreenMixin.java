package com.evandev.reliable_recipes.mixin.rrv;

import cc.cassian.rrv.api.recipe.ReliableClientRecipe;
import cc.cassian.rrv.common.recipe.inventory.RecipeViewScreen;
import com.evandev.reliable_recipes.client.RrvInteractions;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(RecipeViewScreen.class)
public abstract class RrvRecipeScreenMixin extends Screen {

    protected RrvRecipeScreenMixin(Component title) {
        super(title);
    }

    @Inject(method = "keyPressed", at = @At("HEAD"), cancellable = true)
    private void reliableRecipes$onKeyPressed(KeyEvent keyEvent, CallbackInfoReturnable<Boolean> cir) {
        if (keyEvent.key() == GLFW.GLFW_KEY_DELETE || keyEvent.key() == GLFW.GLFW_KEY_BACKSPACE) {

            double mouseX = this.minecraft.mouseHandler.xpos() * (double) this.minecraft.getWindow().getGuiScaledWidth() / (double) this.minecraft.getWindow().getScreenWidth();
            double mouseY = this.minecraft.mouseHandler.ypos() * (double) this.minecraft.getWindow().getGuiScaledHeight() / (double) this.minecraft.getWindow().getScreenHeight();

            ReliableClientRecipe hoveredRecipe = reliableRecipes$getHoveredRecipe(mouseX, mouseY);

            if (hoveredRecipe != null) {
                if (RrvInteractions.requestDeletion(hoveredRecipe)) {
                    this.onClose();
                    cir.setReturnValue(true);
                }
            }
        }
    }

    @Unique
    private ReliableClientRecipe reliableRecipes$getHoveredRecipe(double mouseX, double mouseY) {
        RecipeViewScreen screen = (RecipeViewScreen) (Object) this;

        int guiLeft = screen.getLeftPos() + screen.getMenu().guiOffsetLeft();
        List<ReliableClientRecipe> currentDisplay = screen.getMenu().getCurrentDisplay();

        for (int i = 0; i < currentDisplay.size(); i++) {
            ReliableClientRecipe recipe = currentDisplay.get(i);
            int guiTop = screen.getTopPos() + screen.getMenu().guiOffsetTop(i);

            int width = recipe.getType().getDisplayWidth();
            int height = recipe.getType().getDisplayHeight();

            if (mouseX >= guiLeft && mouseX <= guiLeft + width && mouseY >= guiTop && mouseY <= guiTop + height) {
                return recipe;
            }
        }
        return null;
    }
}