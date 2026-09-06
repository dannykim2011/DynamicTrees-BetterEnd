package com.dannykim.dtbetterend.systems.mushroom;

import com.ferreusveritas.dynamictreesplus.block.mushroom.CapProperties;
import com.ferreusveritas.dynamictreesplus.block.mushroom.DynamicCapCenterBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;

import javax.annotation.Nullable;

public final class StableDynamicCapCenterBlock extends DynamicCapCenterBlock {
    public StableDynamicCapCenterBlock(final CapProperties capProperties, final Properties properties) {
        super(capProperties, properties);
        this.registerDefaultState(this.defaultBlockState().setValue(StableDynamicCapBlock.COLOR, 0));
    }

    @Override
    protected void createBlockStateDefinition(final StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(StableDynamicCapBlock.COLOR);
    }

    @Override
    public SoundType getSoundType(final BlockState state, final LevelReader level,
                                  final BlockPos pos, @Nullable final Entity entity) {
        final BlockState primitive = getProperties(state).getPrimitiveCap();
        return primitive.getBlock().getSoundType(primitive, level, pos, entity);
    }
}

