package com.dannykim.dtbetterend.systems.growthlogic;

import com.ferreusveritas.dynamictrees.growthlogic.GrowthLogicKit;
import com.ferreusveritas.dynamictrees.growthlogic.GrowthLogicKitConfiguration;
import com.ferreusveritas.dynamictrees.growthlogic.context.DirectionManipulationContext;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;

public final class DragonHelixTreeLogic extends GrowthLogicKit {
    private static final Direction[] TURN = {Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST};

    public DragonHelixTreeLogic(final ResourceLocation registryName) {
        super(registryName);
    }

    @Override
    public int[] populateDirectionProbabilityMap(
            final GrowthLogicKitConfiguration configuration,
            final DirectionManipulationContext context
    ) {
        final int[] map = new int[6];
        final int height = context.signal().delta.getY();
        final int spread = Math.abs(context.signal().delta.getX()) + Math.abs(context.signal().delta.getZ());
        final int phase = Math.floorMod((int) Math.round(height * 1.15), 4);

        if (height < 11) {
            map[Direction.UP.ordinal()] = 18;
            if (height >= 2 && spread < 4) {
                map[TURN[phase].ordinal()] = 3;
            }
        } else if (height < 17) {
            map[Direction.UP.ordinal()] = spread < 3 ? 6 : 2;
            map[TURN[phase].ordinal()] = 12;
            map[TURN[(phase + 1) & 3].ordinal()] = 5;
        } else if (spread < 5) {
            map[TURN[phase].ordinal()] = 8;
            map[TURN[(phase + 1) & 3].ordinal()] = 4;
            map[Direction.UP.ordinal()] = 1;
        }

        map[context.signal().dir.getOpposite().ordinal()] = 0;
        return map;
    }
}
