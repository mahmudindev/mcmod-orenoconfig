package com.github.mahmudindev.mcmod.orenoconfig.config.parser;

import com.github.mahmudindev.mcmod.orenoconfig.config.ConfigNode;

import java.io.Reader;
import java.io.Writer;

public interface ConfigParser {
    void serialize(ConfigNode node, Writer writer);

    void deserialize(Reader reader, ConfigNode node);

    default String getFileExtension() {
        return null;
    }
}
