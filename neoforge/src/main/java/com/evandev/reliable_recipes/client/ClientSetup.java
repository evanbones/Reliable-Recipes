package com.evandev.reliable_recipes.client;

import net.neoforged.fml.ModContainer;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

public class ClientSetup {

    public static void init(ModContainer modContainer) {
        modContainer.registerExtensionPoint(IConfigScreenFactory.class, (container, parent) -> ModConfigScreen.createScreen(parent));
    }
}
