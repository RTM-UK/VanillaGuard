package com.vanillareachguard.mixin;

import com.vanillareachguard.ReachGuard;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Player.class)
public abstract class PlayerMixin {
    @Inject(method = "blockInteractionRange", at = @At("RETURN"), cancellable = true)
    private void vanillareachguard$clampBlockInteractionRange(CallbackInfoReturnable<Double> cir) {
        cir.setReturnValue(ReachGuard.clampBlockReach(cir.getReturnValue()));
    }

    @Inject(method = "entityInteractionRange", at = @At("RETURN"), cancellable = true)
    private void vanillareachguard$clampEntityInteractionRange(CallbackInfoReturnable<Double> cir) {
        cir.setReturnValue(ReachGuard.clampEntityReach(cir.getReturnValue()));
    }
}
