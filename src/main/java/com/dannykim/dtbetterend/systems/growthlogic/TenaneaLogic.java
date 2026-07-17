package com.dannykim.dtbetterend.systems.growthlogic;

import com.dtteam.dynamictrees.systems.growthlogic.GrowthLogicKit;
import com.dtteam.dynamictrees.systems.growthlogic.GrowthLogicKitConfiguration;
import com.dtteam.dynamictrees.systems.growthlogic.context.DirectionManipulationContext;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;

/**
 * A low, broad multi-stem tree. Branches leave the base early and terminate
 * before they can acquire the height of Lucernia.
 */
public final class TenaneaLogic extends GrowthLogicKit {
    public TenaneaLogic(final Identifier name) {
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
        if (y < 3 && spread < 2) {
            map[Direction.UP.ordinal()] = 3;
            for (final Direction direction : Direction.Plane.HORIZONTAL) {
                map[direction.ordinal()] = 9;
            }
        } else {
            map[Direction.UP.ordinal()] = y < 9 ? 5 : 0;
            if (context.signal().dir.getAxis().isHorizontal()) {
                map[context.signal().dir.ordinal()] = spread < 8 ? 10 : 0;
            }
        }
        map[context.signal().dir.getOpposite().ordinal()] = 0;
        return map;
    }
}
