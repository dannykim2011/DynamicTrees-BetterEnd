package com.dannykim.dtbetterend.worldgen;

import com.dtteam.dynamictrees.api.worldgen.BiomePropertySelectors;
import com.dtteam.dynamictrees.api.worldgen.FeatureCanceller;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;

public final class FeatureBlockCanceller extends FeatureCanceller {
    private final Set<String> blockIds;

    public FeatureBlockCanceller(final Identifier registryName, final Set<String> blockIds) {
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
                || configuredFeature.getSubFeatures().anyMatch(feature -> matchesFeatureTree(feature.value(), seen));
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

}
