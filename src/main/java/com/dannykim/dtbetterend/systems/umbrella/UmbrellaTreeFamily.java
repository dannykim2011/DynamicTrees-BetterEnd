package com.dannykim.dtbetterend.systems.umbrella;

import com.dtteam.dynamictrees.api.registry.TypedRegistry;
import com.dtteam.dynamictrees.api.network.BranchDestructionData;
import com.dtteam.dynamictrees.block.branch.BasicBranchBlock;
import com.dtteam.dynamictrees.block.branch.BranchBlock;
import com.dtteam.dynamictrees.block.branch.ThickBranchBlock;
import com.dtteam.dynamictrees.tree.family.Family;
import com.dtteam.dynamictrees.tree.TreeHelper;
import com.dtteam.dynamictrees.tree.species.Species;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class UmbrellaTreeFamily extends Family {
    public static final TypedRegistry.EntryType<Family> TYPE = TypedRegistry.newType(UmbrellaTreeFamily::new);

    public UmbrellaTreeFamily(final ResourceLocation name) {
        super(name);
    }

    @Override
    protected BranchBlock createBranchBlock(final ResourceLocation name) {
        final BasicBranchBlock branch = this.isThick()
                ? new UmbrellaTreeThickBranchBlock(name, this.getProperties())
                : new UmbrellaTreeBranchBlock(name, this.getProperties());
        if (this.isFireProof()) {
            branch.setFireSpreadSpeed(0).setFlammability(0);
        }
        return branch;
    }
}

final class UmbrellaTreeBranchBlock extends BasicBranchBlock {
    private static final ResourceLocation CLUSTER_ID =
            ResourceLocation.fromNamespaceAndPath("betterend", "umbrella_tree_cluster");
    private static final ResourceLocation MEMBRANE_ID =
            ResourceLocation.fromNamespaceAndPath("betterend", "umbrella_tree_membrane");

    UmbrellaTreeBranchBlock(final ResourceLocation name, final Properties properties) {
        super(name, properties);
    }

    @Override
    public BranchDestructionData destroyBranchFromNode(final Level level, final BlockPos cutPos,
                                                       final Direction fromDir, final boolean wholeTree,
                                                       final LivingEntity entity) {
        disableRegrowth(level, cutPos);
        return super.destroyBranchFromNode(level, cutPos, fromDir, wholeTree, entity);
    }

    static void disableRegrowth(final Level level, final BlockPos cutPos) {
        final BlockPos rootPos = TreeHelper.findRootNode(level, cutPos);
        if (rootPos == null) return;
        TreeHelper.getRootyOpt(level.getBlockState(rootPos))
                .ifPresent(rooty -> rooty.setFertility(level, rootPos, 0));
    }

    @Override
    public void destroyLeaves(final Level level, final BlockPos cutPos, final Species species,
                              final ItemStack tool, final List<BlockPos> endPoints,
                              final Map<BlockPos, BlockState> destroyedLeaves,
                              final List<BranchBlock.ItemStackPos> drops) {
        super.destroyLeaves(level, cutPos, species, tool, endPoints, destroyedLeaves, drops);
        collectAttachedBlocks(level, cutPos, species, tool, endPoints, destroyedLeaves, drops);
    }

    static void collectAttachedBlocks(final Level level, final BlockPos cutPos,
                                      final Species species, final ItemStack tool,
                                      final List<BlockPos> endPoints,
                                      final Map<BlockPos, BlockState> destroyedLeaves,
                                      final List<BranchBlock.ItemStackPos> drops) {
        final Set<BlockPos> collected = new HashSet<>();
        final ArrayDeque<BlockPos> open = new ArrayDeque<>();
        for (final BlockPos endPoint : endPoints) {
            addAttachedCanopySeeds(level, endPoint, open);
        }
        while (!open.isEmpty() && collected.size() < 8192) {
            final BlockPos pos = open.removeFirst();
            if (!collected.add(pos) || !isCanopyBlock(level.getBlockState(pos))) continue;
            for (int offsetX = -1; offsetX <= 1; offsetX++) {
                for (int offsetY = -1; offsetY <= 1; offsetY++) {
                    for (int offsetZ = -1; offsetZ <= 1; offsetZ++) {
                        if (offsetX == 0 && offsetY == 0 && offsetZ == 0) continue;
                        final BlockPos next = pos.offset(offsetX, offsetY, offsetZ).immutable();
                        if (!collected.contains(next) && isCanopyBlock(level.getBlockState(next))) {
                            open.addLast(next);
                        }
                    }
                }
            }
        }
        for (final BlockPos pos : collected) {
            final BlockState state = level.getBlockState(pos);
            if (!isCanopyBlock(state)) continue;
            destroyedLeaves.put(pos.subtract(cutPos), state);
            if (isMembrane(state)) {
                for (final ItemStack drop : species.getLeavesProperties().getDrops(level, pos, tool, species)) {
                    drops.add(new BranchBlock.ItemStackPos(drop, pos.subtract(cutPos)));
                }
            }
            if (isCluster(state)) {
                final ItemStack cluster = new ItemStack(state.getBlock());
                if (!cluster.isEmpty()) drops.add(new BranchBlock.ItemStackPos(cluster, pos.subtract(cutPos)));
            }
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
        }
        for (final Map.Entry<BlockPos, BlockState> entry : destroyedLeaves.entrySet()) {
            if (!isCanopyBlock(entry.getValue())) continue;
            final BlockPos worldPos = cutPos.offset(entry.getKey());
            if (isCanopyBlock(level.getBlockState(worldPos))) {
                level.setBlock(worldPos, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
            }
        }
    }

    private static void addAttachedCanopySeeds(final Level level, final BlockPos endPoint,
                                               final ArrayDeque<BlockPos> open) {
        for (int distance = 1; distance <= 5; distance++) {
            final BlockPos seed = endPoint.above(distance).immutable();
            if (isCanopyBlock(level.getBlockState(seed))) open.addLast(seed);
        }
    }

    private static boolean isCanopyBlock(final BlockState state) {
        return isCluster(state) || isMembrane(state);
    }

    private static boolean isCluster(final BlockState state) {
        return CLUSTER_ID.equals(BuiltInRegistries.BLOCK.getKey(state.getBlock()));
    }

    private static boolean isMembrane(final BlockState state) {
        return MEMBRANE_ID.equals(BuiltInRegistries.BLOCK.getKey(state.getBlock()));
    }
}

final class UmbrellaTreeThickBranchBlock extends ThickBranchBlock {
    UmbrellaTreeThickBranchBlock(final ResourceLocation name, final Properties properties) {
        super(name, properties);
    }

    @Override
    public BranchDestructionData destroyBranchFromNode(final Level level, final BlockPos cutPos,
                                                       final Direction fromDir, final boolean wholeTree,
                                                       final LivingEntity entity) {
        UmbrellaTreeBranchBlock.disableRegrowth(level, cutPos);
        return super.destroyBranchFromNode(level, cutPos, fromDir, wholeTree, entity);
    }

    @Override
    public int setRadius(final LevelAccessor level, final BlockPos pos, final int radius,
                         final Direction originDir, final int flags) {
        final int clampedRadius = Math.max(1, Math.min(radius, getMaxRadius()));
        final int result = super.setRadius(level, pos, clampedRadius, originDir, flags);
        if (clampedRadius > 8 && result <= 8) {
            final BlockState state = level.getBlockState(pos);
            if (state.getBlock() == this) {
                level.setBlock(pos, state.setValue(RADIUS_DOUBLE, clampedRadius), flags);
            }
            return clampedRadius;
        }
        return result;
    }

    @Override
    public void destroyLeaves(final Level level, final BlockPos cutPos, final Species species,
                              final ItemStack tool, final List<BlockPos> endPoints,
                              final Map<BlockPos, BlockState> destroyedLeaves,
                              final List<BranchBlock.ItemStackPos> drops) {
        super.destroyLeaves(level, cutPos, species, tool, endPoints, destroyedLeaves, drops);
        UmbrellaTreeBranchBlock.collectAttachedBlocks(
                level, cutPos, species, tool, endPoints, destroyedLeaves, drops);
    }
}
