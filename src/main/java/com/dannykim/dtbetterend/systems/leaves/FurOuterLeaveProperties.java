package com.dannykim.dtbetterend.systems.leaves;

import com.dtteam.dynamictrees.tree.TreeHelper;
import com.dtteam.dynamictrees.api.registry.TypedRegistry;
import com.dtteam.dynamictrees.api.treedata.TreePart;
import com.dtteam.dynamictrees.block.leaves.DynamicLeavesBlock;
import com.dtteam.dynamictrees.block.leaves.LeavesProperties;
import com.dtteam.dynamictrees.utility.CoordUtils;
import com.dannykim.dtbetterend.DynamicTreesBetterEnd;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

import javax.annotation.Nonnull;
import java.util.List;

public class FurOuterLeaveProperties extends LeavesProperties {
    public static final TypedRegistry.EntryType<LeavesProperties> TYPE = TypedRegistry.newType(FurOuterLeaveProperties::new);

    public Block outer = Blocks.AIR;

    public FurOuterLeaveProperties(Identifier registryName) {
        super(registryName);
    }

    @Nonnull
    protected DynamicLeavesBlock createDynamicLeaves(@Nonnull BlockBehaviour.Properties properties) {
        return new DynamicLeavesBlock(this.getBlockRegistryName(), this, properties) {
            List<Direction> ALL = List.of(Direction.DOWN, Direction.UP, Direction.SOUTH, Direction.WEST, Direction.NORTH, Direction.EAST);


            @Override
            public void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource rand) {
                super.randomTick(state, level, pos, rand);
                for (Direction direction : ALL) {
                    if (level.getRandom().nextFloat() > 0.95)
                        if (level.isEmptyBlock(pos.relative(direction)) && direction != Direction.DOWN) {
                            level.setBlock(pos.relative(direction),
                                    getOuter().defaultBlockState().hasProperty(BlockStateProperties.FACING) ?
                                            getOuter().defaultBlockState().setValue(BlockStateProperties.FACING,
                                                    direction) :
                                            getOuter().defaultBlockState(),
                                    Block.UPDATE_ALL);
                        }
                }
            }

            @Override
            protected void affectNeighborsAfterRemoval(final BlockState state, final ServerLevel level,
                                                       final BlockPos pos, final boolean movedByPiston) {
                removeOwnedOuterDecorations(level, pos);
                super.affectNeighborsAfterRemoval(state, level, pos, movedByPiston);
            }

            private void removeOwnedOuterDecorations(final ServerLevel level, final BlockPos supportPos) {
                for (final Direction direction : ALL) {
                    final BlockPos decorationPos = supportPos.relative(direction);
                    final BlockState decoration = level.getBlockState(decorationPos);
                    final Identifier id = BuiltInRegistries.BLOCK.getKey(decoration.getBlock());
                    if (!"lucernia_outer_leaves".equals(id.getPath())
                            || !("betterend".equals(id.getNamespace())
                            || DynamicTreesBetterEnd.MOD_ID.equals(id.getNamespace()))) {
                        continue;
                    }
                    if (decoration.hasProperty(BlockStateProperties.FACING)
                            && decoration.getValue(BlockStateProperties.FACING) == direction) {
                        level.setBlock(decorationPos, Blocks.AIR.defaultBlockState(),
                                Block.UPDATE_ALL | Block.UPDATE_SUPPRESS_DROPS);
                    }
                }
            }

            public int getHydrationLevelFromNeighbors(LevelAccessor level, BlockPos pos, LeavesProperties leavesProperties) {
                int hydro = super.getHydrationLevelFromNeighbors(level, pos, leavesProperties);
                if (hydro <= maxHydro) {
                    int hash = CoordUtils.coordHashCode(pos, 2) % 1000;
                    float rand = hash / 1000f;
                    if (rand >= 0.85) return 0;
                }
                for (Direction dir : Direction.values()) {
                    final BlockPos deltaPos = pos.relative(dir);
                    final BlockState state = level.getBlockState(deltaPos);
                    final TreePart part = TreeHelper.getTreePart(state);

                    if (part.getRadius(state) > 4) return 0;
                }

                return hydro;
            }
        };
    }

    public void setOuter(Block block) {
        this.outer = block;
    }

    public Block getOuter() {
        return this.outer;
    }
}
