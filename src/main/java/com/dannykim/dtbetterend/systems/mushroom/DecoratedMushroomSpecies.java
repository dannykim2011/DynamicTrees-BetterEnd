package com.dannykim.dtbetterend.systems.mushroom;

import com.ferreusveritas.dynamictrees.api.registry.TypedRegistry;
import com.ferreusveritas.dynamictrees.tree.family.Family;
import com.ferreusveritas.dynamictrees.tree.species.Species;
import com.ferreusveritas.dynamictrees.models.FallingTreeEntityModel;
import com.ferreusveritas.dynamictreesplus.block.mushroom.CapProperties;
import com.ferreusveritas.dynamictreesplus.block.mushroom.DynamicCapBlock;
import com.ferreusveritas.dynamictreesplus.block.mushroom.DynamicCapCenterBlock;
import com.ferreusveritas.dynamictreesplus.tree.HugeMushroomSpecies;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import com.ferreusveritas.dynamictrees.util.BranchDestructionData;

import java.util.HashMap;

public class DecoratedMushroomSpecies extends HugeMushroomSpecies {
    public static final TypedRegistry.EntryType<Species> TYPE =
            HugeMushroomSpecies.createDefaultMushroomType(DecoratedMushroomSpecies::new);

    public DecoratedMushroomSpecies(final ResourceLocation name, final Family family, final CapProperties capProperties) {
        super(name, family, capProperties);
    }

    @Override
    public int encodeLeavesBlocks(final BlockPos pos, final BlockState state, final Block block,
                                  final BranchDestructionData destructionData) {
        final int encoded = super.encodeLeavesBlocks(pos, state, block, destructionData);
        return state.hasProperty(StableDynamicCapBlock.COLOR)
                ? encoded | state.getValue(StableDynamicCapBlock.COLOR) << 16
                : encoded;
    }

    @Override
    public HashMap<BlockPos, BlockState> getFellingLeavesClusters(
            final BranchDestructionData destructionData) {
        final HashMap<BlockPos, BlockState> blocks = new HashMap<>();
        for (int index = 0; index < destructionData.getNumLeaves(); index++) {
            final int encoded = destructionData.destroyedLeavesBlockIndex[index];
            final int capData = encoded & 0xFFFF;
            final boolean center = (capData & 1) == 1;
            BlockState state = getCapProperties().getDynamicCapState(center);
            if (center) {
                state = state.setValue(DynamicCapCenterBlock.AGE, capData >> 1);
            } else {
                final boolean[] directions = new boolean[6];
                for (int direction = 0; direction < directions.length; direction++) {
                    directions[direction] = (capData >> (direction + 1) & 1) == 1;
                }
                state = DynamicCapBlock.setDirectionValues(state, directions);
            }
            if (state.hasProperty(StableDynamicCapBlock.COLOR)) {
                state = state.setValue(StableDynamicCapBlock.COLOR, encoded >> 16 & 7);
            }
            final BlockPos pos = destructionData.getLeavesRelPos(index);
            blocks.put(pos, state);
        }
        return blocks;
    }

    @Override
    public int colorTreeQuads(final int color, final FallingTreeEntityModel.TreeQuadData quad) {
        return getJellyColor(quad.state, color);
    }

    private static int getJellyColor(final BlockState state, final int fallback) {
        if (!state.hasProperty(StableDynamicCapBlock.COLOR)) return fallback;
        final int color = state.getValue(StableDynamicCapBlock.COLOR);
        final int red = 217 + (164 - 217) * color / 7;
        final int green = 142 + (0 - 142) * color / 7;
        return red << 16 | green << 8 | 255;
    }

    public static boolean isFellingDecoration(final BlockState state) {
        final ResourceLocation key = BuiltInRegistries.BLOCK.getKey(state.getBlock());
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
}
