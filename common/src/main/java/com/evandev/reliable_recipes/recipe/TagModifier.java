package com.evandev.reliable_recipes.recipe;

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
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public class TagModifier {

    public static void apply() {
        applyToRegistry(BuiltInRegistries.ITEM, "Item");
        applyToRegistry(BuiltInRegistries.BLOCK, "Block");

        if (ReliableRecipesAPI.hasItemHidingCapabilities()) {
            applyHiddenItemRules();
        }
    }

    private static <T> void applyToRegistry(Registry<T> registry, String debugName) {
        AtomicInteger removalCount = new AtomicInteger();
        List<TagRule> rules = RecipeConfigIO.loadTagRules();
        Map<Object, Set<T>> batchedRemovals = new HashMap<>();

        for (TagRule rule : rules) {
            try {
                switch (rule.action()) {
                    case REMOVE_ALL_TAGS -> {
                        if (rule.items() == null) continue;
                        for (Identifier id : rule.items()) {
                            T object = registry.get(id).map(Holder.Reference::value).orElse(null);
                            if (object != null) {
                                var holder = registry.wrapAsHolder(object);
                                for (TagKey<T> key : holder.tags().toList()) {
                                    registry.get(key).ifPresent(tag ->
                                            batchedRemovals.computeIfAbsent(tag, _ -> new HashSet<>()).add(object)
                                    );
                                }
                            }
                        }
                    }
                    case REMOVE_FROM_TAG -> {
                        if (rule.tags() == null || rule.items() == null) continue;
                        for (Identifier tagId : rule.tags()) {
                            TagKey<T> key = TagKey.create(registry.key(), tagId);
                            registry.get(key).ifPresent(vanillaTag -> {
                                for (Identifier id : rule.items()) {
                                    registry.get(id).map(Holder.Reference::value).ifPresent(object -> batchedRemovals.computeIfAbsent(vanillaTag, _ -> new HashSet<>()).add(object));
                                }
                            });
                        }
                    }
                    case CLEAR_TAG -> {
                        if (rule.tags() == null) continue;
                        for (Identifier tagId : rule.tags()) {
                            TagKey<T> key = TagKey.create(registry.key(), tagId);
                            registry.get(key).ifPresent(vanillaTag -> {
                                int size = vanillaTag.size();
                                if (size > 0) {
                                    Constants.LOG.info("TagModifier: Clearing tag '{}' (contained {} items)", tagId, size);
                                    removalCount.addAndGet(size);
                                    clearTag(vanillaTag);
                                }
                            });
                        }
                    }
                }
            } catch (Exception e) {
                Constants.LOG.error("Error processing {} tag rule: {}", debugName, rule, e);
            }
        }

        for (Map.Entry<Object, Set<T>> entry : batchedRemovals.entrySet()) {
            removalCount.addAndGet(batchRemoveFromTag(entry.getKey(), entry.getValue()));
        }

        if (removalCount.get() > 0) {
            Constants.LOG.info("TagModifier removed {} {}-tag associations.", removalCount, debugName);
        }
    }

    private static void applyHiddenItemRules() {
        int removalCount = 0;
        try {
            Set<Item> hiddenItems = new HashSet<>();
            Set<Block> hiddenBlocks = new HashSet<>();

            for (Item item : BuiltInRegistries.ITEM) {
                ItemStack defaultInstance = item.getDefaultInstance();

                if (ReliableRecipesAPI.isItemHidden(defaultInstance)) {
                    hiddenItems.add(item);

                    var block = Block.byItem(item);
                    if (block != net.minecraft.world.level.block.Blocks.AIR) {
                        hiddenBlocks.add(block);
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
                registry.get(tagKey).ifPresent(tag ->
                        batchedRemovals.computeIfAbsent(tag, _ -> new HashSet<>()).add(hiddenValue)
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