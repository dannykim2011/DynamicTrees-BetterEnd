package com.dannykim.dtbetterend.worldgen;

import com.dtteam.dynamictrees.api.worldgen.BiomePropertySelectors;
import com.dtteam.dynamictrees.api.worldgen.FeatureCanceller;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.configurations.RandomPatchConfiguration;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;

public final class FeatureBlockCanceller extends FeatureCanceller {
    private final Set<String> blockIds;

    public FeatureBlockCanceller(final ResourceLocation registryName, final Set<String> blockIds) {
        super(registryName);
        this.blockIds = Set.copyOf(blockIds);
    }

    @Override
    public boolean shouldCancel(
            final ConfiguredFeature<?, ?> configuredFeature,
            final BiomePropertySelectors.NormalFeatureCancellation cancellations
    ) {
        return matchesFeatureTree(configuredFeature, Collections.newSetFromMap(new IdentityHashMap<>()));
    }

    private boolean matchesFeatureTree(
            final ConfiguredFeature<?, ?> configuredFeature,
            final Set<ConfiguredFeature<?, ?>> seen
    ) {
        if (!seen.add(configuredFeature)) {
            return false;
        }
        return matchesOwnConfig(configuredFeature)
                || configuredFeature.getFeatures().anyMatch(feature -> matchesFeatureTree(feature, seen))
                || matchesRandomPatchTarget(configuredFeature, seen);
    }

    private boolean matchesOwnConfig(final ConfiguredFeature<?, ?> configuredFeature) {
        final String config = configuredFeature.config().toString();
        for (final String blockId : this.blockIds) {
            if (config.contains(blockId)) {
                return true;
            }
        }
        return false;
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
