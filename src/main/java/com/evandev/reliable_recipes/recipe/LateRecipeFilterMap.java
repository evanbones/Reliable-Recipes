package com.evandev.reliable_recipes.recipe;

import com.google.gson.JsonElement;
import net.minecraft.resources.Identifier;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiPredicate;

public class LateRecipeFilterMap extends HashMap<Identifier, JsonElement> {
    private static final StackWalker STACK_WALKER = StackWalker.getInstance();
    private final BiPredicate<Identifier, JsonElement> filter;

    public LateRecipeFilterMap(Map<Identifier, JsonElement> recipes, BiPredicate<Identifier, JsonElement> filter) {
        super(recipes);
        this.filter = filter;
    }

    @Override
    public JsonElement put(Identifier id, JsonElement json) {
        return isScriptAddition() || filter.test(id, json) ? super.put(id, json) : get(id);
    }

    @Override
    public void putAll(Map<? extends Identifier, ? extends JsonElement> recipes) {
        recipes.forEach(this::put);
    }

    @Override
    public JsonElement putIfAbsent(Identifier id, JsonElement json) {
        JsonElement existing = get(id);
        return existing != null ? existing : put(id, json);
    }

    private static boolean isScriptAddition() {
        return STACK_WALKER.walk(frames -> frames.anyMatch(frame -> frame.getClassName().startsWith("dev.latvian.mods.kubejs.")));
    }
}
