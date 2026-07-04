package com.dannykim.dtbetterend.systems;

import com.dannykim.dtbetterend.DynamicTreesBetterEnd;
import com.dannykim.dtbetterend.data.EnglishLanguageProvider;
import com.dannykim.dtbetterend.systems.leaves.FurOuterLeaveProperties;
import com.dtteam.dynamictrees.event.ApplierRegistryEvent;
import com.dtteam.dynamictrees.event.RegistryEvent;
import com.dtteam.dynamictrees.event.TypeRegistryEvent;
import com.dtteam.dynamictrees.block.leaves.LeavesProperties;
import com.dtteam.dynamictrees.deserialization.PropertyAppliers;
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
        event.getGenerator().addProvider(
                event.includeClient(),
                new EnglishLanguageProvider(event.getGenerator().getPackOutput())
        );
    }

    @SubscribeEvent
    public static void registerLeavesPropertiesTypes(final TypeRegistryEvent<LeavesProperties> event) {
        if (!event.isEntryOfType(LeavesProperties.class)) return;
        event.registerType(DynamicTreesBetterEnd.location("fur"), FurOuterLeaveProperties.TYPE);
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
