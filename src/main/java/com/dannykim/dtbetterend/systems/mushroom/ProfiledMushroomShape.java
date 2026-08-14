package com.dannykim.dtbetterend.systems.mushroom;

import com.ferreusveritas.dynamictreesplus.block.mushroom.DynamicCapCenterBlock;
import com.ferreusveritas.dynamictreesplus.systems.mushroomlogic.MushroomShapeConfiguration;
import com.ferreusveritas.dynamictreesplus.systems.mushroomlogic.context.MushroomCapContext;
import com.ferreusveritas.dynamictreesplus.systems.mushroomlogic.shapekits.MushroomShapeKit;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.resources.ResourceLocation;

import java.util.LinkedList;
import java.util.List;

abstract class ProfiledMushroomShape extends MushroomShapeKit {
    protected ProfiledMushroomShape(final ResourceLocation name) {
        super(name);
    }

    @Override
    public final void generateMushroomCap(
            final MushroomShapeConfiguration configuration,
            final MushroomCapContext context
    ) {
        apply(context, Operation.PLACE);
    }

    @Override
    public final void clearMushroomCap(
            final MushroomShapeConfiguration configuration,
            final MushroomCapContext context
    ) {
        apply(context, Operation.CLEAR);
    }

    @Override
    public final List<BlockPos> getShapeCluster(
            final MushroomShapeConfiguration configuration,
            final MushroomCapContext context
    ) {
        return apply(context, Operation.GET);
    }

    private List<BlockPos> apply(final MushroomCapContext context, final Operation operation) {
        final List<BlockPos> blocks = new LinkedList<>();
        final DynamicCapCenterBlock cap = context.species().getCapProperties()
                .getDynamicCapCenterBlock().orElse(null);
        if (cap == null) {
            return blocks;
        }
        final int age = Math.min(context.age(), getMaximumAge());
        for (int radius = 1; radius <= age; radius++) {
            final BlockPos centre = context.pos().below(depth(radius, age));
            final boolean rim = radius == age || depth(radius, age) != depth(Math.min(radius + 1, age), age);
            if (operation == Operation.PLACE) {
                final boolean placed = cap.placeRing(context.level(), centre, radius, age, rim, underside(radius, age));
                applyOriginalColorStep(context.level(), cap.getRing(context.level(), centre, radius),
                        Mth.clamp(Mth.floor(radius / (double) age * 7.0), 0, 7));
                if (!placed) {
                    break;
                }
            } else if (operation == Operation.CLEAR) {
                cap.clearRing(context.level(), centre, radius);
            } else {
                blocks.addAll(cap.getRing(context.level(), centre, radius));
            }
        }
        if (operation == Operation.PLACE) {
            applyOriginalColorStep(context.level(), List.of(context.pos()), 0);
        }
        blocks.add(context.pos());
        return blocks;
    }

    private static void applyOriginalColorStep(final LevelAccessor level, final List<BlockPos> positions,
                                               final int color) {
        for (final BlockPos pos : positions) {
            final BlockState state = level.getBlockState(pos);
            if (state.hasProperty(StableDynamicCapBlock.COLOR)) {
                level.setBlock(pos, state.setValue(StableDynamicCapBlock.COLOR, color), Block.UPDATE_CLIENTS);
            }
        }
    }

    @Override
    public final int getMaxCapAge(final MushroomShapeConfiguration configuration) {
        return getMaximumAge();
    }

    @Override
    public final float getChanceToAge(final MushroomShapeConfiguration configuration) {
        return ageingChance();
    }

    protected abstract int getMaximumAge();

    protected abstract int depth(int radius, int age);

    protected boolean underside(final int radius, final int age) {
        return radius == age;
    }

    protected float ageingChance() {
        return 0.75F;
    }

    private enum Operation {
        PLACE, CLEAR, GET
    }
}
