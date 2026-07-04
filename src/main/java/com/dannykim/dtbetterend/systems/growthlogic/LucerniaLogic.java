package com.dannykim.dtbetterend.systems.growthlogic;

import com.dtteam.dynamictrees.systems.growthlogic.GrowthLogicKit;
import com.dtteam.dynamictrees.systems.growthlogic.GrowthLogicKitConfiguration;
import com.dtteam.dynamictrees.systems.growthlogic.context.DirectionManipulationContext;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;

/**
 * Produces 3-6 ascending stems radiating from a shared low base, matching the
 * curved fan of the original Lucernia spline.
 */
public final class LucerniaLogic extends GrowthLogicKit {
    public LucerniaLogic(final ResourceLocation name) {
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
        if (y < 4 && spread < 2) {
            map[Direction.UP.ordinal()] = 5;
            for (final Direction direction : Direction.Plane.HORIZONTAL) {
                map[direction.ordinal()] = 8;
            }
        } else if (spread < 10 && y < 18) {
            map[Direction.UP.ordinal()] = 8;
            if (context.signal().dir.getAxis().isHorizontal()) {
                map[context.signal().dir.ordinal()] = 11;
            }
        } else {
            map[Direction.UP.ordinal()] = y < 20 ? 4 : 0;
        }
        map[context.signal().dir.getOpposite().ordinal()] = 0;
        return map;
    }
}
