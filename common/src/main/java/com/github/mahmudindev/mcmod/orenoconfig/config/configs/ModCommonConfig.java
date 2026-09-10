package com.github.mahmudindev.mcmod.orenoconfig.config.configs;

import com.github.mahmudindev.mcmod.orenoconfig.config.ConfigNode;
import com.github.mahmudindev.mcmod.orenoconfig.config.network.ModCommonConfigPacket;

import java.util.HashMap;
import java.util.Map;

public class ModCommonConfig extends ModConfig {
    private static final Map<String, ModCommonConfig> SERVER_VALUES = new HashMap<>();

    public ModCommonConfig(String id, String name) {
        super(id, name);
    }

    public ModCommonConfig(ModCommonConfig modCommonConfig) {
        this(modCommonConfig, false);
    }

    public ModCommonConfig(ModCommonConfig modCommonConfig, boolean onlyDefault) {
        super(modCommonConfig, onlyDefault);
    }

    @Override
    protected ConfigNode getRNode() {
        ModCommonConfig serverValue = SERVER_VALUES.get(this.getName());
        if (serverValue != null) {
            return serverValue.getRNode();
        }

        return super.getRNode();
    }

    @Override
    protected String getNameSuffix() {
        return "-common";
    }

    public void registerServerSync() {
        String name = this.getName();
        ModCommonConfigPacket.registerHandler(name, new ModCommonConfigPacket.Handler() {
            @Override
            public ConfigNode getGetterNode() {
                return ModCommonConfig.this.getRNode();
            }

            @Override
            public ConfigNode getSynchronizedGetterNode() {
                if (SERVER_VALUES.containsKey(name)) {
                    return SERVER_VALUES.get(name).getRNode();
                }

                ModCommonConfig config = new ModCommonConfig(ModCommonConfig.this, true);
                SERVER_VALUES.put(name, config);
                return config.getRNode();
            }
        });
    }

    public static void clearServerSync() {
        SERVER_VALUES.clear();
    }
}
