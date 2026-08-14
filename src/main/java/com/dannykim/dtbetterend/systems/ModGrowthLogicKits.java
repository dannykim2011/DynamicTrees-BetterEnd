package com.dannykim.dtbetterend.systems;

import com.ferreusveritas.dynamictrees.api.registry.Registry;
import com.ferreusveritas.dynamictrees.growthlogic.GrowthLogicKit;
import com.dannykim.dtbetterend.DynamicTreesBetterEnd;
import com.dannykim.dtbetterend.systems.growthlogic.*;


public class ModGrowthLogicKits {
    public static final GrowthLogicKit DRAGON_TREE = new DragonTreeLogic(DynamicTreesBetterEnd.location("dragon_tree"));
    public static final GrowthLogicKit DRAGON_HELIX_TREE = new DragonHelixTreeLogic(DynamicTreesBetterEnd.location("dragon_helix_tree"));
    public static final GrowthLogicKit LACUGROVE = new LacugroveLogic(DynamicTreesBetterEnd.location("lacugrove"));
    public static final GrowthLogicKit LUCERNIA = new LucerniaLogic(DynamicTreesBetterEnd.location("lucernia"));
    public static final GrowthLogicKit PYTHADENDRON = new PythadendronLogic(DynamicTreesBetterEnd.location("pythadendron"));
    public static final GrowthLogicKit UMBRELLA_TREE = new UmbrellaTreeLogic(DynamicTreesBetterEnd.location("umbrella_tree"));
    public static final GrowthLogicKit HELIX_TREE = new HelixTreeLogic(DynamicTreesBetterEnd.location("helix_tree"));
    public static final GrowthLogicKit NEON_CACTUS = new NeonCactusLogic(DynamicTreesBetterEnd.location("neon_cactus"));
    public static final GrowthLogicKit GIGANTIC_AMARANITA = new MushroomStemLogic(DynamicTreesBetterEnd.location("gigantic_amaranita"), 18, 2);
    public static final GrowthLogicKit JELLYSHROOM = new MushroomStemLogic(DynamicTreesBetterEnd.location("jellyshroom"), 12, 3);
    public static final GrowthLogicKit MOSSY_GLOWSHROOM = new MushroomStemLogic(DynamicTreesBetterEnd.location("mossy_glowshroom"), 15, 2);

    public static void register(final Registry<GrowthLogicKit> registry) {
        registry.registerAll(DRAGON_TREE, DRAGON_HELIX_TREE, LACUGROVE, LUCERNIA, PYTHADENDRON, UMBRELLA_TREE, HELIX_TREE, NEON_CACTUS,
                GIGANTIC_AMARANITA, JELLYSHROOM, MOSSY_GLOWSHROOM);
    }
}

