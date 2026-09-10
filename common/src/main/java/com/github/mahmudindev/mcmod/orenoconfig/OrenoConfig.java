package com.github.mahmudindev.mcmod.orenoconfig;

import com.github.mahmudindev.mcmod.orenoconfig.network.ConfigNetwork;

public final class OrenoConfig {
    public static final String MOD_ID = "orenoconfig";

    public static void init() {
        ConfigNetwork.init();
    }
}
