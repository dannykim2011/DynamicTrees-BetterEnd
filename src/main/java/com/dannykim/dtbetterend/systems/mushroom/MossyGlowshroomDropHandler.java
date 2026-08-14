package com.dannykim.dtbetterend.systems.mushroom;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.level.BlockDropsEvent;

public final class MossyGlowshroomDropHandler {
    private MossyGlowshroomDropHandler() {
    }

    @SubscribeEvent
    public static void replaceDetachedSaplingDrop(final BlockDropsEvent event) {
        if (event.getBreaker() != null) {
            return;
        }

        final Identifier blockKey = BuiltInRegistries.BLOCK.getKey(event.getState().getBlock());
        if (!"betterend".equals(blockKey.getNamespace())
                || !"mossy_glowshroom_fur".equals(blockKey.getPath())) {
            return;
        }

        event.getDrops().removeIf(drop -> {
            final Identifier itemKey = BuiltInRegistries.ITEM.getKey(drop.getItem().getItem());
            return "betterend".equals(itemKey.getNamespace())
                    && "mossy_glowshroom_sapling".equals(itemKey.getPath());
        });
    }
}
