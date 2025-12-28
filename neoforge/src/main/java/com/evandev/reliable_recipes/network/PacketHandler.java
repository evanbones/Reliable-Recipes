package com.evandev.reliable_recipes.network;

import com.evandev.reliable_recipes.Constants;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

@EventBusSubscriber(modid = Constants.MOD_ID, bus = EventBusSubscriber.Bus.MOD)
public class PacketHandler {

    @SubscribeEvent
    public static void register(RegisterPayloadHandlersEvent event) {
        final PayloadRegistrar registrar = event.registrar("1");

        registrar.playToServer(
                DeleteRecipePayload.TYPE,
                DeleteRecipePayload.CODEC,
                DeleteRecipePayload::handle
        );

        registrar.playToClient(
                ClientboundDeleteRecipePayload.TYPE,
                ClientboundDeleteRecipePayload.CODEC,
                ClientboundDeleteRecipePayload::handle
        );
    }
}