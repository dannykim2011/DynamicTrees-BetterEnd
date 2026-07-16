package com.dannykim.dtbetterend.systems.mushroom;

import net.minecraft.resources.Identifier;

/**
 * Age-sensitive dome that becomes a thick, flatter mature Amaranita cap.
 */
final class GiganticAmaranitaShape extends ProfiledMushroomShape {
    GiganticAmaranitaShape(final Identifier name) {
        super(name);
    }

    @Override
    protected int getMaximumAge() {
        return 6;
    }

    @Override
    protected int depth(final int radius, final int age) {
        if (age < 4) {
            return Math.max(0, radius - 1);
        }
        return radius == age ? 2 : (radius > age / 2 ? 1 : 0);
    }

    @Override
    protected float ageingChance() {
        return 0.7F;
    }
}
