package com.dannykim.dtbetterend.systems.leaves;

import com.dtteam.dynamictrees.api.registry.TypedRegistry;
import com.dtteam.dynamictrees.block.leaves.DynamicLeavesBlock;
import com.dtteam.dynamictrees.block.leaves.LeavesProperties;
import com.dtteam.dynamictrees.block.branch.BranchBlock;
import com.dtteam.dynamictrees.systems.GrowSignal;
import com.dtteam.dynamictrees.tree.TreeHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.util.StringRepresentable;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public final class UmbrellaLeavesProperties extends LeavesProperties {
    public static final TypedRegistry.EntryType<LeavesProperties> TYPE =
            TypedRegistry.newType(UmbrellaLeavesProperties::new);
    public static final EnumProperty<Part> PART = EnumProperty.create("part", Part.class);
    public static final IntegerProperty COLOR = IntegerProperty.create("color", 0, 7);
    public static final BooleanProperty WORLDGEN = BooleanProperty.create("worldgen");

    public UmbrellaLeavesProperties(final ResourceLocation registryName) {
        super(registryName);
    }

    @Override
    @Nonnull
    protected DynamicLeavesBlock createDynamicLeaves(@Nonnull final BlockBehaviour.Properties properties) {
        return new UmbrellaDynamicLeavesBlock(this, properties);
    }

    public static BlockState membraneState(final Block block, final int color) {
        return membraneState(block, color, true);
    }

    public static BlockState membraneState(final Block block, final int color, final boolean worldGen) {
        return specialState(block, Part.MEMBRANE, color, worldGen);
    }

    public static BlockState clusterState(final Block block) {
        return clusterState(block, true);
    }

    public static BlockState clusterState(final Block block, final boolean worldGen) {
        return specialState(block, Part.CLUSTER, 0, worldGen);
    }

    public static BlockState clusterState(final Block block, final boolean worldGen,
                                          final int variation) {
        return specialState(block, Part.CLUSTER, variation, worldGen);
    }

    private static BlockState specialState(final Block block, final Part part, final int color,
                                           final boolean worldGen) {
        BlockState state = block.defaultBlockState();
        if (state.hasProperty(PART)) state = state.setValue(PART, part);
        if (state.hasProperty(COLOR)) state = state.setValue(COLOR, Mth.clamp(color, 0, 7));
        if (state.hasProperty(WORLDGEN)) state = state.setValue(WORLDGEN, worldGen);
        if (state.hasProperty(DynamicLeavesBlock.DISTANCE)) {
            state = state.setValue(DynamicLeavesBlock.DISTANCE, 1);
        }
        return state;
    }

    public static boolean isSpecial(final BlockState state) {
        return state.hasProperty(PART) && state.getValue(PART) != Part.NORMAL;
    }

    public static boolean isMembrane(final BlockState state) {
        return state.hasProperty(PART) && state.getValue(PART) == Part.MEMBRANE;
    }

    public static boolean isCluster(final BlockState state) {
        return state.hasProperty(PART) && state.getValue(PART) == Part.CLUSTER;
    }

    public static boolean isGrowthSpecial(final BlockState state) {
        return isSpecial(state) && state.hasProperty(WORLDGEN) && !state.getValue(WORLDGEN);
    }

    private static final class UmbrellaDynamicLeavesBlock extends DynamicLeavesBlock {
        private UmbrellaDynamicLeavesBlock(final LeavesProperties properties,
                                           final BlockBehaviour.Properties blockProperties) {
            super(properties, blockProperties);
            registerDefaultState(defaultBlockState().setValue(PART, Part.NORMAL).setValue(COLOR, 1)
                    .setValue(WORLDGEN, false));
        }

        @Override
        protected void createBlockStateDefinition(final StateDefinition.Builder<Block, BlockState> builder) {
            super.createBlockStateDefinition(builder);
            builder.add(PART, COLOR, WORLDGEN);
        }

        @Override
        public int updateHydro(final LevelAccessor level, final BlockPos pos,
                               final BlockState state, final boolean worldGen) {
            if (isSpecial(state)) return state.getValue(DISTANCE);
            return super.updateHydro(level, pos, state, worldGen);
        }

        @Override
        public GrowSignal branchOut(final Level level, final BlockPos pos, final GrowSignal signal) {
            if (isGrowthSpecial(level.getBlockState(pos))
                    && belongsToAnotherTree(level, pos, signal.rootPos)) {
                signal.success = false;
                return signal;
            }
            return super.branchOut(level, pos, signal);
        }

        private static boolean belongsToAnotherTree(final Level level, final BlockPos pos,
                                                    @Nullable final BlockPos growingRoot) {
            if (growingRoot == null) return false;
            BlockPos nearestRoot = null;
            int nearestDistance = Integer.MAX_VALUE;
            for (final BlockPos mutable : BlockPos.betweenClosed(pos.offset(-12, -12, -12),
                    pos.offset(12, 12, 12))) {
                final BlockPos anchor = mutable.immutable();
                if (!level.hasChunkAt(anchor)) continue;
                final BlockState anchorState = level.getBlockState(anchor);
                if (!isGrowthSpecial(anchorState) || !isCluster(anchorState)) continue;
                final BlockPos support = anchor.below();
                if (!(level.getBlockState(support).getBlock() instanceof BranchBlock)) continue;
                final BlockPos root = TreeHelper.findRootNode(level, support);
                if (root == null) continue;
                final int distance = squaredDistance(pos, anchor);
                if (distance < nearestDistance || distance == nearestDistance && root.equals(growingRoot)) {
                    nearestDistance = distance;
                    nearestRoot = root;
                }
            }
            return nearestRoot != null && !nearestRoot.equals(growingRoot);
        }

        private static int squaredDistance(final BlockPos first, final BlockPos second) {
            final int x = first.getX() - second.getX();
            final int y = first.getY() - second.getY();
            final int z = first.getZ() - second.getZ();
            return x * x + y * y + z * z;
        }

        @Override
        public SoundType getSoundType(final BlockState state, final LevelReader level,
                                      final BlockPos pos, @Nullable final Entity entity) {
            final BlockState primitive = getLeavesProperties().getPrimitiveLeaves();
            return primitive.getBlock().getSoundType(primitive, level, pos, entity);
        }
    }

    public enum Part implements StringRepresentable {
        NORMAL("normal"),
        MEMBRANE("membrane"),
        CLUSTER("cluster");

        private final String name;

        Part(final String name) {
            this.name = name;
        }

        @Override
        public String getSerializedName() {
            return name;
        }
    }
}
