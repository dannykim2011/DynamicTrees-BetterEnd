package com.dannykim.dtbetterend.systems;

import com.dannykim.dtbetterend.DynamicTreesBetterEnd;
import com.dannykim.dtbetterend.systems.featuregen.BaseTrunkClearanceGenFeature;
import com.dannykim.dtbetterend.systems.featuregen.BetterEndDecorationsGenFeature;
import com.dannykim.dtbetterend.systems.featuregen.FlowerVinesGenFeature;
import com.dannykim.dtbetterend.systems.featuregen.RemoveBrokenChorusGenFeature;
import com.dannykim.dtbetterend.systems.featuregen.UmbrellaTreeCanopyGenFeature;
import com.dannykim.dtbetterend.systems.featuregen.UmbrellaTreeClustersGenFeature;
import com.ferreusveritas.dynamictrees.api.registry.Registry;
import com.ferreusveritas.dynamictrees.systems.genfeature.GenFeature;

public final class ModFeatures {
    public static final GenFeature BASE_TRUNK_CLEARANCE =
            new BaseTrunkClearanceGenFeature(DynamicTreesBetterEnd.location("base_trunk_clearance"));
    public static final GenFeature FLOWER_VINES =
            new FlowerVinesGenFeature(DynamicTreesBetterEnd.location("flower_vines"));
    public static final GenFeature REMOVE_BROKEN_CHORUS =
            new RemoveBrokenChorusGenFeature(DynamicTreesBetterEnd.location("remove_broken_chorus"));
    public static final GenFeature UMBRELLA_TREE_CANOPY =
            new UmbrellaTreeCanopyGenFeature(DynamicTreesBetterEnd.location("umbrella_tree_canopy"));
    public static final GenFeature UMBRELLA_TREE_CLUSTERS =
            new UmbrellaTreeClustersGenFeature(DynamicTreesBetterEnd.location("umbrella_tree_clusters"));
    public static final GenFeature MOSSY_GLOWSHROOM_DETAILS =
            new BetterEndDecorationsGenFeature(
                    DynamicTreesBetterEnd.location("mossy_glowshroom_details"),
                    BetterEndDecorationsGenFeature.Kind.MOSSY_GLOWSHROOM
            );
    public static final GenFeature GIGANTIC_AMARANITA_DETAILS =
            new BetterEndDecorationsGenFeature(
                    DynamicTreesBetterEnd.location("amaranita_details"),
                    BetterEndDecorationsGenFeature.Kind.GIGANTIC_AMARANITA
            );

    private ModFeatures() {
    }

    public static void register(final Registry<GenFeature> registry) {
        registry.registerAll(
                BASE_TRUNK_CLEARANCE,
                FLOWER_VINES,
                REMOVE_BROKEN_CHORUS,
                UMBRELLA_TREE_CANOPY,
                UMBRELLA_TREE_CLUSTERS,
                MOSSY_GLOWSHROOM_DETAILS,
                GIGANTIC_AMARANITA_DETAILS
        );
    }
}
