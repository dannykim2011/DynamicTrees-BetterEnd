package com.dannykim.dtbetterend.systems.mushroom;

import com.dtteam.dynamictreesplus.block.mushroom.DynamicCapBlock;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.level.block.BreakBlockEvent;

public final class MushroomDecorationEvents {
    private MushroomDecorationEvents() {
    }

    @SubscribeEvent
    public static void onBlockBreak(final BreakBlockEvent event) {
        if (event.getLevel().isClientSide()) return;
        if (!MossyGlowshroomDecorationCleanup.isHymenophore(event.getState())) return;
        if (!(event.getLevel().getBlockState(event.getPos().above()).getBlock() instanceof DynamicCapBlock)) return;
        MossyGlowshroomDecorationCleanup.removeOwnedFur(event.getLevel(), event.getPos());
    }
}
