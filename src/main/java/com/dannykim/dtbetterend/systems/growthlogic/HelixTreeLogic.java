package com.dannykim.dtbetterend.systems.growthlogic;

import com.dtteam.dynamictrees.systems.growthlogic.GrowthLogicKit;
import com.dtteam.dynamictrees.systems.growthlogic.GrowthLogicKitConfiguration;
import com.dtteam.dynamictrees.systems.growthlogic.context.DirectionManipulationContext;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;

public final class HelixTreeLogic extends GrowthLogicKit {
    private static final Direction[] TURN = {Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST};

    public HelixTreeLogic(final Identifier registryName) {
        super(registryName);
    }

    @Override
    public int[] populateDirectionProbabilityMap(
            final GrowthLogicKitConfiguration configuration,
            final DirectionManipulationContext context
    ) {
        final int[] map = new int[6];
        final int height = context.signal().delta.getY();
        final int step = context.signal().numSteps;
        final int spread = Math.abs(context.signal().delta.getX()) + Math.abs(context.signal().delta.getZ());

        if (height < 18) {
            // Two opposed choices at each level establish the two strands; the
            // preferred axis rotates every two vertical steps.
            final int phase = (height / 2) & 3;
            map[TURN[phase].ordinal()] = 12;
            map[TURN[(phase + 2) & 3].ordinal()] = 12;
            map[Direction.UP.ordinal()] = spread > 8 ? 16 : 7;
        } else {
            // The original joins into a tall central stem before its leaf helix.
            map[Direction.UP.ordinal()] = height < 38 ? 20 : 0;
            if (height > 22 && height < 36 && (step & 1) == 0) {
                final int phase = (height / 2) & 3;
                map[TURN[phase].ordinal()] = 5;
            }
        }
        map[context.signal().dir.getOpposite().ordinal()] = 0;
        return map;
    }
}
