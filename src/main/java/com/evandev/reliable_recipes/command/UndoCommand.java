package com.evandev.reliable_recipes.command;

import com.evandev.reliable_recipes.config.RecipeConfigIO;
import com.evandev.reliable_recipes.platform.Services;
import com.evandev.reliable_recipes.recipe.RecipeModifier;
import net.minecraft.world.item.crafting.RecipeHolder;
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
                            RecipeHolder<?> restored = RecipeModifier.restoreRecipeAndGet(recipeManager, recipeKey);

                            if (restored != null) {
                                recipeManager.finalizeRecipeLoading(ctx.getSource().getServer().getWorldData().enabledFeatures());
                                // Sync restored vanilla recipes to all clients
                                ClientboundUpdateRecipesPacket packet = new ClientboundUpdateRecipesPacket(
                                        recipeManager.getSynchronizedItemProperties(),
                                        recipeManager.getSynchronizedStonecutterRecipes()
                                );
                                ctx.getSource().getServer().getPlayerList().getPlayers().forEach(p -> {
                                    p.connection.send(packet);
                                    Services.PLATFORM.sendAddRecipePacketToPlayer(p, restored);
                                });

                                ctx.getSource().sendSuccess(() -> Component.translatable("commands.reliable_recipes.undo.success", id), true);
                            } else {
                                ctx.getSource().sendFailure(Component.translatable("commands.reliable_recipes.undo.failure", id));
                            }
                            return 1;
                        })
                )
        );
    }
}