package com.dannykim.dtbetterend.systems.umbrella;

import com.ferreusveritas.dynamictrees.api.registry.TypedRegistry;
import com.ferreusveritas.dynamictrees.api.TreeHelper;
import com.ferreusveritas.dynamictrees.api.network.MapSignal;
import com.ferreusveritas.dynamictrees.block.leaves.LeavesProperties;
import com.ferreusveritas.dynamictrees.systems.nodemapper.FindEndsNode;
import com.ferreusveritas.dynamictrees.tree.family.Family;
import com.ferreusveritas.dynamictrees.tree.species.Species;
import com.ferreusveritas.dynamictrees.util.BranchDestructionData;
import com.ferreusveritas.dynamictrees.worldgen.GenerationContext;
import com.dannykim.dtbetterend.systems.featuregen.UmbrellaTreeCanopyGenFeature;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.LevelAccessor;

import java.util.HashMap;

public final class UmbrellaTreeSpecies extends Species {
    public static final TypedRegistry.EntryType<Species> TYPE = Species.createDefaultType(UmbrellaTreeSpecies::new);
    private static final ResourceLocation MEMBRANE_ID = new ResourceLocation("betterend", "umbrella_tree_membrane");
    private static final ResourceLocation CLUSTER_ID = new ResourceLocation("betterend", "umbrella_tree_cluster");
    private static final int MEMBRANE_INDEX = -1;
    private static final int CLUSTER_INDEX = -2;

    public UmbrellaTreeSpecies(final ResourceLocation name, final Family family,
                               final LeavesProperties leavesProperties) {
        super(name, family, leavesProperties);
    }

    @Override
    public LeavesProperties getValidLeavesProperties(final int index) {
        if (index == MEMBRANE_INDEX || index == CLUSTER_INDEX) return getLeavesProperties();
        return super.getValidLeavesProperties(index);
    }

    @Override
    public boolean generate(final GenerationContext context) {
        final BlockPos initialRoot = context.rootPos().immutable();
        final BlockState initialDirt = context.level().getBlockState(initialRoot);
        final boolean generated = super.generate(context);
        final BlockPos rootPos = context.rootPos().immutable();
        final Block membrane = BuiltInRegistries.BLOCK.get(MEMBRANE_ID);
        if (!hasCanopy(context.level(), rootPos, membrane)) {
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
                if (level.getBlockState(pos).is(membrane)) return true;
            }
        }
        return false;
    }

    @Override
    public boolean canEncodeLeavesBlocks(final BlockPos pos, final BlockState state, final Block block,
                                         final BranchDestructionData destructionData) {
        return specialIndex(block) != 0 || super.canEncodeLeavesBlocks(pos, state, block, destructionData);
    }

    @Override
    public int encodeLeavesPos(final BlockPos pos, final BlockState state, final Block block,
                               final BranchDestructionData destructionData) {
        return specialIndex(block) != 0
                ? BranchDestructionData.encodeRelBlockPos(pos)
                : super.encodeLeavesPos(pos, state, block, destructionData);
    }

    @Override
    public int encodeLeavesBlocks(final BlockPos pos, final BlockState state, final Block block,
                                  final BranchDestructionData destructionData) {
        final int special = specialIndex(block);
        return special != 0 ? special : super.encodeLeavesBlocks(pos, state, block, destructionData);
    }

    @Override
    public HashMap<BlockPos, BlockState> getFellingLeavesClusters(final BranchDestructionData destructionData) {
        final HashMap<BlockPos, BlockState> blocks = new HashMap<>();
        for (int index = 0; index < destructionData.getNumLeaves(); index++) {
            final int blockIndex = destructionData.destroyedLeavesBlockIndex[index];
            final BlockState state;
            if (blockIndex == MEMBRANE_INDEX) {
                state = BuiltInRegistries.BLOCK.get(MEMBRANE_ID).defaultBlockState();
            } else if (blockIndex == CLUSTER_INDEX) {
                state = BuiltInRegistries.BLOCK.get(CLUSTER_ID).defaultBlockState();
            } else {
                state = destructionData.getLeavesBlockState(index);
            }
            if (state != null) blocks.put(destructionData.getLeavesRelPos(index), state);
        }
        return blocks;
    }

    private static int specialIndex(final Block block) {
        final ResourceLocation id = BuiltInRegistries.BLOCK.getKey(block);
        if (MEMBRANE_ID.equals(id)) return MEMBRANE_INDEX;
        if (CLUSTER_ID.equals(id)) return CLUSTER_INDEX;
        return 0;
    }
}
