package com.evandev.reliable_recipes.recipe;

import com.evandev.reliable_recipes.Constants;
import com.evandev.reliable_recipes.api.ReliableRecipesAPI;
import com.evandev.reliable_recipes.config.ModConfig;
import com.evandev.reliable_recipes.config.RecipeConfigIO;
import com.evandev.reliable_recipes.mixin.accessor.HolderSetNamedAccessor;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class TagModifier {

    public static void apply() {
        applyToRegistry(BuiltInRegistries.ITEM, "Item");
        applyToRegistry(BuiltInRegistries.BLOCK, "Block");

        if (ReliableRecipesAPI.hasItemHidingCapabilities()) {
            applyHiddenItemRules();
        }
    }

    private static <T> void applyToRegistry(Registry<T> registry, String debugName) {
        int removalCount = 0;
        List<TagRule> rules = RecipeConfigIO.loadTagRules();

        for (TagRule rule : rules) {
            try {
                switch (rule.action()) {
                    case REMOVE_ALL_TAGS -> {
                        for (T object : registry) {
                            ResourceLocation id = registry.getKey(object);
                            if (id != null && rule.itemMatcher().test(id)) {
                                removalCount += removeAllTagsFrom(registry, object);
                            }
                        }
                    }
                    case REMOVE_FROM_TAG -> {
                        for (var pair : registry.getTags().toList()) {
                            ResourceLocation tagId = pair.getFirst().location();
                            if (rule.tagMatcher().test(tagId)) {
                                var vanillaTag = pair.getSecond();
                                for (T object : registry) {
                                    ResourceLocation id = registry.getKey(object);
                                    if (id != null && rule.itemMatcher().test(id) && vanillaTag.contains(registry.wrapAsHolder(object))) {
                                        removeFromTag(vanillaTag, object);
                                        removalCount++;
                                    }
                                }
                            }
                        }
                    }
                    case CLEAR_TAG -> {
                        for (var pair : registry.getTags().toList()) {
                            ResourceLocation tagId = pair.getFirst().location();
                            if (rule.tagMatcher().test(tagId)) {
                                var vanillaTag = pair.getSecond();
                                if (vanillaTag != null && vanillaTag.size() > 0) {
                                    removalCount += vanillaTag.size();
                                    clearTag(vanillaTag);
                                }
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
            Set<Item> hiddenItems = new HashSet<>();
            Set<Block> hiddenBlocks = new HashSet<>();

            for (Item item : BuiltInRegistries.ITEM) {
                if (item != null) {
                    ItemStack defaultInstance = item.getDefaultInstance();

                    if (ReliableRecipesAPI.isItemHidden(defaultInstance, "tag:item")) {
                        hiddenItems.add(item);
                    }

                    if (ReliableRecipesAPI.isItemHidden(defaultInstance, "tag:block")) {
                        var block = Block.byItem(item);
                        if (block != net.minecraft.world.level.block.Blocks.AIR) {
                            hiddenBlocks.add(block);
                        }
                    }
                }
            }

            if (!hiddenItems.isEmpty()) {
                removalCount += removeHiddenValuesFromTags(BuiltInRegistries.ITEM, hiddenItems);
            }
            if (!hiddenBlocks.isEmpty()) {
                removalCount += removeHiddenValuesFromTags(BuiltInRegistries.BLOCK, hiddenBlocks);
            }

        } catch (Exception e) {
            Constants.LOG.error("Error processing hidden items integration", e);
        }

        if (removalCount > 0) {
            Constants.LOG.info("Hidden items integration removed {} item-tag associations.", removalCount);
        }
    }

    @SuppressWarnings("unchecked")
    private static <T> int removeHiddenValuesFromTags(Registry<T> registry, Set<T> hiddenValues) {
        int count = 0;
        List<String> ignoredTags = ModConfig.get().ignoredTags;

        for (var pair : registry.getTags().toList()) {
            ResourceLocation tagId = pair.getFirst().location();

            if (ignoredTags != null && ignoredTags.contains(tagId.toString())) {
                continue;
            }

            var tagSet = pair.getSecond();

            if (tagSet instanceof HolderSetNamedAccessor accessor) {
                List<Holder<T>> currentContents = (List<Holder<T>>) (Object) accessor.getContents();

                if (currentContents != null && !currentContents.isEmpty()) {
                    List<Holder<T>> mutableContents = new ArrayList<>(currentContents);
                    int initialSize = mutableContents.size();

                    if (mutableContents.removeIf(h -> hiddenValues.contains(h.value()))) {
                        accessor.setContents((List<Holder<?>>) (Object) mutableContents);
                        count += (initialSize - mutableContents.size());
                    }
                }
            }
        }
        return count;
    }

    private static void clearTag(Object tag) {
        if (tag instanceof HolderSetNamedAccessor accessor) {
            accessor.setContents(new ArrayList<>());
        }
    }

    @SuppressWarnings("unchecked")
    private static <T> void removeFromTag(Object tag, T value) {
        if (tag instanceof HolderSetNamedAccessor accessor) {
            List<Holder<T>> currentContents = (List<Holder<T>>) (Object) accessor.getContents();
            if (currentContents != null) {
                List<Holder<T>> mutableContents = new ArrayList<>(currentContents);
                if (mutableContents.removeIf(holder -> holder.value() == value)) {
                    accessor.setContents((List<Holder<?>>) (Object) mutableContents);
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