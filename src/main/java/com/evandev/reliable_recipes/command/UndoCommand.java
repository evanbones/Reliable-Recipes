package com.evandev.reliable_recipes.command;

import com.evandev.reliable_recipes.config.RecipeConfigIO;
import com.evandev.reliable_recipes.platform.Services;
import com.evandev.reliable_recipes.recipe.RecipeUndoCache;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.IdentifierArgument;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundUpdateRecipesPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
//? if >=1.21.2 {
import net.minecraft.server.permissions.Permission;
import net.minecraft.server.permissions.PermissionLevel;
//?}

public class UndoCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("rrecipes_undo")
                //? if <1.21.2 {
                /*.requires(source -> source.hasPermission(2))
                *///?} else {
                .requires(source -> source.permissions().hasPermission(new Permission.HasCommandLevel(PermissionLevel.GAMEMASTERS)))
                //?}
                .then(Commands.argument("id", IdentifierArgument.id())
                        .executes(ctx -> {
                            Identifier id = IdentifierArgument.getId(ctx, "id");
                            ResourceKey<Recipe<?>> recipeKey = ResourceKey.create(Registries.RECIPE, id);
                            MinecraftServer server = ctx.getSource().getServer();

                            // Remove from config
                            RecipeConfigIO.removeRemovalRule(id.toString());

                            RecipeManager recipeManager = server.getRecipeManager();

                            // Restore in memory
                            RecipeHolder<?> restored = RecipeUndoCache.restoreRecipe(recipeManager, recipeKey);

                            if (restored != null) {
                                // Sync restored recipes to all clients
                                //? if <1.21.2 {
                                /*ClientboundUpdateRecipesPacket packet = new ClientboundUpdateRecipesPacket(recipeManager.getRecipes());
                                *///?} else {
                                recipeManager.finalizeRecipeLoading(server.getWorldData().enabledFeatures());
                                ClientboundUpdateRecipesPacket packet = new ClientboundUpdateRecipesPacket(
                                        recipeManager.getSynchronizedItemProperties(),
                                        recipeManager.getSynchronizedStonecutterRecipes()
                                );
                                //?}
                                server.getPlayerList().getPlayers().forEach(p -> {
                                    p.connection.send(packet);
                                    Services.PLATFORM.sendAddRecipePacketToPlayer(p, restored);
                                });

                                ctx.getSource().sendSuccess(() -> Component.translatable("commands.reliable_recipes.undo.success", id.toString()), true);
                            } else {
                                ctx.getSource().sendFailure(Component.translatable("commands.reliable_recipes.undo.failure", id.toString()));
                            }
                            return 1;
                        })
                )
        );
    }
}
