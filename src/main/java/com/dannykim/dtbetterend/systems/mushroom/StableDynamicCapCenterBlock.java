package com.dannykim.dtbetterend.systems.mushroom;

import com.dtteam.dynamictreesplus.block.mushroom.CapProperties;
import com.dtteam.dynamictreesplus.block.mushroom.DynamicCapCenterBlock;
import com.dtteam.dynamictreesplus.tree.HugeMushroomSpecies;
import com.dtteam.dynamictrees.systems.GrowSignal;
import net.minecraft.resources.Identifier;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;

import javax.annotation.Nullable;

public final class StableDynamicCapCenterBlock extends DynamicCapCenterBlock {
    public StableDynamicCapCenterBlock(final Identifier name, final CapProperties capProperties, final Properties properties) {
        super(name, capProperties, properties);
        this.registerDefaultState(this.defaultBlockState().setValue(StableDynamicCapBlock.COLOR, 0));
    }

    @Override
    protected void createBlockStateDefinition(final StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(StableDynamicCapBlock.COLOR);
    }

    @Override
    public GrowSignal growSignal(final net.minecraft.world.level.Level level, final BlockPos pos,
                                 final GrowSignal signal) {
        if (!(signal.getSpecies() instanceof HugeMushroomSpecies)
                || !"mossy_glowshroom".equals(signal.getSpecies().getRegistryName().getPath())) {
            return super.growSignal(level, pos, signal);
        }
        if (signal.step()) {
            final BlockState state = level.getBlockState(pos);
            final int age = state.hasProperty(AGE) ? state.getValue(AGE) : 0;
            branchOut(level, pos, signal, age);
        }
        return signal;
    }

    @Override
    public SoundType getSoundType(final BlockState state, final LevelReader level,
                                  final BlockPos pos, @Nullable final Entity entity) {
        final BlockState primitive = getProperties(state).getPrimitiveCap();
        return primitive.getBlock().getSoundType(primitive, level, pos, entity);
    }
}
