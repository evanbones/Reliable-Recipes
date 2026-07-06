package com.evandev.reliable_recipes;

import com.evandev.reliable_recipes.command.UndoCommand;
import com.evandev.reliable_recipes.network.PacketHandler;
import com.evandev.reliable_recipes.recipe.TransmuteRecipe;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.RegisterEvent;

@Mod(Constants.MOD_ID)
public class ReliableRecipesMod {

    public ReliableRecipesMod() {
        CommonClass.init();
        MinecraftForge.EVENT_BUS.register(this);
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::commonSetup);
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::onRegister);
    }

    private void onRegister(RegisterEvent event) {
        if (event.getRegistryKey().equals(Registries.RECIPE_SERIALIZER)) {
            event.register(Registries.RECIPE_SERIALIZER, new ResourceLocation("crafting_transmute"), () -> TransmuteRecipe.SERIALIZER);
        } else if (event.getRegistryKey().equals(Registries.RECIPE_TYPE)) {
            event.register(Registries.RECIPE_TYPE, new ResourceLocation("crafting_transmute"), () -> TransmuteRecipe.TYPE);
        }
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        PacketHandler.register();
    }

    @SubscribeEvent
    public void registerCommands(RegisterCommandsEvent event) {
        UndoCommand.register(event.getDispatcher());
    }

}