package com.dannykim.dtbetterend.systems.mushroom;

import com.ferreusveritas.dynamictrees.block.branch.BranchBlock;
import com.ferreusveritas.dynamictreesplus.block.mushroom.MushroomBranchBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
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
        super.destroyMushroomCap(level, cutPos, species, tool, endPoints, destroyedCapBlocks, drops);
        this.collectAttachedDecorations(level, cutPos, destroyedCapBlocks);
    }

    private void collectAttachedDecorations(final Level level,
                                            final BlockPos cutPos,
                                            final Map<BlockPos, BlockState> destroyedCapBlocks) {
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
            destroyedCapBlocks.put(immutable.subtract(cutPos), state);
            level.setBlock(immutable, Blocks.AIR.defaultBlockState(), 3);
            collected++;

            for (final Direction direction : Direction.values()) {
                queue.add(immutable.relative(direction));
            }
        }
    }
}
