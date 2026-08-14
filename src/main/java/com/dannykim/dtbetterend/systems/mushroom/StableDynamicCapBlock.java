package com.dannykim.dtbetterend.systems.mushroom;

import com.dtteam.dynamictreesplus.block.mushroom.CapProperties;
import com.dtteam.dynamictreesplus.block.mushroom.DynamicCapBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.Block;

public final class StableDynamicCapBlock extends DynamicCapBlock {
    public static final IntegerProperty COLOR = IntegerProperty.create("color", 0, 7);

    public StableDynamicCapBlock(final CapProperties capProperties, final Properties properties) {
        super(capProperties, properties);
        this.registerDefaultState(this.defaultBlockState().setValue(COLOR, 0));
    }

    @Override
    protected void createBlockStateDefinition(final StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(COLOR);
    }

    @Override
    public void tick(final BlockState state, final ServerLevel level, final BlockPos pos, final RandomSource random) {
        // Cap removal is owned by mushroom felling; a hand-made hole must not collapse the whole canopy.
    }
}
