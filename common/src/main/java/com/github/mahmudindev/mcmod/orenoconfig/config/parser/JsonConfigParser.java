package com.github.mahmudindev.mcmod.orenoconfig.config.parser;

import com.github.mahmudindev.mcmod.orenoconfig.config.ConfigNode;
import com.google.gson.*;

import java.io.Reader;
import java.io.Writer;

public class JsonConfigParser implements ConfigParser {
    private static final Gson GSON = JsonConfigParser.getGson();

    @Override
    public void serialize(ConfigNode parent, Writer writer) {
        GSON.toJson(this.serializeNode(parent), writer);
    }

    private JsonObject serializeNode(ConfigNode node) {
        JsonObject json = new JsonObject();

        for (var entry : node.getChildren().entrySet()) {
            String key = entry.getKey();
            ConfigNode child = entry.getValue();

            if (child.isLeaf()) {
                json.add(key, GSON.toJsonTree(child.getValue()));
                continue;
            }

            json.add(key, this.serializeNode(child));
        }

        return json;
    }

    @Override
    public void deserialize(Reader reader, ConfigNode node) {
        this.deserializeNode(node, GSON.fromJson(reader, JsonObject.class));
    }

    private void deserializeNode(ConfigNode node, JsonObject json) {
        for (String key : json.keySet()) {
            ConfigNode child = node.getOrCreatePath(new String[]{key}, 0);

            JsonElement element = json.get(key);

            if (child.isLeaf()) {
                Class<?> childValueType = child.getValueType();

                if (childValueType == null) {
                    childValueType = Object.class;

                    Object childValue = child.getValue();
                    if (childValue != null) {
                        childValueType = childValue.getClass();
                    }
                }

                child.setValue(GSON.fromJson(element, childValueType));
                continue;
            }

            if (element.isJsonObject()) {
                this.deserializeNode(child, element.getAsJsonObject());
            }
        }
    }

    private static Gson getGson() {
        return new GsonBuilder()
                .setObjectToNumberStrategy(ToNumberPolicy.LONG_OR_DOUBLE)
                .setPrettyPrinting()
                .create();
    }

    @Override
    public String getFileExtension() {
        return "json";
    }
}
