package com.dannykim.dtbetterend.systems.growthlogic;

import com.dtteam.dynamictrees.systems.growthlogic.GrowthLogicKit;
import com.dtteam.dynamictrees.systems.growthlogic.GrowthLogicKitConfiguration;
import com.dtteam.dynamictrees.systems.growthlogic.context.DirectionManipulationContext;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;

/**
 * Lacugrove is deliberately not a spreading tree: one 15-25 block bole supports
 * a dense, rounded crown centred near the upper fifth of the trunk.
 */
public final class LacugroveLogic extends GrowthLogicKit {
    public LacugroveLogic(final ResourceLocation name) {
        super(name);
    }

    @Override
    public int[] populateDirectionProbabilityMap(
            final GrowthLogicKitConfiguration configuration,
            final DirectionManipulationContext context
    ) {
        final int[] map = new int[6];
        final int y = context.signal().delta.getY();
        final int spread = Math.abs(context.signal().delta.getX()) + Math.abs(context.signal().delta.getZ());
        if (y < 15) {
            map[Direction.UP.ordinal()] = 24;
        } else if (spread < 2 && y < 24) {
            map[Direction.UP.ordinal()] = 12;
            for (final Direction direction : Direction.Plane.HORIZONTAL) {
                map[direction.ordinal()] = 3;
            }
        } else {
            map[Direction.UP.ordinal()] = y < 22 ? 3 : 0;
            if (context.signal().dir.getAxis().isHorizontal()) {
                map[context.signal().dir.ordinal()] = spread < 7 ? 8 : 0;
            }
        }
        map[context.signal().dir.getOpposite().ordinal()] = 0;
        return map;
    }
}
