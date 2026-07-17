package com.dannykim.dtbetterend.systems.growthlogic;

import com.ferreusveritas.dynamictrees.growthlogic.GrowthLogicKit;
import com.ferreusveritas.dynamictrees.growthlogic.GrowthLogicKitConfiguration;
import com.ferreusveritas.dynamictrees.growthlogic.context.DirectionManipulationContext;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;

/**
 * Neon cactus follows the original saguaro-like habit: a bright central column, a few raised arms, and almost no broad tree crown.
 */
public final class NeonCactusLogic extends GrowthLogicKit {
    public NeonCactusLogic(final ResourceLocation name) {
        super(name);
    }

    @Override
    public int[] populateDirectionProbabilityMap(
            final GrowthLogicKitConfiguration configuration,
            final DirectionManipulationContext context
    ) {
        final int[] map = new int[6];
        final int y = context.signal().delta.getY();
        final int spread = Math.abs(context.signal().delta.getX()) + Math.abs(context.signal().delta.getZ());

        if (spread == 0 && y < 7) {
            map[Direction.UP.ordinal()] = 34;
            if (y > 3 && y % 3 == 0) {
                for (final Direction direction : Direction.Plane.HORIZONTAL) {
                    map[direction.ordinal()] = 3;
                }
            }
        } else if (context.signal().dir.getAxis().isHorizontal()) {
            map[context.signal().dir.ordinal()] = spread < 2 && y >= 3 ? 12 : 0;
            map[Direction.UP.ordinal()] = y < 10 ? 16 : 0;
        } else {
            map[Direction.UP.ordinal()] = y < 10 ? 20 : 0;
            if (y >= 4 && y <= 8 && spread < 2) {
                for (final Direction direction : Direction.Plane.HORIZONTAL) {
                    map[direction.ordinal()] = 2;
                }
            }
        }

        map[context.signal().dir.getOpposite().ordinal()] = 0;
        return map;
    }
}
