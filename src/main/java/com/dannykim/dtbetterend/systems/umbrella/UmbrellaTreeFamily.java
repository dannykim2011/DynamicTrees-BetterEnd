package com.dannykim.dtbetterend.systems.umbrella;

import com.dannykim.dtbetterend.systems.leaves.UmbrellaLeavesProperties;
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
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayDeque;
import java.util.ArrayList;
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
        final CanopyRestoration restoration = collectAttachedBlocks(
                level, cutPos, species, tool, endPoints, destroyedLeaves, drops);
        super.destroyLeaves(level, cutPos, species, tool, endPoints, destroyedLeaves, drops);
        restoration.restore(level);
    }

    static CanopyRestoration collectAttachedBlocks(final Level level, final BlockPos cutPos,
                                                    final Species species, final ItemStack tool,
                                                    final List<BlockPos> endPoints,
                                                    final Map<BlockPos, BlockState> destroyedLeaves,
                                                    final List<BranchBlock.ItemStackPos> drops) {
        final ArrayDeque<BlockPos> open = new ArrayDeque<>();
        final List<BlockPos> ownedAnchors = new ArrayList<>();
        for (final BlockPos endPoint : endPoints) {
            final BlockPos anchor = findAttachedCanopyAnchor(level, endPoint);
            if (anchor != null && !ownedAnchors.contains(anchor)) ownedAnchors.add(anchor);
        }
        if (ownedAnchors.isEmpty()) return CanopyRestoration.EMPTY;

        final Map<BlockPos, BlockState> connected = new java.util.HashMap<>();
        open.addAll(ownedAnchors);
        while (!open.isEmpty() && connected.size() < 16384) {
            final BlockPos pos = open.removeFirst();
            final BlockState state = level.getBlockState(pos);
            if (connected.containsKey(pos) || !isCanopyBlock(state)) continue;
            connected.put(pos, state);
            for (int offsetX = -1; offsetX <= 1; offsetX++) {
                for (int offsetY = -1; offsetY <= 1; offsetY++) {
                    for (int offsetZ = -1; offsetZ <= 1; offsetZ++) {
                        if (offsetX == 0 && offsetY == 0 && offsetZ == 0) continue;
                        final BlockPos next = pos.offset(offsetX, offsetY, offsetZ).immutable();
                        if (!connected.containsKey(next) && isCanopyBlock(level.getBlockState(next))) {
                            open.addLast(next);
                        }
                    }
                }
            }
        }
        final BlockPos cutRoot = TreeHelper.findRootNode(level, cutPos);
        final List<BlockPos> survivingAnchors = findSurvivingAnchors(
                level, connected.keySet(), ownedAnchors, cutRoot);
        final List<BlockPos> allAnchors = new ArrayList<>(ownedAnchors);
        allAnchors.addAll(survivingAnchors);
        final Map<BlockPos, BlockState> renderedCanopy = rebuildCanopies(
                connected, ownedAnchors, allAnchors);
        final Map<BlockPos, BlockState> survivorCanopy = rebuildCanopies(
                connected, survivingAnchors, allAnchors);

        for (final Map.Entry<BlockPos, BlockState> entry : renderedCanopy.entrySet()) {
            final BlockPos pos = entry.getKey();
            final BlockState state = entry.getValue();
            destroyedLeaves.put(pos.subtract(cutPos), state);
            if (isMembrane(state)) {
                for (final ItemStack drop : species.getLeavesProperties().getDrops(level, pos, tool, species)) {
                    drops.add(new BranchBlock.ItemStackPos(drop, pos.subtract(cutPos)));
                }
            }
            if (isCluster(state)) {
                final Block clusterBlock = UmbrellaLeavesProperties.isCluster(state)
                        ? BuiltInRegistries.BLOCK.get(CLUSTER_ID)
                        : state.getBlock();
                final ItemStack cluster = new ItemStack(clusterBlock);
                if (!cluster.isEmpty()) drops.add(new BranchBlock.ItemStackPos(cluster, pos.subtract(cutPos)));
            }
        }
        for (final BlockPos pos : connected.keySet()) {
            if (isCanopyBlock(level.getBlockState(pos))) {
                level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
            }
        }
        return new CanopyRestoration(survivorCanopy, survivingAnchors);
    }

    private static BlockPos findAttachedCanopyAnchor(final Level level, final BlockPos endPoint) {
        BlockPos fallback = null;
        for (int distance = 1; distance <= 5; distance++) {
            final BlockPos seed = endPoint.above(distance).immutable();
            final BlockState state = level.getBlockState(seed);
            if (isCluster(state)) return seed;
            if (fallback == null && isCanopyBlock(state)) fallback = seed;
        }
        return fallback;
    }

    private static List<BlockPos> findSurvivingAnchors(final Level level,
                                                       final Set<BlockPos> canopy,
                                                       final List<BlockPos> ownedAnchors,
                                                       final BlockPos cutRoot) {
        final List<BlockPos> survivors = new ArrayList<>();
        for (final BlockPos pos : canopy) {
            if (!isCluster(level.getBlockState(pos)) || ownedAnchors.contains(pos)) continue;
            final BlockPos support = pos.below();
            if (!(level.getBlockState(support).getBlock() instanceof BranchBlock)) continue;
            final BlockPos root = TreeHelper.findRootNode(level, support);
            if (root != null && (cutRoot == null || !root.equals(cutRoot))) survivors.add(pos);
        }
        return survivors;
    }

    private static BlockPos nearestAnchor(final BlockPos pos, final List<BlockPos> anchors,
                                          final List<BlockPos> preferred) {
        BlockPos nearest = null;
        long nearestDistance = Long.MAX_VALUE;
        for (final BlockPos anchor : anchors) {
            final long distance = distanceSquared(pos, anchor);
            if (distance < nearestDistance
                    || distance == nearestDistance && preferred.contains(anchor)) {
                nearest = anchor;
                nearestDistance = distance;
            }
        }
        return nearest;
    }

    private static Map<BlockPos, BlockState> rebuildCanopies(
            final Map<BlockPos, BlockState> connected, final List<BlockPos> selectedAnchors,
            final List<BlockPos> allAnchors) {
        final Map<BlockPos, BlockState> rebuilt = new java.util.HashMap<>();
        for (final BlockPos anchor : selectedAnchors) {
            final BlockState anchorState = connected.get(anchor);
            if (anchorState == null || !UmbrellaLeavesProperties.isGrowthSpecial(anchorState)) {
                for (final Map.Entry<BlockPos, BlockState> entry : connected.entrySet()) {
                    if (anchor.equals(nearestAnchor(entry.getKey(), allAnchors, selectedAnchors))) {
                        rebuilt.put(entry.getKey(), entry.getValue());
                    }
                }
                continue;
            }
            final int radius = inferGrowthRadius(anchor, connected, allAnchors);
            final BlockPos center = anchor.below(2);
            final Block target = anchorState.getBlock();
            final double localRadius = radius + ((radius == 4 || radius == 5) ? 0.5 : 0.35);
            final int verticalRange = radius + 3;
            for (int x = -radius - 2; x <= radius + 2; x++) {
                for (int z = -radius - 2; z <= radius + 2; z++) {
                    final double radialSquared = (double) x * x + (double) z * z;
                    if (radialSquared > localRadius * localRadius) continue;
                    final double normalizedRadius = Math.sqrt(radialSquared) / localRadius;
                    final int peakHeight = Math.max(2, Mth.ceil(radius * 0.4));
                    final int rimDrop = Math.max(2, Mth.ceil(radius * 0.8));
                    final int surfaceY = peakHeight
                            - Mth.floor(rimDrop * Math.pow(normalizedRadius, 1.65));
                    final int thickness = normalizedRadius < 0.72 && radius >= 4 ? 2 : 1;
                    final int color = Math.sqrt(radialSquared) <= 2.5 ? 0
                            : Mth.clamp(Mth.floor(normalizedRadius * 7.0), 1, 7);
                    for (int y = -verticalRange; y <= verticalRange; y++) {
                        if (y > surfaceY || y <= surfaceY - thickness
                                || x == 0 && z == 0 && y == 1) continue;
                        rebuilt.put(center.offset(x, y, z).immutable(),
                                UmbrellaLeavesProperties.membraneState(target, color, false));
                    }
                }
            }
            rebuilt.put(anchor, anchorState);
            for (final Map.Entry<BlockPos, BlockState> entry : connected.entrySet()) {
                if (isCluster(entry.getValue())
                        && anchor.equals(nearestAnchor(entry.getKey(), allAnchors, selectedAnchors))) {
                    rebuilt.put(entry.getKey(), entry.getValue());
                }
            }
        }
        return rebuilt;
    }

    private static int inferGrowthRadius(final BlockPos anchor,
                                         final Map<BlockPos, BlockState> connected,
                                         final List<BlockPos> allAnchors) {
        final BlockPos center = anchor.below(2);
        int maximumSquared = 4;
        for (final Map.Entry<BlockPos, BlockState> entry : connected.entrySet()) {
            if (!UmbrellaLeavesProperties.isMembrane(entry.getValue())
                    || !anchor.equals(nearestAnchor(entry.getKey(), allAnchors, List.of(anchor)))) continue;
            final int x = entry.getKey().getX() - center.getX();
            final int z = entry.getKey().getZ() - center.getZ();
            maximumSquared = Math.max(maximumSquared, x * x + z * z);
        }
        return Mth.clamp((int) Math.ceil(Math.sqrt(maximumSquared)), 2, 10);
    }

    private static long distanceSquared(final BlockPos first, final BlockPos second) {
        final long x = first.getX() - second.getX();
        final long y = first.getY() - second.getY();
        final long z = first.getZ() - second.getZ();
        return x * x + y * y + z * z;
    }

    private static boolean isCanopyBlock(final BlockState state) {
        return UmbrellaLeavesProperties.isSpecial(state) || isCluster(state) || isMembrane(state);
    }

    private static boolean isCluster(final BlockState state) {
        return UmbrellaLeavesProperties.isCluster(state)
                || CLUSTER_ID.equals(BuiltInRegistries.BLOCK.getKey(state.getBlock()));
    }

    private static boolean isMembrane(final BlockState state) {
        return UmbrellaLeavesProperties.isMembrane(state)
                || MEMBRANE_ID.equals(BuiltInRegistries.BLOCK.getKey(state.getBlock()));
    }

    static final class CanopyRestoration {
        static final CanopyRestoration EMPTY = new CanopyRestoration(Map.of(), List.of());
        private final Map<BlockPos, BlockState> blocks;
        private final List<BlockPos> anchors;

        private CanopyRestoration(final Map<BlockPos, BlockState> blocks,
                                  final List<BlockPos> anchors) {
            this.blocks = blocks;
            this.anchors = anchors;
        }

        void restore(final Level level) {
            for (final Map.Entry<BlockPos, BlockState> entry : blocks.entrySet()) {
                final BlockState current = level.getBlockState(entry.getKey());
                if (current.isAir() || current.canBeReplaced() || isCanopyBlock(current)) {
                    level.setBlock(entry.getKey(), entry.getValue(), Block.UPDATE_CLIENTS);
                }
            }
            for (final BlockPos anchor : anchors) {
                if (!isCluster(level.getBlockState(anchor))
                        && level.getBlockState(anchor.below()).getBlock() instanceof BranchBlock) {
                    final BlockState original = blocks.get(anchor);
                    if (original != null) level.setBlock(anchor, original, Block.UPDATE_CLIENTS);
                }
            }
        }
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
        final UmbrellaTreeBranchBlock.CanopyRestoration restoration =
                UmbrellaTreeBranchBlock.collectAttachedBlocks(
                level, cutPos, species, tool, endPoints, destroyedLeaves, drops);
        super.destroyLeaves(level, cutPos, species, tool, endPoints, destroyedLeaves, drops);
        restoration.restore(level);
    }
}
