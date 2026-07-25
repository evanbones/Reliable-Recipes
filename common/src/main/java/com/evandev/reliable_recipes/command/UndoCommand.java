package com.evandev.reliable_recipes.command;

import com.evandev.reliable_recipes.config.RecipeConfigIO;
import com.evandev.reliable_recipes.recipe.RecipeUndoCache;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundUpdateRecipesPacket;
import net.minecraft.resources.ResourceLocation;

public class UndoCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("rrecipes_undo")
                .requires(s -> s.hasPermission(2))
                .then(Commands.argument("id", ResourceLocationArgument.id())
                        .executes(ctx -> {
                            ResourceLocation id = ResourceLocationArgument.getId(ctx, "id");

                            RecipeConfigIO.removeRemovalRule(id.toString());
                            boolean restored = RecipeUndoCache.restoreRecipe(ctx.getSource().getServer().getRecipeManager(), id);

                            if (restored) {
                                ctx.getSource().getServer().getPlayerList().getPlayers().forEach(p ->
                                        p.connection.send(new ClientboundUpdateRecipesPacket(ctx.getSource().getServer().getRecipeManager().getRecipes()))
                                );
                                ctx.getSource().sendSuccess(() -> Component.literal("Restored recipe " + id), true);
                            } else {
                                ctx.getSource().sendFailure(Component.literal("Could not restore recipe " + id + " (it likely already exists!)"));
                            }
                            return 1;
                        })
                )
        );
    }
}