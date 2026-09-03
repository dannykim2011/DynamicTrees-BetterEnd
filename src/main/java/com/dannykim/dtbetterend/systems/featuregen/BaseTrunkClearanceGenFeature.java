package com.dannykim.dtbetterend.systems.featuregen;

import com.dtteam.dynamictrees.block.branch.BranchBlock;
import com.dtteam.dynamictrees.block.branch.TrunkShellBlock;
import com.dtteam.dynamictrees.systems.genfeature.GenFeature;
import com.dtteam.dynamictrees.systems.genfeature.GenFeatureConfiguration;
import com.dtteam.dynamictrees.systems.genfeature.context.PostGenerationContext;
import com.dtteam.dynamictrees.systems.genfeature.context.PostGrowContext;
import com.dtteam.dynamictrees.systems.genfeature.context.PreGenerationContext;
import com.dtteam.dynamictrees.tree.TreeHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Set;

public final class BaseTrunkClearanceGenFeature extends GenFeature {
    public BaseTrunkClearanceGenFeature(final ResourceLocation registryName) {
        super(registryName);
    }

    @Override
    protected void registerProperties() {
    }

    @Override
    protected BlockPos preGenerate(final GenFeatureConfiguration configuration,
                                   final PreGenerationContext context) {
        clearForRadius(context.level(), context.pos(), context.radius());
        return context.pos();
    }

    @Override
    protected boolean postGenerate(final GenFeatureConfiguration configuration,
                                   final PostGenerationContext context) {
        return clearAndReconcileTrunk(
                context.level(), context.pos(), context.pos().above(), context.radius());
    }

    @Override
    protected boolean postGrow(final GenFeatureConfiguration configuration, final PostGrowContext context) {
        return clearAndReconcileTrunk(context.level(), context.pos(), context.treePos(), 0);
    }

    private static boolean clearAndReconcileTrunk(final LevelAccessor level, final BlockPos rootPos,
                                                  final BlockPos treePos, final int generatedRadius) {
        final BlockState baseState = level.getBlockState(treePos);
        final int baseRadius = TreeHelper.getRadius(level, treePos);
        int upperRadius = 0;
        for (int y = 0; y <= 4; y++) {
            upperRadius = Math.max(upperRadius,
                    TreeHelper.getRadius(level, treePos.above(y)));
        }
        final int requiredRadius = Math.max(generatedRadius, Math.max(baseRadius, upperRadius));
        boolean changed = clearForRadius(level, rootPos, requiredRadius);
        if (requiredRadius > baseRadius && baseState.getBlock() instanceof BranchBlock branch) {
            branch.setRadius(level, treePos, requiredRadius, null);
            changed = true;
        }
        changed |= clearConnectedThickTrunk(level, treePos);
        return changed;
    }

    private static boolean clearConnectedThickTrunk(final LevelAccessor level, final BlockPos treePos) {
        final BlockState startState = level.getBlockState(treePos);
        if (!(startState.getBlock() instanceof BranchBlock startBranch)) return false;

        final ArrayDeque<BlockPos> pending = new ArrayDeque<>();
        final Set<BlockPos> visited = new HashSet<>();
        pending.add(treePos.immutable());
        boolean changed = false;

        while (!pending.isEmpty()) {
            final BlockPos current = pending.removeFirst();
            if (!visited.add(current)) continue;

            final BlockState state = level.getBlockState(current);
            if (!(state.getBlock() instanceof BranchBlock branch)
                    || branch.getFamily() != startBranch.getFamily()) {
                continue;
            }

            final int radius = branch.getRadius(state);
            if (radius <= 8) continue;
            changed |= clearShellAt(level, current, radius);

            for (final Direction direction : Direction.values()) {
                final BlockPos next = current.relative(direction);
                if (!visited.contains(next)) pending.addLast(next.immutable());
            }
        }
        return changed;
    }

    private static boolean clearShellAt(final LevelAccessor level, final BlockPos branchPos,
                                        final int radius) {
        final int shellRange = Math.max(1, (radius - 1) / 8);
        boolean changed = false;
        for (int x = -shellRange; x <= shellRange; x++) {
            for (int z = -shellRange; z <= shellRange; z++) {
                if (x == 0 && z == 0) continue;
                final BlockPos pos = branchPos.offset(x, 0, z);
                final BlockState state = level.getBlockState(pos);
                if (!isBlockingDecoration(state)) continue;
                level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
                changed = true;
            }
        }
        return changed;
    }

    private static boolean clearForRadius(final LevelAccessor level, final BlockPos rootPos,
                                          final int radius) {
        if (radius <= 8) return false;
        final int shellRange = Math.max(1, (radius - 1) / 8);
        boolean changed = false;
        for (int y = 1; y <= shellRange; y++) {
            for (int x = -shellRange; x <= shellRange; x++) {
                for (int z = -shellRange; z <= shellRange; z++) {
                    final BlockPos pos = rootPos.offset(x, y, z);
                    final BlockState state = level.getBlockState(pos);
                    if (!isBlockingDecoration(state)) continue;
                    level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
                    changed = true;
                }
            }
        }
        return changed;
    }

    private static boolean isBlockingDecoration(final BlockState state) {
        return !state.isAir()
                && state.getFluidState().isEmpty()
                && !(state.getBlock() instanceof BranchBlock)
                && !(state.getBlock() instanceof TrunkShellBlock);
    }
}
