package com.github.mahmudindev.mcmod.orenoconfig.config;

import com.github.mahmudindev.mcmod.orenoconfig.config.annotation.ConfigCategory;
import com.github.mahmudindev.mcmod.orenoconfig.config.annotation.ConfigOption;
import com.github.mahmudindev.mcmod.orenoconfig.config.parser.ConfigParser;

import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.*;

public class Config {
    private final ConfigNode root;
    private final Map<String, List<Object>> pojo = new HashMap<>();

    public Config() {
        this.root = new ConfigNode();
    }

    public Config(Config config) {
        this(config, false);
    }

    public Config(Config config, boolean onlyDefault) {
        this.root = new ConfigNode(config.root, onlyDefault);
    }

    protected ConfigNode getRNode() {
        return this.root;
    }

    protected ConfigNode getWNode() {
        return this.root;
    }

    public void registerValue(String path, Object defaultValue) {
        this.registerValue(path, defaultValue.getClass(), defaultValue);
    }

    public void registerValue(String path, Type type, Object defaultValue) {
        if (defaultValue == null) {
            throw new IllegalArgumentException("Default value cannot be null");
        }

        ConfigNode root = this.getWNode();
        ConfigNode node = root.getOrCreatePath(this.getSplitPath(path), 0);
        node.setValueType(type);
        node.setDefaultValue(defaultValue);
    }

    public void registerPojo(String path, Object pojo) {
        if (pojo == null) {
            throw new IllegalArgumentException("Pojo object cannot be null");
        }

        this.registerPojo(this.getSplitPath(path), pojo);

        this.pojo.computeIfAbsent(path, k -> new ArrayList<>()).add(pojo);
    }

    protected void registerPojo(String[] path, Object pojo) {
        String[] paths = Arrays.copyOf(path, path.length + 1);

        Class<?> clazz = pojo.getClass();
        while (clazz != Object.class) {
            for (Field field : clazz.getDeclaredFields()) {
                final int fieldModifiers = field.getModifiers();

                if (Modifier.isStatic(fieldModifiers)) {
                    continue;
                }

                if (Modifier.isTransient(fieldModifiers)) {
                    continue;
                }

                field.setAccessible(true);

                try {
                    paths[paths.length - 1] = field.getName();

                    Object value = field.get(pojo);

                    if (field.isAnnotationPresent(ConfigCategory.class)) {
                        ConfigCategory category = field.getAnnotation(ConfigCategory.class);

                        String name = category.name();
                        if (!name.isEmpty()) {
                            paths[paths.length - 1] = name;
                        }

                        if (value == null) {
                            value = field.getType().getDeclaredConstructor().newInstance();
                            field.set(pojo, value);
                        }

                        this.registerPojo(paths, value);

                        continue;
                    }

                    ConfigOption option = field.getAnnotation(ConfigOption.class);
                    if (option != null) {
                        String pathX = option.path();
                        if (!pathX.isEmpty()) {
                            paths[paths.length - 1] = pathX;
                        }
                    }

                    ConfigNode root = this.getWNode();
                    ConfigNode node = root.getOrCreatePath(paths, 0);
                    node.setValueType(field.getGenericType());
                    node.setDefaultValue(value);
                } catch (Exception e) {
                    throw new IllegalStateException("Failed to register Pojo object", e);
                }
            }

            clazz = clazz.getSuperclass();
        }
    }

    public void syncPojo() {
        this.pojo.forEach((path, v) -> v.forEach(pojo -> {
            this.syncPojo(this.getSplitPath(path), pojo);
        }));
    }

    protected void syncPojo(String[] path, Object pojo) {
        String[] paths = Arrays.copyOf(path, path.length + 1);

        Class<?> clazz = pojo.getClass();
        while (clazz != Object.class) {
            for (Field field : clazz.getDeclaredFields()) {
                final int fieldModifiers = field.getModifiers();

                if (Modifier.isStatic(fieldModifiers)) {
                    continue;
                }

                if (Modifier.isTransient(fieldModifiers)) {
                    continue;
                }

                field.setAccessible(true);

                try {
                    paths[paths.length - 1] = field.getName();

                    if (field.isAnnotationPresent(ConfigCategory.class)) {
                        ConfigCategory category = field.getAnnotation(ConfigCategory.class);

                        String name = category.name();
                        if (!name.isEmpty()) {
                            paths[paths.length - 1] = name;
                        }

                        this.syncPojo(paths, field.get(pojo));

                        continue;
                    }

                    ConfigOption option = field.getAnnotation(ConfigOption.class);
                    if (option != null) {
                        String pathX = option.path();
                        if (!pathX.isEmpty()) {
                            paths[paths.length - 1] = pathX;
                        }
                    }

                    ConfigNode root = this.getRNode();
                    ConfigNode node = root.getPath(paths, 0);
                    field.set(pojo, node.getValue(field.getGenericType()));
                } catch (Exception e) {
                    throw new IllegalStateException("Failed to sync Pojo object", e);
                }
            }

            clazz = clazz.getSuperclass();
        }
    }

    public void load(ConfigParser parser, Reader reader) {
        parser.deserialize(reader, this.root);

        this.syncPojo();
    }

    public void save(ConfigParser parser, Writer writer) {
        parser.serialize(this.root, writer);
    }

    public Object getRaw(String path) {
        ConfigNode root = this.getRNode();
        ConfigNode node = root.getPath(this.getSplitPath(path), 0);

        return node != null ? node.getValue() : null;
    }

    public <T> T get(String path, Type type) {
        ConfigNode root = this.getRNode();
        ConfigNode node = root.getPath(this.getSplitPath(path), 0);

        return node != null ? node.getValue(type) : null;
    }

    public void set(String path, Object value) {
        ConfigNode root = this.getWNode();
        ConfigNode node = root.getOrCreatePath(this.getSplitPath(path), 0);
        node.setValue(value);
    }

    public void remove(String path) {
        ConfigNode root = this.getWNode();
        root.removePath(this.getSplitPath(path), 0, false);
    }

    public Boolean isDefault(String path) {
        ConfigNode root = this.getRNode();
        ConfigNode node = root.getPath(this.getSplitPath(path), 0);

        return node != null ? node.isDefault() : null;
    }

    public void clear() {
        ConfigNode root = this.getWNode();
        root.clear(false);

        this.syncPojo();
    }

    protected String[] getSplitPath(String path) {
        if (path.isEmpty()) {
            return new String[0];
        }

        return path.split("\\.");
    }
}
