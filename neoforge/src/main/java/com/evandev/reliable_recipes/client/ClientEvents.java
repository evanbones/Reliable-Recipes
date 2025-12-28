package com.evandev.reliable_recipes.client;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ScreenEvent;

@EventBusSubscriber(modid = "reliable_recipes", value = Dist.CLIENT)
public class ClientEvents {

    @SubscribeEvent
    public static void onScreenRender(ScreenEvent.Render.Post event) {
        DeletionToastOverlay.render(event.getGuiGraphics());
    }
}