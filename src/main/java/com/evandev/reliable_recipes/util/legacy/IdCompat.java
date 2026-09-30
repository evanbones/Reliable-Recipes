package com.evandev.reliable_recipes.util.legacy;

//? if <1.21 {
/*import net.minecraft.resources.Identifier;

public final class IdCompat {
    private IdCompat() {
    }

    public static Identifier fromNamespaceAndPath(String namespace, String path) {
        return new Identifier(namespace, path);
    }

    public static Identifier parse(String id) {
        return new Identifier(id);
    }

    public static Identifier withDefaultNamespace(String path) {
        return new Identifier(Identifier.DEFAULT_NAMESPACE, path);
    }
}
*///?}
