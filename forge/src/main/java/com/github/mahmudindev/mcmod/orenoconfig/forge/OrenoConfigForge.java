package com.github.mahmudindev.mcmod.orenoconfig.forge;

import com.github.mahmudindev.mcmod.orenoconfig.OrenoConfig;
import com.github.mahmudindev.mcmod.orenoconfig.forge.client.OrenoConfigForgeClient;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;

@Mod(OrenoConfig.MOD_ID)
public final class OrenoConfigForge {
    public OrenoConfigForge() {
        // Run our common setup.
        OrenoConfig.init();

        DistExecutor.safeRunWhenOn(Dist.CLIENT, () -> OrenoConfigForgeClient::new);
    }
}
