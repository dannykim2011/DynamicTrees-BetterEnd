package com.dannykim.dtbetterend.systems.umbrella;

import com.dtteam.dynamictrees.entity.FallingTreeEntity;
import com.dtteam.dynamictrees.entity.animation.AnimationHandler;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

final class UmbrellaFellingAnimationHandler implements AnimationHandler {
    private final AnimationHandler delegate;
    private final List<ItemStack> clusterDrops = new ArrayList<>();
    private boolean clustersDropped;
    UmbrellaFellingAnimationHandler(final AnimationHandler delegate) { this.delegate = delegate; }
    @Override public String getName() { return delegate.getName() + "_umbrella_cluster_payload"; }
    @Override public void initMotion(final FallingTreeEntity entity) {
        entity.getDestroyData().leavesDrops.removeIf(drop -> {
            final var id = BuiltInRegistries.ITEM.getKey(drop.stack.getItem());
            if (!"betterend".equals(id.getNamespace()) || !"umbrella_tree_cluster".equals(id.getPath())) return false;
            clusterDrops.add(drop.stack.copy());
            return true;
        });
        delegate.initMotion(entity);
        if (delegate.shouldDie(entity)) dropClusters(entity);
    }
    @Override public void handleMotion(final FallingTreeEntity entity) { delegate.handleMotion(entity); }
    @Override public void dropPayload(final FallingTreeEntity entity) { delegate.dropPayload(entity); dropClusters(entity); }
    @Override public boolean shouldDie(final FallingTreeEntity entity) { return delegate.shouldDie(entity); }
    @Override public void renderTransform(final FallingTreeEntity entity, final float yaw, final float partialTicks, final PoseStack stack) { delegate.renderTransform(entity, yaw, partialTicks, stack); }
    @Override public boolean shouldRender(final FallingTreeEntity entity, final double x, final double y, final double z) { return delegate.shouldRender(entity, x, y, z); }

    private void dropClusters(final FallingTreeEntity entity) {
        if (clustersDropped) return;
        clustersDropped = true;
        clusterDrops.forEach(stack -> FallingTreeEntity.spawnItemAsEntity(
                entity.level(), entity.getDestroyData().cutPos, stack));
    }
}
