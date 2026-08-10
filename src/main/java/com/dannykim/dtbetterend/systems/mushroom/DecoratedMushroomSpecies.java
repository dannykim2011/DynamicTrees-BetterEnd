package com.dannykim.dtbetterend.systems.mushroom;

import com.dtteam.dynamictrees.api.network.BranchDestructionData;
import com.dtteam.dynamictrees.api.registry.TypedRegistry;
import com.dtteam.dynamictrees.tree.family.Family;
import com.dtteam.dynamictrees.tree.species.Species;
import com.dtteam.dynamictreesplus.block.mushroom.CapProperties;
import com.dtteam.dynamictreesplus.block.mushroom.DynamicCapBlock;
import com.dtteam.dynamictreesplus.block.mushroom.DynamicCapCenterBlock;
import com.dtteam.dynamictreesplus.tree.HugeMushroomSpecies;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;

import java.util.HashMap;

public class DecoratedMushroomSpecies extends HugeMushroomSpecies {
    private static final int DECORATION_FLAG = 0x40000000;
    private static final int FACING_SHIFT = 20;
    private static final int BLOCK_MASK = 0x000FFFFF;

    public static final TypedRegistry.EntryType<Species> TYPE =
            HugeMushroomSpecies.createDefaultMushroomType(DecoratedMushroomSpecies::new);

    public DecoratedMushroomSpecies(final Identifier name, final Family family, final CapProperties capProperties) {
        super(name, family, capProperties);
    }

    @Override
    public boolean canEncodeLeavesBlocks(final BlockPos pos,
                                         final BlockState state,
                                         final Block block,
                                         final BranchDestructionData data) {
        return super.canEncodeLeavesBlocks(pos, state, block, data) || isFellingDecoration(state);
    }

    @Override
    public int encodeLeavesPos(final BlockPos pos, final BlockState state, final Block block, final BranchDestructionData data) {
        if (isFellingDecoration(state)) {
            return BranchDestructionData.encodeRelBlockPos(pos);
        }
        return super.encodeLeavesPos(pos, state, block, data);
    }

    @Override
    public int encodeLeavesBlocks(final BlockPos pos, final BlockState state, final Block block, final BranchDestructionData data) {
        if (isFellingDecoration(state)) {
            return DECORATION_FLAG | (getFacingIndex(state) << FACING_SHIFT) | BuiltInRegistries.BLOCK.getId(block);
        }
        return super.encodeLeavesBlocks(pos, state, block, data);
    }

    @Override
    public HashMap<BlockPos, BlockState> getFellingLeavesClusters(final BranchDestructionData destructionData) {
        final HashMap<BlockPos, BlockState> map = new HashMap<>();
        for (int index = 0; index < destructionData.getNumLeaves(); index++) {
            final int blockCode = destructionData.destroyedLeavesBlockIndex[index];
            final BlockPos pos = BranchDestructionData.decodeRelPos(destructionData.destroyedLeaves[index]);
            if ((blockCode & DECORATION_FLAG) == 0) {
                final boolean isCenter = (blockCode & 0x1) == 1;
                BlockState state = this.getCapProperties().getDynamicCapState(isCenter);
                if (isCenter) {
                    state = state.setValue(DynamicCapCenterBlock.AGE, blockCode >> 0x1);
                } else {
                    final boolean[] sides = new boolean[6];
                    for (int side = 0; side < 6; side++) {
                        sides[side] = ((blockCode >> side + 1) & 0x1) == 0x1;
                    }
                    state = DynamicCapBlock.setDirectionValues(state, sides);
                }
                map.put(pos, state);
            } else {
                final Block block = BuiltInRegistries.BLOCK.byId(blockCode & BLOCK_MASK);
                final BlockState state = withFacing(block.defaultBlockState(), Direction.values()[(blockCode >> FACING_SHIFT) & 7]);
                map.put(pos, state);
            }
        }
        return map;
    }

    public static boolean isFellingDecoration(final BlockState state) {
        final Identifier key = BuiltInRegistries.BLOCK.getKey(state.getBlock());
        if (!"betterend".equals(key.getNamespace())) {
            return false;
        }
        return switch (key.getPath()) {
            case "mossy_glowshroom_hymenophore",
                 "mossy_glowshroom_fur",
                 "amaranita_hymenophore",
                 "amaranita_lantern",
                 "amaranita_fur" -> true;
            default -> false;
        };
    }

    private static int getFacingIndex(final BlockState state) {
        for (final Property<?> property : state.getProperties()) {
            if ("facing".equals(property.getName())) {
                return ((Direction) getUnchecked(state, property)).ordinal();
            }
        }
        return Direction.DOWN.ordinal();
    }

    private static BlockState withFacing(final BlockState state, final Direction direction) {
        for (final Property<?> property : state.getProperties()) {
            if ("facing".equals(property.getName())) {
                return setUnchecked(state, property, direction);
            }
        }
        return state;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static Object getUnchecked(final BlockState state, final Property property) {
        return state.getValue(property);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static BlockState setUnchecked(final BlockState state, final Property property, final Object value) {
        return state.setValue(property, (Comparable) value);
    }
}
