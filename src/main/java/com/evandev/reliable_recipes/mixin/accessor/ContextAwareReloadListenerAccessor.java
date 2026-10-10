package com.evandev.reliable_recipes.mixin.accessor;

//? if neoforge {
/*import com.google.gson.JsonElement;
import net.neoforged.neoforge.common.conditions.ConditionalOps;
import net.neoforged.neoforge.resource.ContextAwareReloadListener;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(ContextAwareReloadListener.class)
public interface ContextAwareReloadListenerAccessor {
    @Invoker("makeConditionalOps")
    ConditionalOps<JsonElement> reliableRecipes$makeConditionalOps();
}
*///?}
