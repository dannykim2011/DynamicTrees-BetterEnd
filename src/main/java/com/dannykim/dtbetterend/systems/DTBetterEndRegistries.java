package com.dannykim.dtbetterend.systems;

import com.dannykim.dtbetterend.DynamicTreesBetterEnd;
import com.dannykim.dtbetterend.data.EnglishLanguageProvider;
import com.dannykim.dtbetterend.systems.leaves.FurOuterLeaveProperties;
import com.ferreusveritas.dynamictrees.api.applier.ApplierRegistryEvent;
import com.ferreusveritas.dynamictrees.api.registry.RegistryEvent;
import com.ferreusveritas.dynamictrees.api.registry.TypeRegistryEvent;
import com.ferreusveritas.dynamictrees.block.leaves.LeavesProperties;
import com.ferreusveritas.dynamictrees.deserialisation.PropertyAppliers;
import com.ferreusveritas.dynamictrees.growthlogic.GrowthLogicKit;
import com.ferreusveritas.dynamictrees.systems.genfeature.GenFeature;
import com.ferreusveritas.dynamictrees.api.worldgen.FeatureCanceller;
import com.google.gson.JsonElement;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.data.event.GatherDataEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

@EventBusSubscriber(modid = DynamicTreesBetterEnd.MOD_ID, bus = EventBusSubscriber.Bus.MOD)
public final class DTBetterEndRegistries {
    private DTBetterEndRegistries() {
    }

    @SubscribeEvent
    public static void registerGrowthLogicKits(final RegistryEvent<GrowthLogicKit> event) {
        ModGrowthLogicKits.register(event.getRegistry());
    }

    @SubscribeEvent
    public static void registerGenFeatures(final RegistryEvent<GenFeature> event) {
        ModFeatures.register(event.getRegistry());
    }

    @SubscribeEvent
    public static void registerFeatureCancellers(final RegistryEvent<FeatureCanceller> event) {
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
