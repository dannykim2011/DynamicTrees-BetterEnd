package com.dannykim.dtbetterend.systems.mushroom;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

final class MossyGlowshroomDecorationCleanup {
    private static final Identifier HYMENOPHORE =
            Identifier.parse("betterend:mossy_glowshroom_hymenophore");
    private static final Identifier FUR =
            Identifier.parse("betterend:mossy_glowshroom_fur");

    private MossyGlowshroomDecorationCleanup() {
    }

    static boolean isHymenophore(final BlockState state) {
        return HYMENOPHORE.equals(BuiltInRegistries.BLOCK.getKey(state.getBlock()));
    }

    static void removeOwnedFur(final LevelAccessor level, final BlockPos supportPos) {
        if (!isHymenophore(level.getBlockState(supportPos))) return;

        for (final Direction direction : Direction.values()) {
            final BlockPos furPos = supportPos.relative(direction);
            final BlockState fur = level.getBlockState(furPos);
            if (!FUR.equals(BuiltInRegistries.BLOCK.getKey(fur.getBlock()))) continue;
            if (fur.hasProperty(BlockStateProperties.FACING)
                    && fur.getValue(BlockStateProperties.FACING) == direction) {
                level.setBlock(furPos, Blocks.AIR.defaultBlockState(),
                        Block.UPDATE_ALL | Block.UPDATE_SUPPRESS_DROPS);
            }
        }
    }
}
