package com.dannykim.dtbetterend.worldgen;

import com.dtteam.dynamictrees.api.worldgen.BiomePropertySelectors;
import com.dtteam.dynamictrees.api.worldgen.FeatureCanceller;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.configurations.RandomPatchConfiguration;

import java.util.Collections;
import java.util.IdentityHashMap;
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
        return matchesFeatureTree(configuredFeature, Collections.newSetFromMap(new IdentityHashMap<>()));
    }

    private boolean matchesFeatureType(final ConfiguredFeature<?, ?> configuredFeature) {
        final ResourceLocation featureType = BuiltInRegistries.FEATURE.getKey(configuredFeature.feature());
        return featureType != null && this.featureTypes.contains(featureType);
    }

    private boolean matchesFeatureTree(
            final ConfiguredFeature<?, ?> configuredFeature,
            final Set<ConfiguredFeature<?, ?>> seen
    ) {
        if (!seen.add(configuredFeature)) {
            return false;
        }
        return matchesFeatureType(configuredFeature)
                || configuredFeature.getFeatures().anyMatch(feature -> matchesFeatureTree(feature, seen))
                || matchesRandomPatchTarget(configuredFeature, seen);
    }

    private boolean matchesRandomPatchTarget(
            final ConfiguredFeature<?, ?> configuredFeature,
            final Set<ConfiguredFeature<?, ?>> seen
    ) {
        if (!(configuredFeature.config() instanceof RandomPatchConfiguration randomPatch)) {
            return false;
        }
        return randomPatch.feature().value().getFeatures().anyMatch(feature -> matchesFeatureTree(feature, seen));
    }
}
