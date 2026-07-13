package com.dannykim.dtbetterend.systems;

import com.dannykim.dtbetterend.DynamicTreesBetterEnd;
import com.dannykim.dtbetterend.worldgen.FeatureTypeCanceller;
import com.dtteam.dynamictrees.api.registry.Registry;
import com.dtteam.dynamictrees.api.worldgen.FeatureCanceller;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.fml.ModList;

import java.util.Set;

public final class ModFeatureCancellers {
    public static final FeatureCanceller BETTEREND_TREES = new FeatureTypeCanceller(
            DynamicTreesBetterEnd.location("betterend_trees"),
            Set.of(
                    ResourceLocation.fromNamespaceAndPath("betterend", "dragon_tree"),
                    ResourceLocation.fromNamespaceAndPath("betterend", "helix_tree"),
                    ResourceLocation.fromNamespaceAndPath("betterend", "lacugrove"),
                    ResourceLocation.fromNamespaceAndPath("betterend", "lucernia"),
                    ResourceLocation.fromNamespaceAndPath("betterend", "pythadendron_tree"),
                    ResourceLocation.fromNamespaceAndPath("betterend", "tenanea"),
                    ResourceLocation.fromNamespaceAndPath("betterend", "tenanea_bush"),
                    ResourceLocation.fromNamespaceAndPath("betterend", "umbrella_tree")
            )
    );

    public static final FeatureCanceller BETTEREND_FUNGI = new FeatureTypeCanceller(
            DynamicTreesBetterEnd.location("betterend_fungi"),
            Set.of(
                    ResourceLocation.fromNamespaceAndPath("betterend", "gigantic_amaranita"),
                    ResourceLocation.fromNamespaceAndPath("betterend", "jellyshroom"),
                    ResourceLocation.fromNamespaceAndPath("betterend", "large_amaranita"),
                    ResourceLocation.fromNamespaceAndPath("betterend", "mossy_glowshroom")
            )
    );

    private ModFeatureCancellers() {
    }

    public static void register(final Registry<FeatureCanceller> registry) {
        registry.register(BETTEREND_TREES);
        if (ModList.get().isLoaded("dynamictreesplus")) {
            registry.register(BETTEREND_FUNGI);
        }
    }
}
