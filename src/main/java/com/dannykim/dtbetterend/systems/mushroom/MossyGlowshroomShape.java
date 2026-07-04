package com.dannykim.dtbetterend.systems.mushroom;

import net.minecraft.resources.ResourceLocation;

/**
 * Deep flared cone: narrow near the stem and rapidly descending toward the
 * twelve-lobed outer rim of BetterEnd's glowshroom.
 */
final class MossyGlowshroomShape extends ProfiledMushroomShape {
    MossyGlowshroomShape(final ResourceLocation name) {
        super(name);
    }

    @Override
    protected int getMaximumAge() {
        return 8;
    }

    @Override
    protected int depth(final int radius, final int age) {
        final float ratio = radius / (float) Math.max(age, 1);
        return Math.round(ratio * ratio * 5.0F);
    }

    @Override
    protected float ageingChance() {
        return 0.9F;
    }
}
