package com.dannykim.dtbetterend.systems.leaves;

import com.dannykim.dtbetterend.DynamicTreesBetterEnd;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.level.BlockDropsEvent;

import java.util.Map;

public final class AttachedLeafDropEvents {
    private static final Map<Identifier, Identifier> PRIMITIVE_SAPLINGS_BY_SOURCE = Map.of(
            Identifier.parse("betterend:mossy_glowshroom_fur"),
            Identifier.parse("betterend:mossy_glowshroom_sapling"),
            Identifier.parse("betterend:lucernia_outer_leaves"),
            Identifier.parse("betterend:lucernia_sapling"),
            Identifier.parse("betterend:lacugrove_leaves"),
            Identifier.parse("betterend:lacugrove_sapling"),
            DynamicTreesBetterEnd.location("lacugrove_leaves"),
            Identifier.parse("betterend:lacugrove_sapling")
    );

    private AttachedLeafDropEvents() {
    }

    @SubscribeEvent
    public static void onBlockDrops(final BlockDropsEvent event) {
        final Identifier sourceId = BuiltInRegistries.BLOCK.getKey(event.getState().getBlock());
        final Identifier primitiveSapling = PRIMITIVE_SAPLINGS_BY_SOURCE.get(sourceId);
        if (primitiveSapling == null) return;

        event.getDrops().removeIf(drop -> primitiveSapling.equals(
                BuiltInRegistries.ITEM.getKey(drop.getItem().getItem())));
    }
}
