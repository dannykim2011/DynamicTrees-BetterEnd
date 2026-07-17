package com.dannykim.dtbetterend.mixin;

import org.betterx.betterend.util.DebugHelpers;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * BetterEnd creates thirteen development-only debug items after the item
 * registry has closed. NeoForge then rejects their unbound intrusive holders.
 * Production never needs these helpers, and the add-on's data run is a
 * development environment, so suppress only this late debug registration.
 */
@Mixin(value = DebugHelpers.class, remap = false)
public abstract class BetterEndDebugHelpersMixin {
    @Inject(method = "generateDebugItems", at = @At("HEAD"), cancellable = true, remap = false)
    private static void dtbetterend$skipLateDebugItems(final CallbackInfo callback) {
        callback.cancel();
    }
}
