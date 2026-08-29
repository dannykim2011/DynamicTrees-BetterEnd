package com.dannykim.dtbetterend.systems.umbrella;

import com.dtteam.dynamictrees.api.network.BranchDestructionData;
import com.dtteam.dynamictrees.api.network.MapSignal;
import com.dtteam.dynamictrees.api.registry.TypedRegistry;
import com.dtteam.dynamictrees.block.leaves.LeavesProperties;
import com.dtteam.dynamictrees.tree.family.Family;
import com.dtteam.dynamictrees.tree.TreeHelper;
import com.dtteam.dynamictrees.tree.species.Species;
import com.dtteam.dynamictrees.entity.FallingTreeEntity;
import com.dtteam.dynamictrees.entity.animation.AnimationHandler;
import com.dtteam.dynamictrees.systems.nodemapper.FindEndsNode;
import com.dtteam.dynamictrees.worldgen.DynamicTreeGenerationContext;
import com.dannykim.dtbetterend.systems.featuregen.UmbrellaTreeCanopyGenFeature;
import com.dannykim.dtbetterend.systems.featuregen.UmbrellaTreeGrowthCanopy;
import com.dannykim.dtbetterend.systems.leaves.UmbrellaLeavesProperties;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.Level;

import java.util.HashMap;

public final class UmbrellaTreeSpecies extends Species {
    public static final TypedRegistry.EntryType<Species> TYPE =
            Species.createDefaultType(UmbrellaTreeSpecies::new);
    private static final ResourceLocation MEMBRANE_ID =
            ResourceLocation.fromNamespaceAndPath("betterend", "umbrella_tree_membrane");
    private static final ResourceLocation CLUSTER_ID =
            ResourceLocation.fromNamespaceAndPath("betterend", "umbrella_tree_cluster");
    private static final int MEMBRANE_INDEX_BASE = -1;
    private static final int MEMBRANE_COLOR_COUNT = 8;
    private static final int CLUSTER_INDEX = -9;
    private static final int NATURAL_CLUSTER_INDEX = -10;
    private static final int DYNAMIC_MEMBRANE_INDEX_BASE = -11;
    private static final int DYNAMIC_CLUSTER_INDEX = -19;

    public UmbrellaTreeSpecies(final ResourceLocation name, final Family family,
                               final LeavesProperties leavesProperties) {
        super(name, family, leavesProperties);
    }

    @Override
    protected boolean transitionToTree(final Level level, final BlockPos pos, final Family family) {
        if (!super.transitionToTree(level, pos, family)) return false;
        final BlockPos rootPos = TreeHelper.findRootNode(level, pos);
        UmbrellaGrowthForm.assignNew(level, rootPos == null ? pos.below() : rootPos, level.getRandom());
        getLeavesProperties().getDynamicLeavesBlock().ifPresent(block ->
                UmbrellaTreeGrowthCanopy.placeInitialGrowthCanopy(level, pos, block));
        return true;
    }

    @Override
    public AnimationHandler selectAnimationHandler(final FallingTreeEntity entity) {
        return new UmbrellaFellingAnimationHandler(super.selectAnimationHandler(entity));
    }

    @Override
    public LeavesProperties getValidLeavesProperties(final int index) {
        if (isMembraneIndex(index) || isDynamicMembraneIndex(index)
                || index == CLUSTER_INDEX || index == NATURAL_CLUSTER_INDEX
                || index == DYNAMIC_CLUSTER_INDEX) {
            return getLeavesProperties();
        }
        return super.getValidLeavesProperties(index);
    }

    @Override
    public boolean generate(final DynamicTreeGenerationContext context) {
        final BlockPos initialRoot = context.rootPos().immutable();
        final BlockState initialDirt = context.level().getBlockState(initialRoot);
        final boolean generated = super.generate(context);
        final BlockPos rootPos = context.rootPos().immutable();
        final Block membrane = BuiltInRegistries.BLOCK.get(MEMBRANE_ID);
        if (context.isWorldGen() && !hasCanopy(context.level(), rootPos, membrane)) {
            UmbrellaTreeCanopyGenFeature.cleanupFailedWorldGen(context.level(), rootPos,
                    getFamily().getBranch().orElse(null), membrane,
                    getLeavesProperties().getDynamicLeavesBlock().orElse(null), initialDirt);
            return false;
        }
        return generated;
    }

    private static boolean hasCanopy(final LevelAccessor level, final BlockPos rootPos, final Block membrane) {
        final FindEndsNode finder = new FindEndsNode();
        TreeHelper.startAnalysisFromRoot(level, rootPos, new MapSignal(finder));
        for (final BlockPos end : finder.getEnds()) {
            for (final BlockPos pos : BlockPos.betweenClosed(end.offset(-12, -12, -12), end.offset(12, 12, 12))) {
                if (!level.hasChunkAt(pos)) continue;
                final BlockState state = level.getBlockState(pos);
                if (state.is(membrane) || UmbrellaLeavesProperties.isMembrane(state)) return true;
            }
        }
        return false;
    }

    @Override
    public boolean canEncodeLeavesBlocks(final BlockPos pos, final BlockState state, final Block block,
                                         final BranchDestructionData destructionData) {
        return UmbrellaLeavesProperties.isSpecial(state)
                || isSpecialBlock(block)
                || super.canEncodeLeavesBlocks(pos, state, block, destructionData);
    }

    @Override
    public int encodeLeavesPos(final BlockPos pos, final BlockState state, final Block block,
                               final BranchDestructionData destructionData) {
        return (UmbrellaLeavesProperties.isSpecial(state) || isSpecialBlock(block))
                ? BranchDestructionData.encodeRelBlockPos(pos)
                : super.encodeLeavesPos(pos, state, block, destructionData);
    }

    @Override
    public int encodeLeavesBlocks(final BlockPos pos, final BlockState state, final Block block,
                                  final BranchDestructionData destructionData) {
        if (UmbrellaLeavesProperties.isMembrane(state)) {
            return DYNAMIC_MEMBRANE_INDEX_BASE - getColor(state);
        }
        if (UmbrellaLeavesProperties.isCluster(state)) return DYNAMIC_CLUSTER_INDEX;
        if (isMembrane(block)) return MEMBRANE_INDEX_BASE - getColor(state);
        if (isCluster(block)) return isNatural(state) ? NATURAL_CLUSTER_INDEX : CLUSTER_INDEX;
        return super.encodeLeavesBlocks(pos, state, block, destructionData);
    }

    @Override
    public HashMap<BlockPos, BlockState> getFellingLeavesClusters(final BranchDestructionData destructionData) {
        final HashMap<BlockPos, BlockState> blocks = new HashMap<>();
        final Block dynamicLeaves = getLeavesProperties().getDynamicLeavesBlock().orElse(null);
        for (int index = 0; index < destructionData.getNumLeaves(); index++) {
            final int blockIndex = destructionData.destroyedLeavesBlockIndex[index];
            final BlockState state;
            if (isMembraneIndex(blockIndex)) {
                state = withColor(BuiltInRegistries.BLOCK.get(MEMBRANE_ID).defaultBlockState(),
                        MEMBRANE_INDEX_BASE - blockIndex);
            } else if (isDynamicMembraneIndex(blockIndex)) {
                state = dynamicLeaves == null ? null : UmbrellaLeavesProperties.membraneState(
                        dynamicLeaves, DYNAMIC_MEMBRANE_INDEX_BASE - blockIndex, false);
            } else if (blockIndex == DYNAMIC_CLUSTER_INDEX) {
                state = dynamicLeaves == null ? null
                        : UmbrellaLeavesProperties.clusterState(dynamicLeaves, false);
            } else if (blockIndex == CLUSTER_INDEX || blockIndex == NATURAL_CLUSTER_INDEX) {
                state = withNatural(BuiltInRegistries.BLOCK.get(CLUSTER_ID).defaultBlockState(),
                        blockIndex == NATURAL_CLUSTER_INDEX);
            } else {
                state = destructionData.getLeavesBlockState(index);
            }
            if (state != null) blocks.put(destructionData.getLeavesRelPos(index), state);
        }
        return blocks;
    }

    private static boolean isSpecialBlock(final Block block) {
        return isMembrane(block) || isCluster(block);
    }

    private static boolean isMembrane(final Block block) {
        return MEMBRANE_ID.equals(BuiltInRegistries.BLOCK.getKey(block));
    }

    private static boolean isCluster(final Block block) {
        return CLUSTER_ID.equals(BuiltInRegistries.BLOCK.getKey(block));
    }

    private static boolean isMembraneIndex(final int index) {
        return index <= MEMBRANE_INDEX_BASE
                && index > MEMBRANE_INDEX_BASE - MEMBRANE_COLOR_COUNT;
    }

    private static boolean isDynamicMembraneIndex(final int index) {
        return index <= DYNAMIC_MEMBRANE_INDEX_BASE
                && index > DYNAMIC_MEMBRANE_INDEX_BASE - MEMBRANE_COLOR_COUNT;
    }

    private static int getColor(final BlockState state) {
        for (final var property : state.getProperties()) {
            if (property instanceof IntegerProperty integerProperty
                    && "color".equals(property.getName())) {
                return state.getValue(integerProperty);
            }
        }
        return 0;
    }

    private static BlockState withColor(final BlockState state, final int color) {
        for (final var property : state.getProperties()) {
            if (property instanceof IntegerProperty integerProperty
                    && "color".equals(property.getName())) {
                return state.setValue(integerProperty, color);
            }
        }
        return state;
    }

    private static boolean isNatural(final BlockState state) {
        for (final var property : state.getProperties()) {
            if (property instanceof BooleanProperty booleanProperty
                    && "natural".equals(property.getName())) {
                return state.getValue(booleanProperty);
            }
        }
        return false;
    }

    private static BlockState withNatural(final BlockState state, final boolean natural) {
        for (final var property : state.getProperties()) {
            if (property instanceof BooleanProperty booleanProperty
                    && "natural".equals(property.getName())) {
                return state.setValue(booleanProperty, natural);
            }
        }
        return state;
    }
}
