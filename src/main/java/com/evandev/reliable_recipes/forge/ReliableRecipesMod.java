package com.evandev.reliable_recipes.forge;

//? if forge {
/*import com.evandev.reliable_recipes.Constants;
import com.evandev.reliable_recipes.command.UndoCommand;
import com.evandev.reliable_recipes.config.ConfigSync;
import com.evandev.reliable_recipes.config.ModConfig;
import com.evandev.reliable_recipes.config.RecipeConfigIO;
import com.evandev.reliable_recipes.config.YaclConfigIntegration;
import com.evandev.reliable_recipes.recipe.BrewingRegistration;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.OnDatapackSyncEvent;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;

@Mod(Constants.MOD_ID)
public class ReliableRecipesMod {

    public ReliableRecipesMod() {
        ModConfig.load();
        ConfigSync.register(Constants.MOD_ID, RecipeConfigIO::createSyncSnapshot, RecipeConfigIO::invalidateCache);

        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        modEventBus.addListener(BrewingRegistration::registerForge);
        modEventBus.addListener(ReliableRecipesMod::commonSetup);
        MinecraftForge.EVENT_BUS.addListener(ReliableRecipesMod::registerCommands);
        MinecraftForge.EVENT_BUS.addListener(ReliableRecipesMod::onDatapackSync);

        if (FMLEnvironment.dist == Dist.CLIENT && ModList.get().isLoaded("yet_another_config_lib_v3")) {
            ModLoadingContext.get().registerExtensionPoint(
                    ConfigScreenHandler.ConfigScreenFactory.class,
                    () -> new ConfigScreenHandler.ConfigScreenFactory((client, parent) -> YaclConfigIntegration.createScreen(parent))
            );
        }
    }

    private static void commonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(ForgeNetworking::register);
    }

    private static void onDatapackSync(OnDatapackSyncEvent event) {
        if (event.getPlayer() != null) {
            ConfigSync.sendTo(event.getPlayer());
        } else {
            event.getPlayerList().getPlayers().forEach(ConfigSync::sendTo);
        }
    }

    private static void registerCommands(RegisterCommandsEvent event) {
        UndoCommand.register(event.getDispatcher());
    }
}
*///?}
