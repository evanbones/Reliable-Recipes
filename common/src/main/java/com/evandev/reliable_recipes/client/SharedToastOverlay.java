package com.evandev.reliable_recipes.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.util.Util;
import net.minecraft.world.item.ItemStack;

public class SharedToastOverlay {
    private static final Identifier BACKGROUND_SPRITE = Identifier.withDefaultNamespace("toast/recipe");
    private static final long DISPLAY_DURATION = 5000L;
    private static final long FADE_DURATION = 600L;

    private static Component currentTitle;
    private static Component currentMessage;
    private static ItemStack iconStack = ItemStack.EMPTY;
    private static long showTime = -1;

    public static void show(Component title, Component message, ItemStack icon) {
        currentTitle = title;
        currentMessage = message;
        iconStack = icon;
        showTime = Util.getMillis();
    }

    public static void extract(GuiGraphicsExtractor guiGraphics) {
        if (showTime == -1 || currentMessage == null) return;

        Minecraft mc = Minecraft.getInstance();

        boolean isInventory = mc.screen instanceof InventoryScreen
                || mc.screen instanceof CreativeModeInventoryScreen;

        if (!isInventory) return;

        long age = Util.getMillis() - showTime;
        if (age >= DISPLAY_DURATION) {
            showTime = -1;
            currentMessage = null;
            currentTitle = null;
            iconStack = ItemStack.EMPTY;
            return;
        }

        int toastWidth = 160;
        int toastHeight = 32;
        int xPos = (guiGraphics.guiWidth() - toastWidth) / 2;
        int yPos = getYPos(age, toastHeight);

        guiGraphics.nextStratum();

        guiGraphics.pose().pushMatrix();
        guiGraphics.pose().translate((float) xPos, (float) yPos);

        guiGraphics.blitSprite(RenderPipelines.GUI_TEXTURED, BACKGROUND_SPRITE, 0, 0, toastWidth, toastHeight);

        if (!iconStack.isEmpty()) {
            guiGraphics.fakeItem(iconStack, 8, 8);
        }

        guiGraphics.text(mc.font, currentTitle != null ? currentTitle : Component.literal("Deleted"), 30, 7, -11534256, false);
        guiGraphics.text(mc.font, currentMessage, 30, 18, -16777216, false);

        guiGraphics.pose().popMatrix();
    }

    private static int getYPos(long age, int toastHeight) {
        float animationProgress = age < FADE_DURATION ? (float) age / FADE_DURATION :
                (age > DISPLAY_DURATION - FADE_DURATION ? (float) (DISPLAY_DURATION - age) / FADE_DURATION : 1.0f);
        animationProgress = Mth.clamp(animationProgress, 0.0f, 1.0f);
        float ease = 1.0f - (float) Math.pow(1.0f - animationProgress, 3);
        return (int) (8 - (toastHeight + 8) * (1.0f - ease));
    }
}