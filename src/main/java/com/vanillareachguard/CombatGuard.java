package com.raffe.vanillareachguard;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public final class CombatGuard {
    private static final long MIN_ATTACK_INTERVAL_MILLIS = 85L;
    private static final float MIN_SURVIVAL_ATTACK_STRENGTH = 0.85F;
    private static long lastAcceptedAttackMillis;

    private CombatGuard() {
    }

    public static boolean isEntityActionAllowed(Entity target) {
        String violation = getEntityActionViolation(target);
        if (violation != null) {
            ReachGuard.warnCheating(violation);
            return false;
        }

        return true;
    }

    public static boolean isAttackAllowed(Entity target) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            return true;
        }

        if (!isEntityActionAllowed(target)) {
            return false;
        }

        if (CheatMonitor.shouldBlockAttack(target)) {
            return false;
        }

        if (!player.getAbilities().instabuild && player.getAttackStrengthScale(0.5F) < MIN_SURVIVAL_ATTACK_STRENGTH) {
            ReachGuard.warnCheating("Autoclicker / Macro");
            return false;
        }

        long now = System.currentTimeMillis();
        if (now - lastAcceptedAttackMillis < MIN_ATTACK_INTERVAL_MILLIS) {
            ReachGuard.warnCheating("Autoclicker / Macro");
            return false;
        }

        lastAcceptedAttackMillis = now;
        return true;
    }

    private static String getEntityActionViolation(Entity target) {
        if (!ReachGuard.isEntityInteractionAllowed(target)) {
            return "Entity Reach";
        }

        if (!isAimingAtTarget(target)) {
            return "Aim Assist / KillAura";
        }

        if (!isNotBlockedByWall(target)) {
            return "Wall Hit";
        }

        return null;
    }

    private static boolean isAimingAtTarget(Entity target) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null || target == null) {
            return true;
        }

        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getViewVector(1.0F);
        double reach = ReachGuard.ENTITY_REACH + 0.1D;
        Vec3 end = eye.add(look.x * reach, look.y * reach, look.z * reach);
        AABB aimBox = target.getBoundingBox().inflate(0.25D);

        return aimBox.clip(eye, end).isPresent();
    }

    private static boolean isNotBlockedByWall(Entity target) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null || target == null) {
            return true;
        }

        Vec3 eye = player.getEyePosition();
        Vec3 targetPoint = target.getBoundingBox().getCenter();
        double targetDistanceSqr = eye.distanceToSqr(targetPoint);
        HitResult blockHit = player.pick(Math.sqrt(targetDistanceSqr), 1.0F, false);

        return blockHit.getType() == HitResult.Type.MISS
                || blockHit.getLocation().distanceToSqr(eye) + 1.0E-4D >= targetDistanceSqr;
    }
}
