package com.evandev.reliable_recipes.fabric.compat;

//? if fabric {

import com.evandev.reliable_recipes.config.YaclConfigIntegration;
import com.evandev.reliable_recipes.platform.Services;
import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;

public class ModMenuIntegration implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        if (Services.PLATFORM.isModLoaded("yet_another_config_lib_v3")) {
            return YaclConfigIntegration::createScreen;
        }
        return _ -> null;
    }
}
//?}
