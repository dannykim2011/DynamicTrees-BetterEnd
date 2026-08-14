package com.dannykim.dtbetterend.systems.featuregen;

import com.dtteam.dynamictrees.api.configuration.ConfigurationProperty;
import com.dtteam.dynamictrees.api.network.MapSignal;
import com.dtteam.dynamictrees.block.branch.BranchBlock;
import com.dtteam.dynamictrees.block.branch.TrunkShellBlock;
import com.dtteam.dynamictrees.systems.genfeature.GenFeature;
import com.dtteam.dynamictrees.systems.genfeature.GenFeatureConfiguration;
import com.dtteam.dynamictrees.systems.genfeature.context.PostGenerationContext;
import com.dtteam.dynamictrees.systems.genfeature.context.PreGenerationContext;
import com.dtteam.dynamictrees.systems.genfeature.context.PostGrowContext;
import com.dtteam.dynamictrees.systems.nodemapper.FindEndsNode;
import com.dtteam.dynamictrees.tree.TreeHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

import java.util.ArrayList;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class UmbrellaTreeCanopyGenFeature extends GenFeature {
    public static final ConfigurationProperty<Block> TARGET_BLOCK = ConfigurationProperty.block("target_block");
    public static final ConfigurationProperty<Block> MEMBRANE_BLOCK = ConfigurationProperty.block("membrane_block");
    public static final ConfigurationProperty<Integer> MIN_RADIUS = ConfigurationProperty.integer("min_radius");
    public static final ConfigurationProperty<Integer> MAX_RADIUS = ConfigurationProperty.integer("max_radius");

    public UmbrellaTreeCanopyGenFeature(final ResourceLocation registryName) {
        super(registryName);
    }

    public static void cleanupFailedWorldGen(final LevelAccessor level, final BlockPos rootPos,
                                             final BranchBlock branch, final Block membrane,
                                             final Block dynamicLeaves, final BlockState initialDirtState) {
        removeFailedTree(level, rootPos, null, branch, membrane, dynamicLeaves, false, initialDirtState);
    }

    @Override
    protected void registerProperties() {
        this.register(TARGET_BLOCK, MEMBRANE_BLOCK, MIN_RADIUS, MAX_RADIUS);
    }

    @Override
    protected GenFeatureConfiguration createDefaultConfiguration() {
        return super.createDefaultConfiguration()
                .with(TARGET_BLOCK, Blocks.AIR)
                .with(MEMBRANE_BLOCK, Blocks.AIR)
                .with(MIN_RADIUS, 6)
                .with(MAX_RADIUS, 10);
    }


    @Override
    protected BlockPos preGenerate(final GenFeatureConfiguration configuration,
                                   final PreGenerationContext context) {
        removeSmallJellyshrooms(context.level(), context.pos());
        return context.pos();
    }

    @Override
    protected boolean postGenerate(final GenFeatureConfiguration configuration,
                                   final PostGenerationContext context) {
        return placeCanopies(configuration, context.level(), context.pos(), context.endPoints(),
                context.radius(), context.species().getFamily().getBranch().orElse(null), false,
                context.initialDirtState());
    }

    @Override
    protected boolean postGrow(final GenFeatureConfiguration configuration, final PostGrowContext context) {
        if (context.fertility() > 7 || configuration.get(MEMBRANE_BLOCK) == Blocks.AIR) return false;
        if (growthForm(context.pos()) == 2) {
            context.species().getFamily().getBranch().ifPresent(branch -> completeDoubleBranches(
                    context.level(), context.pos(), configuration.get(TARGET_BLOCK), branch,
                    configuration.get(MAX_RADIUS)));
        }
        final FindEndsNode endFinder = new FindEndsNode();
        TreeHelper.startAnalysisFromRoot(context.level(), context.pos(), new MapSignal(endFinder));
        if (hasExistingCanopy(endFinder.getEnds(), context.level(), configuration.get(MEMBRANE_BLOCK))) return false;
        return placeCanopies(configuration, context.level(), context.pos(), endFinder.getEnds(),
                TreeHelper.getRadius(context.level(), context.treePos()),
                context.species().getFamily().getBranch().orElse(null), true, null);
    }

    public static boolean completeDoubleBranches(final LevelAccessor level, final BlockPos rootPos,
                                                 final Block dynamicLeaves, final BranchBlock branch,
                                                 final int canopyRadius) {
        if (growthForm(rootPos) != 2 || branch == null) return false;
        final FindEndsNode finder = new FindEndsNode();
        TreeHelper.startAnalysisFromRoot(level, rootPos, new MapSignal(finder));
        final List<BlockPos> ends = finder.getEnds().stream().distinct()
                .filter(pos -> horizontalDistanceSquared(rootPos, pos) > 0)
                .sorted((a, b) -> Integer.compare(score(level, b), score(level, a)))
                .limit(2).toList();
        if (ends.size() != 2) return false;
        final long hash = treeHash(rootPos);
        final int targetHeight = 50 + Math.floorMod((int) (hash >>> 21), 31);
        final List<List<BlockPos>> extensions = new ArrayList<>();
        for (final BlockPos end : ends) {
            int directionX = Integer.signum(end.getX() - rootPos.getX());
            int directionZ = Integer.signum(end.getZ() - rootPos.getZ());
            if (directionX == 0 && directionZ == 0) return false;
            final List<BlockPos> extension = buildRisingCurve(end, directionX, directionZ,
                    Math.max(9, canopyRadius + 1), rootPos.getY() + targetHeight, 3);
            if (!canPlaceBranchPath(level, extension, dynamicLeaves)) return false;
            extensions.add(extension);
        }
        for (final List<BlockPos> extension : extensions) {
            final BlockState startState = level.getBlockState(extension.get(0));
            final int startRadius = startState.getBlock() instanceof BranchBlock startBranch
                    ? Math.min(8, Math.max(4, startBranch.getRadius(startState))) : 4;
            final int steps = Math.max(1, extension.size() - 1);
            for (int index = 1; index <= steps; index++) {
                final BlockPos pos = extension.get(index);
                final int desired = Math.max(1, Mth.ceil(startRadius * (1.0 - index / (double) (steps + 1))));
                final BlockState state = level.getBlockState(pos);
                if (state.getBlock() instanceof TrunkShellBlock) {
                    level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
                }
                final int existing = state.getBlock() instanceof BranchBlock existingBranch
                        ? existingBranch.getRadius(state) : 0;
                if (desired > existing) {
                    clearShellObstacles(level, pos, desired);
                    branch.setRadius(level, pos, desired, null);
                }
            }
        }
        return true;
    }

    private static boolean canPlaceBranchPath(final LevelAccessor level, final List<BlockPos> path,
                                              final Block dynamicLeaves) {
        final Set<BlockPos> allowedCores = Set.of(path.get(0));
        for (int index = 1; index < path.size(); index++) {
            final BlockPos pos = path.get(index);
            final BlockState state = level.getBlockState(pos);
            if (state.getBlock() instanceof BranchBlock
                    || isForeignTrunkShell(level, pos, state, allowedCores)) return false;
        }
        return true;
    }

    private static boolean isForeignTrunkShell(final LevelAccessor level, final BlockPos pos,
                                               final BlockState state, final Set<BlockPos> allowedCores) {
        if (!(state.getBlock() instanceof TrunkShellBlock shell)) return false;
        final TrunkShellBlock.ShellMuse muse = shell.getMuse(level, state, pos);
        return muse == null || !allowedCores.contains(muse.pos());
    }

    private static void clearShellObstacles(final LevelAccessor level, final BlockPos corePos,
                                            final int desiredRadius) {
        if (desiredRadius <= 8) return;
        for (int x = -1; x <= 1; x++) for (int z = -1; z <= 1; z++) {
            if (x == 0 && z == 0) continue;
            final BlockPos pos = corePos.offset(x, 0, z);
            if (!level.hasChunkAt(pos)) continue;
            final BlockState state = level.getBlockState(pos);
            if (state.isAir() || state.getBlock() instanceof BranchBlock
                    || state.getBlock() instanceof TrunkShellBlock) continue;
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    private static long treeHash(final BlockPos rootPos) {
        long hash = rootPos.getX() * 341873128712L ^ rootPos.getZ() * 132897987541L;
        hash ^= hash >>> 17;
        return hash;
    }

    private static boolean hasExistingCanopy(final List<BlockPos> ends, final LevelAccessor level,
                                             final Block membrane) {
        for (final BlockPos end : ends) {
            for (final BlockPos pos : BlockPos.betweenClosed(end.offset(-2, -2, -2), end.offset(2, 2, 2))) {
                if (level.getBlockState(pos).is(membrane)) return true;
            }
        }
        return false;
    }

    private static boolean placeCanopies(final GenFeatureConfiguration configuration,
                                         final LevelAccessor level,
                                         final BlockPos rootPos,
                                         final List<BlockPos> endPoints,
                                         final int trunkRadius,
                                         final BranchBlock supportingBranch,
                                         final boolean growingTree,
                                         final BlockState initialDirtState) {
        final Block membrane = configuration.get(MEMBRANE_BLOCK);
        if (membrane == Blocks.AIR || endPoints.isEmpty()) {
            removeFailedTree(level, rootPos, null, supportingBranch, membrane,
                    configuration.get(TARGET_BLOCK), growingTree, initialDirtState);
            return false;
        }

        final int baseRadius = Mth.clamp(configuration.get(MIN_RADIUS) + trunkRadius * 3 / 4,
                configuration.get(MIN_RADIUS), configuration.get(MAX_RADIUS));
        final BlockPos candidate = endPoints.stream()
                .min((first, second) -> {
                    final int firstDistance = horizontalDistanceSquared(rootPos, first);
                    final int secondDistance = horizontalDistanceSquared(rootPos, second);
                    return firstDistance != secondDistance
                            ? Integer.compare(firstDistance, secondDistance)
                            : Integer.compare(score(level, second), score(level, first));
                }).orElse(null);
        if (candidate == null) {
            removeFailedTree(level, rootPos, null, supportingBranch, membrane,
                    configuration.get(TARGET_BLOCK), growingTree, initialDirtState);
            return false;
        }
        final List<BlockPos> candidates = new ArrayList<>();
        candidates.add(candidate);
        final BlockPos existingSecond = findSecondCrownEnd(candidate, endPoints,
                configuration.get(MIN_RADIUS) * 2);
        if (existingSecond != null) {
            candidates.add(existingSecond);
        } else if (growingTree && growthForm(rootPos) == 2) {
            final BlockPos second = createOppositeCrownBranch(level, rootPos, candidate, supportingBranch,
                    configuration.get(TARGET_BLOCK), baseRadius);
            if (second == null) {
                removeFailedTree(level, rootPos, candidate, supportingBranch, membrane,
                        configuration.get(TARGET_BLOCK), growingTree, initialDirtState);
                return false;
            }
            candidates.add(second);
        }

        final List<CanopyPlan> plans = new ArrayList<>();
        final boolean doubleCrown = candidates.size() == 2;
        final Set<BlockPos> completeSupportingTree = new HashSet<>();
        for (final BlockPos crownEnd : candidates) {
            completeSupportingTree.addAll(findBranchPath(level, rootPos, crownEnd));
        }
        for (final BlockPos crownEnd : candidates) {
            final int requestedRadius = Mth.clamp(baseRadius
                            + Math.floorMod(Long.hashCode(crownEnd.asLong()), 5) - 2,
                    configuration.get(MIN_RADIUS), configuration.get(MAX_RADIUS));
            final Set<BlockPos> supportingPath = new HashSet<>(findBranchPath(level, rootPos, crownEnd));
            final int relativeHeight = crownEnd.getY() - rootPos.getY();
            if (doubleCrown && relativeHeight > 80) {
                removeFailedTree(level, rootPos, candidate, supportingBranch, membrane,
                        configuration.get(TARGET_BLOCK), growingTree, initialDirtState);
                return false;
            }
            final int minimumLift = doubleCrown ? Math.max(0, 50 - relativeHeight) : 0;
            final int maximumLift = doubleCrown ? Math.max(0, 80 - relativeHeight) : 80;
            CanopyPlacement placement = findFreeCenter(level, crownEnd, requestedRadius,
                    configuration.get(MIN_RADIUS), membrane, configuration.get(TARGET_BLOCK), supportingBranch,
                    doubleCrown ? completeSupportingTree : supportingPath, minimumLift, maximumLift, plans, doubleCrown);
            if (placement == null && doubleCrown && !plans.isEmpty()) {
                placement = retryDoublePlacementWithShrunkFirst(level, crownEnd, requestedRadius,
                        3, membrane, configuration.get(TARGET_BLOCK), supportingBranch,
                        completeSupportingTree, minimumLift, maximumLift, plans);
            } else if (placement == null) {
                placement = findFreeCenter(level, crownEnd, configuration.get(MIN_RADIUS) - 1,
                        3, membrane, configuration.get(TARGET_BLOCK), supportingBranch,
                        supportingPath, minimumLift, maximumLift, plans, doubleCrown);
            }
            if (placement == null) {
                removeFailedTree(level, rootPos, candidate, supportingBranch, membrane,
                        configuration.get(TARGET_BLOCK), growingTree, initialDirtState);
                return false;
            }
            plans.add(new CanopyPlan(crownEnd, placement));
        }

        int placed = 0;
        for (final CanopyPlan plan : plans) {
            clearDynamicCanopy(level, plan.endPoint(), configuration.get(TARGET_BLOCK),
                    plan.placement().radius() + 2);
            placed += extendSupportingBranch(level, rootPos, plan.endPoint(), plan.placement().center().above(),
                    supportingBranch, plan.placement().radius());
            if (supportingBranch == null
                    || level.getBlockState(plan.placement().center().above()).getBlock() != supportingBranch) {
                removeFailedTree(level, rootPos, plan.endPoint(), supportingBranch, membrane,
                        configuration.get(TARGET_BLOCK), growingTree, initialDirtState);
                return false;
            }
        }
        for (final CanopyPlan plan : plans) {
            final int membranePlaced = placeMembrane(configuration, level, plan.placement().center(),
                    plan.placement().radius());
            if (membranePlaced == 0) {
                removeFailedTree(level, rootPos, plan.endPoint(), supportingBranch, membrane,
                        configuration.get(TARGET_BLOCK), growingTree, initialDirtState);
                return false;
            }
            placed += membranePlaced;
        }
        for (final BlockPos endPoint : endPoints) {
            clearDynamicCanopy(level, endPoint, configuration.get(TARGET_BLOCK), 6);
        }
        return placed > 0;
    }

    private static int placeMembrane(final GenFeatureConfiguration configuration,
                                     final LevelAccessor level,
                                     final BlockPos center,
                                     final int radius) {
        final Block target = configuration.get(TARGET_BLOCK);
        final BlockState membrane = configuration.get(MEMBRANE_BLOCK).defaultBlockState();
        final double phase = Math.floorMod(Long.hashCode(center.asLong()), 628) / 100.0;
        final int minimumRays = Math.max(5, radius / 2);
        final int rayCount = minimumRays + Math.floorMod(Long.hashCode(center.asLong() * 31L),
                Math.max(1, radius - minimumRays + 1));
        final int verticalRange = radius + 3;
        final Set<BlockPos> placedPositions = new HashSet<>();

        for (int x = -radius - 2; x <= radius + 2; x++) {
            for (int z = -radius - 2; z <= radius + 2; z++) {
                final double angle = Math.atan2(z, x);
                final double localRadius = radius + Math.sin(angle * rayCount + phase) * 0.6;
                final double radialSq = (double) x * x + (double) z * z;
                final double radiusSq = localRadius * localRadius;
                if (radialSq > radiusSq) continue;

                final double normalizedRadius = Math.sqrt(radialSq) / localRadius;
                final int peakHeight = Math.max(3, Mth.ceil(radius * 0.4));
                final int rimDrop = Math.max(4, Mth.ceil(radius * 0.8));
                final int surfaceY = peakHeight - Mth.floor(rimDrop * Math.pow(normalizedRadius, 1.65));
                final int thickness = normalizedRadius < 0.72 ? 2 : 1;
                final int color = Math.sqrt(radialSq) <= 2.5
                        ? 0
                        : Mth.clamp(Mth.floor(normalizedRadius * 7.0), 1, 7);
                for (int y = -verticalRange; y <= verticalRange; y++) {
                    final boolean inUmbrella = y <= surfaceY && y > surfaceY - thickness;
                    final BlockPos pos = center.offset(x, y, z);
                    final BlockState state = level.getBlockState(pos);

                    if (inUmbrella && !(x == 0 && z == 0 && y == 1)) {
                        if (!(state.getBlock() instanceof BranchBlock) && !state.is(membrane.getBlock())) {
                            level.setBlock(pos, withColor(membrane, color), Block.UPDATE_CLIENTS);
                            placedPositions.add(pos.immutable());
                        }
                    } else if (state.is(target)) {
                        level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
                    }
                }
            }
        }
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                final BlockPos socket = center.offset(x, 2, z);
                final BlockState state = level.getBlockState(socket);
                if (!(state.getBlock() instanceof BranchBlock) && !state.is(membrane.getBlock())) {
                    level.setBlock(socket, membrane, Block.UPDATE_CLIENTS);
                    placedPositions.add(socket.immutable());
                }
            }
        }
        final int[][] collar = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
        for (final int[] offset : collar) {
            final BlockPos socket = center.offset(offset[0], 1, offset[1]);
            final BlockState state = level.getBlockState(socket);
            if (!(state.getBlock() instanceof BranchBlock) && !state.is(membrane.getBlock())) {
                level.setBlock(socket, membrane, Block.UPDATE_CLIENTS);
                placedPositions.add(socket.immutable());
            }
        }
        return removeDisconnectedMembrane(level, center.above(), placedPositions);
    }

    private static BlockState withColor(final BlockState state, final int color) {
        for (final var property : state.getProperties()) {
            if (property instanceof IntegerProperty integerProperty && "color".equals(property.getName())) {
                return state.setValue(integerProperty, Mth.clamp(color, 0, 7));
            }
        }
        return state;
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

    private static int horizontalDistanceSquared(final BlockPos rootPos, final BlockPos pos) {
        final int x = pos.getX() - rootPos.getX();
        final int z = pos.getZ() - rootPos.getZ();
        return x * x + z * z;
    }

    private static int growthForm(final BlockPos rootPos) {
        return Math.floorMod((int) (treeHash(rootPos) >>> 7), 3);
    }

    private static BlockPos findSecondCrownEnd(final BlockPos primary, final List<BlockPos> endPoints,
                                               final int minimumSeparation) {
        BlockPos best = null;
        int bestDistance = minimumSeparation * minimumSeparation - 1;
        for (final BlockPos endPoint : endPoints) {
            if (endPoint.equals(primary) || Math.abs(endPoint.getY() - primary.getY()) > 6) continue;
            final int distance = horizontalDistanceSquared(primary, endPoint);
            if (distance > bestDistance) {
                bestDistance = distance;
                best = endPoint;
            }
        }
        return best;
    }

    private static BlockPos createOppositeCrownBranch(final LevelAccessor level, final BlockPos rootPos,
                                                       final BlockPos primaryEnd, final BranchBlock branch,
                                                       final Block dynamicLeaves, final int canopyRadius) {
        if (branch == null) return null;
        final List<BlockPos> mainPath = findBranchPath(level, rootPos, primaryEnd);
        if (mainPath.size() < 8) return null;
        final int splitHeight = rootPos.getY() + Math.max(8, (primaryEnd.getY() - rootPos.getY()) / 2);
        BlockPos split = mainPath.get(0);
        for (final BlockPos pos : mainPath) {
            if (Math.abs(pos.getY() - splitHeight) < Math.abs(split.getY() - splitHeight)) split = pos;
        }

        final int dx = primaryEnd.getX() - split.getX();
        final int dz = primaryEnd.getZ() - split.getZ();
        int firstX = Integer.signum(dx);
        int firstZ = Integer.signum(dz);
        if (firstX == 0 && firstZ == 0) {
            final int[][] vectors = {{0, -1}, {1, -1}, {1, 0}, {1, 1},
                    {0, 1}, {-1, 1}, {-1, 0}, {-1, -1}};
            final int[] vector = vectors[Math.floorMod(Long.hashCode(rootPos.asLong()), vectors.length)];
            firstX = vector[0];
            firstZ = vector[1];
        }
        final int reach = Math.max(10, canopyRadius + 2);
        final int targetHeight = Math.max(split.getY() + reach + 2, primaryEnd.getY() - 1);
        final List<BlockPos> secondPath = buildRisingCurve(split, -firstX, -firstZ, reach, targetHeight, 3);
        final Set<BlockPos> mainPositions = new HashSet<>(mainPath);
        for (int index = 1; index < secondPath.size(); index++) {
            final BlockPos pos = secondPath.get(index);
            final BlockState state = level.getBlockState(pos);
            if (mainPositions.contains(pos) || state.getBlock() instanceof BranchBlock
                    || isForeignTrunkShell(level, pos, state, mainPositions)) return null;
        }

        final BlockState splitState = level.getBlockState(split);
        final int splitRadius = splitState.getBlock() instanceof BranchBlock splitBranch
                ? Math.max(splitBranch.getRadius(splitState), Mth.clamp(canopyRadius + 6, 12, 18))
                : Mth.clamp(canopyRadius + 6, 12, 18);
        final int steps = secondPath.size() - 1;
        for (int index = 1; index <= steps; index++) {
            final BlockPos pos = secondPath.get(index);
            final double progress = index / (double) steps;
            final int horizontalClearance = Math.max(Math.abs(pos.getX() - split.getX()),
                    Math.abs(pos.getZ() - split.getZ()));
            final int clearanceLimit = Math.max(4, horizontalClearance * 4);
            final int desired = Math.min(clearanceLimit, Math.max(1, Mth.ceil(1 + (splitRadius - 1)
                    * (1.0 - progress * progress))));
            final BlockState state = level.getBlockState(pos);
            final int existing = state.getBlock() instanceof BranchBlock existingBranch
                    ? existingBranch.getRadius(state) : 0;
            if (!(state.getBlock() instanceof BranchBlock) && !state.isAir()) {
                level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
            }
            if (desired > existing) {
                clearShellObstacles(level, pos, desired);
                branch.setRadius(level, pos, desired, null);
            }
        }
        return secondPath.get(secondPath.size() - 1);
    }

    private static List<BlockPos> buildRisingCurve(final BlockPos split, final int directionX,
                                                    final int directionZ, final int reach, final int targetY,
                                                    final int initialHorizontalClearance) {
        final List<BlockPos> path = new ArrayList<>();
        BlockPos cursor = split.immutable();
        path.add(cursor);
        int step = 0;
        while (Math.max(Math.abs(cursor.getX() - split.getX()),
                Math.abs(cursor.getZ() - split.getZ())) < initialHorizontalClearance) {
            if (directionX != 0 && (directionZ == 0 || Math.floorMod(step, 2) == 0)) {
                cursor = cursor.offset(directionX, 0, 0);
            } else if (directionZ != 0) {
                cursor = cursor.offset(0, 0, directionZ);
            }
            path.add(cursor.immutable());
            step++;
        }
        final int rise = Math.max(reach, targetY - split.getY());
        for (int level = 1; level <= rise; level++) {
            final boolean diagonal = directionX != 0 && directionZ != 0;
            final int desiredAxisOffset = diagonal
                    ? Mth.ceil(Math.min(reach, reach * level / (double) rise) * 0.70710678)
                    : Math.min(reach, Mth.ceil(reach * level / (double) rise));
            final int currentX = Math.abs(cursor.getX() - split.getX());
            final int currentZ = Math.abs(cursor.getZ() - split.getZ());
            if (directionX != 0 && currentX < desiredAxisOffset
                    && (directionZ == 0 || currentZ >= desiredAxisOffset || Math.floorMod(level, 2) == 0)) {
                cursor = cursor.offset(directionX, 0, 0);
                path.add(cursor.immutable());
            } else if (directionZ != 0 && currentZ < desiredAxisOffset) {
                cursor = cursor.offset(0, 0, directionZ);
                path.add(cursor.immutable());
            }
            if (cursor.getY() < targetY) {
                cursor = cursor.above();
                path.add(cursor.immutable());
            }
        }
        return path;
    }

    private static boolean overlapsAnotherPlan(final List<CanopyPlan> plans,
                                               final CanopyPlacement candidate) {
        for (final CanopyPlan plan : plans) {
            final int required = plan.placement().radius() + candidate.radius();
            if (horizontalDistanceSquared(plan.placement().center(), candidate.center()) < required * required) {
                return true;
            }
        }
        return false;
    }

    private static void removeSmallJellyshrooms(final LevelAccessor level, final BlockPos rootPos) {
        for (final BlockPos mutable : BlockPos.betweenClosed(
                rootPos.offset(-6, -1, -6), rootPos.offset(6, 8, 6))) {
            final BlockPos pos = mutable.immutable();
            final var id = BuiltInRegistries.BLOCK.getKey(level.getBlockState(pos).getBlock());
            if ("betterend".equals(id.getNamespace()) && "small_jellyshroom".equals(id.getPath())) {
                level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
            }
        }
    }

    private static boolean collidesWithExistingCanopy(final LevelAccessor level, final BlockPos center,
                                                       final int radius, final Block membrane) {
        final int horizontalRange = radius + 2;
        final int verticalRange = radius + 4;
        for (int x = -horizontalRange; x <= horizontalRange; x++) {
            for (int z = -horizontalRange; z <= horizontalRange; z++) {
                if (x * x + z * z > horizontalRange * horizontalRange) continue;
                for (int y = -verticalRange; y <= verticalRange; y++) {
                    if (level.getBlockState(center.offset(x, y, z)).is(membrane)) return true;
                }
            }
        }
        return false;
    }

    private static CanopyPlacement findFreeCenter(final LevelAccessor level, final BlockPos branchEnd,
                                                  final int requestedRadius, final int minimumRadius,
                                                  final Block membrane,
                                                  final Block dynamicLeaves,
                                                   final BranchBlock supportingBranch,
                                                   final Set<BlockPos> supportingPath,
                                                   final int minimumLift,
                                                   final int maximumLift,
                                                  final List<CanopyPlan> existingPlans,
                                                  final boolean prioritizeDouble) {
        for (int radius = requestedRadius; radius >= minimumRadius; radius--) {
            for (int lift = minimumLift; lift <= maximumLift; lift++) {
                final BlockPos center = branchEnd.above(lift);
                final CanopyPlacement placement = new CanopyPlacement(center, radius);
                if (overlapsAnotherPlan(existingPlans, placement)) continue;
                if (canPlaceCanopy(level, branchEnd, center, radius, membrane, dynamicLeaves, supportingBranch,
                        supportingPath, prioritizeDouble)) return placement;
            }
        }
        return null;
    }

    private static CanopyPlacement retryDoublePlacementWithShrunkFirst(final LevelAccessor level,
                                                                        final BlockPos branchEnd,
                                                                        final int requestedRadius,
                                                                        final int minimumRadius,
                                                                        final Block membrane,
                                                                        final Block dynamicLeaves,
                                                                         final BranchBlock supportingBranch,
                                                                         final Set<BlockPos> supportingTree,
                                                                         final int minimumLift,
                                                                         final int maximumLift,
                                                                        final List<CanopyPlan> plans) {
        final CanopyPlan originalFirst = plans.get(0);
        for (int firstRadius = originalFirst.placement().radius();
             firstRadius >= minimumRadius; firstRadius--) {
            plans.set(0, new CanopyPlan(originalFirst.endPoint(),
                    new CanopyPlacement(originalFirst.placement().center(), firstRadius)));
            final CanopyPlacement second = findFreeCenter(level, branchEnd, requestedRadius, minimumRadius,
                    membrane, dynamicLeaves, supportingBranch, supportingTree, minimumLift, maximumLift, plans, true);
            if (second != null) return second;
        }
        plans.set(0, originalFirst);
        return null;
    }

    private static void removeFailedTree(final LevelAccessor level, final BlockPos rootPos,
                                         final BlockPos branchSeed,
                                         final BranchBlock umbrellaBranch, final Block membrane,
                                         final Block dynamicLeaves, final boolean growingTree,
                                         final BlockState initialDirtState) {
        if (growingTree) return;
        BlockPos seed = branchSeed;
        if (umbrellaBranch != null && (seed == null || level.getBlockState(seed).getBlock() != umbrellaBranch)) {
            seed = findRootBranch(level, rootPos, umbrellaBranch);
        }
        if (umbrellaBranch != null && seed != null) {
            final Set<BlockPos> tree = collectUmbrellaBranches(level, seed, Set.of(), umbrellaBranch);
            if (!tree.isEmpty()) removeUmbrellaTree(level, tree, umbrellaBranch, membrane, dynamicLeaves);
        }
        restoreFailedRootSite(level, rootPos, initialDirtState);
    }

    private static BlockPos findRootBranch(final LevelAccessor level, final BlockPos rootPos,
                                           final BranchBlock umbrellaBranch) {
        for (final BlockPos mutable : BlockPos.betweenClosed(rootPos.offset(-2, 0, -2),
                rootPos.offset(2, 4, 2))) {
            final BlockPos pos = mutable.immutable();
            if (level.getBlockState(pos).getBlock() == umbrellaBranch) return pos;
        }
        return null;
    }

    private static void restoreFailedRootSite(final LevelAccessor level, final BlockPos rootPos,
                                              final BlockState initialDirtState) {
        if (initialDirtState == null || initialDirtState.isAir()) return;
        for (final BlockPos mutable : BlockPos.betweenClosed(rootPos.offset(-2, -2, -2),
                rootPos.offset(2, 2, 2))) {
            final BlockPos pos = mutable.immutable();
            final BlockState state = level.getBlockState(pos);
            final ResourceLocation id = BuiltInRegistries.BLOCK.getKey(state.getBlock());
            final boolean rootyEndStone = "dynamictrees".equals(id.getNamespace())
                    && "rooty_end_stone".equals(id.getPath());
            final boolean exposedMarker = state.is(Blocks.END_STONE) && level.getBlockState(pos.above()).isAir()
                    && Math.abs(pos.getY() - rootPos.getY()) <= 1;
            if (rootyEndStone || exposedMarker) {
                level.setBlock(pos, initialDirtState, Block.UPDATE_ALL);
            }
        }
    }

    private static boolean canPlaceCanopy(final LevelAccessor level, final BlockPos branchEnd,
                                          final BlockPos center, final int radius, final Block membrane,
                                           final Block dynamicLeaves,
                                           final BranchBlock supportingBranch,
                                           final Set<BlockPos> supportingPath,
                                           final boolean allowBranchOverlap) {
        return !collidesWithExistingCanopy(level, center, radius, membrane)
                && !collidesWithCanopyCenterBranch(level, center, radius, supportingPath)
                && canExtendBranch(level, branchEnd, center.above(), supportingBranch, dynamicLeaves, membrane);
    }

    private static Set<BlockPos> collectUmbrellaBranches(final LevelAccessor level, final BlockPos seed,
                                                          final Set<BlockPos> excluded,
                                                          final BranchBlock umbrellaBranch) {
        final Set<BlockPos> branches = new HashSet<>();
        final ArrayDeque<BlockPos> open = new ArrayDeque<>();
        open.add(seed);
        while (!open.isEmpty() && branches.size() < 16384) {
            final BlockPos pos = open.removeFirst();
            if (excluded.contains(pos) || branches.contains(pos) || !level.hasChunkAt(pos)
                    || level.getBlockState(pos).getBlock() != umbrellaBranch) continue;
            branches.add(pos);
            for (final Direction direction : Direction.values()) open.addLast(pos.relative(direction).immutable());
        }
        return branches;
    }

    private static int branchNeighbours(final Set<BlockPos> branches, final BlockPos pos) {
        int count = 0;
        for (final Direction direction : Direction.values()) if (branches.contains(pos.relative(direction))) count++;
        return count;
    }

    private static void removeUmbrellaTree(final LevelAccessor level, final Set<BlockPos> branches,
                                           final BranchBlock umbrellaBranch, final Block membrane,
                                           final Block dynamicLeaves) {
        final int minY = branches.stream().mapToInt(BlockPos::getY).min().orElse(0);
        branches.stream().filter(pos -> branchNeighbours(branches, pos) <= 1)
                .sorted((a, b) -> Integer.compare(b.getY(), a.getY())).limit(8).forEach(end -> {
                    for (final BlockPos mutable : BlockPos.betweenClosed(end.offset(-14, -14, -14),
                            end.offset(14, 14, 14))) {
                        final BlockPos pos = mutable.immutable();
                        if (!level.hasChunkAt(pos)) continue;
                        final BlockState state = level.getBlockState(pos);
                        final var id = BuiltInRegistries.BLOCK.getKey(state.getBlock());
                        if (state.is(membrane) || state.is(dynamicLeaves)
                                || ("betterend".equals(id.getNamespace())
                                && "umbrella_tree_cluster".equals(id.getPath()))) {
                            level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
                        }
                    }
                });
        final BlockPos rootPos = branches.stream().filter(pos -> pos.getY() == minY).findFirst().orElseThrow().below();
        for (final BlockPos mutable : BlockPos.betweenClosed(rootPos.offset(-5, -1, -5), rootPos.offset(5, 3, 5))) {
            final BlockPos pos = mutable.immutable();
            if (!level.hasChunkAt(pos)) continue;
            final var id = BuiltInRegistries.BLOCK.getKey(level.getBlockState(pos).getBlock());
            if ("dtbetterend".equals(id.getNamespace()) && "umbrella_tree_root".equals(id.getPath())) {
                level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
            }
        }
        for (final BlockPos branchPos : branches) for (final Direction direction : Direction.values()) {
            final BlockPos shellPos = branchPos.relative(direction);
            if (level.hasChunkAt(shellPos) && level.getBlockState(shellPos).getBlock() instanceof TrunkShellBlock) {
                level.setBlock(shellPos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
            }
        }
        branches.stream().sorted((a, b) -> Integer.compare(b.getY(), a.getY())).forEach(pos -> {
            if (level.getBlockState(pos).getBlock() == umbrellaBranch) {
                level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
            }
        });
    }

    private record CanopyPlacement(BlockPos center, int radius) {
    }

    private record CanopyPlan(BlockPos endPoint, CanopyPlacement placement) {
    }

    private static boolean collidesWithCanopyCenterBranch(final LevelAccessor level, final BlockPos center,
                                                          final int radius,
                                                          final Set<BlockPos> supportingPath) {
        final int verticalRange = radius + 3;
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                for (int y = -verticalRange; y <= verticalRange; y++) {
                    final BlockPos pos = center.offset(x, y, z);
                    if (!supportingPath.contains(pos)
                            && level.getBlockState(pos).getBlock() instanceof BranchBlock) return true;
                }
            }
        }
        return false;
    }

    private static void clearDynamicCanopy(final LevelAccessor level, final BlockPos center,
                                           final Block target, final int radius) {
        final int verticalRadius = Math.max(4, radius / 2 + 2);
        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                if (x * x + z * z > radius * radius) continue;
                for (int y = -verticalRadius; y <= verticalRadius; y++) {
                    final BlockPos pos = center.offset(x, y, z);
                    if (level.getBlockState(pos).is(target)) {
                        level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
                    }
                }
            }
        }
    }

    private static boolean canExtendBranch(final LevelAccessor level, final BlockPos branchEnd,
                                           final BlockPos center, final BranchBlock supportingBranch,
                                           final Block dynamicLeaves, final Block membrane) {
        if (supportingBranch == null) return center.equals(branchEnd);
        for (final BlockPos pos : buildCardinalPath(branchEnd, center)) {
            if (pos.equals(branchEnd)) continue;
            final BlockState state = level.getBlockState(pos);
            if (state.is(membrane) || isUmbrellaCluster(state)) {
                final CanopyContact contact = inspectCanopyContact(
                        level, pos, supportingBranch, membrane);
                if (contact == CanopyContact.REMOVED || contact == CanopyContact.OUTER) continue;
                return false;
            }
            if (state.getBlock() instanceof BranchBlock && state.getBlock() != supportingBranch) return false;
        }
        return true;
    }

    private static CanopyContact inspectCanopyContact(final LevelAccessor level, final BlockPos seed,
                                                      final BranchBlock supportingBranch,
                                                      final Block membrane) {
        final Set<BlockPos> component = new HashSet<>();
        final ArrayDeque<BlockPos> open = new ArrayDeque<>();
        open.add(seed.immutable());
        boolean supported = false;
        boolean central = false;
        while (!open.isEmpty() && component.size() < 16384) {
            final BlockPos pos = open.removeFirst();
            if (!component.add(pos) || !level.hasChunkAt(pos)) continue;
            final BlockState state = level.getBlockState(pos);
            if (!state.is(membrane) && !isUmbrellaCluster(state)) {
                component.remove(pos);
                continue;
            }
            for (final Direction direction : Direction.values()) {
                final BlockPos adjacent = pos.relative(direction).immutable();
                if (level.hasChunkAt(adjacent)
                        && level.getBlockState(adjacent).getBlock() == supportingBranch) {
                    supported = true;
                    final int dx = adjacent.getX() - seed.getX();
                    final int dz = adjacent.getZ() - seed.getZ();
                    if (dx * dx + dz * dz <= 4) central = true;
                }
            }
            for (int x = -1; x <= 1; x++) for (int y = -1; y <= 1; y++) for (int z = -1; z <= 1; z++) {
                if (x == 0 && y == 0 && z == 0) continue;
                final BlockPos next = pos.offset(x, y, z).immutable();
                if (!component.contains(next) && level.hasChunkAt(next)) {
                    final BlockState nextState = level.getBlockState(next);
                    if (nextState.is(membrane) || isUmbrellaCluster(nextState)) open.addLast(next);
                }
            }
        }
        if (supported) return central ? CanopyContact.CENTER : CanopyContact.OUTER;
        if (component.isEmpty()) return CanopyContact.CENTER;
        for (final BlockPos pos : component) {
            if (level.getBlockState(pos).is(membrane) || isUmbrellaCluster(level.getBlockState(pos))) {
                level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
            }
        }
        return CanopyContact.REMOVED;
    }

    private enum CanopyContact {
        REMOVED,
        OUTER,
        CENTER
    }

    private static boolean isUmbrellaCluster(final BlockState state) {
        final var id = BuiltInRegistries.BLOCK.getKey(state.getBlock());
        return "betterend".equals(id.getNamespace()) && "umbrella_tree_cluster".equals(id.getPath());
    }

    private static int extendSupportingBranch(final LevelAccessor level, final BlockPos rootPos,
                                               final BlockPos branchEnd,
                                               final BlockPos center, final BranchBlock supportingBranch,
                                               final int canopyRadius) {
        if (supportingBranch == null) return 0;
        final List<BlockPos> path = findBranchPath(level, rootPos, branchEnd);
        if (path.size() <= 1 && !isRootBranch(rootPos, branchEnd)) return 0;
        final List<BlockPos> extension = buildCardinalPath(branchEnd, center);
        final BlockState baseState = level.getBlockState(path.get(0));
        final int existingBaseRadius = baseState.getBlock() instanceof BranchBlock baseBranch
                ? baseBranch.getRadius(baseState) : TreeHelper.getRadius(level, branchEnd);
        final int baseRadius = Math.max(existingBaseRadius, Mth.clamp(canopyRadius + 6, 12, 18));
        final int startRadius = Math.max(1, TreeHelper.getRadius(level, branchEnd));
        final int steps = Math.max(1, extension.size() - 1);
        final int tipRadius = Math.max(1,
                startRadius - Math.min(steps, Math.max(1, startRadius / 2)));
        int placed = 0;
        final int totalSteps = Math.max(1, path.size() + extension.size() - 2);
        for (int index = 0; index < path.size(); index++) {
            final BlockPos pos = path.get(index);
            final BlockState state = level.getBlockState(pos);
            if (!(state.getBlock() instanceof BranchBlock branch)) continue;
            final double progress = index / (double) totalSteps;
            final int desired = Mth.ceil(tipRadius + (baseRadius - tipRadius)
                    * (1.0 - progress * progress * progress));
            final int existing = branch.getRadius(state);
            if (desired > existing) {
                clearShellObstacles(level, pos, desired);
                branch.setRadius(level, pos, desired, null);
            }
        }
        for (int step = 1; step < extension.size(); step++) {
            final BlockPos pos = extension.get(step);
            final int index = path.size() - 1 + step;
            final double progress = index / (double) totalSteps;
            final int desired = Mth.ceil(tipRadius + (baseRadius - tipRadius)
                    * (1.0 - progress * progress * progress));
            final BlockState state = level.getBlockState(pos);
            final int existing = state.getBlock() instanceof BranchBlock branch ? branch.getRadius(state) : 0;
            if (desired > existing) {
                clearShellObstacles(level, pos, desired);
                supportingBranch.setRadius(level, pos, desired, null);
            }
            placed++;
        }
        return placed;
    }

    private static List<BlockPos> buildCardinalPath(final BlockPos start, final BlockPos target) {
        final List<BlockPos> path = new ArrayList<>();
        BlockPos cursor = start.immutable();
        path.add(cursor);
        while (!cursor.equals(target)) {
            final int dx = target.getX() - cursor.getX();
            final int dy = target.getY() - cursor.getY();
            final int dz = target.getZ() - cursor.getZ();
            if (Math.abs(dx) >= Math.abs(dz) && dx != 0) {
                cursor = cursor.offset(Integer.signum(dx), 0, 0);
            } else if (dz != 0) {
                cursor = cursor.offset(0, 0, Integer.signum(dz));
            } else {
                cursor = cursor.offset(0, Integer.signum(dy), 0);
            }
            path.add(cursor.immutable());
        }
        return path;
    }

    private static List<BlockPos> findBranchPath(final LevelAccessor level, final BlockPos rootPos,
                                                 final BlockPos branchEnd) {
        final ArrayDeque<BlockPos> open = new ArrayDeque<>();
        final Map<BlockPos, BlockPos> parent = new HashMap<>();
        final Set<BlockPos> visited = new HashSet<>();
        final BlockPos start = branchEnd.immutable();
        open.add(start);
        visited.add(start);
        BlockPos rootBranch = null;
        while (!open.isEmpty() && visited.size() < 16384) {
            final BlockPos current = open.removeFirst();
            if (isRootBranch(rootPos, current)) {
                rootBranch = current;
                break;
            }
            for (final Direction direction : Direction.values()) {
                final BlockPos next = current.relative(direction).immutable();
                if (visited.contains(next) || !withinTreeBounds(rootPos, next)
                        || !(level.getBlockState(next).getBlock() instanceof BranchBlock)) continue;
                visited.add(next);
                parent.put(next, current);
                open.addLast(next);
            }
        }
        if (rootBranch == null) return List.of(start);
        final List<BlockPos> path = new ArrayList<>();
        BlockPos cursor = rootBranch;
        path.add(cursor);
        while (!cursor.equals(start)) {
            cursor = parent.get(cursor);
            if (cursor == null) return List.of(start);
            path.add(cursor);
        }
        return path;
    }

    private static boolean isRootBranch(final BlockPos rootPos, final BlockPos pos) {
        return Math.abs(pos.getX() - rootPos.getX()) <= 1
                && Math.abs(pos.getZ() - rootPos.getZ()) <= 1
                && pos.getY() >= rootPos.getY()
                && pos.getY() <= rootPos.getY() + 3;
    }

    private static boolean withinTreeBounds(final BlockPos rootPos, final BlockPos pos) {
        return Math.abs(pos.getX() - rootPos.getX()) <= 160
                && Math.abs(pos.getZ() - rootPos.getZ()) <= 160
                && pos.getY() >= rootPos.getY() - 2
                && pos.getY() <= rootPos.getY() + 120;
    }

}
