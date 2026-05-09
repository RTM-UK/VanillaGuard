package com.raffe.vanillareachguard.mixin;

import com.raffe.vanillareachguard.CombatGuard;
import com.raffe.vanillareachguard.ReachGuard;
import net.minecraft.client.multiplayer.ClientCommonPacketListenerImpl;
import net.minecraft.client.Minecraft;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.network.protocol.game.ServerboundInteractPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemOnPacket;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientCommonPacketListenerImpl.class)
public abstract class ClientPacketListenerMixin {
    @Inject(method = "send", at = @At("HEAD"), cancellable = true)
    private void vanillareachguard$blockSuspiciousReachPackets(Packet<?> packet, CallbackInfo ci) {
        Entity target = vanillareachguard$getEntityTarget(packet);
        if (target != null && !CombatGuard.isEntityActionAllowed(target)) {
            ci.cancel();
            return;
        }

        Vec3 hitLocation = vanillareachguard$getBlockHitLocation(packet);
        if (hitLocation != null && !ReachGuard.isBlockInteractionAllowed(hitLocation)) {
            ReachGuard.warnCheating("Block Reach");
            ci.cancel();
        }
    }

    private static Vec3 vanillareachguard$getBlockHitLocation(Packet<?> packet) {
        if (packet instanceof ServerboundUseItemOnPacket useItemOnPacket) {
            return useItemOnPacket.getHitResult().getLocation();
        }

        if (packet instanceof ServerboundPlayerActionPacket playerActionPacket) {
            return Vec3.atCenterOf(playerActionPacket.getPos());
        }

        return null;
    }

    private static Entity vanillareachguard$getEntityTarget(Packet<?> packet) {
        if (!(packet instanceof ServerboundInteractPacket interactPacket)) {
            return null;
        }

        if (Minecraft.getInstance().level == null) {
            return null;
        }

        int entityId = ((ServerboundInteractPacketAccessor) interactPacket).vanillareachguard$getEntityId();
        return Minecraft.getInstance().level.getEntity(entityId);
    }
}
