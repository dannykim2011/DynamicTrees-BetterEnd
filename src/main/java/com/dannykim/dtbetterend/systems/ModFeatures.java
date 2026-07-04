package com.dannykim.dtbetterend.systems;

import com.dannykim.dtbetterend.DynamicTreesBetterEnd;
import com.dannykim.dtbetterend.systems.featuregen.FlowerVinesGenFeature;
import com.ferreusveritas.dynamictrees.api.registry.Registry;
import com.ferreusveritas.dynamictrees.systems.genfeature.GenFeature;

public final class ModFeatures {
    public static final GenFeature FLOWER_VINES =
            new FlowerVinesGenFeature(DynamicTreesBetterEnd.location("flower_vines"));

    private ModFeatures() {
    }

    public static void register(final Registry<GenFeature> registry) {
        registry.registerAll(FLOWER_VINES);
    }
}
