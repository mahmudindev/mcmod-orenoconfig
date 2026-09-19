package com.github.mahmudindev.mcmod.orenoconfig.config.parser;

import com.github.mahmudindev.mcmod.orenoconfig.config.ConfigNode;
import com.google.gson.*;

import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;

public class JsonConfigParser implements ConfigParser {
    private final Gson gson = this.getGson();

    @Override
    public void serialize(ConfigNode parent, Writer writer) {
        this.gson.toJson(this.serializeNode(parent), writer);
    }

    private JsonObject serializeNode(ConfigNode node) {
        JsonObject json = new JsonObject();

        for (var entry : node.getChildren().entrySet()) {
            String key = entry.getKey();
            ConfigNode child = entry.getValue();

            if (child.isLeaf()) {
                json.add(key, this.gson.toJsonTree(child.getValue()));
                continue;
            }

            json.add(key, this.serializeNode(child));
        }

        return json;
    }

    @Override
    public void deserialize(Reader reader, ConfigNode node) {
        this.deserializeNode(node, this.gson.fromJson(reader, JsonObject.class));
    }

    private void deserializeNode(ConfigNode node, JsonObject json) {
        for (String key : json.keySet()) {
            ConfigNode child = node.getOrCreatePath(new String[]{key}, 0);

            JsonElement element = json.get(key);

            if (child.isLeaf()) {
                Type childValueType = child.getValueType();

                if (childValueType == null) {
                    childValueType = Object.class;

                    Object childValue = child.getValue();
                    if (childValue != null) {
                        childValueType = childValue.getClass();
                    }
                }

                child.setValue(this.gson.fromJson(element, childValueType));
                continue;
            }

            if (element.isJsonObject()) {
                this.deserializeNode(child, element.getAsJsonObject());
            }
        }
    }

    private Gson getGson() {
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
