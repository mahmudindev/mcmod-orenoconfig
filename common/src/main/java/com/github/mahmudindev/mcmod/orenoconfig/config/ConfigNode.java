package com.github.mahmudindev.mcmod.orenoconfig.config;

import com.github.mahmudindev.mcmod.orenoconfig.config.convert.ConfigValueConverter;

import java.util.LinkedHashMap;
import java.util.Map;

public class ConfigNode {
    private Class<?> valueType;
    private Object defaultValue;
    private Object value;
    private ConfigValueConverter valueConverter = ConfigValueConverter.GLOBAL;
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

    public Class<?> getValueType() {
        Class<?> clazz = this.valueType;

        if (clazz == null && this.defaultValue != null) {
            clazz = this.defaultValue.getClass();
        }

        return clazz;
    }

    protected void setValueType(Class<?> valueType) {
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

    public <T> T getValue(Class<T> clazz) {
        if (clazz == null) {
            return null;
        }

        Object value = this.getValue();

        if (value == null) {
            return null;
        }

        Class<?> claxx = value.getClass();

        if (claxx == clazz) {
            return (T) value;
        }

        if (this.valueConverter != null && this.valueConverter.canConvert(claxx, clazz)) {
            try {
                return valueConverter.convert(value, clazz);
            } catch (Exception e) {
                throw new RuntimeException(String.format(
                        "Unable to get and convert value from type %s to type %s",
                        claxx,
                        clazz
                ), e);
            }
        }

        if (clazz.isAssignableFrom(claxx)) {
            return clazz.cast(value);
        }

        throw new IllegalArgumentException(String.format(
                "Unable to get value from type %s to type %s",
                claxx,
                clazz
        ));
    }

    public void setValue(Object value) {
        if (value == null) {
            this.value = null;
            return;
        }

        Class<?> typeA = this.getValueType();
        if (typeA == null) {
            this.value = value;
            return;
        }

        Class<?> typeB = value.getClass();

        if (typeB == typeA) {
            this.value = value;
            return;
        }

        if (this.valueConverter != null && this.valueConverter.canConvert(typeB, typeA)) {
            try {
                this.value = valueConverter.convert(value, typeA);
                return;
            } catch (Exception e) {
                throw new RuntimeException(String.format(
                        "Unable to convert and set value to type %s from type %s",
                        typeA,
                        typeB
                ), e);
            }
        }

        if (typeA.isAssignableFrom(typeB)) {
            this.value = typeA.cast(value);
            return;
        }

        throw new IllegalArgumentException(String.format(
                "Unable to set value to type %s from type %s",
                typeA,
                typeB
        ));
    }

    public boolean isDefault() {
        if (this.defaultValue != null) {
            return this.value == null || this.value == this.defaultValue;
        }

        return false;
    }

    protected void setValueConverter(ConfigValueConverter valueConverter) {
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
