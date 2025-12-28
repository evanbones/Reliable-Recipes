package com.evandev.reliable_recipes.recipe;

import com.evandev.reliable_recipes.Constants;
import com.evandev.reliable_recipes.config.RecipeConfigIO;
import com.evandev.reliable_recipes.mixin.accessor.HolderSetNamedAccessor;
import com.evandev.reliable_recipes.platform.Services;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

import java.util.ArrayList;
import java.util.List;

public class TagModifier {

    public static void apply() {
        applyToRegistry(BuiltInRegistries.ITEM, "Item");
        applyToRegistry(BuiltInRegistries.BLOCK, "Block");

        if (Services.PLATFORM.hasItemHidingCapabilities()) {
            applyHiddenItemRules();
        }
    }

    /**
     * Generic method to apply tag rules to any registry (Items, Blocks, etc.)
     */
    private static <T> void applyToRegistry(Registry<T> registry, String debugName) {
        int removalCount = 0;
        List<TagRule> rules = RecipeConfigIO.loadTagRules();

        for (TagRule rule : rules) {
            try {
                switch (rule.action()) {
                    case REMOVE_ALL_TAGS -> {
                        if (rule.items() == null) continue;
                        for (ResourceLocation id : rule.items()) {
                            T object = registry.get(id);
                            if (object != null) {
                                removalCount += removeAllTagsFrom(registry, object);
                            }
                        }
                    }
                    case REMOVE_FROM_TAG -> {
                        if (rule.tags() == null || rule.items() == null) continue;
                        for (ResourceLocation tagId : rule.tags()) {
                            TagKey<T> key = TagKey.create(registry.key(), tagId);
                            var vanillaTag = registry.getTag(key).orElse(null);

                            for (ResourceLocation id : rule.items()) {
                                T object = registry.get(id);
                                if (object != null && vanillaTag != null && vanillaTag.contains(registry.wrapAsHolder(object))) {
                                    removeFromTag(vanillaTag, object);
                                    removalCount++;
                                }
                            }
                        }
                    }
                    case CLEAR_TAG -> {
                        if (rule.tags() == null) continue;
                        for (ResourceLocation tagId : rule.tags()) {
                            TagKey<T> key = TagKey.create(registry.key(), tagId);
                            var vanillaTag = registry.getTag(key).orElse(null);

                            if (vanillaTag != null && vanillaTag.size() > 0) {
                                Constants.LOG.info("TagModifier: Clearing tag '{}' (contained {} items)", tagId, vanillaTag.size());
                                removalCount += vanillaTag.size();
                                clearTag(vanillaTag);
                            }
                        }
                    }
                }
            } catch (Exception e) {
                Constants.LOG.error("Error processing {} tag rule: {}", debugName, rule, e);
            }
        }

        if (removalCount > 0) {
            Constants.LOG.info("TagModifier removed {} {}-tag associations.", removalCount, debugName);
        }
    }

    private static void applyHiddenItemRules() {
        int removalCount = 0;
        try {
            for (Item item : BuiltInRegistries.ITEM) {
                if (item != null && Services.PLATFORM.isItemHidden(item.getDefaultInstance())) {

                    // Remove tags from the item itself
                    removalCount += removeAllTagsFrom(BuiltInRegistries.ITEM, item);

                    // Check if it's a BlockItem and remove tags from the Block as well
                    var block = net.minecraft.world.level.block.Block.byItem(item);
                    if (block != net.minecraft.world.level.block.Blocks.AIR) {
                        removalCount += removeAllTagsFrom(BuiltInRegistries.BLOCK, block);
                    }
                }
            }
        } catch (Exception e) {
            Constants.LOG.error("Error processing hidden items integration", e);
        }

        if (removalCount > 0) {
            Constants.LOG.info("Hidden items integration removed {} item-tag associations.", removalCount);
        }
    }

    private static void clearTag(Object tag) {
        if (tag instanceof HolderSetNamedAccessor accessor) {
            accessor.setContents(new ArrayList<>());
        } else {
            Constants.LOG.debug("TagModifier: Tag object {} is not HolderSet.Named", tag.getClass().getSimpleName());
        }
    }

    private static <T> void removeFromTag(Object tag, T value) {
        if (tag instanceof HolderSetNamedAccessor accessor) {
            List<Holder<?>> currentContents = accessor.getContents();
            if (currentContents != null) {
                List<Holder<?>> mutableContents = new ArrayList<>(currentContents);
                if (mutableContents.removeIf(holder -> holder.value() == value)) {
                    accessor.setContents(mutableContents);
                }
            }
        }
    }

    private static <T> int removeAllTagsFrom(Registry<T> registry, T value) {
        if (value == null) return 0;
        int count = 0;

        var holder = registry.wrapAsHolder(value);
        var tags = holder.tags().toList();

        for (TagKey<T> key : tags) {
            var vanillaTag = registry.getTag(key).orElse(null);
            if (vanillaTag != null) {
                removeFromTag(vanillaTag, value);
                count++;
            }
        }
        return count;
    }
}