package com.dannykim.dtbetterend.systems.featuregen;

import com.dtteam.dynamictrees.systems.genfeature.ClearVolumeGenFeature;
import com.dtteam.dynamictrees.systems.genfeature.GenFeatureConfiguration;
import com.dtteam.dynamictrees.systems.genfeature.context.PreGenerationContext;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;

public final class TreeSafeClearVolumeGenFeature extends ClearVolumeGenFeature {
    public TreeSafeClearVolumeGenFeature(ResourceLocation name) {
        super(name);
    }

    @Override
    protected BlockPos preGenerate(GenFeatureConfiguration configuration, PreGenerationContext context) {
        final BlockPos root = context.pos();
        for (BlockPos pos : BlockPos.betweenClosed(root.offset(-1, 1, -1),
                root.offset(1, configuration.get(HEIGHT), 1))) {
            if (!TreeSafeMoundGenFeature.isTreeStructure(context.level(), pos)) {
                context.level().removeBlock(pos, false);
            }
        }
        return root;
    }
}
