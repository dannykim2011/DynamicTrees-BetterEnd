package com.dannykim.dtbetterend.systems.growthlogic;

import com.dtteam.dynamictrees.api.configuration.ConfigurationProperty;
import com.dtteam.dynamictrees.systems.growthlogic.GrowthLogicKit;
import com.dtteam.dynamictrees.systems.growthlogic.GrowthLogicKitConfiguration;
import com.dtteam.dynamictrees.systems.growthlogic.context.DirectionManipulationContext;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

public final class UmbrellaTreeLogic extends GrowthLogicKit {
    public static final ConfigurationProperty<Integer> MAX_HEIGHT =
            ConfigurationProperty.integer("max_height");
    private static final int[][] LEAN_VECTORS = {
            {0, -1}, {1, -1}, {1, 0}, {1, 1},
            {0, 1}, {-1, 1}, {-1, 0}, {-1, -1}
    };

    public UmbrellaTreeLogic(final ResourceLocation name) {
        super(name);
    }

    @Override
    protected void registerProperties() {
        this.register(MAX_HEIGHT);
    }

    @Override
    protected GrowthLogicKitConfiguration createDefaultConfiguration() {
        return super.createDefaultConfiguration().with(MAX_HEIGHT, 50);
    }

    @Override
    public int[] populateDirectionProbabilityMap(
            final GrowthLogicKitConfiguration configuration,
            final DirectionManipulationContext context
    ) {
        final int[] map = new int[6];
        final int x = context.signal().delta.getX();
        final int y = context.signal().delta.getY();
        final int z = context.signal().delta.getZ();
        final int maxHeight = configuration.get(MAX_HEIGHT);
        final long treeHash = treeHash(context.pos(), context.signal().delta);
        final int growthForm = Math.floorMod((int) (treeHash >>> 7), 3);
        final int topHeight = growthForm == 2
                ? maxHeight + Math.floorMod((int) (treeHash >>> 21), 31)
                : maxHeight - 20 + Math.floorMod((int) (treeHash >>> 21), 21);
        final int[] lean = LEAN_VECTORS[Math.floorMod((int) (treeHash >>> 11), LEAN_VECTORS.length)];
        final int desiredOffset = desiredOffset(growthForm, y, topHeight, treeHash);
        final boolean growthComplete = y >= topHeight;

        if (!growthComplete) {
            if (growthForm == 2) {
                populateDoubleCrownDirections(map, x, y, z, lean, desiredOffset, topHeight);
            } else {
                final Direction leanDirection = nextLeanDirection(x, y, z, lean, desiredOffset);
                if (leanDirection != null) {
                    map[leanDirection.ordinal()] = 32;
                } else {
                    map[Direction.UP.ordinal()] = 32;
                }
            }
        } else {
            context.signal().energy = 0.0F;
        }

        map[Direction.DOWN.ordinal()] = 0;
        final Direction reverse = context.signal().dir.getOpposite();
        if (reverse != Direction.UP) {
            map[reverse.ordinal()] = 0;
        }
        return map;
    }

    private static void populateDoubleCrownDirections(final int[] map, final int x, final int y, final int z,
                                                      final int[] lean, final int desiredOffset,
                                                      final int topHeight) {
        final int splitHeight = Math.max(12, topHeight / 2);
        if (y < splitHeight) {
            map[Direction.UP.ordinal()] = 32;
            return;
        }
        final int projection = x * lean[0] + z * lean[1];
        if (x == 0 && z == 0) {
            final Direction positive = lean[0] != 0
                    ? lean[0] > 0 ? Direction.EAST : Direction.WEST
                    : lean[1] > 0 ? Direction.SOUTH : Direction.NORTH;
            map[positive.ordinal()] = 24;
            map[positive.getOpposite().ordinal()] = 24;
            return;
        }
        final int side = projection >= 0 ? 1 : -1;
        final Direction next = nextDoubleDirection(x, y, z, lean, desiredOffset, side);
        map[(next == null ? Direction.UP : next).ordinal()] = 32;
    }

    private static Direction nextDoubleDirection(final int x, final int y, final int z,
                                                 final int[] lean, final int desiredOffset, final int side) {
        final boolean diagonal = lean[0] != 0 && lean[1] != 0;
        final int axisTarget = diagonal ? Mth.ceil(desiredOffset * 0.70710678) : desiredOffset;
        final int targetX = lean[0] == 0 ? 0 : axisTarget * lean[0] * side;
        final int targetZ = lean[1] == 0 ? 0 : axisTarget * lean[1] * side;
        final boolean needsX = lean[0] != 0 && x != targetX;
        final boolean needsZ = lean[1] != 0 && z != targetZ;
        if (needsX && (!needsZ || Math.floorMod(x + y + z, 2) == 0)) {
            return targetX > x ? Direction.EAST : Direction.WEST;
        }
        if (needsZ) return targetZ > z ? Direction.SOUTH : Direction.NORTH;
        return null;
    }

    private static Direction nextLeanDirection(final int x, final int y, final int z,
                                               final int[] lean, final int desiredOffset) {
        final boolean diagonal = lean[0] != 0 && lean[1] != 0;
        final int targetX = lean[0] == 0 ? 0 : diagonal ? Mth.ceil(desiredOffset * 0.70710678) : desiredOffset;
        final int targetZ = lean[1] == 0 ? 0 : diagonal ? Mth.ceil(desiredOffset * 0.70710678) : desiredOffset;
        final boolean needsX = lean[0] != 0 && x * lean[0] < targetX;
        final boolean needsZ = lean[1] != 0 && z * lean[1] < targetZ;
        if (needsX && (!needsZ || Math.floorMod(x + y + z, 2) == 0)) {
            return lean[0] > 0 ? Direction.EAST : Direction.WEST;
        }
        if (needsZ) return lean[1] > 0 ? Direction.SOUTH : Direction.NORTH;
        return null;
    }

    private static int desiredOffset(final int growthForm, final int y,
                                     final int topHeight, final long treeHash) {
        if (growthForm == 0) return 0;
        if (growthForm == 1) {
            final int leanStart = Math.max(10, topHeight / 3);
            final int leanEnd = Math.max(leanStart, topHeight - 5);
            if (y < leanStart) return 0;
            final int interval = 6 + Math.floorMod((int) (treeHash >>> 15), 3);
            return Math.min(5, 1 + (Math.min(y, leanEnd) - leanStart) / interval);
        }

        final int leanStart = Math.max(12, topHeight / 2);
        final int leanEnd = Math.max(leanStart + 1, topHeight - 3);
        if (y < leanStart) return 0;
        if (y >= leanEnd) return 10;
        final double progress = (Math.min(y, leanEnd) - leanStart)
                / (double) (leanEnd - leanStart);
        final double smoothProgress = progress * progress * (3.0 - 2.0 * progress);
        return Math.min(10, (int) Math.round(10.0 * smoothProgress));
    }

    private static long treeHash(final BlockPos currentPos, final BlockPos delta) {
        final long rootX = currentPos.getX() - delta.getX();
        final long rootZ = currentPos.getZ() - delta.getZ();
        long hash = rootX * 341873128712L ^ rootZ * 132897987541L;
        hash ^= hash >>> 17;
        return hash;
    }

}
