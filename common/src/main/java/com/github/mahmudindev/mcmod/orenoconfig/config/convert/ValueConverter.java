package com.github.mahmudindev.mcmod.orenoconfig.config.convert;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.*;
import java.util.function.Function;

public class ValueConverter {
    public static final ValueConverter GLOBAL = new ValueConverter();

    private final Map<Key, Function<?, ?>> converters = new HashMap<>();
    private final List<GenericHandler> genericHandlers = new ArrayList<>();

    public ValueConverter() {
    }

    public ValueConverter(ValueConverter valueConverter) {
        this.converters.putAll(valueConverter.converters);
        this.genericHandlers.addAll(valueConverter.genericHandlers);
    }

    public <S, T> void register(Class<S> source, Class<T> target, Function<S, T> converter) {
        this.converters.put(new Key(source, target), converter);
    }

    public void registerGeneric(GenericHandler genericHandler) {
        this.genericHandlers.add(0, genericHandler);
    }

    public <S, T> T convert(S source, Type sourceType, Type targetType) {
        if (source == null || targetType == null) {
            return null;
        }

        if (sourceType == null) {
            sourceType = source.getClass();
        }

        if (sourceType.equals(targetType)) {
            return (T) source;
        }

        Function<?, ?> converterA = this.converters.get(new Key(sourceType, targetType));
        if (converterA != null) {
            return ((Function<S, T>) converterA).apply(source);
        }

        Class<?> sourceClass = this.getRawClass(sourceType);
        Class<?> targetClass = this.getRawClass(targetType);

        if (targetType instanceof ParameterizedType parameterizedType) {
            ParameterizedType parameterizedTypeX = null;
            if (sourceType instanceof ParameterizedType) {
                parameterizedTypeX = (ParameterizedType) sourceType;
            }

            for (GenericHandler genericHandler : this.genericHandlers) {
                if (!genericHandler.supports(sourceClass, targetClass)) {
                    continue;
                }

                return (T) genericHandler.convert(
                        source,
                        parameterizedTypeX,
                        parameterizedType,
                        this::convert
                );
            }
        }

        if (targetClass.isAssignableFrom(sourceClass)) {
            return (T) source;
        }

        Function<?, ?> converterB = this.converters.get(new Key(sourceClass, targetClass));
        if (converterB != null) {
            return ((Function<S, T>) converterB).apply(source);
        }

        throw new IllegalArgumentException(String.format(
                "No converter found for %s -> %s",
                sourceType.getTypeName(),
                targetType.getTypeName()
        ));
    }

    private Class<?> getRawClass(Type type) {
        if (type instanceof Class<?> clazz) {
            return clazz;
        }

        if (type instanceof ParameterizedType parameterizedType) {
            return (Class<?>) parameterizedType.getRawType();
        }

        throw new IllegalArgumentException(String.format("Unsupported type: %s", type));
    }

    private record Key(Type source, Type target) {}

    public interface GenericHandler {
        boolean supports(Class<?> rawSource, Class<?> rawTarget);

        Object convert(
                Object source,
                ParameterizedType sourceType,
                ParameterizedType targetType,
                ConversionCallback callback
        );
    }

    @FunctionalInterface
    public interface ConversionCallback {
        <S, T> T convert(S source, Type sourceType, Type targetType);
    }

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

        GLOBAL.registerGeneric(new GenericHandler() {
            @Override
            public boolean supports(Class<?> rawSource, Class<?> rawTarget) {
                if (Map.class.isAssignableFrom(rawSource)) {
                    return Map.class.isAssignableFrom(rawTarget);
                }

                return false;
            }

            @Override
            public Object convert(
                    Object source,
                    ParameterizedType sourceType,
                    ParameterizedType targetType,
                    ConversionCallback callback
            ) {
                Map<Object, Object> result = new HashMap<>();

                Type sourceKeyType = (sourceType != null)
                        ? sourceType.getActualTypeArguments()[0]
                        : null;
                Type sourceValueType = (sourceType != null)
                        ? sourceType.getActualTypeArguments()[1]
                        : null;
                Type targetKeyType = targetType.getActualTypeArguments()[0];
                Type targetValueType = targetType.getActualTypeArguments()[1];

                for (Map.Entry<?, ?> entry : ((Map<?, ?>) source).entrySet()) {
                    Object rawKey = entry.getKey();
                    Object convertedKey = (rawKey != null)
                            ? callback.convert(rawKey, sourceKeyType, targetKeyType)
                            : null;

                    Object rawValue = entry.getValue();
                    Object convertedValue = (rawValue != null)
                            ? callback.convert(rawValue, sourceValueType, targetValueType)
                            : null;

                    result.put(convertedKey, convertedValue);
                }

                return result;
            }
        });

        GLOBAL.registerGeneric(new GenericHandler() {
            @Override
            public boolean supports(Class<?> rawSource, Class<?> rawTarget) {
                if (Collection.class.isAssignableFrom(rawSource)) {
                    return Collection.class.isAssignableFrom(rawTarget);
                }

                return false;
            }

            @Override
            public Object convert(
                    Object source,
                    ParameterizedType sourceType,
                    ParameterizedType targetType,
                    ConversionCallback callback
            ) {
                Class<?> targetClass = (Class<?>) targetType.getRawType();
                Collection<Object> result = Set.class.isAssignableFrom(targetClass)
                        ? new HashSet<>()
                        : new ArrayList<>();

                Type sourceElementType = (sourceType != null)
                        ? sourceType.getActualTypeArguments()[0]
                        : null;
                Type targetElementType = targetType.getActualTypeArguments()[0];

                for (Object item : (Collection<?>) source) {
                    if (item == null) {
                        result.add(null);

                        continue;
                    }

                    result.add(callback.convert(
                            item,
                            sourceElementType,
                            targetElementType
                    ));
                }

                return result;
            }
        });
    }
}
