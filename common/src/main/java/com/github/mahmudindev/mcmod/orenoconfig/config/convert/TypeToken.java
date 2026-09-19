package com.github.mahmudindev.mcmod.orenoconfig.config.convert;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

public abstract class TypeToken {
    private final Type type;

    protected TypeToken() {
        Type genericSuperclass = getClass().getGenericSuperclass();

        if (genericSuperclass instanceof ParameterizedType parameterizedType) {
            this.type = parameterizedType.getActualTypeArguments()[0];
            return;
        }

        throw new IllegalArgumentException("TypeToken must be instantiated with generics");
    }

    public Type getType() {
        return this.type;
    }
}
