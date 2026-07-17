package com.dannykim.dtbetterend.systems.featuregen;

import com.dtteam.dynamictrees.systems.genfeature.GenFeature;
import com.dtteam.dynamictrees.systems.genfeature.GenFeatureConfiguration;
import com.dtteam.dynamictrees.systems.genfeature.context.PostGenerationContext;
import com.dtteam.dynamictrees.systems.genfeature.context.PreGenerationContext;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class RemoveBrokenChorusGenFeature extends GenFeature {
    private static final int HORIZONTAL_RANGE = 18;
    private static final int DOWN_RANGE = 3;
    private static final int UP_RANGE = 72;
    private static final int MAX_COMPONENT_SIZE = 2048;
    private static final Map<String, List<Set<BlockPos>>> PENDING = new ConcurrentHashMap<>();

    public RemoveBrokenChorusGenFeature(final Identifier registryName) {
        super(registryName);
    }

    @Override
    protected void registerProperties() {
    }

    @Override
    protected BlockPos preGenerate(final GenFeatureConfiguration configuration, final PreGenerationContext context) {
        final List<Set<BlockPos>> components = findChorusComponents(context.level(), context.pos());
        if (!components.isEmpty()) {
            PENDING.put(key(context.level(), context.pos()), components);
        }
        return context.pos();
    }

    @Override
    protected boolean postGenerate(final GenFeatureConfiguration configuration, final PostGenerationContext context) {
        final List<Set<BlockPos>> components = PENDING.remove(key(context.level(), context.originPos()));
        if (components == null || components.isEmpty()) {
            return false;
        }
        for (Set<BlockPos> component : components) {
            if (component.stream().anyMatch(pos -> !isChorus(context.level().getBlockState(pos)))) {
                removeRemainingComponent(context.level(), component);
            }
        }
        return true;
    }

    private static List<Set<BlockPos>> findChorusComponents(final LevelAccessor level, final BlockPos origin) {
        final List<Set<BlockPos>> components = new ArrayList<>();
        final Set<BlockPos> visited = new HashSet<>();
        for (BlockPos pos : BlockPos.betweenClosed(
                origin.offset(-HORIZONTAL_RANGE, -DOWN_RANGE, -HORIZONTAL_RANGE),
                origin.offset(HORIZONTAL_RANGE, UP_RANGE, HORIZONTAL_RANGE)
        )) {
            final BlockPos immutable = pos.immutable();
            if (!visited.contains(immutable) && isChorus(level.getBlockState(immutable))) {
                components.add(collectComponent(level, immutable, visited));
            }
        }
        return components;
    }

    private static Set<BlockPos> collectComponent(final LevelAccessor level, final BlockPos start, final Set<BlockPos> visited) {
        final Set<BlockPos> component = new HashSet<>();
        final ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        queue.add(start);
        visited.add(start);
        while (!queue.isEmpty() && component.size() < MAX_COMPONENT_SIZE) {
            final BlockPos pos = queue.removeFirst();
            component.add(pos);
            for (Direction direction : Direction.values()) {
                final BlockPos next = pos.relative(direction);
                if (!visited.contains(next) && isChorus(level.getBlockState(next))) {
                    visited.add(next);
                    queue.add(next);
                }
            }
        }
        return component;
    }

    private static void removeRemainingComponent(final LevelAccessor level, final Set<BlockPos> originalComponent) {
        final Set<BlockPos> removed = new HashSet<>();
        for (BlockPos pos : originalComponent) {
            if (isChorus(level.getBlockState(pos))) {
                removeConnected(level, pos, removed);
            }
        }
    }

    private static void removeConnected(final LevelAccessor level, final BlockPos start, final Set<BlockPos> removed) {
        final ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        queue.add(start);
        removed.add(start);
        while (!queue.isEmpty() && removed.size() < MAX_COMPONENT_SIZE) {
            final BlockPos pos = queue.removeFirst();
            if (!isChorus(level.getBlockState(pos))) {
                continue;
            }
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
            for (Direction direction : Direction.values()) {
                final BlockPos next = pos.relative(direction);
                if (!removed.contains(next) && isChorus(level.getBlockState(next))) {
                    removed.add(next);
                    queue.add(next);
                }
            }
        }
    }

    private static boolean isChorus(final BlockState state) {
        return state.is(Blocks.CHORUS_PLANT) || state.is(Blocks.CHORUS_FLOWER);
    }

    private static String key(final LevelAccessor level, final BlockPos pos) {
        return System.identityHashCode(level) + ":" + pos.asLong();
    }
}
