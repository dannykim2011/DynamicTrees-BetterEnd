package com.dannykim.dtbetterend.systems.growthlogic;

import com.dannykim.dtbetterend.systems.umbrella.UmbrellaGrowthForm;
import com.dtteam.dynamictrees.block.branch.BranchBlock;
import com.dtteam.dynamictrees.api.configuration.ConfigurationProperty;
import com.dtteam.dynamictrees.systems.growthlogic.GrowthLogicKit;
import com.dtteam.dynamictrees.systems.growthlogic.GrowthLogicKitConfiguration;
import com.dtteam.dynamictrees.systems.growthlogic.context.DirectionManipulationContext;
import com.dtteam.dynamictrees.tree.TreeHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;

public final class UmbrellaTreeLogic extends GrowthLogicKit {
    public static final ConfigurationProperty<Integer> MAX_HEIGHT =
            ConfigurationProperty.integer("max_height");
    private static final int[][] LEAN_VECTORS = {
            {0, -1}, {1, 0}, {0, 1}, {-1, 0}
    };
    private static final int[][] DOUBLE_LEAN_VECTORS = {
            {0, -1}, {1, 0}, {0, 1}, {-1, 0}
    };

    public UmbrellaTreeLogic(final Identifier name) {
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
        final BlockPos rootPos = context.signal().rootPos != null
                ? context.signal().rootPos : context.pos().subtract(context.signal().delta);
        final int growthSeed = UmbrellaGrowthForm.seed(context.level(), rootPos, context.signal().rand);
        final long treeHash = treeHash(rootPos, growthSeed);
        final int growthForm = UmbrellaGrowthForm.selectedForm(growthSeed);
        final int topHeight = maxHeight - 20
                + Math.floorMod((int) (treeHash >>> 21), 21);
        final int[] lean = growthForm == 2
                ? DOUBLE_LEAN_VECTORS[Math.floorMod((int) (treeHash >>> 11), DOUBLE_LEAN_VECTORS.length)]
                : LEAN_VECTORS[Math.floorMod((int) (treeHash >>> 11), LEAN_VECTORS.length)];
        final int trunkRadius = TreeHelper.getRadius(context.level(), rootPos.above());
        final int desiredOffset = desiredOffset(growthForm, y, topHeight, treeHash, trunkRadius);
        final boolean growthComplete = y >= topHeight;

        if (!growthComplete) {
            if (growthForm == 2) {
                populateDoubleCrownDirections(map, x, y, z, lean, desiredOffset, topHeight,
                        context.level(), context.pos(), context.signal().dir);
            } else {
                final Direction leanDirection = growthForm == 1
                        && context.signal().dir.getAxis().isHorizontal()
                        ? Direction.UP : nextLeanDirection(x, y, z, lean, desiredOffset);
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
                                                       final int topHeight, final Level level,
                                                       final BlockPos currentPos,
                                                       final Direction incomingDirection) {
        final int splitHeight = doubleSplitHeight(topHeight);
        if (y < splitHeight) {
            map[Direction.UP.ordinal()] = 32;
            return;
        }
        final int projection = x * lean[0] + z * lean[1];
        if (x == 0 && z == 0 && y == splitHeight) {
            final Direction positive = lean[0] != 0
                    ? lean[0] > 0 ? Direction.EAST : Direction.WEST
                    : lean[1] > 0 ? Direction.SOUTH : Direction.NORTH;
            final Direction negative = positive.getOpposite();
            final boolean positiveExists = level.getBlockState(currentPos.relative(positive))
                    .getBlock() instanceof BranchBlock;
            final boolean negativeExists = level.getBlockState(currentPos.relative(negative))
                    .getBlock() instanceof BranchBlock;
            if (!positiveExists || negativeExists) map[positive.ordinal()] = 32;
            if (!negativeExists || positiveExists) map[negative.ordinal()] = 32;
            return;
        }
        if (x == 0 && z == 0) return;
        final int side = projection >= 0 ? 1 : -1;
        final Direction next = nextDoubleDirection(
                x, y, z, lean, desiredOffset, side, incomingDirection);
        map[(next == null ? Direction.UP : next).ordinal()] = 32;
    }

    private static Direction nextDoubleDirection(final int x, final int y, final int z,
                                                 final int[] lean, final int desiredOffset, final int side,
                                                 final Direction incomingDirection) {
        if (incomingDirection.getAxis().isHorizontal()) return Direction.UP;
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
        if (lean[0] != 0 && x * lean[0] < desiredOffset) {
            return lean[0] > 0 ? Direction.EAST : Direction.WEST;
        }
        if (lean[1] != 0 && z * lean[1] < desiredOffset) {
            return lean[1] > 0 ? Direction.SOUTH : Direction.NORTH;
        }
        return null;
    }

    private static int desiredOffset(final int growthForm, final int y,
                                     final int topHeight, final long treeHash,
                                     final int trunkRadius) {
        if (growthForm == 0) return 0;
        if (growthForm == 1) {
            final int leanStart = Math.max(10, topHeight / 3);
            final int leanEnd = Math.max(leanStart, topHeight - 5);
            if (y < leanStart) return 0;
            final int interval = 6 + Math.floorMod((int) (treeHash >>> 15), 3);
            return Math.min(5, 1 + (Math.min(y, leanEnd) - leanStart) / interval);
        }

        final int splitHeight = doubleSplitHeight(topHeight);
        if (y < splitHeight) return 0;
        final int canopyRadius = Mth.clamp(2 + topHeight / 9
                + Math.max(0, trunkRadius - 8) / 8, 2, 10);
        final int targetOffset = canopyRadius + 2;
        final int spreadHeight = Math.max(1, topHeight - splitHeight - 1);
        final int progress = Mth.clamp(y - splitHeight, 0, spreadHeight);
        return 1 + (progress * (targetOffset - 1) + spreadHeight - 1) / spreadHeight;
    }

    private static int doubleSplitHeight(final int topHeight) {
        return topHeight / 2;
    }

    private static long treeHash(final BlockPos rootPos, final int growthSeed) {
        long hash = rootPos.getX() * 341873128712L ^ rootPos.getZ() * 132897987541L;
        hash ^= Integer.toUnsignedLong(growthSeed) * 0x9E3779B97F4A7C15L;
        hash ^= hash >>> 17;
        return hash;
    }

}
