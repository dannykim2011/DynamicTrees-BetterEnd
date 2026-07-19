package com.dannykim.dtbetterend.systems.growthlogic;

import com.dtteam.dynamictrees.systems.growthlogic.GrowthLogicKit;
import com.dtteam.dynamictrees.systems.growthlogic.GrowthLogicKitConfiguration;
import com.dtteam.dynamictrees.systems.growthlogic.context.DirectionManipulationContext;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;

public final class MushroomStemLogic extends GrowthLogicKit {
    private final int upward;
    private final int lateral;

    public MushroomStemLogic(final Identifier name, final int upward, final int lateral) {
        super(name);
        this.upward = upward;
        this.lateral = lateral;
    }

    @Override
    public int[] populateDirectionProbabilityMap(
            final GrowthLogicKitConfiguration configuration,
            final DirectionManipulationContext context
    ) {
        final int[] map = new int[6];
        map[Direction.UP.ordinal()] = this.upward;
        if (context.signal().delta.getY() > 1) {
            map[Direction.NORTH.ordinal()] = this.lateral;
            map[Direction.SOUTH.ordinal()] = this.lateral;
            map[Direction.WEST.ordinal()] = this.lateral;
            map[Direction.EAST.ordinal()] = this.lateral;
        }
        map[context.signal().dir.getOpposite().ordinal()] = 0;
        return map;
    }
}
