package com.dannykim.dtbetterend.client;

import com.dannykim.dtbetterend.DynamicTreesBetterEnd;
import com.dtteam.dynamictrees.client.TintSources.SuppliedConstantTintSource;
import java.util.List;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.Block;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;

public final class DTBetterEndClient {

    private DTBetterEndClient() {
    }

    public static void register(final IEventBus modEventBus) {
        modEventBus.addListener(DTBetterEndClient::registerBlockColors);
    }

    private static void registerBlockColors(final RegisterColorHandlersEvent.BlockTintSources event) {
        final Block helixLeaves = BuiltInRegistries.BLOCK.getValue(DynamicTreesBetterEnd.location("helix_tree_leaves"));

        event.register(List.of(new SuppliedConstantTintSource(() -> 0xFFFFFF)), helixLeaves);
    }

}
