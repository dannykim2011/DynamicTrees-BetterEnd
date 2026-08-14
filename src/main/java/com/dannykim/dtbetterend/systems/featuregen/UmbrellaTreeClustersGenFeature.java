package com.dannykim.dtbetterend.systems.featuregen;

import com.dtteam.dynamictrees.api.configuration.ConfigurationProperty;
import com.dtteam.dynamictrees.api.network.MapSignal;
import com.dtteam.dynamictrees.block.branch.BranchBlock;
import com.dtteam.dynamictrees.block.branch.TrunkShellBlock;
import com.dtteam.dynamictrees.systems.genfeature.GenFeature;
import com.dtteam.dynamictrees.systems.genfeature.GenFeatureConfiguration;
import com.dtteam.dynamictrees.systems.genfeature.context.PostGenerationContext;
import com.dtteam.dynamictrees.systems.genfeature.context.PostRotContext;
import com.dtteam.dynamictrees.systems.nodemapper.FindEndsNode;
import com.dtteam.dynamictrees.tree.TreeHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;

import java.util.ArrayList;
import java.util.List;

public final class UmbrellaTreeClustersGenFeature extends GenFeature {
    public static final ConfigurationProperty<Block> TARGET_BLOCK =
            ConfigurationProperty.block("target_block");
    public static final ConfigurationProperty<Block> CLUSTER_BLOCK =
            ConfigurationProperty.block("cluster_block");

    public UmbrellaTreeClustersGenFeature(final Identifier registryName) {
        super(registryName);
    }

    @Override
    protected void registerProperties() {
        this.register(TARGET_BLOCK, CLUSTER_BLOCK, PLACE_CHANCE, MAX_COUNT, MAX_HEIGHT);
    }

    @Override
    protected GenFeatureConfiguration createDefaultConfiguration() {
        return super.createDefaultConfiguration()
                .with(TARGET_BLOCK, Blocks.AIR)
                .with(CLUSTER_BLOCK, Blocks.AIR)
                .with(PLACE_CHANCE, 1.0F)
                .with(MAX_COUNT, 72)
                .with(MAX_HEIGHT, 100);
    }

    @Override
    protected boolean postGenerate(
            final GenFeatureConfiguration configuration,
            final PostGenerationContext context
    ) {
        if (configuration.get(CLUSTER_BLOCK) == Blocks.AIR
                || context.random().nextFloat() > configuration.get(PLACE_CHANCE)) {
            return false;
        }

        final FindEndsNode endFinder = new FindEndsNode();
        TreeHelper.startAnalysisFromRoot(context.level(), context.pos(), new MapSignal(endFinder));
        final List<BlockPos> anchors = new ArrayList<>();
        int placed = 0;
        for (final BlockPos endPoint : endFinder.getEnds()) {
            final BlockPos junction = findJunction(context.level(), endPoint, configuration.get(TARGET_BLOCK));
            if (junction == null || isNearAnchor(anchors, junction, 8)) continue;
            anchors.add(junction);
            final int canopyRadius = measureCanopyRadius(
                    context.level(), junction, configuration.get(TARGET_BLOCK));
            final int layers = canopyRadius >= 17 ? 3 : canopyRadius >= 11 ? 2 : 1;
            placed += placeWrappedClusters(configuration, context, junction, layers,
                    configuration.get(MAX_COUNT) - placed);
            if (placed >= configuration.get(MAX_COUNT)) break;
        }
        return placed > 0;
    }

    @Override
    protected boolean postRot(
            final GenFeatureConfiguration configuration,
            final PostRotContext context
    ) {
        int removed = 0;
        for (int x = -4; x <= 4; x++) {
            for (int y = -4; y <= 4; y++) {
                for (int z = -4; z <= 4; z++) {
                    final BlockPos pos = context.pos().offset(x, y, z);
                    if (!canAccess(context.level(), pos)) continue;
                    final BlockState state = context.level().getBlockState(pos);
                    if (!state.is(configuration.get(CLUSTER_BLOCK))) continue;
                    if (hasNearbyBranch(context.level(), pos, 4)
                            || countBlocks(context.level(), pos, configuration.get(TARGET_BLOCK), 3) > 0) continue;
                    if (context.level() instanceof Level level) {
                        Block.popResource(level, pos, new ItemStack(state.getBlock()));
                    }
                    context.level().setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
                    removed++;
                }
            }
        }
        return removed > 0;
    }

    private static int placeWrappedClusters(final GenFeatureConfiguration configuration,
                                            final PostGenerationContext context,
                                            final BlockPos junction, final int layers,
                                            final int remaining) {
        if (remaining <= 0) return 0;
        if (!canAccess(context.level(), junction)) return 0;
        final BlockState branchState = context.level().getBlockState(junction);
        final int branchRadius = branchState.getBlock() instanceof BranchBlock branch
                ? branch.getRadius(branchState) : 8;
        final int ringRadius = Math.max(1, Math.min(3, (branchRadius + 7) / 8));
        final List<BlockPos> candidates = new ArrayList<>();
        candidates.add(junction.offset(1, 0, 0));
        candidates.add(junction.offset(-1, 0, 0));
        candidates.add(junction.offset(0, 0, 1));
        candidates.add(junction.offset(0, 0, -1));
        for (int layer = 0; layer < layers; layer++) {
            final int radius = Math.min(4, ringRadius + layer);
            final int y = junction.getY() - layer;
            final int offset = context.random().nextInt(Math.max(1, radius * 2));
            for (int x = -radius; x <= radius; x++) for (int z = -radius; z <= radius; z++) {
                if (Math.max(Math.abs(x), Math.abs(z)) != radius) continue;
                if (Math.floorMod(x + z + offset, 3) == 0 || context.random().nextFloat() < 0.42F)
                    candidates.add(new BlockPos(junction.getX() + x, y, junction.getZ() + z));
            }
        }
        int placed = placeCandidates(configuration, context, candidates, remaining, false);
        if (placed < Math.min(4, remaining)) {
            candidates.clear();
            for (int radius = ringRadius; radius <= Math.min(4, ringRadius + 1); radius++)
                for (int x = -radius; x <= radius; x++) for (int z = -radius; z <= radius; z++)
                    if (Math.max(Math.abs(x), Math.abs(z)) == radius) candidates.add(junction.offset(x, 0, z));
            placed += placeCandidates(configuration, context, candidates, remaining - placed, true);
        }
        return placed;
    }

    private static int placeCandidates(final GenFeatureConfiguration configuration,
                                       final PostGenerationContext context,
                                       final List<BlockPos> candidates, final int limit,
                                       final boolean skipExisting) {
        int placed = 0;
        for (final BlockPos pos : candidates) {
            if (placed >= limit) break;
            if (!canAccess(context.level(), pos) || !canAccess(context.level(), pos.above())) continue;
            final BlockState currentState = context.level().getBlockState(pos);
            if (currentState.is(configuration.get(CLUSTER_BLOCK))) continue;
            if (currentState.getBlock() instanceof BranchBlock
                    || currentState.getBlock() instanceof TrunkShellBlock) continue;
            if (!currentState.isAir()
                    && !currentState.is(configuration.get(TARGET_BLOCK))
                    && !currentState.canBeReplaced()) continue;
            if (!context.level().getBlockState(pos.above()).is(configuration.get(TARGET_BLOCK))) continue;
            context.level().setBlock(pos, withNatural(configuration.get(CLUSTER_BLOCK).defaultBlockState()), Block.UPDATE_ALL);
            placed++;
        }
        return placed;
    }

    private static BlockPos findJunction(final LevelAccessor level, final BlockPos endPoint, final Block targetLeaves) {
        BlockPos best = null;
        int bestScore = -1;
        for (int y = -6; y <= 50; y++) for (int x = -2; x <= 2; x++) for (int z = -2; z <= 2; z++) {
            final BlockPos pos = endPoint.offset(x, y, z);
            if (!canAccess(level, pos) || !canAccess(level, pos.above())) continue;
            final BlockState state = level.getBlockState(pos);
            if (!(state.getBlock() instanceof BranchBlock branch)) continue;
            if (!level.getBlockState(pos.above()).is(targetLeaves)) continue;
            final int score = branch.getRadius(state) * 8
                    - Math.abs(x) - Math.abs(y) - Math.abs(z);
            if (score > bestScore) { bestScore = score; best = pos.immutable(); }
        }
        return best;
    }

    private static int measureCanopyRadius(final LevelAccessor level, final BlockPos junction,
                                           final Block targetLeaves) {
        int maximumSquared = 0;
        for (int x = -12; x <= 12; x++) {
            for (int z = -12; z <= 12; z++) {
                final int horizontalSquared = x * x + z * z;
                if (horizontalSquared <= maximumSquared) continue;
                for (int y = -12; y <= 6; y++) {
                    final BlockPos pos = junction.offset(x, y, z);
                    if (canAccess(level, pos) && level.getBlockState(pos).is(targetLeaves)) {
                        maximumSquared = horizontalSquared;
                        break;
                    }
                }
            }
        }
        return (int) Math.floor(Math.sqrt(maximumSquared));
    }

    private static int countBlocks(final LevelAccessor level, final BlockPos center, final Block block, final int radius) {
        int count = 0;
        for (int x = -radius; x <= radius; x++) for (int y = -radius; y <= radius; y++)
            for (int z = -radius; z <= radius; z++) {
                final BlockPos pos = center.offset(x, y, z);
                if (canAccess(level, pos) && level.getBlockState(pos).is(block)) count++;
            }
        return count;
    }

    private static boolean canAccess(final LevelAccessor level, final BlockPos pos) {
        return level.hasChunkAt(pos);
    }

    private static boolean isNearAnchor(final List<BlockPos> anchors, final BlockPos candidate, final int distance) {
        final long limit = (long) distance * distance;
        for (final BlockPos anchor : anchors) {
            final long dx = anchor.getX() - candidate.getX();
            final long dz = anchor.getZ() - candidate.getZ();
            if (dx * dx + dz * dz <= limit) return true;
        }
        return false;
    }

    private static boolean hasNearbyBranch(
            final LevelAccessor level,
            final BlockPos center,
            final int radius
    ) {
        for (int x = -radius; x <= radius; x++) {
            for (int y = -radius; y <= radius; y++) {
                for (int z = -radius; z <= radius; z++) {
                    final BlockPos pos = center.offset(x, y, z);
                    if (canAccess(level, pos)
                            && level.getBlockState(pos).getBlock() instanceof BranchBlock) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private static BlockState withNatural(final BlockState state) {
        for (final Property<?> property : state.getProperties()) {
            if ("natural".equals(property.getName())) {
                return setUnchecked(state, property, true);
            }
        }
        return state;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static BlockState setUnchecked(
            final BlockState state,
            final Property property,
            final Object value
    ) {
        return state.setValue(property, (Comparable) value);
    }
}
