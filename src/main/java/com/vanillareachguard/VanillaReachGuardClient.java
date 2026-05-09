package com.vanillareachguard;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class VanillaReachGuardClient implements ClientModInitializer {
    public static final String MOD_ID = "vanillareachguard";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitializeClient() {
        KnownHackClientBlocker.logKnownHackClientsForTesting();
        ClientTickEvents.END_CLIENT_TICK.register(CheatMonitor::onClientTick);
        LOGGER.info("Vanilla Reach Guard 1.0.3 is active. Client interactions and PvP behavior are being monitored.");
    }
}
