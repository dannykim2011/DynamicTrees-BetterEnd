package com.dannykim.dtbetterend.systems.featuregen;

import com.ferreusveritas.dynamictrees.block.branch.BranchBlock;
import com.ferreusveritas.dynamictrees.block.branch.TrunkShellBlock;
import com.ferreusveritas.dynamictrees.systems.genfeature.GenFeature;
import com.ferreusveritas.dynamictrees.systems.genfeature.GenFeatureConfiguration;
import com.ferreusveritas.dynamictrees.systems.genfeature.context.PostGenerationContext;
import com.ferreusveritas.dynamictrees.systems.genfeature.context.PostGrowContext;
import com.ferreusveritas.dynamictrees.systems.genfeature.context.PreGenerationContext;
import com.ferreusveritas.dynamictrees.api.TreeHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

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
