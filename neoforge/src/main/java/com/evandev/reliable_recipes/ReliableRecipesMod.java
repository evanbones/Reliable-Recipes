package com.evandev.reliable_recipes;

import com.evandev.reliable_recipes.client.ModConfigScreen;
import com.evandev.reliable_recipes.command.UndoCommand;
import com.evandev.reliable_recipes.networking.ClientboundDeleteRecipePayload;
import com.evandev.reliable_recipes.networking.DeleteRecipePayload;
import com.evandev.reliable_recipes.recipe.BrewingRecipe;
import net.minecraft.client.Minecraft;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.neoforged.neoforge.registries.RegisterEvent;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import com.evandev.reliable_recipes.recipe.TransmuteRecipe;

@Mod(Constants.MOD_ID)
@EventBusSubscriber(modid = Constants.MOD_ID)
public class ReliableRecipesMod {

    public ReliableRecipesMod(IEventBus eventBus, ModContainer modContainer) {
        CommonClass.init();

        eventBus.addListener(ReliableRecipesMod::registerPayloadHandlers);
        eventBus.addListener(ReliableRecipesMod::onRegister);

        if (FMLEnvironment.dist.isClient()) {
            modContainer.registerExtensionPoint(IConfigScreenFactory.class, (container, parent) -> ModConfigScreen.createScreen(parent));
        }
    }

    @SubscribeEvent
    public static void registerCommands(RegisterCommandsEvent event) {
        UndoCommand.register(event.getDispatcher());
    }

    private static void registerPayloadHandlers(RegisterPayloadHandlersEvent event) {
        final PayloadRegistrar registrar = event.registrar("1");
        registrar.playToServer(
                DeleteRecipePayload.TYPE,
                DeleteRecipePayload.STREAM_CODEC,
                (payload, context) -> DeleteRecipePayload.handle(payload.recipeId(), context.player().getServer(), (ServerPlayer) context.player())
        );
        registrar.playToClient(
                ClientboundDeleteRecipePayload.TYPE,
                ClientboundDeleteRecipePayload.STREAM_CODEC,
                (payload, context) -> ClientboundDeleteRecipePayload.handle(payload.recipeId(), Minecraft.getInstance())
        );
    }

    private static void onRegister(RegisterEvent event) {
        if (event.getRegistryKey().equals(Registries.RECIPE_SERIALIZER)) {
            event.register(Registries.RECIPE_SERIALIZER, ResourceLocation.withDefaultNamespace("crafting_transmute"), () -> TransmuteRecipe.SERIALIZER);
            event.register(Registries.RECIPE_SERIALIZER, ResourceLocation.withDefaultNamespace("brewing"), () -> BrewingRecipe.SERIALIZER);
        } else if (event.getRegistryKey().equals(Registries.RECIPE_TYPE)) {
            event.register(Registries.RECIPE_TYPE, ResourceLocation.withDefaultNamespace("crafting_transmute"), () -> TransmuteRecipe.TYPE);
            event.register(Registries.RECIPE_TYPE, ResourceLocation.withDefaultNamespace("brewing"), () -> BrewingRecipe.TYPE);
        }
    }
}