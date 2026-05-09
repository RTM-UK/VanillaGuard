package com.vanillareachguard;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;
import java.util.StringJoiner;

public final class CheatMonitor {
    private static final float SNAP_ROTATION_DEGREES = 12.0F;
    private static final float HARD_SNAP_ROTATION_DEGREES = 28.0F;
    private static final float PRECISE_AIM_ERROR_DEGREES = 2.0F;
    private static final float PERFECT_AIM_ERROR_DEGREES = 0.7F;
    private static final long INSTANT_REACTION_MILLIS = 60L;
    private static final long SUSPICION_DECAY_MILLIS = 1000L;
    private static final int BLOCK_SUSPICION_SCORE = 14;
    private static final int HARD_BLOCK_SUSPICION_SCORE = 18;
    private static final int TIMING_WINDOW_SIZE = 8;
    private static final int CATEGORY_AIM = 1;
    private static final int CATEGORY_TIMING = 1 << 1;
    private static final int CATEGORY_REACTION = 1 << 2;
    private static final int CATEGORY_COOLDOWN = 1 << 3;

    private static float previousYaw;
    private static float previousPitch;
    private static float lastRotationDelta;
    private static int crosshairEntityId = -1;
    private static int ticksOnCrosshairEntity;
    private static long lastAttackMillis;
    private static long previousAttackIntervalMillis;
    private static int repeatedIntervalCount;
    private static int perfectSwingCount;
    private static int precisionHitCount;
    private static int repeatedHitSpotCount;
    private static int centerMassHitCount;
    private static int instantReactionCount;
    private static int targetAcquisitionEntityId = -1;
    private static long targetAcquiredMillis;
    private static final long[] attackIntervals = new long[TIMING_WINDOW_SIZE];
    private static int attackIntervalIndex;
    private static int attackIntervalCount;
    private static HitLanding previousHitLanding;
    private static int suspicionScore;
    private static int suspicionCategories;
    private static int observedPlayerAttacks;
    private static final Set<String> suspicionLabels = new LinkedHashSet<>();
    private static long lastSuspicionMillis;
    private static boolean initialized;

    private CheatMonitor() {
    }

    public static void onClientTick(Minecraft minecraft) {
        LocalPlayer player = minecraft.player;
        if (player == null) {
            reset();
            return;
        }

        float yaw = player.getYRot();
        float pitch = player.getXRot();

        if (!initialized) {
            previousYaw = yaw;
            previousPitch = pitch;
            initialized = true;
        }

        float yawDelta = Math.abs(Mth.wrapDegrees(yaw - previousYaw));
        float pitchDelta = Math.abs(pitch - previousPitch);
        lastRotationDelta = yawDelta + pitchDelta;
        previousYaw = yaw;
        previousPitch = pitch;

        int lookedAtEntityId = getCrosshairEntityId(minecraft.hitResult);
        if (lookedAtEntityId == crosshairEntityId && lookedAtEntityId != -1) {
            ticksOnCrosshairEntity++;
        } else {
            crosshairEntityId = lookedAtEntityId;
            ticksOnCrosshairEntity = lookedAtEntityId == -1 ? 0 : 1;
            targetAcquisitionEntityId = lookedAtEntityId;
            targetAcquiredMillis = lookedAtEntityId == -1 ? 0L : System.currentTimeMillis();
        }

        decaySuspicion();
    }

    public static boolean shouldBlockAttack(Entity target) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null || target == null) {
            return false;
        }

        if (!(target instanceof Player)) {
            return false;
        }

        observedPlayerAttacks++;

        float aimError = getAimErrorDegrees(player, target);
        float attackStrength = player.getAttackStrengthScale(0.5F);
        long now = System.currentTimeMillis();
        long interval = lastAttackMillis == 0L ? 0L : now - lastAttackMillis;
        HitLanding landing = getHitLanding(player, target);

        if (lastRotationDelta >= HARD_SNAP_ROTATION_DEGREES && aimError <= PERFECT_AIM_ERROR_DEGREES) {
            addSuspicion(3, "Aim Assist", CATEGORY_AIM);
        }

        if (aimError <= PERFECT_AIM_ERROR_DEGREES) {
            precisionHitCount++;
            if (precisionHitCount >= 10) {
                addSuspicion(1, "Aim Assist", CATEGORY_AIM);
            }
        } else {
            precisionHitCount = 0;
        }

        if (!player.getAbilities().instabuild && attackStrength >= 0.98F) {
            perfectSwingCount++;
        } else if (attackStrength < 0.75F) {
            perfectSwingCount = 0;
        }

        if (crosshairEntityId == target.getId()
                && ticksOnCrosshairEntity <= 2
                && attackStrength >= 0.90F
                && lastRotationDelta >= 8.0F) {
            addSuspicion(1, "Triggerbot", CATEGORY_REACTION);
        }

        if (interval > 0L) {
            recordAttackInterval(interval);

            long intervalDifference = Math.abs(interval - previousAttackIntervalMillis);
            if (previousAttackIntervalMillis > 0L && intervalDifference <= 6L && interval >= 85L && interval <= 900L) {
                repeatedIntervalCount++;
                if (repeatedIntervalCount >= 7) {
                    addSuspicion(2, "Autoclicker", CATEGORY_TIMING);
                }
            } else {
                repeatedIntervalCount = 0;
            }

            previousAttackIntervalMillis = interval;

            if (attackIntervalCount >= 8 && getAttackIntervalStandardDeviation() <= 3.5D) {
                addSuspicion(2, "Autoclicker", CATEGORY_TIMING);
                if (perfectSwingCount >= 8) {
                    addSuspicion(1, "Perfect Swing Macro", CATEGORY_COOLDOWN);
                }
            }
        }

        if (target.getId() == targetAcquisitionEntityId && targetAcquiredMillis > 0L) {
            long reactionMillis = now - targetAcquiredMillis;
            if (reactionMillis > 0L && reactionMillis <= INSTANT_REACTION_MILLIS) {
                instantReactionCount++;
                if (instantReactionCount >= 5 && lastRotationDelta >= 8.0F) {
                    addSuspicion(2, "Triggerbot", CATEGORY_REACTION);
                }
            } else if (reactionMillis > 250L) {
                instantReactionCount = 0;
            }
        }

        if (landing.valid()) {
            if (landing.centerDistance() <= 0.055D && aimError <= PRECISE_AIM_ERROR_DEGREES) {
                centerMassHitCount++;
                if (centerMassHitCount >= 10) {
                    addSuspicion(1, "Aim Assist", CATEGORY_AIM);
                }
            } else if (landing.centerDistance() >= 0.20D) {
                centerMassHitCount = 0;
            }

            if (previousHitLanding != null && landing.distanceTo(previousHitLanding) <= 0.045D) {
                repeatedHitSpotCount++;
                if (repeatedHitSpotCount >= 8) {
                    addSuspicion(2, "Aim Assist", CATEGORY_AIM);
                }
            } else {
                repeatedHitSpotCount = 0;
            }

            previousHitLanding = landing;
        }

        lastAttackMillis = now;

        boolean shouldBlock = shouldBlockForCurrentEvidence();
        if (shouldBlock) {
            ReachGuard.warnCheating(getSuspicionLabel());
        }

        return shouldBlock;
    }

    public static boolean isSuspicious() {
        decaySuspicion();
        return shouldBlockForCurrentEvidence();
    }

    private static void addSuspicion(int amount, String label, int category) {
        suspicionScore = Math.min(HARD_BLOCK_SUSPICION_SCORE, suspicionScore + amount);
        suspicionCategories |= category;
        suspicionLabels.add(label);
        lastSuspicionMillis = System.currentTimeMillis();

        if (shouldBlockForCurrentEvidence()) {
            VanillaReachGuardClient.LOGGER.warn(
                    "PvP cheat pattern detected: {} (score={}, categories={})",
                    getSuspicionLabel(),
                    suspicionScore,
                    Integer.bitCount(suspicionCategories)
            );
        }
    }

    private static boolean shouldBlockForCurrentEvidence() {
        return suspicionScore >= HARD_BLOCK_SUSPICION_SCORE
                || (observedPlayerAttacks >= 8
                && suspicionScore >= BLOCK_SUSPICION_SCORE
                && Integer.bitCount(suspicionCategories) >= 2);
    }

    private static String getSuspicionLabel() {
        if (suspicionLabels.isEmpty()) {
            return "Suspicious PvP Pattern";
        }

        StringJoiner joiner = new StringJoiner(" / ");
        for (String label : suspicionLabels) {
            joiner.add(label);
        }
        return joiner.toString();
    }

    private static void decaySuspicion() {
        if (suspicionScore == 0) {
            return;
        }

        long now = System.currentTimeMillis();
        long decays = (now - lastSuspicionMillis) / SUSPICION_DECAY_MILLIS;
        if (decays <= 0L) {
            return;
        }

        suspicionScore = Math.max(0, suspicionScore - (int) decays);
        if (suspicionScore == 0) {
            suspicionCategories = 0;
            suspicionLabels.clear();
        }
        lastSuspicionMillis = now;
    }

    private static int getCrosshairEntityId(HitResult hitResult) {
        if (hitResult instanceof EntityHitResult entityHitResult) {
            return entityHitResult.getEntity().getId();
        }

        return -1;
    }

    private static float getAimErrorDegrees(LocalPlayer player, Entity target) {
        Vec3 eye = player.getEyePosition();
        Vec3 targetPoint = target.getBoundingBox().getCenter();
        double dx = targetPoint.x - eye.x;
        double dy = targetPoint.y - eye.y;
        double dz = targetPoint.z - eye.z;
        double horizontalDistance = Math.sqrt(dx * dx + dz * dz);

        float targetYaw = (float) (Mth.atan2(dz, dx) * Mth.RAD_TO_DEG) - 90.0F;
        float targetPitch = (float) (-(Mth.atan2(dy, horizontalDistance) * Mth.RAD_TO_DEG));

        float yawError = Math.abs(Mth.wrapDegrees(player.getYRot() - targetYaw));
        float pitchError = Math.abs(Mth.wrapDegrees(player.getXRot() - targetPitch));
        return yawError + pitchError;
    }

    private static HitLanding getHitLanding(LocalPlayer player, Entity target) {
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getViewVector(1.0F);
        Vec3 end = eye.add(look.x * (ReachGuard.ENTITY_REACH + 0.35D),
                look.y * (ReachGuard.ENTITY_REACH + 0.35D),
                look.z * (ReachGuard.ENTITY_REACH + 0.35D));

        AABB box = target.getBoundingBox().inflate(0.12D);
        Optional<Vec3> clipped = box.clip(eye, end);
        if (clipped.isEmpty()) {
            return HitLanding.invalid();
        }

        Vec3 hit = clipped.get();
        double x = normalize(hit.x, box.minX, box.maxX);
        double y = normalize(hit.y, box.minY, box.maxY);
        double z = normalize(hit.z, box.minZ, box.maxZ);
        return new HitLanding(x, y, z);
    }

    private static double normalize(double value, double min, double max) {
        double size = max - min;
        if (size <= 1.0E-6D) {
            return 0.5D;
        }

        return Mth.clamp((value - min) / size, 0.0D, 1.0D);
    }

    private static void recordAttackInterval(long interval) {
        attackIntervals[attackIntervalIndex] = interval;
        attackIntervalIndex = (attackIntervalIndex + 1) % attackIntervals.length;
        attackIntervalCount = Math.min(attackIntervalCount + 1, attackIntervals.length);
    }

    private static double getAttackIntervalStandardDeviation() {
        if (attackIntervalCount == 0) {
            return Double.MAX_VALUE;
        }

        double total = 0.0D;
        for (int i = 0; i < attackIntervalCount; i++) {
            total += attackIntervals[i];
        }

        double average = total / attackIntervalCount;
        double variance = 0.0D;
        for (int i = 0; i < attackIntervalCount; i++) {
            double delta = attackIntervals[i] - average;
            variance += delta * delta;
        }

        return Math.sqrt(variance / attackIntervalCount);
    }

    private static void reset() {
        initialized = false;
        crosshairEntityId = -1;
        ticksOnCrosshairEntity = 0;
        lastAttackMillis = 0L;
        previousAttackIntervalMillis = 0L;
        repeatedIntervalCount = 0;
        perfectSwingCount = 0;
        precisionHitCount = 0;
        repeatedHitSpotCount = 0;
        centerMassHitCount = 0;
        instantReactionCount = 0;
        observedPlayerAttacks = 0;
        targetAcquisitionEntityId = -1;
        targetAcquiredMillis = 0L;
        attackIntervalIndex = 0;
        attackIntervalCount = 0;
        previousHitLanding = null;
        suspicionScore = 0;
        suspicionCategories = 0;
        suspicionLabels.clear();
    }

    private record HitLanding(double x, double y, double z, boolean valid) {
        static HitLanding invalid() {
            return new HitLanding(0.0D, 0.0D, 0.0D, false);
        }

        HitLanding(double x, double y, double z) {
            this(x, y, z, true);
        }

        double centerDistance() {
            double dx = x - 0.5D;
            double dy = y - 0.5D;
            double dz = z - 0.5D;
            return Math.sqrt(dx * dx + dy * dy + dz * dz);
        }

        double distanceTo(HitLanding other) {
            if (!valid || !other.valid) {
                return Double.MAX_VALUE;
            }

            double dx = x - other.x;
            double dy = y - other.y;
            double dz = z - other.z;
            return Math.sqrt(dx * dx + dy * dy + dz * dz);
        }
    }
}
