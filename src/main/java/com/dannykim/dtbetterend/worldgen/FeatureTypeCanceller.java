package com.dannykim.dtbetterend.worldgen;

import com.ferreusveritas.dynamictrees.api.worldgen.BiomePropertySelectors;
import com.ferreusveritas.dynamictrees.api.worldgen.FeatureCanceller;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;

import java.util.Set;

public final class FeatureTypeCanceller extends FeatureCanceller {
    private final Set<ResourceLocation> featureTypes;

    public FeatureTypeCanceller(final ResourceLocation registryName, final Set<ResourceLocation> featureTypes) {
        super(registryName);
        this.featureTypes = Set.copyOf(featureTypes);
    }

    @Override
    public boolean shouldCancel(
            final ConfiguredFeature<?, ?> configuredFeature,
            final BiomePropertySelectors.NormalFeatureCancellation cancellations
    ) {
        return configuredFeature.getFeatures().anyMatch(feature -> {
            final ResourceLocation featureType = BuiltInRegistries.FEATURE.getKey(feature.feature());
            return featureType != null && this.featureTypes.contains(featureType);
        });
    }
}
