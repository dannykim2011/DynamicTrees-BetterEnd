package com.dannykim.dtbetterend.client;

import com.dannykim.dtbetterend.DynamicTreesBetterEnd;
import com.dtteam.dynamictrees.client.TintSources.SuppliedConstantTintSource;
import java.util.List;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.client.color.block.BlockTintSource;
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

    private static void registerBlockColors(final RegisterColorHandlersEvent.BlockTintSources event) {
        final Block helixLeaves = BuiltInRegistries.BLOCK.getValue(DynamicTreesBetterEnd.location("helix_tree_leaves"));
        final Block cap = BuiltInRegistries.BLOCK.getValue(DynamicTreesBetterEnd.location("jellyshroom_cap"));
        final Block center = BuiltInRegistries.BLOCK.getValue(DynamicTreesBetterEnd.location("jellyshroom_cap_center"));

        event.register(List.of(new SuppliedConstantTintSource(() -> 0xFFFFFF)), helixLeaves);
        event.register(List.of(new JellyshroomTintSource()), cap, center);
    }

    private record JellyshroomTintSource() implements BlockTintSource {
        @Override
        public int color(final BlockState state) {
            int color = 0;
            for (final var property : state.getProperties()) {
                if (property instanceof IntegerProperty integerProperty && "color".equals(property.getName())) {
                    color = state.getValue(integerProperty);
                    break;
                }
            }
            final int red = 217 + (164 - 217) * color / 7;
            final int green = 142 + (0 - 142) * color / 7;
            return 0xFF000000 | (red << 16) | (green << 8) | 0xFF;
        }
    }

}
