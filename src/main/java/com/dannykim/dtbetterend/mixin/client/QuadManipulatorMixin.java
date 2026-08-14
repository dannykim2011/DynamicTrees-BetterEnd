package com.dannykim.dtbetterend.mixin.client;

import com.dtteam.dynamictrees.model.QuadManipulator;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.core.Direction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.List;

@Mixin(value = QuadManipulator.class, remap = false)
public abstract class QuadManipulatorMixin {
    @Redirect(
            method = "getQuads(Lnet/minecraft/client/renderer/block/dispatch/BlockStateModel;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/phys/Vec3;[Lnet/minecraft/core/Direction;Lnet/minecraft/util/RandomSource;Lcom/dtteam/dynamictrees/model/ModelConnections;)Ljava/util/List;",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/block/dispatch/BlockStateModelPart;getQuads(Lnet/minecraft/core/Direction;)Ljava/util/List;"
            ),
            remap = false
    )
    private static List<BakedQuad> dtbetterend$skipMissingModelPart(
            final BlockStateModelPart part,
            final Direction direction
    ) {
        return part == null ? List.of() : part.getQuads(direction);
    }
}
