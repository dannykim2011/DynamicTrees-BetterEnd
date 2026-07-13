package com.dannykim.dtbetterend;

import com.dannykim.dtbetterend.systems.DTBetterEndRegistries;
import com.dtteam.dynamictrees.registry.NeoForgeRegistryHandler;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModList;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;

@Mod(DynamicTreesBetterEnd.MOD_ID)
public final class DynamicTreesBetterEnd {
    public static final String MOD_ID = "dtbetterend";

    public DynamicTreesBetterEnd(final IEventBus modEventBus, final ModContainer modContainer) {
        modEventBus.register(DTBetterEndRegistries.class);
        if (ModList.get().isLoaded("dynamictreesplus")) {
            modEventBus.register(com.dannykim.dtbetterend.systems.mushroom.DTPlusRegistries.class);
        }
        NeoForgeRegistryHandler.setup(MOD_ID, modEventBus);
    }

    public static ResourceLocation location(final String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }
}
