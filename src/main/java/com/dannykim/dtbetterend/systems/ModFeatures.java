package com.dannykim.dtbetterend.systems;

import com.dannykim.dtbetterend.DynamicTreesBetterEnd;
import com.dannykim.dtbetterend.systems.featuregen.FlowerVinesGenFeature;
import com.dannykim.dtbetterend.systems.featuregen.RemoveBrokenChorusGenFeature;
import com.dtteam.dynamictrees.api.registry.Registry;
import com.dtteam.dynamictrees.systems.genfeature.GenFeature;

public final class ModFeatures {
    public static final GenFeature FLOWER_VINES =
            new FlowerVinesGenFeature(DynamicTreesBetterEnd.location("flower_vines"));
    public static final GenFeature REMOVE_BROKEN_CHORUS =
            new RemoveBrokenChorusGenFeature(DynamicTreesBetterEnd.location("remove_broken_chorus"));

    private ModFeatures() {
    }

    public static void register(final Registry<GenFeature> registry) {
        registry.registerAll(FLOWER_VINES, REMOVE_BROKEN_CHORUS);
    }
}
