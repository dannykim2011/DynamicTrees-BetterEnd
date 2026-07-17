package com.dannykim.dtbetterend.systems.featuregen;

import com.dtteam.dynamictrees.block.leaves.DynamicLeavesBlock;
import com.dtteam.dynamictrees.systems.genfeature.GenFeatureConfiguration;
import com.dtteam.dynamictrees.systems.genfeature.VinesGenFeature;
import com.dtteam.dynamictrees.tree.species.Species;
import com.dtteam.dynamictrees.utility.CoordUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.betterx.bclib.blocks.BaseVineBlock;
import org.betterx.wover.block.api.BlockProperties;

import javax.annotation.Nullable;
import java.util.Random;

public final class FlowerVinesGenFeature extends VinesGenFeature {
    public FlowerVinesGenFeature(final Identifier registryName) {
        super(registryName);
    }

    private BlockPos findGround(final LevelAccessor level, final BlockPos vinePos) {
        final BlockPos.MutableBlockPos cursor = vinePos.mutable();
        do {
            cursor.move(Direction.DOWN);
            if (cursor.getY() <= level.getMinY()) {
                return BlockPos.ZERO;
            }
        } while (level.isEmptyBlock(cursor)
                || level.getBlockState(cursor).getBlock() instanceof DynamicLeavesBlock);
        return cursor.above();
    }

    @Override
    protected void addVerticalVines(
            final GenFeatureConfiguration configuration,
            final LevelAccessor level,
            final Species species,
            final BlockPos rootPos,
            final BlockPos branchPos,
            final boolean worldgen
    ) {
        BlockPos vinePos = CoordUtils.getRayTraceFruitPos(level, species, rootPos, branchPos, worldgen);

        if (configuration.get(VINE_TYPE) == VineType.FLOOR) {
            vinePos = findGround(level, vinePos);
        }
        if (vinePos.equals(BlockPos.ZERO)) {
            return;
        }

        final Block vineBlock = configuration.get(BLOCK);
        if (vineBlock == Blocks.AIR) {
            return;
        }

        while (level.getBlockState(vinePos).is(vineBlock)) {
            vinePos = vinePos.below();
        }
        if (!level.isEmptyBlock(vinePos)) {
            return;
        }

        long seed = CoordUtils.coordHashCode(vinePos, 3);
        if (level instanceof ServerLevel serverLevel) {
            seed += serverLevel.getSeed();
        }
        if (new Random(seed).nextFloat() <= 0.75F && !worldgen) {
            return;
        }

        BlockState bodyState = vineBlock.defaultBlockState();
        if (bodyState.hasProperty(BaseVineBlock.SHAPE)) {
            level.setBlock(
                    vinePos,
                    bodyState.setValue(BaseVineBlock.SHAPE, BlockProperties.TripleShape.TOP),
                    Block.UPDATE_ALL
            );
            bodyState = bodyState.setValue(BaseVineBlock.SHAPE, BlockProperties.TripleShape.MIDDLE);
        }

        final BlockState tipState = configuration.getAsOptional(TIP_BLOCK)
                .map(block -> {
                    BlockState state = block.defaultBlockState();
                    if (state.hasProperty(BaseVineBlock.SHAPE)) {
                        state = state.setValue(BaseVineBlock.SHAPE, BlockProperties.TripleShape.BOTTOM);
                    }
                    return state;
                })
                .orElse(null);
        placeVines(
                level,
                vinePos.below(),
                bodyState,
                configuration.get(MAX_LENGTH),
                tipState,
                configuration.get(VINE_TYPE),
                worldgen
        );
    }

    @Override
    protected void placeVines(
            final LevelAccessor level,
            final BlockPos vinePos,
            final BlockState vinesState,
            final int maxLength,
            @Nullable BlockState tipState,
            final VineType vineType,
            final boolean worldGen
    ) {
        final int length = Mth.clamp(level.getRandom().nextInt(maxLength) + 1, 1, maxLength);
        final BlockPos.MutableBlockPos cursor = vinePos.mutable();
        tipState = tipState == null ? vinesState : tipState;

        for (int index = 0; index < length; index++) {
            if (level.isEmptyBlock(cursor)) {
                level.setBlock(cursor, index == length - 1 ? tipState : vinesState, Block.UPDATE_ALL);
                cursor.move(vineType == VineType.FLOOR ? Direction.UP : Direction.DOWN);
            } else {
                if (index > 0 && vineType != VineType.SIDE) {
                    cursor.move(vineType == VineType.FLOOR ? Direction.DOWN : Direction.UP);
                    level.setBlock(cursor, tipState, Block.UPDATE_ALL);
                }
                break;
            }
        }
    }
}
