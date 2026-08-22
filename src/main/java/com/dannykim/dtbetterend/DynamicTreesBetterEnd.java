package com.dannykim.dtbetterend;

import com.dannykim.dtbetterend.loot.LootModifiers;
import com.dannykim.dtbetterend.systems.DTBetterEndRegistries;
import com.dtteam.dynamictrees.registry.NeoForgeRegistryHandler;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModList;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;

@Mod(DynamicTreesBetterEnd.MOD_ID)
public final class DynamicTreesBetterEnd {
    public static final String MOD_ID = "dtbetterend";

    public DynamicTreesBetterEnd(final IEventBus modEventBus, final ModContainer modContainer) {
        LootModifiers.register(modEventBus);
        modEventBus.register(DTBetterEndRegistries.class);
        if (ModList.get().isLoaded("dynamictreesplus")) {
            modEventBus.register(com.dannykim.dtbetterend.systems.mushroom.DTPlusRegistries.class);
        }
        if (FMLEnvironment.dist == Dist.CLIENT) {
            com.dannykim.dtbetterend.client.DTBetterEndClient.register(modEventBus);
        }
        NeoForgeRegistryHandler.setup(MOD_ID, modEventBus);
    }

    public static ResourceLocation location(final String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }
}
