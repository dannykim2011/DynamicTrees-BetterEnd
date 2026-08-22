package com.dannykim.dtbetterend.systems.mushroom;

import com.ferreusveritas.dynamictrees.block.branch.BranchBlock;
import com.ferreusveritas.dynamictrees.entity.FallingTreeEntity;
import com.ferreusveritas.dynamictrees.entity.animation.AnimationHandler;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.core.registries.BuiltInRegistries;

import java.util.ArrayList;
import java.util.List;

final class MushroomFellingAnimationHandler implements AnimationHandler {
    private final AnimationHandler delegate;

    MushroomFellingAnimationHandler(final AnimationHandler delegate) {
        this.delegate = delegate;
    }

    @Override public String getName() { return delegate.getName() + "_deferred_caps"; }

    @Override
    public void initMotion(final FallingTreeEntity entity) {
        final List<BranchBlock.ItemStackPos> capDrops = entity.getDestroyData().leavesDrops;
        final List<BranchBlock.ItemStackPos> deferred = new ArrayList<>();
        capDrops.removeIf(drop -> {
            if (isSeedDrop(drop.stack)) return false;
            deferred.add(drop);
            return true;
        });
        delegate.initMotion(entity);
        capDrops.clear();
        capDrops.addAll(deferred);
        if (delegate.shouldDie(entity)) FallingTreeEntity.standardDropLeavesPayLoad(entity);
    }

    private static boolean isSeedDrop(final net.minecraft.world.item.ItemStack stack) {
        return BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath().endsWith("_seed");
    }

    @Override public void handleMotion(final FallingTreeEntity entity) { delegate.handleMotion(entity); }

    @Override
    public void dropPayload(final FallingTreeEntity entity) {
        delegate.dropPayload(entity);
        FallingTreeEntity.standardDropLeavesPayLoad(entity);
    }

    @Override public boolean shouldDie(final FallingTreeEntity entity) { return delegate.shouldDie(entity); }
    @Override public void renderTransform(final FallingTreeEntity entity, final float yaw, final float partialTicks,
                                          final PoseStack stack) {
        delegate.renderTransform(entity, yaw, partialTicks, stack);
    }
    @Override public boolean shouldRender(final FallingTreeEntity entity) { return delegate.shouldRender(entity); }
}
