package com.dannykim.dtbetterend.systems.mushroom;

import com.dtteam.dynamictreesplus.block.mushroom.CapProperties;
import com.dtteam.dynamictreesplus.block.mushroom.DynamicCapBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.Block;

import javax.annotation.Nullable;

public final class StableDynamicCapBlock extends DynamicCapBlock {
    public static final IntegerProperty COLOR = IntegerProperty.create("color", 0, 7);

    public StableDynamicCapBlock(final Identifier name, final CapProperties capProperties, final Properties properties) {
        super(name, capProperties, properties);
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

    @Override
    protected void affectNeighborsAfterRemoval(final BlockState state, final ServerLevel level,
                                               final BlockPos pos, final boolean movedByPiston) {
        MossyGlowshroomDecorationCleanup.removeOwnedFur(level, pos.below());
        super.affectNeighborsAfterRemoval(state, level, pos, movedByPiston);
    }

    @Override
    public SoundType getSoundType(final BlockState state, final LevelReader level,
                                  final BlockPos pos, @Nullable final Entity entity) {
        final BlockState primitive = getProperties(state).getPrimitiveCap();
        return primitive.getBlock().getSoundType(primitive, level, pos, entity);
    }
}
