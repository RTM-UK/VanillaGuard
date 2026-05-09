package com.vanillareachguard.mixin;

import com.vanillareachguard.ReachGuard;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(Entity.class)
public abstract class EntityMixin {
    @ModifyVariable(method = "pick", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private double vanillareachguard$clampBlockRaycastDistance(double requestedDistance) {
        return ReachGuard.clampBlockReach(requestedDistance);
    }
}
