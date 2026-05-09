package com.raffe.vanillareachguard;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.StringJoiner;

public final class ReachGuard {
    public static final double SURVIVAL_BLOCK_REACH = 4.5D;
    public static final double CREATIVE_BLOCK_REACH = 5.0D;
    public static final double ENTITY_REACH = 3.0D;

    private static long lastWarningMillis;
    private static long cheatWarningUntilMillis;
    private static final Set<String> warningReasons = new LinkedHashSet<>();

    private ReachGuard() {
    }

    public static float clampPickRange(float requested) {
        return (float) Math.min(requested, vanillaBlockReach());
    }

    public static double clampBlockReach(double requested) {
        return Math.min(requested, vanillaBlockReach());
    }

    public static double clampEntityReach(double requested) {
        return Math.min(requested, ENTITY_REACH);
    }

    public static double vanillaBlockReach() {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player != null && player.getAbilities().instabuild) {
            return CREATIVE_BLOCK_REACH;
        }

        return SURVIVAL_BLOCK_REACH;
    }

    public static boolean isBlockInteractionAllowed(Vec3 hitLocation) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            return true;
        }

        double reach = vanillaBlockReach();
        return player.getEyePosition().distanceToSqr(hitLocation) <= reach * reach + 1.0E-6D;
    }

    public static boolean isBlockInteractionAllowed(BlockPos pos) {
        return isBlockInteractionAllowed(Vec3.atCenterOf(pos));
    }

    public static boolean isEntityInteractionAllowed(Entity target) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null || target == null) {
            return true;
        }

        double reach = ENTITY_REACH;
        return target.getBoundingBox().distanceToSqr(player.getEyePosition()) <= reach * reach + 1.0E-6D;
    }

    public static void warnBlockedReach() {
        warnCheating("Reach");
    }

    public static void warnCheating(String reason) {
        long now = System.currentTimeMillis();
        warningReasons.add(reason);
        cheatWarningUntilMillis = now + 2000L;

        LocalPlayer player = Minecraft.getInstance().player;
        if (player != null && now - lastWarningMillis >= 1500L) {
            lastWarningMillis = now;
            player.displayClientMessage(
                    Component.literal("YOU ARE CHEATING: " + getWarningReasons()),
                    true
            );
        }
    }

    public static boolean shouldRenderCheatWarning() {
        return System.currentTimeMillis() < cheatWarningUntilMillis;
    }

    public static String getWarningReasons() {
        if (warningReasons.isEmpty()) {
            return "Cheat detected";
        }

        StringJoiner joiner = new StringJoiner(", ");
        for (String reason : warningReasons) {
            joiner.add(reason);
        }
        return joiner.toString();
    }

    public static void clearExpiredWarningReasons() {
        if (!shouldRenderCheatWarning()) {
            warningReasons.clear();
        }
    }
}
