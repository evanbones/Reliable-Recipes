package com.evandev.reliable_recipes;

import com.evandev.reliable_recipes.command.UndoCommand;
import com.evandev.reliable_recipes.config.ClothConfigIntegration;
import com.evandev.reliable_recipes.networking.ClientboundDeleteRecipePayload;
import com.evandev.reliable_recipes.networking.DeleteRecipePayload;
import com.evandev.reliable_recipes.recipe.RecipeModifier;
import com.evandev.reliable_recipes.recipe.TagModifier;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.protocol.game.ClientboundUpdateRecipesPacket;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import net.neoforged.fml.ModLoadingContext;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLConstructModEvent;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.TagsUpdatedEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import org.jetbrains.annotations.NotNull;

@Mod(Constants.MOD_ID)
@EventBusSubscriber(modid = Constants.MOD_ID)
public class ReliableRecipesMod {

    public ReliableRecipesMod(IEventBus eventBus) {
        CommonClass.init();

        if (ModList.get().isLoaded("cloth_config")) {
            eventBus.register(new Object() {
                @SubscribeEvent
                public void onConstructMod(FMLConstructModEvent event) {
                    ModLoadingContext.get().registerExtensionPoint(
                            IConfigScreenFactory.class,
                            () -> new IConfigScreenFactory() {
                                @Override
                                public @NotNull Screen createScreen(@NotNull ModContainer modContainer, @NotNull Screen parent) {
                                    return ClothConfigIntegration.createScreen(parent);
                                }
                            }
                    );
                }
            });
        }
    }

    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event) {
        TagModifier.apply();
        RecipeModifier.apply(event.getServer().getRecipeManager());
    }

    @SubscribeEvent
    public static void registerCommands(RegisterCommandsEvent event) {
        UndoCommand.register(event.getDispatcher());
    }

    @SubscribeEvent
    private static void registerPayloadHandlers(RegisterPayloadHandlersEvent event) {
        final PayloadRegistrar registrar = event.registrar("1");
        registrar.playToServer(
                DeleteRecipePayload.TYPE,
                DeleteRecipePayload.STREAM_CODEC,
                (payload, context)-> DeleteRecipePayload.handle(payload.recipeId(), context.player().getServer(), (ServerPlayer) context.player())
        );
        registrar.playToClient(
                ClientboundDeleteRecipePayload.TYPE,
                ClientboundDeleteRecipePayload.STREAM_CODEC,
                (payload, context)-> ClientboundDeleteRecipePayload.handle(payload.recipeId(), Minecraft.getInstance())
        );
    }

    @SubscribeEvent
    public static void onTagsUpdated(TagsUpdatedEvent event) {
        TagModifier.apply();

        var server = ServerLifecycleHooks.getCurrentServer();
        if (server != null) {
            RecipeModifier.apply(server.getRecipeManager());

            server.getPlayerList().getPlayers().forEach(player ->
                    player.connection.send(new ClientboundUpdateRecipesPacket(server.getRecipeManager().getRecipes()))
            );
        }
    }
}