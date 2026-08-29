package com.dannykim.dtbetterend.systems.featuregen;

import com.dtteam.dynamictrees.api.network.MapSignal;
import com.dtteam.dynamictrees.block.branch.BranchBlock;
import com.dtteam.dynamictrees.block.branch.TrunkShellBlock;
import com.dtteam.dynamictrees.systems.genfeature.GenFeatureConfiguration;
import com.dtteam.dynamictrees.systems.genfeature.context.PostGrowContext;
import com.dtteam.dynamictrees.systems.nodemapper.FindEndsNode;
import com.dtteam.dynamictrees.tree.TreeHelper;
import com.dannykim.dtbetterend.systems.leaves.UmbrellaLeavesProperties;
import com.dannykim.dtbetterend.systems.umbrella.UmbrellaGrowthForm;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.dannykim.dtbetterend.systems.featuregen.UmbrellaTreeCanopyGenFeature.MAX_RADIUS;
import static com.dannykim.dtbetterend.systems.featuregen.UmbrellaTreeCanopyGenFeature.MEMBRANE_BLOCK;
import static com.dannykim.dtbetterend.systems.featuregen.UmbrellaTreeCanopyGenFeature.TARGET_BLOCK;

public final class UmbrellaTreeGrowthCanopy {
    private UmbrellaTreeGrowthCanopy() {
    }

    public static boolean postGrow(final GenFeatureConfiguration configuration,
                                   final PostGrowContext context) {
        if (context.fertility() <= 0 || configuration.get(MEMBRANE_BLOCK) == Blocks.AIR) return false;
        final FindEndsNode endFinder = new FindEndsNode();
        TreeHelper.startAnalysisFromRoot(context.level(), context.pos(), new MapSignal(endFinder));
        return placeGrowingCanopies(configuration, context.level(), context.pos(), endFinder.getEnds(),
                TreeHelper.getRadius(context.level(), context.treePos()));
    }

    private static boolean placeGrowingCanopies(final GenFeatureConfiguration configuration,
                                                final LevelAccessor level, final BlockPos rootPos,
                                                final List<BlockPos> endPoints, final int trunkRadius) {
        if (endPoints.isEmpty() || configuration.get(TARGET_BLOCK) == Blocks.AIR) return false;
        final int crownCount = growthForm(level, rootPos) == 2 ? 2 : 1;
        final List<BlockPos> crowns = endPoints.stream().distinct()
                .sorted((first, second) -> Integer.compare(score(level, second), score(level, first)))
                .limit(crownCount).toList();
        final List<Integer> variations = crowns.stream()
                .map(endPoint -> nextGrowthVariation(level, endPoint,
                        configuration.get(TARGET_BLOCK), configuration.get(MAX_RADIUS) + 3))
                .toList();
        final boolean refresh = crowns.stream().anyMatch(endPoint -> !hasCurrentGrowthCrown(level, endPoint));
        if (!refresh) return true;
        final List<Integer> radii = crowns.stream().map(endPoint -> {
            final int height = Math.max(1, endPoint.getY() - rootPos.getY());
            return Mth.clamp(2 + height / 9 + Math.max(0, trunkRadius - 8) / 8,
                    2, configuration.get(MAX_RADIUS));
        }).toList();
        for (final BlockPos endPoint : crowns) {
            clearPreviousGrowthCanopy(level, rootPos, endPoint, configuration.get(TARGET_BLOCK),
                    configuration.get(MAX_RADIUS) + 3);
        }
        int placed = 0;
        for (int crownIndex = 0; crownIndex < crowns.size(); crownIndex++) {
            final BlockPos endPoint = crowns.get(crownIndex);
            placed += placeMembrane(configuration, level, endPoint.below(), radii.get(crownIndex),
                    false, false, variations.get(crownIndex));
        }
        for (int crownIndex = 0; crownIndex < crowns.size(); crownIndex++) {
            restoreGrowthCrownAnchor(level, crowns.get(crownIndex), configuration.get(TARGET_BLOCK),
                    variations.get(crownIndex));
        }
        return placed > 0;
    }

    private static void restoreGrowthCrownAnchor(final LevelAccessor level, final BlockPos endPoint,
                                                 final Block target, final int variation) {
        final BlockPos anchor = endPoint.above();
        final BlockState state = level.getBlockState(anchor);
        if (state.getBlock() instanceof BranchBlock || state.getBlock() instanceof TrunkShellBlock) return;
        level.setBlock(anchor, UmbrellaLeavesProperties.clusterState(target, false,
                Math.floorMod(variation, 8)), Block.UPDATE_CLIENTS);
    }

    private static boolean hasCurrentGrowthCrown(final LevelAccessor level,
                                                 final BlockPos endPoint) {
        return isGrowthCluster(level.getBlockState(endPoint.above()));
    }

    private static boolean isGrowthCluster(final BlockState state) {
        return UmbrellaLeavesProperties.isGrowthSpecial(state)
                && UmbrellaLeavesProperties.isCluster(state);
    }

    private static int nextGrowthVariation(final LevelAccessor level, final BlockPos endPoint,
                                           final Block target, final int radius) {
        int nearestDistance = Integer.MAX_VALUE;
        int previousVariation = -1;
        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                if (x * x + z * z > radius * radius) continue;
                for (int y = -radius; y <= radius; y++) {
                    final BlockState state = level.getBlockState(endPoint.offset(x, y, z));
                    if (!state.is(target) || !UmbrellaLeavesProperties.isGrowthSpecial(state)
                            || !UmbrellaLeavesProperties.isCluster(state)) continue;
                    final int distance = x * x + y * y + z * z;
                    if (distance < nearestDistance) {
                        nearestDistance = distance;
                        previousVariation = state.getValue(UmbrellaLeavesProperties.COLOR);
                    }
                }
            }
        }
        return Math.floorMod(previousVariation + 1, 8);
    }

    private static void clearPreviousGrowthCanopy(final LevelAccessor level, final BlockPos rootPos,
                                                  final BlockPos center, final Block target,
                                                  final int radius) {
        final int verticalRadius = radius + 3;
        final Set<BlockPos> protectedAnchors = findForeignGrowthCrownAnchors(
                level, rootPos, center, target, radius, verticalRadius);
        final BlockPos currentAnchor = center.above();
        for (int x = -radius; x <= radius; x++) for (int z = -radius; z <= radius; z++) {
            if (x * x + z * z > radius * radius) continue;
            for (int y = -verticalRadius; y <= verticalRadius; y++) {
                final BlockPos pos = center.offset(x, y, z);
                final BlockState state = level.getBlockState(pos);
                if (state.is(target) && (!UmbrellaLeavesProperties.isSpecial(state)
                        || UmbrellaLeavesProperties.isGrowthSpecial(state))
                        && !belongsToForeignCrown(pos, currentAnchor, protectedAnchors)) {
                    level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
                }
            }
        }
    }

    private static Set<BlockPos> findForeignGrowthCrownAnchors(final LevelAccessor level,
                                                                final BlockPos rootPos,
                                                                final BlockPos center,
                                                                final Block target,
                                                                final int radius,
                                                                final int verticalRadius) {
        final Set<BlockPos> anchors = new HashSet<>();
        if (!(level instanceof Level concreteLevel)) return anchors;
        for (final BlockPos mutable : BlockPos.betweenClosed(
                center.offset(-radius, -verticalRadius, -radius),
                center.offset(radius, verticalRadius, radius))) {
            final BlockPos anchor = mutable.immutable();
            if (!level.getBlockState(anchor).is(target) || !isGrowthCluster(level.getBlockState(anchor))) continue;
            final BlockPos support = anchor.below();
            if (!(level.getBlockState(support).getBlock() instanceof BranchBlock)) continue;
            final BlockPos supportingRoot = TreeHelper.findRootNode(concreteLevel, support);
            if (supportingRoot != null && !supportingRoot.equals(rootPos)) anchors.add(anchor);
        }
        return anchors;
    }

    private static boolean belongsToForeignCrown(final BlockPos pos, final BlockPos currentAnchor,
                                                  final Set<BlockPos> foreignAnchors) {
        final long currentDistance = distanceSquared(pos, currentAnchor);
        for (final BlockPos foreignAnchor : foreignAnchors) {
            if (distanceSquared(pos, foreignAnchor) <= currentDistance) return true;
        }
        return false;
    }

    private static long distanceSquared(final BlockPos first, final BlockPos second) {
        final long x = first.getX() - second.getX();
        final long y = first.getY() - second.getY();
        final long z = first.getZ() - second.getZ();
        return x * x + y * y + z * z;
    }

    public static boolean placeInitialGrowthCanopy(final LevelAccessor level,
                                                   final BlockPos branchPos,
                                                   final Block target) {
        return target != null && target != Blocks.AIR
                && placeMembrane(level, branchPos, 2, target, false, false, 0L) > 0;
    }

    private static int placeMembrane(final GenFeatureConfiguration configuration,
                                     final LevelAccessor level, final BlockPos center,
                                     final int radius, final boolean worldGen,
                                     final boolean clearOutside) {
        return placeMembrane(configuration, level, center, radius, worldGen, clearOutside, 0L);
    }

    private static int placeMembrane(final GenFeatureConfiguration configuration,
                                     final LevelAccessor level, final BlockPos center,
                                     final int radius, final boolean worldGen,
                                     final boolean clearOutside, final long variation) {
        return placeMembrane(level, center, radius, configuration.get(TARGET_BLOCK),
                worldGen, clearOutside, variation);
    }

    private static int placeMembrane(final LevelAccessor level, final BlockPos center,
                                     final int radius, final Block target,
                                     final boolean worldGen, final boolean clearOutside,
                                     final long variation) {
        final int verticalRange = radius + 3;
        final Set<BlockPos> placedPositions = new HashSet<>();

        for (int x = -radius - 2; x <= radius + 2; x++) {
            for (int z = -radius - 2; z <= radius + 2; z++) {
                final double radialSq = (double) x * x + (double) z * z;
                final double localRadius = radius + ((radius == 4 || radius == 5) ? 0.5 : 0.35);
                final double radiusSq = localRadius * localRadius;
                if (radialSq > radiusSq) continue;

                final double normalizedRadius = Math.sqrt(radialSq) / localRadius;
                final int peakHeight = worldGen ? Math.max(3, Mth.ceil(radius * 0.4))
                        : Math.max(2, Mth.ceil(radius * 0.4));
                final int rimDrop = worldGen ? Math.max(4, Mth.ceil(radius * 0.8))
                        : Math.max(2, Mth.ceil(radius * 0.8));
                final int calculatedSurfaceY = peakHeight
                        - Mth.floor(rimDrop * Math.pow(normalizedRadius, 1.65));
                final int surfaceY = calculatedSurfaceY;
                final int thickness = normalizedRadius < 0.72 && (worldGen || radius >= 4) ? 2 : 1;
                final int color = Math.sqrt(radialSq) <= 2.5
                        ? 0
                        : Mth.clamp(Mth.floor(normalizedRadius * 7.0), 1, 7);
                for (int y = -verticalRange; y <= verticalRange; y++) {
                    final boolean inUmbrella = y <= surfaceY && y > surfaceY - thickness;
                    final BlockPos pos = center.offset(x, y, z);
                    final BlockState state = level.getBlockState(pos);

                    if (inUmbrella && !(x == 0 && z == 0 && y == 1)) {
                        if (!(state.getBlock() instanceof BranchBlock)
                                && !isGrowthCluster(state)
                                && (worldGen || !state.is(target)
                                && (state.isAir() || state.canBeReplaced()))) {
                            level.setBlock(pos, UmbrellaLeavesProperties.membraneState(target, color, worldGen),
                                    Block.UPDATE_CLIENTS);
                            placedPositions.add(pos.immutable());
                        }
                    } else if (clearOutside && state.is(target)) {
                        level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
                    }
                }
            }
        }
        placeConnectedClusters(level, center, radius, target, placedPositions, worldGen, variation);
        return removeDisconnectedMembrane(level, center.above(), placedPositions);
    }

    private static void placeConnectedClusters(final LevelAccessor level, final BlockPos center,
                                               final int radius, final Block target,
                                               final Set<BlockPos> placedPositions,
                                               final boolean worldGen, final long variation) {
        // World-generated trees receive these decorations from UmbrellaTreeClustersGenFeature.
        if (worldGen) return;

        final BlockPos junction = center.above();
        final int maxCount = 72;
        int placed = 0;

        for (final Direction direction : Direction.Plane.HORIZONTAL) {
            if (placeGrowthCluster(level, junction.relative(direction), target, placedPositions,
                    variation, center, false)) placed++;
        }
        if (placed < maxCount && placeGrowthCluster(level, junction.above(), target, placedPositions,
                variation, center, true)) placed++;

        final BlockState branchState = level.getBlockState(junction);
        final int branchRadius = branchState.getBlock() instanceof BranchBlock branch
                ? branch.getRadius(branchState) : 8;
        final int ringRadius = Math.max(1, Math.min(3, (branchRadius + 7) / 8));
        final int layers = radius >= 8 ? 2 : 1;
        for (int layer = 0; layer < layers && placed < maxCount; layer++) {
            final int layerRadius = Math.min(4, ringRadius + layer);
            final int y = junction.getY() + 1 + layer;
            final int offset = Math.floorMod(Long.hashCode(variation ^ junction.asLong()),
                    Math.max(1, layerRadius * 2));
            for (int x = -layerRadius; x <= layerRadius && placed < maxCount; x++) {
                for (int z = -layerRadius; z <= layerRadius && placed < maxCount; z++) {
                    if (Math.max(Math.abs(x), Math.abs(z)) != layerRadius) continue;
                    final BlockPos candidate = new BlockPos(junction.getX() + x, y, junction.getZ() + z);
                    if (Math.floorMod(x + z + offset, 3) != 0
                            && clusterChance(candidate, variation) >= 0.42F) continue;
                    if (placeGrowthCluster(level, candidate, target, placedPositions,
                            variation, center, true)) placed++;
                }
            }
        }
    }

    private static boolean placeGrowthCluster(final LevelAccessor level, final BlockPos pos,
                                                final Block target, final Set<BlockPos> placedPositions,
                                                final long variation, final BlockPos center,
                                                final boolean requireMembraneAbove) {
        if (!level.hasChunkAt(pos) || !level.hasChunkAt(pos.above())) return false;
        final BlockState currentState = level.getBlockState(pos);
        if (UmbrellaLeavesProperties.isCluster(currentState)
                || currentState.getBlock() instanceof BranchBlock
                || currentState.getBlock() instanceof TrunkShellBlock) return false;
        if (!currentState.isAir() && !currentState.is(target) && !currentState.canBeReplaced()) return false;
        if (requireMembraneAbove && !level.getBlockState(pos.above()).is(target)) return false;
        level.setBlock(pos, UmbrellaLeavesProperties.clusterState(target, false,
                Math.floorMod((int) variation, 8)), Block.UPDATE_CLIENTS);
        placedPositions.add(pos.immutable());
        return true;
    }

    private static float clusterChance(final BlockPos pos, final long variation) {
        long hash = variation ^ pos.asLong() * 0x9E3779B97F4A7C15L;
        hash ^= hash >>> 33;
        hash *= 0xff51afd7ed558ccdL;
        hash ^= hash >>> 33;
        return (hash & 0xFFFFFFL) / (float) 0x1000000L;
    }

    private static int removeDisconnectedMembrane(final LevelAccessor level, final BlockPos branchTip,
                                                  final Set<BlockPos> placedPositions) {
        final Set<BlockPos> connected = new HashSet<>();
        final ArrayDeque<BlockPos> open = new ArrayDeque<>();
        for (final Direction direction : Direction.values()) {
            final BlockPos seed = branchTip.relative(direction).immutable();
            if (placedPositions.contains(seed)) open.addLast(seed);
        }
        while (!open.isEmpty()) {
            final BlockPos pos = open.removeFirst();
            if (!connected.add(pos)) continue;
            for (int offsetX = -1; offsetX <= 1; offsetX++) {
                for (int offsetY = -1; offsetY <= 1; offsetY++) {
                    for (int offsetZ = -1; offsetZ <= 1; offsetZ++) {
                        if (offsetX == 0 && offsetY == 0 && offsetZ == 0) continue;
                        final BlockPos next = pos.offset(offsetX, offsetY, offsetZ).immutable();
                        if (placedPositions.contains(next) && !connected.contains(next)) open.addLast(next);
                    }
                }
            }
        }
        for (final BlockPos pos : placedPositions) {
            if (!connected.contains(pos)) {
                level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
            }
        }
        return connected.size();
    }

    private static int score(final LevelAccessor level, final BlockPos pos) {
        return pos.getY() * 32 + TreeHelper.getRadius(level, pos);
    }

    private static int growthForm(final LevelAccessor level, final BlockPos rootPos) {
        return UmbrellaGrowthForm.form(level, rootPos, level.getRandom());
    }
}
