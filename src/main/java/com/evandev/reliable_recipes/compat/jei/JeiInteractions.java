package com.evandev.reliable_recipes.compat.jei;

import com.evandev.reliable_recipes.config.ModConfig;
import com.evandev.reliable_recipes.platform.Services;
import com.evandev.reliable_recipes.util.CompatUtil;
import mezz.jei.api.gui.IRecipeLayoutDrawable;
import mezz.jei.api.runtime.IRecipesGui;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
//? if >=1.21.2 {
import net.minecraft.server.permissions.Permissions;
//?}

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;

public class JeiInteractions {
    private static final int MAX_SEARCH_DEPTH = 3;

    /**
     * Deletes the recipe under the mouse when the screen is JEI's recipe screen.
     *
     * @return true if a deletion was requested.
     */
    public static boolean tryDeleteHoveredRecipe() {
        Minecraft mc = Minecraft.getInstance();
        //? if >=26.2 {
        Screen screen = mc.gui.screen();
        //?} else {
        /*Screen screen = mc.screen;
        *///?}
        if (!(screen instanceof IRecipesGui)) return false;

        double mouseX = mc.mouseHandler.xpos() * (double) mc.getWindow().getGuiScaledWidth() / (double) mc.getWindow().getScreenWidth();
        double mouseY = mc.mouseHandler.ypos() * (double) mc.getWindow().getGuiScaledHeight() / (double) mc.getWindow().getScreenHeight();

        IRecipeLayoutDrawable<?> hovered = findHoveredLayout(screen, mouseX, mouseY);
        if (hovered == null) return false;

        Identifier id = getRecipeId(hovered);
        if (id == null) {
            if (mc.player != null) {
                mc.player.sendSystemMessage(Component.translatable("chat.reliable_recipes.no_id_dynamically"));
            }
            return false;
        }

        if (requestDeletion(unwrapViewerId(id))) {
            screen.onClose();
            return true;
        }
        return false;
    }

    public static boolean requestDeletion(Identifier id) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return false;

        //? if <1.21.2 {
        /*boolean hasPermission = mc.player.hasPermissions(2);
        *///?} else {
        boolean hasPermission = mc.player.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER);
        //?}
        if (!hasPermission) {
            mc.player.sendSystemMessage(Component.translatable("toast.reliable_recipes.permission_denied"));
            return false;
        }

        if (!ModConfig.get().devMode) {
            mc.player.sendSystemMessage(Component.translatable("toast.reliable_recipes.dev_mode"));
            return false;
        }

        if (!Services.PLATFORM.canSendToServer()) {
            mc.player.sendSystemMessage(Component.translatable("toast.reliable_recipes.server_missing"));
            return false;
        }

        Services.PLATFORM.sendDeleteRecipePacket(CompatUtil.recipeKey(id));
        return true;
    }

    public static ItemStack onRecipeRemoved(Identifier recipeId) {
        return ReliableRecipesJeiPlugin.hideDeletedRecipe(recipeId);
    }

    public static void onRecipeAdded(Identifier recipeId) {
        ReliableRecipesJeiPlugin.restoreDeletedRecipe(recipeId);
    }

    public static void refresh() {
        ReliableRecipesJeiPlugin.refresh();
    }

    private static <T> Identifier getRecipeId(IRecipeLayoutDrawable<T> layout) {
        return ReliableRecipesJeiPlugin.getRecipeId(layout.getRecipeCategory(), layout.getRecipe());
    }

    // Converts "jei:/modid/path/to/recipe" -> "modid:path/to/recipe"
    private static Identifier unwrapViewerId(Identifier id) {
        if (!id.getNamespace().equals("jei")) return id;

        String path = id.getPath();
        if (path.startsWith("/")) {
            path = path.substring(1);
        }

        int splitIndex = path.indexOf('/');
        if (splitIndex > 0) {
            try {
                return Identifier.fromNamespaceAndPath(path.substring(0, splitIndex), path.substring(splitIndex + 1));
            } catch (Exception ignored) {
            }
        }
        return id;
    }

    private static IRecipeLayoutDrawable<?> findHoveredLayout(Screen screen, double mouseX, double mouseY) {
        List<IRecipeLayoutDrawable<?>> layouts = new ArrayList<>();
        collectLayouts(screen, 0, Collections.newSetFromMap(new IdentityHashMap<>()), layouts);

        for (IRecipeLayoutDrawable<?> layout : layouts) {
            if (layout.isMouseOver(mouseX, mouseY)) {
                return layout;
            }
        }
        return null;
    }

    private static void collectLayouts(Object owner, int depth, Set<Object> visited, List<IRecipeLayoutDrawable<?>> layouts) {
        if (owner == null || depth > MAX_SEARCH_DEPTH || !visited.add(owner)) return;

        for (Class<?> type = owner.getClass(); type != null && isJeiClass(type); type = type.getSuperclass()) {
            for (Field field : type.getDeclaredFields()) {
                if (Modifier.isStatic(field.getModifiers()) || field.getType().isPrimitive()) continue;

                Object value;
                try {
                    field.setAccessible(true);
                    value = field.get(owner);
                } catch (Exception e) {
                    continue;
                }

                if (value instanceof Collection<?> collection) {
                    for (Object element : collection) {
                        IRecipeLayoutDrawable<?> layout = asLayout(element);
                        if (layout != null) layouts.add(layout);
                    }
                } else if (value instanceof IRecipeLayoutDrawable<?> layout) {
                    layouts.add(layout);
                } else if (value != null && isJeiClass(value.getClass())) {
                    collectLayouts(value, depth + 1, visited, layouts);
                }
            }
        }
    }

    private static IRecipeLayoutDrawable<?> asLayout(Object element) {
        if (element instanceof IRecipeLayoutDrawable<?> layout) return layout;
        if (element == null || !isJeiClass(element.getClass())) return null;

        for (Method method : element.getClass().getMethods()) {
            if (method.getParameterCount() == 0 && IRecipeLayoutDrawable.class.isAssignableFrom(method.getReturnType())) {
                try {
                    return (IRecipeLayoutDrawable<?>) method.invoke(element);
                } catch (Exception ignored) {
                }
            }
        }
        return null;
    }

    private static boolean isJeiClass(Class<?> type) {
        return type.getName().startsWith("mezz.jei.");
    }
}
