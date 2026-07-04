package com.dannykim.dtbetterend.systems.mushroom;

import com.dannykim.dtbetterend.DynamicTreesBetterEnd;
import com.ferreusveritas.dynamictrees.api.registry.RegistryEvent;
import com.ferreusveritas.dynamictreesplus.systems.mushroomlogic.shapekits.MushroomShapeKit;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/**
 * Loaded only after Dynamic Trees Plus is confirmed present.
 */
public final class DTPlusRegistries {
    private static final MushroomShapeKit MOSSY_GLOWSHROOM =
            new MossyGlowshroomShape(DynamicTreesBetterEnd.location("mossy_glowshroom"));
    private static final MushroomShapeKit JELLYSHROOM =
            new JellyshroomShape(DynamicTreesBetterEnd.location("jellyshroom"));
    private static final MushroomShapeKit GIGANTIC_AMARANITA =
            new GiganticAmaranitaShape(DynamicTreesBetterEnd.location("gigantic_amaranita"));

    private DTPlusRegistries() {
    }

    @SubscribeEvent
    public static void registerShapes(final RegistryEvent<MushroomShapeKit> event) {
        event.getRegistry().registerAll(MOSSY_GLOWSHROOM, JELLYSHROOM, GIGANTIC_AMARANITA);
    }
}
