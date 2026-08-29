package com.dannykim.dtbetterend.systems.umbrella;

import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;

public final class UmbrellaGrowthForm {
    private static final String SEED_KEY = "dtbetterend_umbrella_growth_seed";
    private static final Map<LevelAccessor, Map<BlockPos, Integer>> PENDING_SEEDS = new WeakHashMap<>();
    private static final Map<LevelAccessor, Map<BlockPos, Integer>> LAST_FORMS = new WeakHashMap<>();

    private UmbrellaGrowthForm() {
    }

    public static void assignNew(final LevelAccessor level, final BlockPos rootPos,
                                 final RandomSource random) {
        final BlockEntity blockEntity = level.getBlockEntity(rootPos);
        final Integer previousForm = storedForm(blockEntity) != null
                ? storedForm(blockEntity) : lastForm(level, rootPos);
        final int nextForm = previousForm == null ? random.nextInt(3) : (previousForm + 1) % 3;
        final int seed = seedForForm(random, nextForm);
        rememberLastForm(level, rootPos, nextForm);
        if (blockEntity != null) {
            writeSeed(blockEntity, seed);
            forgetPending(level, rootPos);
        } else {
            rememberPending(level, rootPos, seed);
        }
    }

    public static int seed(final LevelAccessor level, final BlockPos rootPos,
                           final RandomSource random) {
        final BlockEntity blockEntity = level.getBlockEntity(rootPos);
        if (blockEntity != null && blockEntity.getPersistentData().contains(SEED_KEY)) {
            final int seed = blockEntity.getPersistentData().getInt(SEED_KEY)
                    .orElseGet(() -> seedForForm(random, random.nextInt(3)));
            rememberLastForm(level, rootPos, Math.floorMod(seed, 3));
            return seed;
        }
        final Integer pendingSeed = pendingSeed(level, rootPos);
        if (pendingSeed != null) {
            if (blockEntity != null) {
                writeSeed(blockEntity, pendingSeed);
                forgetPending(level, rootPos);
            }
            rememberLastForm(level, rootPos, Math.floorMod(pendingSeed, 3));
            return pendingSeed;
        }
        final int form = random.nextInt(3);
        final int seed = seedForForm(random, form);
        rememberLastForm(level, rootPos, form);
        writeSeed(blockEntity, seed);
        if (blockEntity == null) rememberPending(level, rootPos, seed);
        return seed;
    }

    public static int form(final LevelAccessor level, final BlockPos rootPos,
                           final RandomSource random) {
        return selectedForm(seed(level, rootPos, random));
    }

    public static int selectedForm(final int seed) {
        return Math.floorMod(seed, 3);
    }

    private static void writeSeed(final BlockEntity blockEntity, final int seed) {
        if (blockEntity == null) return;
        blockEntity.getPersistentData().putInt(SEED_KEY, seed);
        blockEntity.setChanged();
    }

    private static synchronized void rememberPending(final LevelAccessor level, final BlockPos rootPos,
                                                     final int seed) {
        PENDING_SEEDS.computeIfAbsent(level, ignored -> new HashMap<>()).put(rootPos.immutable(), seed);
    }

    private static synchronized Integer pendingSeed(final LevelAccessor level, final BlockPos rootPos) {
        final Map<BlockPos, Integer> seeds = PENDING_SEEDS.get(level);
        return seeds == null ? null : seeds.get(rootPos);
    }

    private static synchronized void forgetPending(final LevelAccessor level, final BlockPos rootPos) {
        final Map<BlockPos, Integer> seeds = PENDING_SEEDS.get(level);
        if (seeds == null) return;
        seeds.remove(rootPos);
        if (seeds.isEmpty()) PENDING_SEEDS.remove(level);
    }

    private static Integer storedForm(final BlockEntity blockEntity) {
        if (blockEntity == null || !blockEntity.getPersistentData().contains(SEED_KEY)) return null;
        return blockEntity.getPersistentData().getInt(SEED_KEY)
                .map(seed -> Math.floorMod(seed, 3)).orElse(null);
    }

    private static int seedForForm(final RandomSource random, final int form) {
        return random.nextInt(715827882) * 3 + form;
    }

    private static synchronized Integer lastForm(final LevelAccessor level, final BlockPos rootPos) {
        final Map<BlockPos, Integer> forms = LAST_FORMS.get(level);
        return forms == null ? null : forms.get(rootPos);
    }

    private static synchronized void rememberLastForm(final LevelAccessor level, final BlockPos rootPos,
                                                      final int form) {
        LAST_FORMS.computeIfAbsent(level, ignored -> new HashMap<>()).put(rootPos.immutable(), form);
    }
}
