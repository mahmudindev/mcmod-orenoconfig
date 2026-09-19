package com.github.mahmudindev.mcmod.orenoconfig.config.parser;

import com.electronwill.nightconfig.core.CommentedConfig;
import com.electronwill.nightconfig.toml.TomlParser;
import com.electronwill.nightconfig.toml.TomlWriter;
import com.github.mahmudindev.mcmod.orenoconfig.config.ConfigNode;
import com.google.gson.*;

import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class TomlConfigParser implements ConfigParser {
    private final TomlParser parser = new TomlParser();
    private final TomlWriter writer = new TomlWriter();
    private final Gson gsonIntermediary = this.getGsonIntermediary();

    @Override
    public void serialize(ConfigNode node, Writer writer) {
        CommentedConfig toml = CommentedConfig.inMemory();

        this.serializeNode(node, toml);

        this.writer.write(toml, writer);
    }

    private void serializeNode(ConfigNode node, CommentedConfig toml) {
        for (var entry : node.getChildren().entrySet()) {
            String key = entry.getKey();
            ConfigNode child = entry.getValue();

            if (child.isLeaf()) {
                JsonElement gson = this.gsonIntermediary.toJsonTree(child.getValue());
                toml.set(key, this.serializeLeafNode(gson));
                continue;
            }

            CommentedConfig subToml = toml.createSubConfig();
            this.serializeNode(child, subToml);
            toml.set(key, subToml);
        }
    }

    private Object serializeLeafNode(JsonElement gson) {
        if (gson.isJsonObject()) {
            CommentedConfig subToml = CommentedConfig.inMemory();

            JsonObject gsonObject = gson.getAsJsonObject();

            for (Map.Entry<String, JsonElement> entry : gsonObject.entrySet()) {
                if (entry.getValue().isJsonObject()) {
                    continue;
                }
                subToml.set(entry.getKey(), this.serializeLeafNode(entry.getValue()));
            }

            for (Map.Entry<String, JsonElement> entry : gsonObject.entrySet()) {
                if (!entry.getValue().isJsonObject()) {
                    continue;
                }
                subToml.set(entry.getKey(), this.serializeLeafNode(entry.getValue()));
            }

            return subToml;
        } else if (gson.isJsonArray()) {
            List<Object> list = new ArrayList<>();

            for (JsonElement item : gson.getAsJsonArray()) {
                list.add(this.serializeLeafNode(item));
            }

            return list;
        } else if (gson.isJsonPrimitive()) {
            JsonPrimitive gsonPrimitive = gson.getAsJsonPrimitive();
            if (gsonPrimitive.isBoolean()) {
                return gsonPrimitive.getAsBoolean();
            } else if (gsonPrimitive.isNumber()) {
                return gsonPrimitive.getAsNumber();
            } else if (gsonPrimitive.isString()) {
                return gsonPrimitive.getAsString();
            }
        }

        return null;
    }

    @Override
    public void deserialize(Reader reader, ConfigNode node) {
        this.deserializeNode(node, this.parser.parse(reader));
    }

    private void deserializeNode(ConfigNode node, CommentedConfig toml) {
        for (CommentedConfig.Entry entry : toml.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();

            ConfigNode child = node.getOrCreatePath(new String[]{key}, 0);

            if (child.isLeaf()) {
                Type childValueType = child.getValueType();

                if (childValueType == null) {
                    childValueType = Object.class;

                    Object childValue = child.getValue();
                    if (childValue != null) {
                        childValueType = childValue.getClass();
                    }
                }

                child.setValue(this.gsonIntermediary.fromJson(
                        this.deserializeLeafNode(value),
                        childValueType
                ));
                continue;
            }

            if (value instanceof CommentedConfig subToml) {
                this.deserializeNode(child, subToml);
            }
        }
    }

    private JsonElement deserializeLeafNode(Object value) {
        if  (value instanceof CommentedConfig subToml) {
            JsonObject gsonObject = new JsonObject();

            for (CommentedConfig.Entry entry : subToml.entrySet()) {
                gsonObject.add(entry.getKey(), this.deserializeLeafNode(entry.getValue()));
            }

            return gsonObject;
        } else if (value instanceof List) {
            JsonArray gsonArray = new JsonArray();

            for (Object item : (List<?>) value) {
                gsonArray.add(this.deserializeLeafNode(item));
            }

            return gsonArray;
        } else if (value instanceof Boolean) {
            return new JsonPrimitive((Boolean) value);
        } else if (value instanceof Number) {
            return new JsonPrimitive((Number) value);
        } else if (value instanceof String) {
            return new JsonPrimitive((String) value);
        } else if (value == null) {
            return JsonNull.INSTANCE;
        }

        return this.gsonIntermediary.toJsonTree(value);
    }

    private Gson getGsonIntermediary() {
        return new GsonBuilder()
                .setObjectToNumberStrategy(ToNumberPolicy.LONG_OR_DOUBLE)
                .create();
    }

    @Override
    public String getFileExtension() {
        return "toml";
    }
}
