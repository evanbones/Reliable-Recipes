package com.evandev.reliable_recipes.mixin.accessor;

import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.tags.TagKey;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Set;

@Mixin(Holder.Reference.class)
public interface HolderReferenceAccessor {
    @Accessor("tags")
    Set<TagKey<?>> getTags();

    @Accessor("tags")
    @Mutable
    void setTags(Set<TagKey<?>> tags);

    @Accessor("components")
    DataComponentMap getComponents();

    @Accessor("components")
    @Mutable
    void setComponents(DataComponentMap components);
}