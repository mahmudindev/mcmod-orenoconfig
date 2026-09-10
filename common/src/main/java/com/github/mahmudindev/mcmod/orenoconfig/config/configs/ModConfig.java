package com.github.mahmudindev.mcmod.orenoconfig.config.configs;

import com.github.mahmudindev.mcmod.orenocommons.platform.UnifiedPlatform;
import com.github.mahmudindev.mcmod.orenoconfig.config.parser.TomlConfigParser;

import java.io.File;

public abstract class ModConfig extends FileConfig {
    protected ModConfig(String id, String name) {
        super(
                UnifiedPlatform.getConfigDir(),
                getPrefixedName(id, name),
                new TomlConfigParser()
        );

        if (!UnifiedPlatform.isModLoaded(id)) {
            throw new IllegalStateException("Mod with id " + id + " is not exist");
        }
    }

    protected ModConfig(ModConfig modConfig) {
        this(modConfig, false);
    }

    protected ModConfig(ModConfig modConfig, boolean onlyDefault) {
        super(modConfig, onlyDefault);
    }

    protected abstract String getNameSuffix();

    @Override
    public String getName() {
        return super.getName() + this.getNameSuffix();
    }

    private static String getPrefixedName(String id, String name) {
        if (name.startsWith(id)) {
            return name;
        }

        if (name.contains("/") || name.contains("\\")) {
            return id + File.separator + name;
        }

        return id + "-" + name;
    }
}
