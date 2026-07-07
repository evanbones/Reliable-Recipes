package com.evandev.reliable_recipes.compat;

import com.evandev.reliable_recipes.config.YaclConfigIntegration;
import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;

public class ModMenuIntegration implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return YaclConfigIntegration::createScreen;
    }
}