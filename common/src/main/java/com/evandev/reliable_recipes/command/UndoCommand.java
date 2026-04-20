package com.evandev.reliable_recipes.command;

import com.evandev.reliable_recipes.compat.RrvCompat;
import com.evandev.reliable_recipes.config.RecipeConfigIO;
import com.evandev.reliable_recipes.platform.Services;
import com.evandev.reliable_recipes.recipe.RecipeModifier;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.IdentifierArgument;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundUpdateRecipesPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.permissions.Permission;
import net.minecraft.server.permissions.PermissionLevel;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeManager;

public class UndoCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("rrecipes_undo")
                .requires(source -> source.permissions().hasPermission(new Permission.HasCommandLevel(PermissionLevel.GAMEMASTERS)))
                .then(Commands.argument("id", IdentifierArgument.id())
                        .executes(ctx -> {
                            Identifier id = IdentifierArgument.getId(ctx, "id");
                            ResourceKey<Recipe<?>> recipeKey = ResourceKey.create(Registries.RECIPE, id);

                            // Remove from config
                            RecipeConfigIO.removeRemovalRule(id.toString());

                            RecipeManager recipeManager = ctx.getSource().getServer().getRecipeManager();

                            // Restore in memory
                            boolean restored = RecipeModifier.restoreRecipe(recipeManager, recipeKey);

                            if (restored) {
                                // Sync restored vanilla recipes to all clients
                                ClientboundUpdateRecipesPacket packet = new ClientboundUpdateRecipesPacket(
                                        recipeManager.getSynchronizedItemProperties(),
                                        recipeManager.getSynchronizedStonecutterRecipes()
                                );
                                ctx.getSource().getServer().getPlayerList().getPlayers().forEach(p -> p.connection.send(packet));

                                // Trigger RRV's sync to clients
                                if (Services.PLATFORM.isModLoaded("rrv")) {
                                    RrvCompat.syncRecipesToAllClients();
                                }
                                ctx.getSource().sendSuccess(() -> Component.literal("Restored recipe " + id), true);
                            } else {
                                ctx.getSource().sendFailure(Component.literal("Could not restore recipe " + id + " (not in cache or already exists)"));
                            }
                            return 1;
                        })
                )
        );
    }
}