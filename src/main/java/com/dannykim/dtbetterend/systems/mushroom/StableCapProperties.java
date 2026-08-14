package com.dannykim.dtbetterend.systems.mushroom;

import com.dtteam.dynamictrees.api.registry.TypedRegistry;
import com.dtteam.dynamictreesplus.block.mushroom.CapProperties;
import com.dtteam.dynamictreesplus.block.mushroom.DynamicCapBlock;
import com.dtteam.dynamictreesplus.block.mushroom.DynamicCapCenterBlock;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockBehaviour;

public final class StableCapProperties extends CapProperties {
    private static final Codec<CapProperties> CODEC = RecordCodecBuilder.create(instance -> instance
            .group(ResourceLocation.CODEC.fieldOf(TypedRegistry.RESOURCE_LOCATION.toString())
                    .forGetter(CapProperties::getRegistryName))
            .apply(instance, StableCapProperties::new));

    public static final TypedRegistry.EntryType<CapProperties> TYPE = new TypedRegistry.EntryType<>(CODEC);

    public StableCapProperties(final ResourceLocation name) {
        super(name);
    }

    @Override
    protected DynamicCapBlock createDynamicCap(final BlockBehaviour.Properties properties) {
        return new StableDynamicCapBlock(this, properties);
    }

    @Override
    protected DynamicCapCenterBlock createDynamicCapCenter(final BlockBehaviour.Properties properties) {
        return new StableDynamicCapCenterBlock(this, properties);
    }
}
