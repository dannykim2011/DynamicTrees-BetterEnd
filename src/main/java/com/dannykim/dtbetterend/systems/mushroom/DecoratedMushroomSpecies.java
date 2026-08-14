package com.dannykim.dtbetterend.systems.mushroom;

import com.ferreusveritas.dynamictrees.api.registry.TypedRegistry;
import com.ferreusveritas.dynamictrees.tree.family.Family;
import com.ferreusveritas.dynamictrees.tree.species.Species;
import com.ferreusveritas.dynamictreesplus.block.mushroom.CapProperties;
import com.ferreusveritas.dynamictreesplus.tree.HugeMushroomSpecies;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;

public class DecoratedMushroomSpecies extends HugeMushroomSpecies {
    public static final TypedRegistry.EntryType<Species> TYPE =
            HugeMushroomSpecies.createDefaultMushroomType(DecoratedMushroomSpecies::new);

    public DecoratedMushroomSpecies(final ResourceLocation name, final Family family, final CapProperties capProperties) {
        super(name, family, capProperties);
    }

    public static boolean isFellingDecoration(final BlockState state) {
        final ResourceLocation key = BuiltInRegistries.BLOCK.getKey(state.getBlock());
        if (!"betterend".equals(key.getNamespace())) {
            return false;
        }
        return switch (key.getPath()) {
            case "mossy_glowshroom_hymenophore",
                 "mossy_glowshroom_fur",
                 "amaranita_hymenophore",
                 "amaranita_lantern",
                 "amaranita_fur" -> true;
            default -> false;
        };
    }
}
