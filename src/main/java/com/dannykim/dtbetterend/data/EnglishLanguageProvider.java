package com.dannykim.dtbetterend.data;

import com.dannykim.dtbetterend.DynamicTreesBetterEnd;
import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.common.data.LanguageProvider;

public final class EnglishLanguageProvider extends LanguageProvider {
    public EnglishLanguageProvider(final PackOutput output) {
        super(output, DynamicTreesBetterEnd.MOD_ID, "en_us");
    }

    @Override
    protected void addTranslations() {
        add("item.dtbetterend.dragon_tree_seed", "Dragon Tree Seed");
        add("item.dtbetterend.gigantic_amaranita_seed", "Gigantic Amaranita Spore");
        add("item.dtbetterend.helix_tree_seed", "Helix Tree Inflorescence");
        add("item.dtbetterend.jellyshroom_seed", "Jellyshroom Spore");
        add("item.dtbetterend.lacugrove_seed", "Lacugrove Seed");
        add("item.dtbetterend.lucernia_seed", "Lucernia Seed");
        add("item.dtbetterend.mossy_glowshroom_seed", "Mossy Glowshroom Spore");
        add("item.dtbetterend.neon_cactus_seed", "Neon Cactus Seed");
        add("item.dtbetterend.pythadendron_seed", "Pythadendron Seed");
        add("item.dtbetterend.tenanea_seed", "Tenanea Seed");
        add("item.dtbetterend.umbrella_tree_seed", "Umbrella Tree Cone");
    }
}
