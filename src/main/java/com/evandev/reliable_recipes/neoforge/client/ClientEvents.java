package com.evandev.reliable_recipes.neoforge.client;

//? if neoforge {
/*import com.evandev.reliable_recipes.Constants;
import com.evandev.reliable_recipes.client.SharedToastOverlay;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ScreenEvent;

@EventBusSubscriber(modid = Constants.MOD_ID, value = Dist.CLIENT)
public class ClientEvents {

    @SubscribeEvent
    public static void onScreenRender(ScreenEvent.Render.Post event) {
        SharedToastOverlay.extract(event.getGuiGraphics());
    }
}
*///?}
