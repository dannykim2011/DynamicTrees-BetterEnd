package com.dannykim.dtbetterend.systems;

import com.dannykim.dtbetterend.DynamicTreesBetterEnd;
import com.dannykim.dtbetterend.systems.featuregen.BetterEndDecorationsGenFeature;
import com.dannykim.dtbetterend.systems.featuregen.FlowerVinesGenFeature;
import com.dannykim.dtbetterend.systems.featuregen.RemoveBrokenChorusGenFeature;
import com.dtteam.dynamictrees.api.registry.Registry;
import com.dtteam.dynamictrees.systems.genfeature.GenFeature;

public final class ModFeatures {
    public static final GenFeature FLOWER_VINES =
            new FlowerVinesGenFeature(DynamicTreesBetterEnd.location("flower_vines"));
    public static final GenFeature REMOVE_BROKEN_CHORUS =
            new RemoveBrokenChorusGenFeature(DynamicTreesBetterEnd.location("remove_broken_chorus"));
    public static final GenFeature MOSSY_GLOWSHROOM_DETAILS =
            new BetterEndDecorationsGenFeature(
                    DynamicTreesBetterEnd.location("mossy_glowshroom_details"),
                    BetterEndDecorationsGenFeature.Kind.MOSSY_GLOWSHROOM
            );
    public static final GenFeature GIGANTIC_AMARANITA_DETAILS =
            new BetterEndDecorationsGenFeature(
                    DynamicTreesBetterEnd.location("gigantic_amaranita_details"),
                    BetterEndDecorationsGenFeature.Kind.GIGANTIC_AMARANITA
            );

    private ModFeatures() {
    }

    public static void register(final Registry<GenFeature> registry) {
        registry.registerAll(
                FLOWER_VINES,
                REMOVE_BROKEN_CHORUS,
                MOSSY_GLOWSHROOM_DETAILS,
                GIGANTIC_AMARANITA_DETAILS
        );
    }
}
