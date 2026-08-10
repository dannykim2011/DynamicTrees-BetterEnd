package com.dannykim.dtbetterend.systems.mushroom;

import com.ferreusveritas.dynamictrees.api.registry.TypedRegistry;
import com.ferreusveritas.dynamictrees.block.branch.BasicBranchBlock;
import com.ferreusveritas.dynamictrees.block.branch.BranchBlock;
import com.ferreusveritas.dynamictrees.tree.family.Family;
import com.ferreusveritas.dynamictreesplus.tree.HugeMushroomFamily;
import net.minecraft.resources.ResourceLocation;

public class DecoratedMushroomFamily extends HugeMushroomFamily {
    public static final TypedRegistry.EntryType<Family> TYPE = TypedRegistry.newType(DecoratedMushroomFamily::new);

    public DecoratedMushroomFamily(final ResourceLocation name) {
        super(name);
    }

    @Override
    protected BranchBlock createBranchBlock(final ResourceLocation name) {
        final BasicBranchBlock branch = new DecoratedMushroomBranchBlock(name, this.getProperties());
        if (this.isFireProof()) {
            branch.setFireSpreadSpeed(0).setFlammability(0);
        }
        return branch;
    }
}
