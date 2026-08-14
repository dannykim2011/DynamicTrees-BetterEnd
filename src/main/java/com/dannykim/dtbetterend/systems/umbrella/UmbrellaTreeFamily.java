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
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class UmbrellaTreeFamily extends Family {
    public static final TypedRegistry.EntryType<Family> TYPE = TypedRegistry.newType(UmbrellaTreeFamily::new);

    public UmbrellaTreeFamily(final Identifier name) {
        super(name);
    }

    @Override
    protected BranchBlock createBranch(final Identifier name, final BlockBehaviour.Properties properties) {
        final BasicBranchBlock branch = this.isThick()
                ? new UmbrellaTreeThickBranchBlock(name, properties)
                : new UmbrellaTreeBranchBlock(name, properties);
        if (this.isFireProof()) branch.setFireSpreadSpeed(0).setFlammability(0);
        return branch;
    }
}

final class UmbrellaTreeBranchBlock extends BasicBranchBlock {
    private static final Identifier CLUSTER_ID = Identifier.fromNamespaceAndPath("betterend", "umbrella_tree_cluster");
    private static final Identifier MEMBRANE_ID = Identifier.fromNamespaceAndPath("betterend", "umbrella_tree_membrane");
    private static final ThreadLocal<Map<BlockPos, BlockState>> PENDING_CANOPY = new ThreadLocal<>();

    UmbrellaTreeBranchBlock(final Identifier name, final BlockBehaviour.Properties properties) {
        super(name, properties);
    }

    @Override
    public BranchDestructionData destroyBranchFromNode(final Level level, final BlockPos cutPos,
                                                       final Direction fromDir, final boolean wholeTree,
                                                       final LivingEntity entity) {
        disableRegrowth(level, cutPos);
        PENDING_CANOPY.set(collectBeforeFelling(level, cutPos, this));
        try {
            return super.destroyBranchFromNode(level, cutPos, fromDir, wholeTree, entity);
        } finally {
            PENDING_CANOPY.remove();
        }
    }

    @Override
    public void destroyLeaves(final Level level, final BlockPos cutPos, final Species species,
                              final ItemStack tool, final List<BlockPos> endPoints,
                              final Map<BlockPos, BlockState> destroyedLeaves,
                              final List<BranchBlock.ItemStackPos> drops) {
        mergePendingCanopy(cutPos, destroyedLeaves);
        if (!hasPendingCanopy()) collectAttachedBlocks(level, cutPos, endPoints, destroyedLeaves);
        super.destroyLeaves(level, cutPos, species, tool, endPoints, destroyedLeaves, drops);
    }

    static Map<BlockPos, BlockState> collectBeforeFelling(final Level level, final BlockPos cutPos,
                                                          final BranchBlock sourceBranch) {
        if (level.isClientSide()) return Map.of();
        final Set<BlockPos> branches = new HashSet<>();
        final ArrayDeque<BlockPos> branchQueue = new ArrayDeque<>();
        branchQueue.add(cutPos.immutable());
        while (!branchQueue.isEmpty() && branches.size() < 16384) {
            final BlockPos pos = branchQueue.removeFirst();
            if (!branches.add(pos)) continue;
            final BlockState state = level.getBlockState(pos);
            if (!(state.getBlock() instanceof BranchBlock branch)
                    || branch.getFamily() != sourceBranch.getFamily()) {
                branches.remove(pos);
                continue;
            }
            for (final Direction direction : Direction.values()) {
                final BlockPos next = pos.relative(direction).immutable();
                if (!branches.contains(next)) branchQueue.addLast(next);
            }
        }

        final Set<BlockPos> canopy = new HashSet<>();
        final Set<BlockPos> sourceSeeds = new HashSet<>();
        final ArrayDeque<BlockPos> canopyQueue = new ArrayDeque<>();
        for (final BlockPos branchPos : branches) {
            boolean foundAdjacentCanopy = false;
            for (final Direction direction : Direction.values()) {
                final BlockPos seed = branchPos.relative(direction).immutable();
                if (isCanopyBlock(level.getBlockState(seed))) {
                    foundAdjacentCanopy = true;
                    sourceSeeds.add(seed);
                    canopyQueue.addLast(seed);
                }
            }
            if (!foundAdjacentCanopy && isTerminalBranch(branchPos, branches)) {
                final BlockPos detachedSeed = findNearestCanopySeed(level, branchPos);
                if (detachedSeed != null) {
                    sourceSeeds.add(detachedSeed);
                    canopyQueue.addLast(detachedSeed);
                }
            }
        }
        while (!canopyQueue.isEmpty() && canopy.size() < 16384) {
            final BlockPos pos = canopyQueue.removeFirst();
            if (!canopy.add(pos) || !isCanopyBlock(level.getBlockState(pos))) continue;
            for (int x = -1; x <= 1; x++) for (int y = -1; y <= 1; y++) for (int z = -1; z <= 1; z++) {
                if (x == 0 && y == 0 && z == 0) continue;
                final BlockPos next = pos.offset(x, y, z).immutable();
                if (!canopy.contains(next) && isCanopyBlock(level.getBlockState(next))) canopyQueue.addLast(next);
            }
        }

        final Set<BlockPos> foreignSeeds = new HashSet<>();
        for (final BlockPos canopyPos : canopy) {
            for (final Direction direction : Direction.values()) {
                final BlockPos adjacent = canopyPos.relative(direction).immutable();
                final BlockState adjacentState = level.getBlockState(adjacent);
                if (!(adjacentState.getBlock() instanceof BranchBlock branch)
                        || branch.getFamily() != sourceBranch.getFamily()) continue;
                if (branches.contains(adjacent)) sourceSeeds.add(canopyPos);
                else foreignSeeds.add(canopyPos);
            }
        }
        if (!foreignSeeds.isEmpty()) {
            final Map<BlockPos, Integer> sourceDistances = canopyDistances(canopy, sourceSeeds);
            final Map<BlockPos, Integer> foreignDistances = canopyDistances(canopy, foreignSeeds);
            canopy.removeIf(pos -> {
                final Integer sourceDistance = sourceDistances.get(pos);
                final Integer foreignDistance = foreignDistances.get(pos);
                return sourceDistance == null
                        || foreignDistance != null && foreignDistance <= sourceDistance;
            });
        }

        final Map<BlockPos, BlockState> collected = new java.util.HashMap<>();
        for (final BlockPos pos : canopy) {
            final BlockState state = level.getBlockState(pos);
            if (!isCanopyBlock(state)) continue;
            collected.put(pos, state);
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
        }
        return collected;
    }

    static void disableRegrowth(final Level level, final BlockPos cutPos) {
        final BlockPos rootPos = TreeHelper.findRootNode(level, cutPos);
        if (rootPos == null) return;
        TreeHelper.getRootyOpt(level.getBlockState(rootPos))
                .ifPresent(rooty -> rooty.setFertility(level, rootPos, 0));
    }

    private static Map<BlockPos, Integer> canopyDistances(final Set<BlockPos> canopy,
                                                          final Set<BlockPos> seeds) {
        final Map<BlockPos, Integer> distances = new java.util.HashMap<>();
        final ArrayDeque<BlockPos> open = new ArrayDeque<>();
        for (final BlockPos seed : seeds) {
            if (canopy.contains(seed) && distances.putIfAbsent(seed, 0) == null) open.addLast(seed);
        }
        while (!open.isEmpty()) {
            final BlockPos pos = open.removeFirst();
            final int nextDistance = distances.get(pos) + 1;
            for (int x = -1; x <= 1; x++) for (int y = -1; y <= 1; y++) for (int z = -1; z <= 1; z++) {
                if (x == 0 && y == 0 && z == 0) continue;
                final BlockPos next = pos.offset(x, y, z).immutable();
                if (canopy.contains(next) && distances.putIfAbsent(next, nextDistance) == null) {
                    open.addLast(next);
                }
            }
        }
        return distances;
    }

    private static boolean isTerminalBranch(final BlockPos pos, final Set<BlockPos> branches) {
        int neighbours = 0;
        for (final Direction direction : Direction.values()) {
            if (branches.contains(pos.relative(direction)) && ++neighbours > 1) return false;
        }
        return true;
    }

    private static BlockPos findNearestCanopySeed(final Level level, final BlockPos branchEnd) {
        BlockPos nearest = null;
        int nearestDistance = Integer.MAX_VALUE;
        for (int x = -4; x <= 4; x++) for (int z = -4; z <= 4; z++) {
            if (x * x + z * z > 16) continue;
            for (int y = -3; y <= 6; y++) {
                final BlockPos pos = branchEnd.offset(x, y, z).immutable();
                if (!level.hasChunkAt(pos) || !isCanopyBlock(level.getBlockState(pos))) continue;
                final int distance = x * x + y * y + z * z;
                if (distance < nearestDistance) {
                    nearest = pos;
                    nearestDistance = distance;
                }
            }
        }
        return nearest;
    }

    static void mergePendingCanopy(final BlockPos cutPos,
                                   final Map<BlockPos, BlockState> destroyedLeaves) {
        final Map<BlockPos, BlockState> pending = PENDING_CANOPY.get();
        if (pending == null) return;
        pending.forEach((pos, state) -> destroyedLeaves.put(pos.subtract(cutPos), state));
    }

    static boolean hasPendingCanopy() {
        final Map<BlockPos, BlockState> pending = PENDING_CANOPY.get();
        return pending != null && !pending.isEmpty();
    }

    static void setPendingCanopy(final Map<BlockPos, BlockState> canopy) {
        PENDING_CANOPY.set(canopy);
    }

    static void clearPendingCanopy() {
        PENDING_CANOPY.remove();
    }

    static void collectAttachedBlocks(final Level level, final BlockPos cutPos,
                                      final List<BlockPos> endPoints,
                                      final Map<BlockPos, BlockState> destroyedLeaves) {
        final Set<BlockPos> collected = new HashSet<>();
        final ArrayDeque<BlockPos> open = new ArrayDeque<>();
        for (final BlockPos endPoint : endPoints) for (final Direction direction : Direction.values()) {
            final BlockPos seed = endPoint.relative(direction).immutable();
            if (isCanopyBlock(level.getBlockState(seed))) open.add(seed);
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
    UmbrellaTreeThickBranchBlock(final Identifier name, final BlockBehaviour.Properties properties) {
        super(name, properties);
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
    public BranchDestructionData destroyBranchFromNode(final Level level, final BlockPos cutPos,
                                                       final Direction fromDir, final boolean wholeTree,
                                                       final LivingEntity entity) {
        UmbrellaTreeBranchBlock.disableRegrowth(level, cutPos);
        UmbrellaTreeBranchBlock.setPendingCanopy(
                UmbrellaTreeBranchBlock.collectBeforeFelling(level, cutPos, this));
        try {
            return super.destroyBranchFromNode(level, cutPos, fromDir, wholeTree, entity);
        } finally {
            UmbrellaTreeBranchBlock.clearPendingCanopy();
        }
    }

    @Override
    public void destroyLeaves(final Level level, final BlockPos cutPos, final Species species,
                              final ItemStack tool, final List<BlockPos> endPoints,
                              final Map<BlockPos, BlockState> destroyedLeaves,
                              final List<BranchBlock.ItemStackPos> drops) {
        UmbrellaTreeBranchBlock.mergePendingCanopy(cutPos, destroyedLeaves);
        if (!UmbrellaTreeBranchBlock.hasPendingCanopy()) {
            UmbrellaTreeBranchBlock.collectAttachedBlocks(level, cutPos, endPoints, destroyedLeaves);
        }
        super.destroyLeaves(level, cutPos, species, tool, endPoints, destroyedLeaves, drops);
    }
}
