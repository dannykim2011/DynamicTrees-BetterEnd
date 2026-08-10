package com.dannykim.dtbetterend.systems.growthlogic;

import com.dtteam.dynamictrees.systems.growthlogic.GrowthLogicKit;
import com.dtteam.dynamictrees.systems.growthlogic.GrowthLogicKitConfiguration;
import com.dtteam.dynamictrees.systems.growthlogic.context.DirectionManipulationContext;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;

public final class TenaneaLogic extends GrowthLogicKit {
    private static final Direction[] HORIZONTALS = {Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST};

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
        final int x = context.signal().delta.getX();
        final int z = context.signal().delta.getZ();
        final int spread = Math.abs(x) + Math.abs(z);
        final Direction outward = outwardDirection(x, z, y);

        if (y <= 2) {
            map[Direction.UP.ordinal()] = 14;
            if (y > 0) {
                for (final Direction direction : HORIZONTALS) {
                    map[direction.ordinal()] = 9;
                }
            }
        } else if (y <= 7 && spread <= 5) {
            map[Direction.UP.ordinal()] = spread < 2 ? 8 : 10;
            if (context.signal().dir.getAxis().isHorizontal()) {
                map[context.signal().dir.ordinal()] = 22;
                map[turn(context.signal().dir, y).ordinal()] = 4;
            } else {
                for (final Direction direction : HORIZONTALS) {
                    map[direction.ordinal()] = 16;
                }
            }
        } else if (y <= 14 && spread <= 12) {
            map[Direction.UP.ordinal()] = spread < 7 ? 7 : 4;
            if (context.signal().dir.getAxis().isHorizontal()) {
                map[context.signal().dir.ordinal()] = 22;
                map[turn(context.signal().dir, y).ordinal()] = 3;
            } else {
                map[outward.ordinal()] = 18;
                map[turn(outward, y).ordinal()] = 5;
            }
        } else if (y <= 20 && spread <= 16) {
            map[Direction.UP.ordinal()] = spread < 11 ? 4 : 1;
            if (context.signal().dir.getAxis().isHorizontal()) {
                map[context.signal().dir.ordinal()] = 14;
            }
            map[outward.ordinal()] = Math.max(map[outward.ordinal()], 10);
        }

        final Direction reverse = context.signal().dir.getOpposite();
        if (reverse != Direction.UP) {
            map[reverse.ordinal()] = 0;
        }
        return map;
    }

    private static Direction outwardDirection(final int x, final int z, final int y) {
        if (Math.abs(x) > Math.abs(z)) {
            return x > 0 ? Direction.EAST : Direction.WEST;
        }
        if (Math.abs(z) > 0) {
            return z > 0 ? Direction.SOUTH : Direction.NORTH;
        }
        return HORIZONTALS[y & 3];
    }

    private static Direction turn(final Direction direction, final int y) {
        if ((y & 1) == 0) {
            return direction;
        }
        return HORIZONTALS[(direction.get2DDataValue() + 1) & 3];
    }
}
