package com.evandev.reliable_recipes.forge.client;

//? if forge {
/*import com.evandev.reliable_recipes.Constants;
import com.evandev.reliable_recipes.client.ClientRecipeSync;
import com.evandev.reliable_recipes.client.SharedToastOverlay;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Constants.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ClientEvents {

    @SubscribeEvent
    public static void onScreenRender(ScreenEvent.Render.Post event) {
        SharedToastOverlay.extract(event.getGuiGraphics());
    }

    @SubscribeEvent
    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        ClientRecipeSync.onDisconnect();
    }
}
*///?}
