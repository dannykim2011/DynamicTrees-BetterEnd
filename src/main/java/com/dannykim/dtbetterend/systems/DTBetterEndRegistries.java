package com.dannykim.dtbetterend.systems;

import com.dannykim.dtbetterend.DynamicTreesBetterEnd;
import com.dannykim.dtbetterend.systems.leaves.FurOuterLeaveProperties;
import com.dannykim.dtbetterend.systems.leaves.UmbrellaLeavesProperties;
import com.dannykim.dtbetterend.systems.umbrella.UmbrellaTreeFamily;
import com.dannykim.dtbetterend.systems.umbrella.UmbrellaTreeSpecies;
import com.dtteam.dynamictrees.event.ApplierRegistryEvent;
import com.dtteam.dynamictrees.event.RegistryEvent;
import com.dtteam.dynamictrees.event.TypeRegistryEvent;
import com.dtteam.dynamictrees.block.leaves.LeavesProperties;
import com.dtteam.dynamictrees.block.soil.SoilProperties;
import com.dtteam.dynamictrees.data.GatherDataHelper;
import com.dtteam.dynamictrees.deserialization.PropertyAppliers;
import com.dtteam.dynamictrees.tree.family.Family;
import com.dtteam.dynamictrees.tree.species.Species;
import com.dtteam.dynamictrees.treepack.Resources;
import com.dtteam.dynamictrees.systems.growthlogic.GrowthLogicKit;
import com.dtteam.dynamictrees.systems.genfeature.GenFeature;
import com.dtteam.dynamictrees.api.worldgen.FeatureCanceller;
import com.google.gson.JsonElement;
import net.minecraft.world.level.block.Block;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.data.event.GatherDataEvent;
public final class DTBetterEndRegistries {
    private DTBetterEndRegistries() {
    }

    @SubscribeEvent
    public static void registerGrowthLogicKits(final RegistryEvent<GrowthLogicKit> event) {
        if (!event.isEntryOfType(GrowthLogicKit.class)) return;
        ModGrowthLogicKits.register(event.getRegistry());
    }

    @SubscribeEvent
    public static void registerGenFeatures(final RegistryEvent<GenFeature> event) {
        if (!event.isEntryOfType(GenFeature.class)) return;
        ModFeatures.register(event.getRegistry());
    }

    @SubscribeEvent
    public static void registerFeatureCancellers(final RegistryEvent<FeatureCanceller> event) {
        if (!event.isEntryOfType(FeatureCanceller.class)) return;
        ModFeatureCancellers.register(event.getRegistry());
    }

    @SubscribeEvent
    public static void gatherData(final GatherDataEvent event) {
        Resources.MANAGER.gatherData();
        GatherDataHelper.gatherAllData(DynamicTreesBetterEnd.MOD_ID, event,
                SoilProperties.REGISTRY, Family.REGISTRY, Species.REGISTRY,
                LeavesProperties.REGISTRY);
    }

    @SubscribeEvent
    public static void registerLeavesPropertiesTypes(final TypeRegistryEvent<LeavesProperties> event) {
        if (!event.isEntryOfType(LeavesProperties.class)) return;
        event.registerType(DynamicTreesBetterEnd.location("fur"), FurOuterLeaveProperties.TYPE);
        event.registerType(DynamicTreesBetterEnd.location("umbrella"), UmbrellaLeavesProperties.TYPE);
    }

    @SubscribeEvent
    public static void registerFamilyTypes(final TypeRegistryEvent<Family> event) {
        if (!event.isEntryOfType(Family.class)) return;
        event.registerType(DynamicTreesBetterEnd.location("umbrella_tree"), UmbrellaTreeFamily.TYPE);
    }

    @SubscribeEvent
    public static void registerSpeciesTypes(final TypeRegistryEvent<Species> event) {
        if (!event.isEntryOfType(Species.class)) return;
        event.registerType(DynamicTreesBetterEnd.location("umbrella_tree"), UmbrellaTreeSpecies.TYPE);
    }

    @SubscribeEvent
    public static void registerLeavesReloadAppliers(
            final ApplierRegistryEvent.Reload<LeavesProperties, JsonElement> event
    ) {
        registerLeavesAppliers(event.getAppliers());
    }

    @SubscribeEvent
    public static void registerLeavesDataAppliers(
            final ApplierRegistryEvent.GatherData<LeavesProperties, JsonElement> event
    ) {
        registerLeavesAppliers(event.getAppliers());
    }

    private static void registerLeavesAppliers(
            final PropertyAppliers<LeavesProperties, JsonElement> appliers
    ) {
        appliers.register(
                "outer_block",
                FurOuterLeaveProperties.class,
                Block.class,
                FurOuterLeaveProperties::setOuter
        );
    }
}
