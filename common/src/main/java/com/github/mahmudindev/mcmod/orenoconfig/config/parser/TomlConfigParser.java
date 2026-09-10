package com.github.mahmudindev.mcmod.orenoconfig.config.parser;

import com.electronwill.nightconfig.core.CommentedConfig;
import com.electronwill.nightconfig.core.conversion.ObjectConverter;
import com.electronwill.nightconfig.toml.TomlParser;
import com.electronwill.nightconfig.toml.TomlWriter;
import com.github.mahmudindev.mcmod.orenoconfig.config.ConfigNode;

import java.io.Reader;
import java.io.Writer;

public class TomlConfigParser implements ConfigParser {
    private static final TomlParser PARSER = new TomlParser();
    private static final TomlWriter WRITER = new TomlWriter();

    @Override
    public void serialize(ConfigNode node, Writer writer) {
        CommentedConfig toml = CommentedConfig.inMemory();

        this.serializeNode(node, toml);

        WRITER.write(toml, writer);
    }

    private void serializeNode(ConfigNode node, CommentedConfig toml) {
        for (var entry : node.getChildren().entrySet()) {
            String key = entry.getKey();
            ConfigNode child = entry.getValue();

            if (child.isLeaf()) {
                toml.set(key, child.getValue());
                continue;
            }

            CommentedConfig subToml = toml.createSubConfig();
            this.serializeNode(child, subToml);
            toml.set(key, subToml);
        }
    }

    @Override
    public void deserialize(Reader reader, ConfigNode node) {
        this.deserializeNode(node, PARSER.parse(reader));
    }

    private void deserializeNode(ConfigNode node, CommentedConfig toml) {
        for (CommentedConfig.Entry entry : toml.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();

            ConfigNode child = node.getOrCreatePath(new String[]{key}, 0);

            if (child.isLeaf()) {
                Class<?> childValueType = child.getValueType();

                if (childValueType == null) {
                    Object childValue = child.getValue();
                    if (childValue != null) {
                        childValueType = childValue.getClass();
                    }
                }

                if (childValueType != null && value instanceof CommentedConfig) {
                    Class<?> objectType = childValueType;
                    child.setValue((new ObjectConverter()).toObject(
                            toml.get(key),
                            () -> {
                                try {
                                    return objectType.getDeclaredConstructor().newInstance();
                                } catch (Exception e) {
                                    throw new RuntimeException(e);
                                }
                            }
                    ));
                    continue;
                }

                child.setValue(value);
                continue;
            }

            if (value instanceof CommentedConfig subToml) {
                this.deserializeNode(child, subToml);
            }
        }
    }

    @Override
    public String getFileExtension() {
        return "toml";
    }
}
