package com.dannykim.dtbetterend.systems.mushroom;

import com.dtteam.dynamictrees.api.registry.TypedRegistry;
import com.dtteam.dynamictrees.block.branch.BasicBranchBlock;
import com.dtteam.dynamictrees.block.branch.BranchBlock;
import com.dtteam.dynamictrees.tree.family.Family;
import com.dtteam.dynamictreesplus.tree.HugeMushroomFamily;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.state.BlockBehaviour;

public class DecoratedMushroomFamily extends HugeMushroomFamily {
    public static final TypedRegistry.EntryType<Family> TYPE = TypedRegistry.newType(DecoratedMushroomFamily::new);

    public DecoratedMushroomFamily(final Identifier name) {
        super(name);
    }

    @Override
    protected BranchBlock createBranch(final Identifier name, final BlockBehaviour.Properties properties) {
        final BasicBranchBlock branch = new DecoratedMushroomBranchBlock(name, properties);
        if (this.isFireProof()) {
            branch.setFireSpreadSpeed(0).setFlammability(0);
        }
        return branch;
    }
}
