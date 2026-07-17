package com.dannykim.dtbetterend.systems;

import com.dannykim.dtbetterend.DynamicTreesBetterEnd;
import com.dannykim.dtbetterend.worldgen.FeatureTypeCanceller;
import com.dtteam.dynamictrees.api.registry.Registry;
import com.dtteam.dynamictrees.api.worldgen.FeatureCanceller;
import net.minecraft.resources.Identifier;
import net.neoforged.fml.ModList;

import java.util.Set;

public final class ModFeatureCancellers {
    public static final FeatureCanceller BETTEREND_TREES = new FeatureTypeCanceller(
            DynamicTreesBetterEnd.location("betterend_trees"),
            Set.of(
                    Identifier.fromNamespaceAndPath("betterend", "dragon_tree"),
                    Identifier.fromNamespaceAndPath("betterend", "helix_tree"),
                    Identifier.fromNamespaceAndPath("betterend", "lacugrove"),
                    Identifier.fromNamespaceAndPath("betterend", "lucernia"),
                    Identifier.fromNamespaceAndPath("betterend", "pythadendron_tree"),
                    Identifier.fromNamespaceAndPath("betterend", "tenanea"),
                    Identifier.fromNamespaceAndPath("betterend", "tenanea_bush"),
                    Identifier.fromNamespaceAndPath("betterend", "umbrella_tree")
            )
    );

    public static final FeatureCanceller BETTEREND_FUNGI = new FeatureTypeCanceller(
            DynamicTreesBetterEnd.location("betterend_fungi"),
            Set.of(
                    Identifier.fromNamespaceAndPath("betterend", "gigantic_amaranita"),
                    Identifier.fromNamespaceAndPath("betterend", "jellyshroom"),
                    Identifier.fromNamespaceAndPath("betterend", "large_amaranita"),
                    Identifier.fromNamespaceAndPath("betterend", "mossy_glowshroom")
            )
    );


    public static final FeatureCanceller BETTEREND_CACTI = new FeatureTypeCanceller(
            DynamicTreesBetterEnd.location("betterend_cacti"),
            Set.of(Identifier.fromNamespaceAndPath("betterend", "neon_cactus"))
    );

    private ModFeatureCancellers() {
    }

    public static void register(final Registry<FeatureCanceller> registry) {
        registry.register(BETTEREND_TREES);
        if (ModList.get().isLoaded("dynamictreesplus")) {
            registry.register(BETTEREND_FUNGI);
            registry.register(BETTEREND_CACTI);
        }
    }
}
