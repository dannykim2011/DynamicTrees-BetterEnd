package com.dannykim.dtbetterend.systems.mushroom;

import com.ferreusveritas.dynamictrees.block.branch.BranchBlock;
import com.ferreusveritas.dynamictreesplus.block.mushroom.MushroomBranchBlock;
import com.ferreusveritas.dynamictreesplus.block.mushroom.DynamicCapCenterBlock;
import com.ferreusveritas.dynamictreesplus.systems.mushroomlogic.context.MushroomCapContext;
import com.ferreusveritas.dynamictreesplus.tree.HugeMushroomSpecies;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class DecoratedMushroomBranchBlock extends MushroomBranchBlock {
    public DecoratedMushroomBranchBlock(final ResourceLocation name, final Properties properties) {
        super(name, properties);
    }

    @Override
    public void destroyMushroomCap(final @NotNull Level level,
                                   final @NotNull BlockPos cutPos,
                                   final @NotNull com.ferreusveritas.dynamictrees.tree.species.Species species,
                                   final @NotNull ItemStack tool,
                                   final @NotNull List<BlockPos> endPoints,
                                   final @NotNull Map<BlockPos, BlockState> destroyedCapBlocks,
                                   final @NotNull List<BranchBlock.ItemStackPos> drops) {
        final Map<BlockPos, BlockState> protectedCaps = this.hideStandingCapOverlaps(level, species, endPoints);
        try {
            super.destroyMushroomCap(level, cutPos, species, tool, endPoints, destroyedCapBlocks, drops);
        } finally {
            protectedCaps.forEach((pos, state) -> level.setBlock(pos, state, Block.UPDATE_CLIENTS));
        }
        this.removeAttachedDecorations(level, cutPos, destroyedCapBlocks, drops);
    }

    private Map<BlockPos, BlockState> hideStandingCapOverlaps(
            final Level level,
            final com.ferreusveritas.dynamictrees.tree.species.Species species,
            final List<BlockPos> endPoints) {
        final Map<BlockPos, BlockState> protectedCaps = new HashMap<>();
        if (level.isClientSide() || !(species instanceof HugeMushroomSpecies mushroomSpecies)) return protectedCaps;
        final Set<BlockPos> felledCenters = new HashSet<>();
        final Set<BlockPos> felledShape = new HashSet<>();
        for (final BlockPos endPoint : endPoints) {
            final BlockPos center = endPoint.above().immutable();
            final int age = DynamicCapCenterBlock.getCapAge(level, center);
            if (age >= 0) {
                felledCenters.add(center);
                felledShape.addAll(mushroomSpecies.getMushroomShapeKit().getShapeCluster(
                        new MushroomCapContext(level, center, mushroomSpecies, age)));
            }
        }
        if (felledShape.isEmpty()) return protectedCaps;
        int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE, minZ = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE, maxZ = Integer.MIN_VALUE;
        for (final BlockPos pos : felledShape) {
            minX = Math.min(minX, pos.getX()); minY = Math.min(minY, pos.getY()); minZ = Math.min(minZ, pos.getZ());
            maxX = Math.max(maxX, pos.getX()); maxY = Math.max(maxY, pos.getY()); maxZ = Math.max(maxZ, pos.getZ());
        }
        final Block capCenter = mushroomSpecies.getCapProperties().getDynamicCapCenterBlock().orElse(null);
        if (capCenter == null) return protectedCaps;
        final Set<BlockPos> standingShape = new HashSet<>();
        for (int x = minX - 8; x <= maxX + 8; x++) for (int y = minY - 8; y <= maxY + 8; y++) for (int z = minZ - 8; z <= maxZ + 8; z++) {
            final BlockPos center = new BlockPos(x, y, z);
            if (felledCenters.contains(center) || level.getBlockState(center).getBlock() != capCenter
                    || !(level.getBlockState(center.below()).getBlock() instanceof BranchBlock)) continue;
            final int age = DynamicCapCenterBlock.getCapAge(level, center);
            if (age >= 0) standingShape.addAll(mushroomSpecies.getMushroomShapeKit().getShapeCluster(
                    new MushroomCapContext(level, center, mushroomSpecies, age)));
        }
        standingShape.retainAll(felledShape);
        standingShape.removeAll(felledCenters);
        for (final BlockPos pos : standingShape) {
            final BlockState state = level.getBlockState(pos);
            if (!state.isAir()) {
                protectedCaps.put(pos.immutable(), state);
                level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
            }
        }
        return protectedCaps;
    }

    private void removeAttachedDecorations(final Level level,
                                           final BlockPos cutPos,
                                           final Map<BlockPos, BlockState> destroyedCapBlocks,
                                           final List<BranchBlock.ItemStackPos> drops) {
        final ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        final Set<BlockPos> visited = new HashSet<>();
        for (final BlockPos relPos : new ArrayList<>(destroyedCapBlocks.keySet())) {
            final BlockPos absPos = cutPos.offset(relPos.getX(), relPos.getY(), relPos.getZ());
            visited.add(absPos);
            for (final Direction direction : Direction.values()) {
                queue.add(absPos.relative(direction));
            }
        }

        int collected = 0;
        while (!queue.isEmpty() && collected < 128) {
            final BlockPos pos = queue.removeFirst();
            if (!visited.add(pos)) {
                continue;
            }
            final BlockState state = level.getBlockState(pos);
            if (!DecoratedMushroomSpecies.isFellingDecoration(state)) {
                continue;
            }

            final BlockPos immutable = pos.immutable();
            final ItemStack decoration = new ItemStack(state.getBlock());
            if (!decoration.isEmpty() && !"mossy_glowshroom_fur".equals(
                    BuiltInRegistries.BLOCK.getKey(state.getBlock()).getPath())) {
                Block.popResource(level, immutable, decoration);
            }
            level.setBlock(immutable, Blocks.AIR.defaultBlockState(), 3);
            collected++;

            for (final Direction direction : Direction.values()) {
                queue.add(immutable.relative(direction));
            }
        }
    }

}
