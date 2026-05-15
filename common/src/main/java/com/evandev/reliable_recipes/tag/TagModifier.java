package com.evandev.reliable_recipes.tag;

import com.evandev.reliable_recipes.Constants;
import com.evandev.reliable_recipes.api.ReliableRecipesAPI;
import com.evandev.reliable_recipes.config.ModConfig;
import com.evandev.reliable_recipes.config.RecipeConfigIO;
import com.evandev.reliable_recipes.mixin.accessor.HolderReferenceAccessor;
import com.evandev.reliable_recipes.mixin.accessor.HolderSetNamedAccessor;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

import java.util.*;

public class TagModifier {

    public static void apply() {
        applyToRegistry(BuiltInRegistries.ITEM, "Item");
        applyToRegistry(BuiltInRegistries.BLOCK, "Block");

        if (ReliableRecipesAPI.hasItemHidingCapabilities()) {
            applyHiddenItemRules();
        }
    }

    private static <T> void applyToRegistry(Registry<T> registry, String debugName) {
        List<TagRule> rules = RecipeConfigIO.loadTagRules();
        if (rules.isEmpty()) return;

        List<TagRule> removeAllRules = new ArrayList<>();
        List<TagRule> removeFromTagRules = new ArrayList<>();
        List<TagRule> clearTagRules = new ArrayList<>();

        for (TagRule rule : rules) {
            switch (rule.action()) {
                case REMOVE_ALL_TAGS -> removeAllRules.add(rule);
                case REMOVE_FROM_TAG -> removeFromTagRules.add(rule);
                case CLEAR_TAG -> clearTagRules.add(rule);
            }
        }

        int removalCount = 0;

        if (!clearTagRules.isEmpty()) {
            for (var pair : registry.getTags().toList()) {
                ResourceLocation tagId = pair.getFirst().location();
                for (TagRule rule : clearTagRules) {
                    if (rule.tagMatcher().test(tagId)) {
                        clearTag(pair.getSecond());
                        break;
                    }
                }
            }
        }

        if (!removeAllRules.isEmpty() || !removeFromTagRules.isEmpty()) {
            Map<Object, Set<T>> batchedRemovals = new HashMap<>();

            for (T object : registry) {
                ResourceLocation id = registry.getKey(object);
                if (id == null) continue;

                boolean shouldRemoveAll = false;
                for (TagRule rule : removeAllRules) {
                    if (rule.itemMatcher().test(id)) {
                        shouldRemoveAll = true;
                        break;
                    }
                }

                var holder = registry.wrapAsHolder(object);
                var tags = holder.tags().toList();

                if (shouldRemoveAll) {
                    for (TagKey<T> key : tags) {
                        registry.getTag(key).ifPresent(tag ->
                                batchedRemovals.computeIfAbsent(tag, k -> new HashSet<>()).add(object)
                        );
                    }
                    continue;
                }

                for (TagKey<T> key : tags) {
                    ResourceLocation tagId = key.location();
                    for (TagRule rule : removeFromTagRules) {
                        if (rule.itemMatcher().test(id) && rule.tagMatcher().test(tagId)) {
                            registry.getTag(key).ifPresent(tag ->
                                    batchedRemovals.computeIfAbsent(tag, k -> new HashSet<>()).add(object)
                            );
                            break;
                        }
                    }
                }
            }

            for (Map.Entry<Object, Set<T>> entry : batchedRemovals.entrySet()) {
                removalCount += batchRemoveFromTag(entry.getKey(), entry.getValue());
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

    private static <T> int removeHiddenValuesFromTags(Registry<T> registry, Set<T> hiddenValues) {
        List<String> ignoredTags = ModConfig.get().ignoredTags;
        Map<Object, Set<T>> batchedRemovals = new HashMap<>();

        for (T hiddenValue : hiddenValues) {
            var holder = registry.wrapAsHolder(hiddenValue);
            for (TagKey<T> tagKey : holder.tags().toList()) {
                if (ignoredTags != null && ignoredTags.contains(tagKey.location().toString())) {
                    continue;
                }
                registry.getTag(tagKey).ifPresent(tag ->
                        batchedRemovals.computeIfAbsent(tag, k -> new HashSet<>()).add(hiddenValue)
                );
            }
        }

        int count = 0;
        for (Map.Entry<Object, Set<T>> entry : batchedRemovals.entrySet()) {
            count += batchRemoveFromTag(entry.getKey(), entry.getValue());
        }
        return count;
    }

    @SuppressWarnings("unchecked")
    private static <T> int batchRemoveFromTag(Object tag, Set<T> valuesToRemove) {
        if (valuesToRemove.isEmpty() || !(tag instanceof HolderSet.Named<?> namedTag) || !(tag instanceof HolderSetNamedAccessor accessor)) {
            return 0;
        }

        List<Holder<T>> currentContents = (List<Holder<T>>) (Object) accessor.getContents();
        if (currentContents == null) return 0;

        List<Holder<T>> mutableContents = new ArrayList<>(currentContents);
        List<Holder<T>> toRemove = new ArrayList<>();

        for (Holder<T> holder : mutableContents) {
            if (valuesToRemove.contains(holder.value())) {
                toRemove.add(holder);
            }
        }

        if (!toRemove.isEmpty()) {
            mutableContents.removeAll(toRemove);
            accessor.setContents((List<Holder<?>>) (Object) mutableContents);

            TagKey<?> tagKey = namedTag.key();
            for (Holder<T> holder : toRemove) {
                if (holder instanceof Holder.Reference<?> ref && ref instanceof HolderReferenceAccessor refAccessor) {
                    Set<TagKey<?>> itemTags = new HashSet<>(refAccessor.getTags());
                    itemTags.remove(tagKey);
                    refAccessor.setTags(Set.copyOf(itemTags));
                }
            }
            return toRemove.size();
        }
        return 0;
    }

    private static void clearTag(Object tag) {
        if (tag instanceof HolderSet.Named<?> namedTag && tag instanceof HolderSetNamedAccessor accessor) {
            List<Holder<?>> contents = accessor.getContents();
            if (contents != null) {
                TagKey<?> tagKey = namedTag.key();
                for (Holder<?> holder : contents) {
                    if (holder instanceof Holder.Reference<?> ref && ref instanceof HolderReferenceAccessor refAccessor) {
                        Set<TagKey<?>> itemTags = new HashSet<>(refAccessor.getTags());
                        itemTags.remove(tagKey);
                        refAccessor.setTags(Set.copyOf(itemTags));
                    }
                }
            }
            accessor.setContents(new ArrayList<>());
        }
    }
}