package com.dannykim.dtbetterend.systems.growthlogic;

import com.dtteam.dynamictrees.systems.growthlogic.GrowthLogicKit;
import com.dtteam.dynamictrees.systems.growthlogic.GrowthLogicKitConfiguration;
import com.dtteam.dynamictrees.systems.growthlogic.context.DirectionManipulationContext;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;

/**
 * Recreates BetterEnd's tall trunk followed by a very broad radial crown.
 */
public final class DragonTreeLogic extends GrowthLogicKit {
    public DragonTreeLogic(final ResourceLocation name) {
        super(name);
    }

    @Override
    public int[] populateDirectionProbabilityMap(
            final GrowthLogicKitConfiguration configuration,
            final DirectionManipulationContext context
    ) {
        final int[] map = new int[6];
        final int y = context.signal().delta.getY();
        final int x = context.signal().delta.getX();
        final int z = context.signal().delta.getZ();
        final int spread = Math.abs(x) + Math.abs(z);

        if (y < 16) {
            map[Direction.UP.ordinal()] = 18;
            if (y > 5) {
                map[horizontalFor(y).ordinal()] = 1;
            }
        } else if (spread < 5) {
            map[Direction.UP.ordinal()] = 3;
            for (final Direction direction : Direction.Plane.HORIZONTAL) {
                map[direction.ordinal()] = 9;
            }
        } else {
            map[context.signal().dir.ordinal()] = spread < 16 ? 13 : 0;
            map[Direction.UP.ordinal()] = spread < 11 ? 3 : 1;
        }
        map[context.signal().dir.getOpposite().ordinal()] = 0;
        return map;
    }

    private static Direction horizontalFor(final int step) {
        return Direction.Plane.HORIZONTAL.stream().toList().get(step & 3);
    }
}
