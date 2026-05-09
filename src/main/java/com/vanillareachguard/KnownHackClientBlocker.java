package com.raffe.vanillareachguard;

import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;

import java.util.LinkedHashMap;
import java.util.Map;

public final class KnownHackClientBlocker {
    private static final Map<String, String> DENIED_MODS = Map.ofEntries(
            Map.entry("meteor-client", "Meteor Client"),
            Map.entry("wurst", "Wurst Client"),
            Map.entry("impact", "Impact Client"),
            Map.entry("aristois", "Aristois Client"),
            Map.entry("bleachhack", "BleachHack"),
            Map.entry("inertia", "Inertia Client"),
            Map.entry("kami", "KAMI"),
            Map.entry("lambda", "Lambda Client"),
            Map.entry("mathax", "MatHax Client"),
            Map.entry("thunderhack", "ThunderHack")
    );

    private KnownHackClientBlocker() {
    }

    public static void logKnownHackClientsForTesting() {
        Map<String, String> detected = new LinkedHashMap<>();

        for (Map.Entry<String, String> deniedMod : DENIED_MODS.entrySet()) {
            FabricLoader.getInstance().getModContainer(deniedMod.getKey())
                    .map(ModContainer::getMetadata)
                    .ifPresent(metadata -> detected.put(deniedMod.getKey(), metadata.getName()));
        }

        if (!detected.isEmpty()) {
            VanillaReachGuardClient.LOGGER.warn(
                    "Known hack-client mods are loaded, but startup blocking is disabled for testing: {}",
                    detected
            );
        }
    }
}
