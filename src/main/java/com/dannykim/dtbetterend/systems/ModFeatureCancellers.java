package com.dannykim.dtbetterend.systems;

import com.dannykim.dtbetterend.DynamicTreesBetterEnd;
import com.dannykim.dtbetterend.worldgen.FeatureTypeCanceller;
import com.ferreusveritas.dynamictrees.api.registry.Registry;
import com.ferreusveritas.dynamictrees.api.worldgen.FeatureCanceller;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.fml.ModList;

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
                    ResourceLocation.tryBuild("betterend", "tenanea_bush"),
                    ResourceLocation.tryBuild("betterend", "umbrella_tree")
            )
    );

    public static final FeatureCanceller BETTEREND_DRAGON_HELIX_TREE = new FeatureTypeCanceller(
            DynamicTreesBetterEnd.location("betterend_dragon_helix_tree"),
            Set.of(ResourceLocation.tryBuild("betterend", "dragon_helix_tree"))
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


    public static final FeatureCanceller BETTEREND_CACTI = new FeatureTypeCanceller(
            DynamicTreesBetterEnd.location("betterend_cacti"),
            Set.of(ResourceLocation.tryBuild("betterend", "neon_cactus"))
    );

    private ModFeatureCancellers() {
    }

    public static void register(final Registry<FeatureCanceller> registry) {
        registry.register(BETTEREND_TREES);
        registry.register(BETTEREND_DRAGON_HELIX_TREE);
        if (ModList.get().isLoaded("dynamictreesplus")) {
            registry.register(BETTEREND_FUNGI);
            registry.register(BETTEREND_CACTI);
        }
    }
}
