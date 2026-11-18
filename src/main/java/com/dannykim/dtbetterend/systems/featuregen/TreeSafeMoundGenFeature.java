package com.dannykim.dtbetterend.systems.featuregen;

import com.dtteam.dynamictrees.tree.TreeHelper;
import com.dtteam.dynamictrees.block.branch.BranchBlock;
import com.dtteam.dynamictrees.block.branch.SurfaceRootBlock;
import com.dtteam.dynamictrees.block.branch.TrunkShellBlock;
import com.dtteam.dynamictrees.systems.genfeature.context.PostGenerationContext;
import net.minecraft.world.level.LevelAccessor;
import com.dtteam.dynamictrees.systems.genfeature.MoundGenFeature;
import com.dtteam.dynamictrees.systems.genfeature.GenFeatureConfiguration;
import com.dtteam.dynamictrees.systems.genfeature.context.PreGenerationContext;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;

public final class TreeSafeMoundGenFeature extends MoundGenFeature {
    public TreeSafeMoundGenFeature(Identifier name) {
        super(name);
    }

    @Override
    protected BlockPos preGenerate(GenFeatureConfiguration configuration, PreGenerationContext context) {
        final BlockPos root = context.pos();
        for (BlockPos pos : BlockPos.betweenClosed(root.offset(-2, -3, -2), root.offset(2, 1, 2))) {
            if (isTreeStructure(context.level(), pos)) return root;
        }
        return super.preGenerate(configuration, context);
    }

    static boolean isTreeStructure(LevelAccessor level, BlockPos pos) {
        var block = level.getBlockState(pos).getBlock();
        return block instanceof BranchBlock || block instanceof TrunkShellBlock || block instanceof SurfaceRootBlock
                || TreeHelper.getRooty(level.getBlockState(pos)) != null;
    }

    @Override
    protected boolean postGenerate(GenFeatureConfiguration configuration, PostGenerationContext context) {
        final BlockPos root = context.pos();
        for (BlockPos pos : BlockPos.betweenClosed(root.offset(-1, -1, -1), root.offset(1, 0, 1))) {
            if (!pos.equals(root) && isTreeStructure(context.level(), pos)) return false;
        }
        return super.postGenerate(configuration, context);
    }
}
