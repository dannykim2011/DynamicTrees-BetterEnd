package com.dannykim.dtbetterend.systems.growthlogic;

import com.dtteam.dynamictrees.systems.growthlogic.GrowthLogicKit;
import com.dtteam.dynamictrees.systems.growthlogic.GrowthLogicKitConfiguration;
import com.dtteam.dynamictrees.systems.growthlogic.context.DirectionManipulationContext;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;

/**
 * Enforces alternating paired forks. Each tier turns ninety degrees from the
 * previous tier, reproducing Pythadendron's recursive binary crown.
 */
public final class PythadendronLogic extends GrowthLogicKit {
    public PythadendronLogic(final Identifier name) {
        super(name);
    }

    @Override
    public int[] populateDirectionProbabilityMap(
            final GrowthLogicKitConfiguration configuration,
            final DirectionManipulationContext context
    ) {
        final int[] map = new int[6];
        final int y = context.signal().delta.getY();
        final int steps = context.signal().numSteps;
        final int spread = Math.abs(context.signal().delta.getX()) + Math.abs(context.signal().delta.getZ());

        if (y < 10 && spread == 0) {
            map[Direction.UP.ordinal()] = 20;
        } else if (isFork(steps) && spread < 14) {
            final boolean xAxis = ((steps / 5) & 1) == 0;
            map[(xAxis ? Direction.EAST : Direction.NORTH).ordinal()] = 10;
            map[(xAxis ? Direction.WEST : Direction.SOUTH).ordinal()] = 10;
            map[Direction.UP.ordinal()] = 2;
        } else {
            map[context.signal().dir.ordinal()] = spread < 18 ? 12 : 0;
            map[Direction.UP.ordinal()] = y < 23 ? 5 : 0;
        }
        map[context.signal().dir.getOpposite().ordinal()] = 0;
        return map;
    }

    private static boolean isFork(final int steps) {
        return steps == 10 || steps == 15 || steps == 20;
    }
}
