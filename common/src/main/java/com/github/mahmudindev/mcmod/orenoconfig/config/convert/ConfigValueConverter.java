package com.github.mahmudindev.mcmod.orenoconfig.config.convert;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

public class ConfigValueConverter {
    private final Map<Key, Function<?, ?>> converters = new HashMap<>();
    public static final ConfigValueConverter GLOBAL = new ConfigValueConverter();

    public ConfigValueConverter() {}

    public ConfigValueConverter(ConfigValueConverter valueConverter) {
        this.converters.putAll(valueConverter.converters);
    }

    public <S, T> void register(Class<S> source, Class<T> target, Function<S, T> converter) {
        converters.put(new Key(source, target), converter);
    }

    public <S, T> T convert(S source, Class<T> targetClass) {
        if (source == null) {
            return null;
        }

        Key key = new Key(source.getClass(), targetClass);
        Function<S, T> converter = (Function<S, T>) converters.get(key);

        if (converter == null) {
            throw new IllegalArgumentException("No converter registered for " + key);
        }

        return converter.apply(source);
    }

    public boolean canConvert(Class<?> source, Class<?> target) {
        return converters.containsKey(new Key(source, target));
    }

    private record Key(Class<?> source, Class<?> target) {}

    static {
        GLOBAL.register(Short.class, Integer.class, Integer::valueOf);
        GLOBAL.register(Short.class, Long.class, Long::valueOf);
        GLOBAL.register(Short.class, Double.class, Double::valueOf);
        GLOBAL.register(Short.class, Float.class, Float::valueOf);
        GLOBAL.register(Short.class, String.class, String::valueOf);
        GLOBAL.register(Integer.class, Long.class, Long::valueOf);
        GLOBAL.register(Integer.class, Double.class, Double::valueOf);
        GLOBAL.register(Integer.class, Float.class, Float::valueOf);
        GLOBAL.register(Integer.class, String.class, String::valueOf);
        GLOBAL.register(Long.class, Double.class, Double::valueOf);
        GLOBAL.register(Long.class, Float.class, Float::valueOf);
        GLOBAL.register(Long.class, String.class, String::valueOf);
        GLOBAL.register(Float.class, Double.class, Double::valueOf);
        GLOBAL.register(Float.class, String.class, String::valueOf);
        GLOBAL.register(Double.class, String.class, String::valueOf);
        GLOBAL.register(Boolean.class, String.class, String::valueOf);

        GLOBAL.register(Integer.class, Short.class, Integer::shortValue);
        GLOBAL.register(Long.class, Short.class, Long::shortValue);
        GLOBAL.register(Long.class, Integer.class, Long::intValue);
        GLOBAL.register(Float.class, Short.class, Float::shortValue);
        GLOBAL.register(Float.class, Integer.class, Float::intValue);
        GLOBAL.register(Float.class, Long.class, Float::longValue);
        GLOBAL.register(Double.class, Short.class, Double::shortValue);
        GLOBAL.register(Double.class, Integer.class, Double::intValue);
        GLOBAL.register(Double.class, Long.class, Double::longValue);
        GLOBAL.register(Double.class, Float.class, Double::floatValue);

        GLOBAL.register(String.class, Short.class, Short::parseShort);
        GLOBAL.register(String.class, Integer.class, Integer::parseInt);
        GLOBAL.register(String.class, Long.class, Long::parseLong);
        GLOBAL.register(String.class, Float.class, Float::parseFloat);
        GLOBAL.register(String.class, Double.class, Double::parseDouble);
        GLOBAL.register(String.class, Boolean.class, Boolean::parseBoolean);
    }
}
