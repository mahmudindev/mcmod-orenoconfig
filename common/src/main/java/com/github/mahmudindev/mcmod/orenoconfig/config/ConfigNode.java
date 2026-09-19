package com.github.mahmudindev.mcmod.orenoconfig.config;

import com.github.mahmudindev.mcmod.orenoconfig.config.convert.ValueConverter;

import java.lang.reflect.Type;
import java.util.LinkedHashMap;
import java.util.Map;

public class ConfigNode {
    private Type valueType;
    private Object defaultValue;
    private Object value;
    private ValueConverter valueConverter = ValueConverter.GLOBAL;
    private final Map<String, ConfigNode> children = new LinkedHashMap<>();

    public ConfigNode() {}

    public ConfigNode(ConfigNode configNode) {
        this(configNode, false);
    }

    public ConfigNode(ConfigNode configNode, boolean onlyDefault) {
        this.valueType = configNode.valueType;

        this.defaultValue = configNode.defaultValue;
        if (!onlyDefault) {
            this.value = configNode.value;
        }

        this.valueConverter = configNode.valueConverter;

        configNode.children.forEach((key, value) -> {
            this.children.put(key, new ConfigNode(value, onlyDefault));
        });
    }

    public Type getValueType() {
        Type type = this.valueType;

        if (type == null && this.defaultValue != null) {
            type = this.defaultValue.getClass();
        }

        return type;
    }

    protected void setValueType(Type valueType) {
        this.valueType = valueType;
    }

    protected boolean isTyped() {
        return this.valueType != null;
    }

    protected void setDefaultValue(Object defaultValue) {
        this.defaultValue = defaultValue;
    }

    public Object getValue() {
        if (this.value != null) {
            return this.value;
        }

        return this.defaultValue;
    }

    public <T> T getValue(Type type) {
        if (type == null) {
            throw new IllegalArgumentException("Type cannot be null");
        }

        Object value = this.getValue();

        if (value == null) {
            return null;
        }

        Type typeX = this.getValueType();

        if (typeX.equals(type)) {
            return (T) value;
        }

        if (this.valueConverter != null) {
            try {
                return valueConverter.convert(value, typeX, type);
            } catch (Exception e) {
                throw new RuntimeException(String.format(
                        "Unable to get and convert value from type %s to type %s",
                        typeX,
                        type
                ), e);
            }
        }

        throw new IllegalArgumentException(String.format(
                "Unable to get value from type %s to type %s",
                typeX,
                type
        ));
    }

    public void setValue(Object value) {
        this.setValue(value.getClass(), value);
    }

    public void setValue(Type type, Object value) {
        if (value == null) {
            this.value = null;
            return;
        }

        Type typeX = this.getValueType();

        if (typeX == null || type.equals(typeX)) {
            this.value = value;
            return;
        }

        if (this.valueConverter != null) {
            try {
                this.value = valueConverter.convert(value, type, typeX);
                return;
            } catch (Exception e) {
                throw new RuntimeException(String.format(
                        "Unable to convert and set value to type %s from type %s",
                        typeX,
                        type
                ), e);
            }
        }

        throw new IllegalArgumentException(String.format(
                "Unable to set value to type %s from type %s",
                typeX,
                type
        ));
    }

    public boolean isDefault() {
        if (this.defaultValue != null) {
            return this.value == null || this.value == this.defaultValue;
        }

        return false;
    }

    protected void setValueConverter(ValueConverter valueConverter) {
        this.valueConverter = valueConverter;

        this.children.forEach((k, v) -> v.setValueConverter(valueConverter));
    }

    public ConfigNode getOrCreatePath(String[] path, int index) {
        if (index >= path.length) {
            return this;
        }

        ConfigNode child = this.children.computeIfAbsent(
                path[index],
                key -> {
                    ConfigNode node = new ConfigNode();
                    node.valueConverter = this.valueConverter;
                    return node;
                }
        );

        return child.getOrCreatePath(path, index + 1);
    }

    public ConfigNode getPath(String[] path, int index) {
        if (index >= path.length) {
            return this;
        }

        ConfigNode child = this.children.get(path[index]);

        return child != null ? child.getPath(path, index + 1) : null;
    }

    public void removePath(String[] path, int index) {
        this.removePath(path, index, true);
    }

    protected boolean removePath(String[] path, int index, boolean withDefault) {
        if (index >= path.length) {
            return false;
        }

        String key = path[index];

        ConfigNode child = this.children.get(key);
        if (child == null) {
            return false;
        }

        if (index == path.length - 1) {
            if (child.isLeaf()) {
                if (child.isTyped()) {
                    return false;
                }

                if (!withDefault && child.defaultValue != null) {
                    return false;
                }
            } else {
                if (!child.clear(withDefault)) {
                    return false;
                }
            }

            this.children.remove(key);
        } else {
            if (child.removePath(path, index + 1, withDefault)) {
                this.children.remove(key);
            }
        }

        if (this.isLeaf()) {
            if (this.isTyped()) {
                return false;
            }

            return withDefault || this.defaultValue == null;
        }

        return false;
    }

    public Map<String, ConfigNode> getChildren() {
        return this.children;
    }

    public boolean isLeaf() {
        return this.children.isEmpty();
    }

    public void clear() {
        this.clear(true);
    }

    protected boolean clear(boolean withDefault) {
        this.value = null;

        if (withDefault) {
            this.defaultValue = null;
        }

        for (Map.Entry<String, ConfigNode> entry : this.children.entrySet()) {
            ConfigNode v = entry.getValue();
            if (v.clear(withDefault)) {
                this.children.remove(entry.getKey());
            }
        }

        return this.isLeaf() && !this.isTyped() && this.defaultValue == null;
    }
}
