package com.dannykim.dtbetterend.systems.growthlogic;

import com.dtteam.dynamictrees.systems.growthlogic.GrowthLogicKit;
import com.dtteam.dynamictrees.systems.growthlogic.GrowthLogicKitConfiguration;
import com.dtteam.dynamictrees.systems.growthlogic.context.DirectionManipulationContext;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;

/** Slightly wandering but predominantly vertical stem used by BetterEnd fungi. */
public final class MushroomStemLogic extends GrowthLogicKit {
    private final int upward;
    private final int lateral;

    public MushroomStemLogic(final ResourceLocation name, final int upward, final int lateral) {
        super(name);
        this.upward = upward;
        this.lateral = lateral;
    }

    @Override
    public int[] populateDirectionProbabilityMap(
            final GrowthLogicKitConfiguration configuration,
            final DirectionManipulationContext context
    ) {
        final int[] map = new int[6];
        map[Direction.UP.ordinal()] = upward;
        if (context.signal().delta.getY() > 1) {
            map[Direction.NORTH.ordinal()] = lateral;
            map[Direction.SOUTH.ordinal()] = lateral;
            map[Direction.WEST.ordinal()] = lateral;
            map[Direction.EAST.ordinal()] = lateral;
        }
        map[context.signal().dir.getOpposite().ordinal()] = 0;
        return map;
    }
}
