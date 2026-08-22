package com.dannykim.dtbetterend.mixin.client;

import com.dtteam.dynamictrees.model.QuadManipulator;
import com.dtteam.dynamictrees.model.ModelConnections;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.multipart.MultiPartModel;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(value = QuadManipulator.class, remap = false)
public abstract class QuadManipulatorMixin {
    @Inject(
            method = "getQuads(Lnet/minecraft/client/renderer/block/dispatch/BlockStateModel;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/util/RandomSource;Lcom/dtteam/dynamictrees/model/ModelConnections;)Ljava/util/List;",
            at = @At("HEAD"),
            cancellable = true,
            remap = false
    )
    private static void dtbetterend$collectSelectedMultipartQuads(
            final BlockStateModel model,
            final BlockState state,
            final Vec3 offset,
            final RandomSource random,
            final ModelConnections connections,
            final CallbackInfoReturnable<List<BakedQuad>> cir
    ) {
        if (model instanceof MultiPartModel) {
            cir.setReturnValue(QuadManipulator.getQuads(
                    model, state, offset, QuadManipulator.everyFace, random, connections));
        }
    }
}
