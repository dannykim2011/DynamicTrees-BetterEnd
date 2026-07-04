package com.dannykim.dtbetterend.systems.mushroom;

import net.minecraft.resources.ResourceLocation;

/**
 * A shallow, broad membrane with a softly dropped rim.
 */
final class JellyshroomShape extends ProfiledMushroomShape {
    JellyshroomShape(final ResourceLocation name) {
        super(name);
    }

    @Override
    protected int getMaximumAge() {
        return 7;
    }

    @Override
    protected int depth(final int radius, final int age) {
        return radius >= age - 1 ? 2 : (radius >= age / 2 ? 1 : 0);
    }

    @Override
    protected float ageingChance() {
        return 0.82F;
    }
}
