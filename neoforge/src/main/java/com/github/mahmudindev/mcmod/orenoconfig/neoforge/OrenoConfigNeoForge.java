package com.github.mahmudindev.mcmod.orenoconfig.neoforge;

import com.github.mahmudindev.mcmod.orenoconfig.OrenoConfig;
import com.github.mahmudindev.mcmod.orenoconfig.neoforge.client.OrenoConfigNeoForgeClient;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;

@Mod(OrenoConfig.MOD_ID)
public final class OrenoConfigNeoForge {
    public OrenoConfigNeoForge() {
        // Run our common setup.
        OrenoConfig.init();

        if (FMLEnvironment.dist == Dist.CLIENT) {
            new OrenoConfigNeoForgeClient();
        }
    }
}
