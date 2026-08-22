package com.dannykim.dtbetterend.loot;

import com.dannykim.dtbetterend.DynamicTreesBetterEnd;
import com.mojang.serialization.Codec;
import net.minecraftforge.common.loot.IGlobalLootModifier;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class LootModifiers {
    private static final DeferredRegister<Codec<? extends IGlobalLootModifier>> SERIALIZERS =
            DeferredRegister.create(ForgeRegistries.Keys.GLOBAL_LOOT_MODIFIER_SERIALIZERS,
                    DynamicTreesBetterEnd.MOD_ID);

    public static final RegistryObject<Codec<PrimitiveSaplingDropModifier>> REPLACE_PRIMITIVE_SAPLINGS =
            SERIALIZERS.register("replace_primitive_saplings", () -> PrimitiveSaplingDropModifier.CODEC);

    private LootModifiers() {
    }

    public static void register(final IEventBus bus) {
        SERIALIZERS.register(bus);
    }
}
