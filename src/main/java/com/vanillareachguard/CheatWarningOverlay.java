package com.raffe.vanillareachguard;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

public final class CheatWarningOverlay {
    private static final Component MESSAGE = Component.literal("YOU ARE CHEATING");

    private CheatWarningOverlay() {
    }

    public static void render(GuiGraphics graphics) {
        ReachGuard.clearExpiredWarningReasons();

        if (!ReachGuard.shouldRenderCheatWarning()) {
            return;
        }

        int width = graphics.guiWidth();
        int height = graphics.guiHeight();
        graphics.fill(0, 0, width, height, 0x88FF0000);

        Minecraft minecraft = Minecraft.getInstance();
        int centerX = width / 2;
        int centerY = height / 2;

        graphics.drawCenteredString(minecraft.font, MESSAGE, centerX + 2, centerY - 18 + 2, 0xAA000000);
        graphics.drawCenteredString(minecraft.font, MESSAGE, centerX, centerY - 18, 0xFFFFFFFF);
        graphics.drawCenteredString(
                minecraft.font,
                Component.literal(ReachGuard.getWarningReasons()),
                centerX,
                centerY,
                0xFFFFF200
        );
    }
}
