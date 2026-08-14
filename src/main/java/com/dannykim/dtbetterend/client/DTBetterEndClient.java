package com.dannykim.dtbetterend.client;

import com.dannykim.dtbetterend.DynamicTreesBetterEnd;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;

public final class DTBetterEndClient {
    private DTBetterEndClient() {
    }

    public static void register(final IEventBus modEventBus) {
        modEventBus.addListener(DTBetterEndClient::registerBlockColors);
    }

    private static void registerBlockColors(final RegisterColorHandlersEvent.Block event) {
        final Block cap = BuiltInRegistries.BLOCK.get(DynamicTreesBetterEnd.location("jellyshroom_cap"));
        final Block center = BuiltInRegistries.BLOCK.get(DynamicTreesBetterEnd.location("jellyshroom_cap_center"));
        event.register((state, level, pos, tintIndex) -> tintIndex == 0 ? jellyColor(state) : 0xFFFFFF,
                cap, center);
    }

    private static int jellyColor(final BlockState state) {
        int color = 0;
        for (final var property : state.getProperties()) {
            if (property instanceof IntegerProperty integerProperty && "color".equals(property.getName())) {
                color = state.getValue(integerProperty);
                break;
            }
        }
        final int red = 217 + (164 - 217) * color / 7;
        final int green = 142 + (0 - 142) * color / 7;
        return red << 16 | green << 8 | 255;
    }
}
