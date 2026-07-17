package com.dannykim.dtbetterend.systems;

import com.dtteam.dynamictrees.api.registry.Registry;
import com.dtteam.dynamictrees.systems.growthlogic.GrowthLogicKit;
import com.dannykim.dtbetterend.DynamicTreesBetterEnd;
import com.dannykim.dtbetterend.systems.growthlogic.*;


public class ModGrowthLogicKits {
    public static final GrowthLogicKit DRAGON_TREE = new DragonTreeLogic(DynamicTreesBetterEnd.location("dragon_tree"));
    public static final GrowthLogicKit LACUGROVE = new LacugroveLogic(DynamicTreesBetterEnd.location("lacugrove"));
    public static final GrowthLogicKit LUCERNIA = new LucerniaLogic(DynamicTreesBetterEnd.location("lucernia"));
    public static final GrowthLogicKit PYTHADENDRON = new PythadendronLogic(DynamicTreesBetterEnd.location("pythadendron"));
    public static final GrowthLogicKit TENANEA = new TenaneaLogic(DynamicTreesBetterEnd.location("tenanea"));
    public static final GrowthLogicKit HELIX_TREE = new HelixTreeLogic(DynamicTreesBetterEnd.location("helix_tree"));
    public static final GrowthLogicKit UMBRELLA_TREE = new UmbrellaTreeLogic(DynamicTreesBetterEnd.location("umbrella_tree"));

    public static void register(final Registry<GrowthLogicKit> registry) {
        registry.registerAll(DRAGON_TREE, LACUGROVE, LUCERNIA, PYTHADENDRON, TENANEA, HELIX_TREE, UMBRELLA_TREE);
    }
}

