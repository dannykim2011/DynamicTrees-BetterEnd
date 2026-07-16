package com.dannykim.dtbetterend.systems.growthlogic;

import com.dtteam.dynamictrees.systems.growthlogic.GrowthLogicKit;
import com.dtteam.dynamictrees.systems.growthlogic.GrowthLogicKitConfiguration;
import com.dtteam.dynamictrees.systems.growthlogic.context.DirectionManipulationContext;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;

public final class UmbrellaTreeLogic extends GrowthLogicKit {
    public UmbrellaTreeLogic(final Identifier registryName) {
        super(registryName);
    }

    @Override
    public int[] populateDirectionProbabilityMap(
            final GrowthLogicKitConfiguration configuration,
            final DirectionManipulationContext context
    ) {
        final int[] map = new int[6];
        final int height = context.signal().delta.getY();
        final int spread = Math.abs(context.signal().delta.getX()) + Math.abs(context.signal().delta.getZ());

        if (height < 4 && spread < 2) {
            // Establish a short pedestal before the broad arms. Keeping a
            // vertical choice lets confined growth chambers produce genuine
            // juvenile variants instead of one repeatedly blocked stub.
            final int rotation = Math.floorMod(context.signal().rootPos.getX() * 31
                    + context.signal().rootPos.getZ(), 4);
            map[horizontal(rotation).ordinal()] = height > 1 ? 7 : 1;
            map[horizontal(rotation + 1).ordinal()] = height > 2 ? 5 : 1;
            map[horizontal(rotation + 3).ordinal()] = height > 2 ? 5 : 1;
            map[Direction.UP.ordinal()] = 18;
        } else if (context.signal().dir == Direction.UP && spread < 2) {
            for (final Direction direction : Direction.Plane.HORIZONTAL) {
                map[direction.ordinal()] = 10;
            }
            map[Direction.UP.ordinal()] = 2;
        } else if (context.signal().dir.getAxis().isHorizontal() && spread < 22) {
            map[context.signal().dir.ordinal()] = 18;
            // Arms climb gradually rather than becoming flat crosses.
            map[Direction.UP.ordinal()] = height < 18 ? 7 : 2;
        }
        map[context.signal().dir.getOpposite().ordinal()] = 0;
        return map;
    }

    private static Direction horizontal(final int index) {
        return switch (index & 3) {
            case 0 -> Direction.NORTH;
            case 1 -> Direction.EAST;
            case 2 -> Direction.SOUTH;
            default -> Direction.WEST;
        };
    }
}
