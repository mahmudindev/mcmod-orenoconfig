package com.github.mahmudindev.mcmod.orenoconfig.fabric.client;

import com.github.mahmudindev.mcmod.orenoconfig.client.OrenoConfigClient;
import net.fabricmc.api.ClientModInitializer;

public final class OrenoConfigFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        // This entrypoint is suitable for setting up client-specific logic, such as rendering.

        OrenoConfigClient.init();
    }
}
