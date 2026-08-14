package com.dannykim.dtbetterend.systems.mushroom;

import com.ferreusveritas.dynamictreesplus.block.mushroom.CapProperties;
import com.ferreusveritas.dynamictreesplus.block.mushroom.DynamicCapCenterBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;

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
}

