package com.evandev.reliable_recipes.compat;

import net.minecraft.world.item.ItemStack;
import elocindev.item_obliterator.fabric_quilt.util.Utils;

public class ItemObliteratorCompat {

    public static boolean shouldHide(ItemStack stack) {
        try {
            return !stack.isEmpty() && Utils.isDisabled(stack);
        } catch (Exception e) {
            return false;
        }
    }
}