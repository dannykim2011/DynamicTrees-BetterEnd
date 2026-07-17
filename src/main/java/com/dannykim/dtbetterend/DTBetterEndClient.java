package com.dannykim.dtbetterend;

import com.dtteam.dynamictrees.client.TintSources.SuppliedConstantTintSource;
import java.util.List;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;

public final class DTBetterEndClient {

    private DTBetterEndClient() {
    }

    public static void registerBlockColors(final RegisterColorHandlersEvent.BlockTintSources event) {
        final Block helixSapling = BuiltInRegistries.BLOCK.getValue(DynamicTreesBetterEnd.location("helix_tree_sapling"));

        event.register(List.of(new SuppliedConstantTintSource(() -> 0xFFFFFF)), helixSapling);
    }

}
