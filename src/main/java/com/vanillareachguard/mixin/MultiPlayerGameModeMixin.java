package com.vanillareachguard.mixin;

import com.vanillareachguard.CombatGuard;
import com.vanillareachguard.ReachGuard;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MultiPlayerGameMode.class)
public abstract class MultiPlayerGameModeMixin {
    @Inject(method = "startDestroyBlock", at = @At("HEAD"), cancellable = true)
    private void vanillareachguard$blockFarStartDestroy(BlockPos pos, Direction direction, CallbackInfoReturnable<Boolean> cir) {
        if (!ReachGuard.isBlockInteractionAllowed(pos)) {
            ReachGuard.warnCheating("Block Reach");
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "continueDestroyBlock", at = @At("HEAD"), cancellable = true)
    private void vanillareachguard$blockFarContinueDestroy(BlockPos pos, Direction direction, CallbackInfoReturnable<Boolean> cir) {
        if (!ReachGuard.isBlockInteractionAllowed(pos)) {
            ReachGuard.warnCheating("Block Reach");
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "destroyBlock", at = @At("HEAD"), cancellable = true)
    private void vanillareachguard$blockFarDestroy(BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        if (!ReachGuard.isBlockInteractionAllowed(pos)) {
            ReachGuard.warnCheating("Block Reach");
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "useItemOn", at = @At("HEAD"), cancellable = true)
    private void vanillareachguard$blockFarUseItemOn(LocalPlayer player, InteractionHand hand, BlockHitResult hitResult, CallbackInfoReturnable<InteractionResult> cir) {
        if (!ReachGuard.isBlockInteractionAllowed(hitResult.getLocation())) {
            ReachGuard.warnCheating("Block Reach");
            cir.setReturnValue(InteractionResult.FAIL);
        }
    }

    @Inject(method = "attack", at = @At("HEAD"), cancellable = true)
    private void vanillareachguard$blockFarAttack(Player player, Entity target, CallbackInfo ci) {
        if (!CombatGuard.isAttackAllowed(target)) {
            ci.cancel();
        }
    }

    @Inject(method = "interact", at = @At("HEAD"), cancellable = true)
    private void vanillareachguard$blockFarInteract(Player player, Entity target, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
        if (!CombatGuard.isEntityActionAllowed(target)) {
            cir.setReturnValue(InteractionResult.FAIL);
        }
    }

    @Inject(method = "interactAt", at = @At("HEAD"), cancellable = true)
    private void vanillareachguard$blockFarInteractAt(Player player, Entity target, EntityHitResult hitResult, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
        if (!CombatGuard.isEntityActionAllowed(target)) {
            cir.setReturnValue(InteractionResult.FAIL);
        }
    }
}
