package com.dannykim.dtbetterend.loot;

import com.dannykim.dtbetterend.DynamicTreesBetterEnd;
import com.dtteam.dynamictrees.tree.TreeHelper;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.neoforged.neoforge.common.loot.IGlobalLootModifier;
import net.neoforged.neoforge.common.loot.LootModifier;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

public final class PrimitiveSaplingDropModifier extends LootModifier {
    public static final MapCodec<PrimitiveSaplingDropModifier> CODEC = RecordCodecBuilder.mapCodec(instance ->
            codecStart(instance).apply(instance, PrimitiveSaplingDropModifier::new));

    private static final Map<Identifier, Identifier> REPLACEMENTS = Map.ofEntries(
            entry("betterend:dragon_tree_sapling", "dragon_tree_seed"),
            entry("betterend:helix_tree_sapling", "helix_tree_seed"),
            entry("betterend:lacugrove_sapling", "lacugrove_seed"),
            entry("betterend:lucernia_sapling", "lucernia_seed"),
            entry("betterend:pythadendron_sapling", "pythadendron_seed"),
            entry("betterend:tenanea_sapling", "tenanea_seed"),
            entry("betterend:umbrella_tree_sapling", "umbrella_tree_seed"),
            entry("betterend:small_amaranita_mushroom", "gigantic_amaranita_seed"),
            entry("betterend:small_jellyshroom", "jellyshroom_seed"),
            entry("betterend:mossy_glowshroom_sapling", "mossy_glowshroom_seed")
    );

    private PrimitiveSaplingDropModifier(final LootItemCondition[] conditions, final int priority) {
        super(conditions, priority);
    }

    @Override
    protected @NotNull ObjectArrayList<ItemStack> doApply(final ObjectArrayList<ItemStack> loot,
                                                           final LootContext context) {
        final BlockState source = context.getOptionalParameter(LootContextParams.BLOCK_STATE);
        if (source != null && TreeHelper.isBranch(source)) return loot;
        if (loot.stream().noneMatch(stack -> REPLACEMENTS.containsKey(BuiltInRegistries.ITEM.getKey(stack.getItem())))) {
            return loot;
        }
        if (loot.stream().anyMatch(PrimitiveSaplingDropModifier::isDynamicSeed)) {
            loot.removeIf(stack -> REPLACEMENTS.containsKey(BuiltInRegistries.ITEM.getKey(stack.getItem())));
            return loot;
        }
        loot.replaceAll(stack -> replace(stack, REPLACEMENTS.get(BuiltInRegistries.ITEM.getKey(stack.getItem()))));
        return loot;
    }

    private static ItemStack replace(final ItemStack original, final Identifier replacementId) {
        if (replacementId == null || !BuiltInRegistries.ITEM.containsKey(replacementId)) return original;
        final Item replacement = BuiltInRegistries.ITEM.getValue(replacementId);
        return replacement == null ? original : new ItemStack(replacement, original.getCount());
    }

    private static boolean isDynamicSeed(final ItemStack stack) {
        final Identifier id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        return DynamicTreesBetterEnd.MOD_ID.equals(id.getNamespace()) && id.getPath().endsWith("_seed");
    }

    private static Map.Entry<Identifier, Identifier> entry(final String primitive, final String seed) {
        return Map.entry(Identifier.parse(primitive), DynamicTreesBetterEnd.location(seed));
    }

    @Override
    public MapCodec<? extends IGlobalLootModifier> codec() {
        return CODEC;
    }
}
