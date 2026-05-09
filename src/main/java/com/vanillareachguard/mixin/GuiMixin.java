package com.raffe.vanillareachguard.mixin;

import com.raffe.vanillareachguard.CheatWarningOverlay;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Gui.class)
public abstract class GuiMixin {
    @Inject(method = "render", at = @At("TAIL"))
    private void vanillareachguard$renderCheatWarning(GuiGraphics graphics, DeltaTracker deltaTracker, CallbackInfo ci) {
        CheatWarningOverlay.render(graphics);
    }
}
