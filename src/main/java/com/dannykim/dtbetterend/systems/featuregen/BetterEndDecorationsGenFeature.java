package com.dannykim.dtbetterend.systems.featuregen;

import com.dtteam.dynamictrees.api.configuration.ConfigurationProperty;
import com.dtteam.dynamictrees.systems.genfeature.GenFeature;
import com.dtteam.dynamictrees.systems.genfeature.GenFeatureConfiguration;
import com.dtteam.dynamictrees.systems.genfeature.context.PostGenerationContext;
import com.dtteam.dynamictrees.systems.genfeature.context.PostRotContext;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;

import java.util.ArrayList;
import java.util.List;

public final class BetterEndDecorationsGenFeature extends GenFeature {
    public enum Kind {
        MOSSY_GLOWSHROOM,
        GIGANTIC_AMARANITA
    }

    public static final ConfigurationProperty<Block> TARGET_BLOCK = ConfigurationProperty.block("target_block");
    public static final ConfigurationProperty<Block> TARGET_CENTER_BLOCK = ConfigurationProperty.block("target_center_block");
    public static final ConfigurationProperty<Block> SUPPORT_BLOCK = ConfigurationProperty.block("support_block");
    public static final ConfigurationProperty<Block> HANGING_BLOCK = ConfigurationProperty.block("hanging_block");
    public static final ConfigurationProperty<Block> SECONDARY_BLOCK = ConfigurationProperty.block("secondary_block");
    public static final ConfigurationProperty<Block> UNDERSIDE_BLOCK = ConfigurationProperty.block("underside_block");

    private final Kind kind;

    public BetterEndDecorationsGenFeature(final Identifier registryName, final Kind kind) {
        super(registryName);
        this.kind = kind;
    }

    @Override
    protected void registerProperties() {
        this.register(
                TARGET_BLOCK,
                TARGET_CENTER_BLOCK,
                SUPPORT_BLOCK,
                HANGING_BLOCK,
                SECONDARY_BLOCK,
                UNDERSIDE_BLOCK,
                PLACE_CHANCE,
                MAX_COUNT,
                MAX_HEIGHT
        );
    }

    @Override
    protected GenFeatureConfiguration createDefaultConfiguration() {
        return super.createDefaultConfiguration()
                .with(TARGET_BLOCK, Blocks.AIR)
                .with(TARGET_CENTER_BLOCK, Blocks.AIR)
                .with(SUPPORT_BLOCK, Blocks.AIR)
                .with(HANGING_BLOCK, Blocks.AIR)
                .with(SECONDARY_BLOCK, Blocks.AIR)
                .with(UNDERSIDE_BLOCK, Blocks.AIR)
                .with(PLACE_CHANCE, 1.0F)
                .with(MAX_COUNT, 8)
                .with(MAX_HEIGHT, 32);
    }

    @Override
    protected boolean postGenerate(final GenFeatureConfiguration configuration, final PostGenerationContext context) {
        if (!context.isWorldGen() && context.random().nextFloat() > configuration.get(PLACE_CHANCE)) {
            return false;
        }

        return switch (this.kind) {
            case MOSSY_GLOWSHROOM -> this.placeMossyGlowshroomDetails(configuration, context);
            case GIGANTIC_AMARANITA -> this.placeAmaranitaDetails(configuration, context);
        };
    }

    @Override
    protected boolean postRot(final GenFeatureConfiguration configuration, final PostRotContext context) {
        return this.cleanupDetachedDecorations(configuration, context.level(), context.pos(), context.radius()) > 0;
    }

    private int cleanupDetachedDecorations(final GenFeatureConfiguration configuration,
                                           final LevelAccessor level,
                                           final BlockPos rootPos,
                                           final int radius) {
        final int horizontalRange = Mth.clamp(radius + 8, 10, 24);
        final int maxHeight = configuration.get(MAX_HEIGHT);
        int removed = 0;
        for (int y = 1; y <= maxHeight; y++) {
            for (int x = -horizontalRange; x <= horizontalRange; x++) {
                for (int z = -horizontalRange; z <= horizontalRange; z++) {
                    final BlockPos pos = rootPos.offset(x, y, z);
                    final BlockState state = level.getBlockState(pos);
                    if (this.isDecoration(configuration, state) && !this.hasDecorationSupport(configuration, level, pos)) {
                        level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
                        removed++;
                    }
                }
            }
        }
        return removed;
    }

    private boolean isDecoration(final GenFeatureConfiguration configuration, final BlockState state) {
        return state.is(configuration.get(HANGING_BLOCK))
                || state.is(configuration.get(SECONDARY_BLOCK))
                || state.is(configuration.get(UNDERSIDE_BLOCK));
    }

    private boolean hasDecorationSupport(final GenFeatureConfiguration configuration,
                                         final LevelAccessor level,
                                         final BlockPos pos) {
        if (this.isTarget(configuration, level.getBlockState(pos.above()))) {
            return true;
        }
        for (final Direction direction : Direction.values()) {
            if (this.isTarget(configuration, level.getBlockState(pos.relative(direction)))
                    || this.isDecoration(configuration, level.getBlockState(pos.relative(direction)))) {
                return true;
            }
        }
        return false;
    }

    private boolean placeMossyGlowshroomDetails(final GenFeatureConfiguration configuration, final PostGenerationContext context) {
        final Block hymenophore = configuration.get(HANGING_BLOCK);
        final Block fur = configuration.get(SECONDARY_BLOCK);
        if (hymenophore == Blocks.AIR) {
            return false;
        }

        int placed = 0;
        for (final BlockPos pos : this.findUndersideCandidates(configuration, context.level(), context.pos(), context.radius(), 3)) {
            if (placed >= configuration.get(MAX_COUNT)) {
                break;
            }
            final BlockPos below = pos.below();
            if (levelIsEmpty(context.level(), below)) {
                context.level().setBlock(below, hymenophore.defaultBlockState(), Block.UPDATE_ALL);
                placed++;
                this.placeFurAround(context.level(), below, fur, 0.45F);
            }
        }
        return placed > 0;
    }

    private boolean placeAmaranitaDetails(final GenFeatureConfiguration configuration, final PostGenerationContext context) {
        final Block lantern = configuration.get(HANGING_BLOCK);
        final Block fur = configuration.get(SECONDARY_BLOCK);
        final Block hymenophore = configuration.get(UNDERSIDE_BLOCK);
        if (lantern == Blocks.AIR && hymenophore == Blocks.AIR) {
            return false;
        }

        int placed = 0;
        for (final BlockPos pos : this.findUndersideCandidates(configuration, context.level(), context.pos(), context.radius(), 2)) {
            if (placed >= configuration.get(MAX_COUNT) && hymenophore == Blocks.AIR) {
                break;
            }
            final BlockPos underside = pos.below();
            if (hymenophore != Blocks.AIR && levelIsEmpty(context.level(), underside)) {
                context.level().setBlock(underside, hymenophore.defaultBlockState(), Block.UPDATE_ALL);
            }
            if (placed >= configuration.get(MAX_COUNT) || lantern == Blocks.AIR || context.random().nextFloat() > 0.55F) {
                continue;
            }
            BlockPos cursor = hymenophore == Blocks.AIR ? underside : underside.below();
            int length = 1 + context.random().nextInt(2);
            for (int index = 0; index < length && levelIsEmpty(context.level(), cursor); index++) {
                context.level().setBlock(cursor, lantern.defaultBlockState(), Block.UPDATE_ALL);
                cursor = cursor.below();
                placed++;
            }
            if (fur != Blocks.AIR && levelIsEmpty(context.level(), cursor)) {
                context.level().setBlock(cursor, withFacing(fur.defaultBlockState(), Direction.DOWN), Block.UPDATE_ALL);
            }
        }
        return placed > 0;
    }

    private List<BlockPos> findUndersideCandidates(final GenFeatureConfiguration configuration,
                                                   final LevelAccessor level,
                                                   final BlockPos rootPos,
                                                   final int radius,
                                                   final int minHeight) {
        final List<BlockPos> candidates = new ArrayList<>();
        final int horizontalRange = Mth.clamp(radius + 8, 10, 24);
        final int maxHeight = configuration.get(MAX_HEIGHT);
        for (int y = minHeight; y <= maxHeight; y++) {
            for (int x = -horizontalRange; x <= horizontalRange; x++) {
                for (int z = -horizontalRange; z <= horizontalRange; z++) {
                    final BlockPos pos = rootPos.offset(x, y, z);
                    if (this.isTarget(configuration, level.getBlockState(pos)) && levelIsEmpty(level, pos.below())) {
                        candidates.add(pos.immutable());
                    }
                }
            }
        }
        return candidates;
    }

    private boolean isTarget(final GenFeatureConfiguration configuration, final BlockState state) {
        final Block target = configuration.get(TARGET_BLOCK);
        final Block center = configuration.get(TARGET_CENTER_BLOCK);
        return state.is(target) || (center != Blocks.AIR && state.is(center));
    }

    private boolean canReplaceDecoration(final GenFeatureConfiguration configuration,
                                         final LevelAccessor level,
                                         final BlockPos pos) {
        final BlockState state = level.getBlockState(pos);
        if (state.isAir()) {
            return true;
        }
        if (this.isTarget(configuration, state) || state.is(configuration.get(SUPPORT_BLOCK))) {
            return false;
        }
        return state.getCollisionShape(level, pos).isEmpty();
    }

    private void placeFurAround(final LevelAccessor level, final BlockPos base, final Block fur, final float chance) {
        if (fur == Blocks.AIR) {
            return;
        }
        for (final Direction direction : Direction.values()) {
            if (level.getRandom().nextFloat() > chance) {
                continue;
            }
            final BlockPos pos = base.relative(direction);
            if (levelIsEmpty(level, pos)) {
                level.setBlock(pos, withFacing(fur.defaultBlockState(), direction), Block.UPDATE_ALL);
            }
        }
    }

    private static BlockState withFacing(final BlockState state, final Direction direction) {
        for (final Property<?> property : state.getProperties()) {
            if ("facing".equals(property.getName())) {
                return setUnchecked(state, property, direction);
            }
        }
        return state;
    }

    private static BlockState withBoolean(final BlockState state, final String name, final boolean value) {
        for (final Property<?> property : state.getProperties()) {
            if (name.equals(property.getName())) {
                return setUnchecked(state, property, value);
            }
        }
        return state;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static BlockState setUnchecked(final BlockState state, final Property property, final Object value) {
        return state.setValue(property, (Comparable) value);
    }

    private static boolean levelIsEmpty(final LevelAccessor level, final BlockPos pos) {
        return level.isEmptyBlock(pos);
    }
}
