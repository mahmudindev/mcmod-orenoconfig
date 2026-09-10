package com.github.mahmudindev.mcmod.orenoconfig.config.configs;

import com.github.mahmudindev.mcmod.orenocommons.platform.EnvSide;
import com.github.mahmudindev.mcmod.orenocommons.platform.UnifiedPlatform;

public class ModClientConfig extends ModConfig {
    public ModClientConfig(String id, String name) {
        super(id, name);
    }

    public ModClientConfig(ModClientConfig modClientConfig) {
        this(modClientConfig, false);
    }

    public ModClientConfig(ModClientConfig modClientConfig, boolean onlyDefault) {
        super(modClientConfig, onlyDefault);
    }

    @Override
    protected String getNameSuffix() {
        return "-client";
    }

    @Override
    public void load() {
        EnvSide envSide = UnifiedPlatform.getEnvSide();
        if (envSide == EnvSide.DEDICATED_SERVER) {
            return;
        }

        super.load();
    }

    @Override
    public void save() {
        EnvSide envSide = UnifiedPlatform.getEnvSide();
        if (envSide == EnvSide.DEDICATED_SERVER) {
            return;
        }

        super.save();
    }
}
