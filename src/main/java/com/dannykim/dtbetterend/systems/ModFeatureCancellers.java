package com.dannykim.dtbetterend.systems;

import com.dannykim.dtbetterend.DynamicTreesBetterEnd;
import com.dannykim.dtbetterend.worldgen.FeatureTypeCanceller;
import com.ferreusveritas.dynamictrees.api.registry.Registry;
import com.ferreusveritas.dynamictrees.api.worldgen.FeatureCanceller;
import net.minecraft.resources.ResourceLocation;

import java.util.Set;

public final class ModFeatureCancellers {
    public static final FeatureCanceller BETTEREND_TREES = new FeatureTypeCanceller(
            DynamicTreesBetterEnd.location("betterend_trees"),
            Set.of(
                    ResourceLocation.tryBuild("betterend", "dragon_tree"),
                    ResourceLocation.tryBuild("betterend", "helix_tree"),
                    ResourceLocation.tryBuild("betterend", "lacugrove"),
                    ResourceLocation.tryBuild("betterend", "lucernia"),
                    ResourceLocation.tryBuild("betterend", "pythadendron_tree"),
                    ResourceLocation.tryBuild("betterend", "tenanea"),
                    ResourceLocation.tryBuild("betterend", "umbrella_tree")
            )
    );

    public static final FeatureCanceller BETTEREND_FUNGI = new FeatureTypeCanceller(
            DynamicTreesBetterEnd.location("betterend_fungi"),
            Set.of(
                    ResourceLocation.tryBuild("betterend", "gigantic_amaranita"),
                    ResourceLocation.tryBuild("betterend", "jellyshroom"),
                    ResourceLocation.tryBuild("betterend", "large_amaranita"),
                    ResourceLocation.tryBuild("betterend", "mossy_glowshroom")
            )
    );

    private ModFeatureCancellers() {
    }

    public static void register(final Registry<FeatureCanceller> registry) {
        registry.registerAll(BETTEREND_TREES, BETTEREND_FUNGI);
    }
}
