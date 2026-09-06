package com.dannykim.dtbetterend.systems.mushroom;

import com.dannykim.dtbetterend.DynamicTreesBetterEnd;
import com.dtteam.dynamictrees.event.RegistryEvent;
import com.dtteam.dynamictrees.event.TypeRegistryEvent;
import com.dtteam.dynamictrees.tree.family.Family;
import com.dtteam.dynamictrees.tree.species.Species;
import com.dtteam.dynamictreesplus.systems.mushroomlogic.shapekits.MushroomShapeKit;
import com.dtteam.dynamictreesplus.block.mushroom.CapProperties;
import net.neoforged.bus.api.SubscribeEvent;

/**
 * Loaded only after Dynamic Trees Plus is confirmed present.
 */
public final class DTPlusRegistries {
    private static final MushroomShapeKit MOSSY_GLOWSHROOM =
            new MossyGlowshroomShape(DynamicTreesBetterEnd.location("mossy_glowshroom"));
    private static final MushroomShapeKit JELLYSHROOM =
            new JellyshroomShape(DynamicTreesBetterEnd.location("jellyshroom"));
    private static final MushroomShapeKit GIGANTIC_AMARANITA =
            new GiganticAmaranitaShape(DynamicTreesBetterEnd.location("amaranita"));

    private DTPlusRegistries() {
    }

    @SubscribeEvent
    public static void registerShapes(final RegistryEvent<MushroomShapeKit> event) {
        if (!event.isEntryOfType(MushroomShapeKit.class)) return;
        event.getRegistry().registerAll(MOSSY_GLOWSHROOM, JELLYSHROOM, GIGANTIC_AMARANITA);
    }

    @SubscribeEvent
    public static void registerFamilyTypes(final TypeRegistryEvent<Family> event) {
        if (event.isEntryOfType(Family.class)) {
            event.registerType(DynamicTreesBetterEnd.location("decorated_mushroom"), DecoratedMushroomFamily.TYPE);
        }
    }

    @SubscribeEvent
    public static void registerSpeciesTypes(final TypeRegistryEvent<Species> event) {
        if (event.isEntryOfType(Species.class)) {
            event.registerType(DynamicTreesBetterEnd.location("decorated_mushroom"), DecoratedMushroomSpecies.TYPE);
        }
    }

    @SubscribeEvent
    public static void registerCapTypes(final TypeRegistryEvent<CapProperties> event) {
        if (event.isEntryOfType(CapProperties.class)) {
            event.registerType(DynamicTreesBetterEnd.location("stable_cap"), StableCapProperties.TYPE);
        }
    }
}
