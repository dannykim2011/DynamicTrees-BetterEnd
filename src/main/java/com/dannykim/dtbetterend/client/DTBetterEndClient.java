package com.dannykim.dtbetterend.client;

import com.dannykim.dtbetterend.DynamicTreesBetterEnd;
import com.dtteam.dynamictrees.client.TintSources.SuppliedConstantTintSource;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.client.color.block.BlockTintSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import org.jspecify.annotations.Nullable;

public final class DTBetterEndClient {

    private DTBetterEndClient() {
    }

    public static void register(final IEventBus modEventBus) {
        modEventBus.addListener(DTBetterEndClient::registerBlockColors);
        modEventBus.addListener(DTBetterEndClient::forceOpaqueUmbrellaGrowthCanopy);
    }

    private static void forceOpaqueUmbrellaGrowthCanopy(final ModelEvent.ModifyBakingResult event) {
        final Block leaves = BuiltInRegistries.BLOCK.getValue(
                DynamicTreesBetterEnd.location("umbrella_tree_leaves"));
        final var models = event.getBakingResult().blockStateModels();
        for (final BlockState state : leaves.getStateDefinition().getPossibleStates()) {
            final BlockStateModel model = models.get(state);
            if (model != null && !(model instanceof CutoutBlockStateModel)) {
                models.put(state, new CutoutBlockStateModel(model));
            }
        }
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

    private record CutoutBlockStateModel(BlockStateModel delegate) implements BlockStateModel {
        @Override
        public void collectParts(final RandomSource random, final List<BlockStateModelPart> output) {
            final List<BlockStateModelPart> parts = new ArrayList<>();
            delegate.collectParts(random, parts);
            wrapParts(parts, output);
        }

        @Override
        public void collectParts(final BlockAndTintGetter level, final BlockPos pos, final BlockState state,
                                 final RandomSource random, final List<BlockStateModelPart> output) {
            final List<BlockStateModelPart> parts = new ArrayList<>();
            delegate.collectParts(level, pos, state, random, parts);
            wrapParts(parts, output);
        }

        private static void wrapParts(final List<BlockStateModelPart> parts,
                                      final List<BlockStateModelPart> output) {
            for (final BlockStateModelPart part : parts) {
                if (part != null) output.add(new CutoutBlockStateModelPart(part));
            }
        }

        @Override public Material.Baked particleMaterial() { return delegate.particleMaterial(); }
        @Override public Material.Baked particleMaterial(final BlockAndTintGetter level, final BlockPos pos,
                                                         final BlockState state) {
            return delegate.particleMaterial(level, pos, state);
        }
        @Override public int materialFlags() {
            return delegate.materialFlags() & ~BakedQuad.FLAG_TRANSLUCENT;
        }
        @Override public int materialFlags(final BlockAndTintGetter level, final BlockPos pos,
                                           final BlockState state) {
            return delegate.materialFlags(level, pos, state) & ~BakedQuad.FLAG_TRANSLUCENT;
        }
        @Override public @Nullable Object createGeometryKey(final BlockAndTintGetter level, final BlockPos pos,
                                                            final BlockState state, final RandomSource random) {
            return delegate.createGeometryKey(level, pos, state, random);
        }
    }

    private record CutoutBlockStateModelPart(BlockStateModelPart delegate) implements BlockStateModelPart {
        @Override
        public List<BakedQuad> getQuads(@Nullable final Direction direction) {
            return delegate.getQuads(direction).stream().map(DTBetterEndClient::forceCutout).toList();
        }
        @Override public boolean useAmbientOcclusion() { return delegate.useAmbientOcclusion(); }
        @Override public Material.Baked particleMaterial() { return delegate.particleMaterial(); }
        @Override public int materialFlags() {
            return delegate.materialFlags() & ~BakedQuad.FLAG_TRANSLUCENT;
        }
    }

    private static BakedQuad forceCutout(final BakedQuad quad) {
        final BakedQuad.MaterialInfo material = quad.materialInfo();
        final BakedQuad.MaterialInfo cutout = new BakedQuad.MaterialInfo(
                material.sprite(), ChunkSectionLayer.CUTOUT, Sheets.cutoutBlockItemSheet(),
                material.tintIndex(), material.shade(), material.lightEmission(), material.ambientOcclusion());
        return new BakedQuad(quad.position0(), quad.position1(), quad.position2(), quad.position3(),
                quad.packedUV0(), quad.packedUV1(), quad.packedUV2(), quad.packedUV3(), quad.direction(),
                cutout, quad.bakedNormals(), quad.bakedColors());
    }

}
