package com.evandev.reliable_recipes.client.legacy;

//? if <1.20 {
/*import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

public class GuiGraphics {
    private final PoseStack pose;

    public GuiGraphics(PoseStack pose) {
        this.pose = pose;
    }

    public PoseStack pose() {
        return pose;
    }

    public int guiWidth() {
        return Minecraft.getInstance().getWindow().getGuiScaledWidth();
    }

    public void blit(Identifier texture, int x, int y, float u, float v, int width, int height, int textureWidth, int textureHeight) {
        RenderSystem.setShaderTexture(0, texture);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        GuiComponent.blit(pose, x, y, u, v, width, height, textureWidth, textureHeight);
    }

    public void renderFakeItem(ItemStack stack, int x, int y) {
        PoseStack modelView = RenderSystem.getModelViewStack();
        modelView.pushPose();
        modelView.mulPoseMatrix(pose.last().pose());
        RenderSystem.applyModelViewMatrix();
        Minecraft.getInstance().getItemRenderer().renderGuiItem(stack, x, y);
        modelView.popPose();
        RenderSystem.applyModelViewMatrix();
    }

    public void drawString(Font font, Component text, int x, int y, int color, boolean shadow) {
        if (shadow) {
            font.drawShadow(pose, text, x, y, color);
        } else {
            font.draw(pose, text, x, y, color);
        }
    }
}
*///?}
