package com.dannykim.dtbetterend.loot;

import com.dannykim.dtbetterend.DynamicTreesBetterEnd;
import com.mojang.serialization.MapCodec;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.loot.IGlobalLootModifier;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

public final class LootModifiers {
    private static final DeferredRegister<MapCodec<? extends IGlobalLootModifier>> SERIALIZERS =
            DeferredRegister.create(NeoForgeRegistries.Keys.GLOBAL_LOOT_MODIFIER_SERIALIZERS,
                    DynamicTreesBetterEnd.MOD_ID);

    public static final DeferredHolder<MapCodec<? extends IGlobalLootModifier>, MapCodec<PrimitiveSaplingDropModifier>>
            REPLACE_PRIMITIVE_SAPLINGS = SERIALIZERS.register("replace_primitive_saplings",
            () -> PrimitiveSaplingDropModifier.CODEC);

    private LootModifiers() {
    }

    public static void register(final IEventBus bus) {
        SERIALIZERS.register(bus);
    }
}
