/*
 * Decompiled with CFR 0.152.
 */
package com.badlogic.gdx.utils;

import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ObjectMap;
import com.badlogic.gdx.utils.Pool;
import com.badlogic.gdx.utils.ReflectionPool;

public class Pools {
    private static final ObjectMap<Class, Pool> typePools = new ObjectMap();

    public static <T> Pool<T> get(Class<T> type, int max) {
        ReflectionPool<T> pool = typePools.get(type);
        if (pool == null) {
            pool = new ReflectionPool<T>(type, 4, max);
            typePools.put(type, pool);
        }
        return pool;
    }

    public static <T> Pool<T> get(Class<T> type) {
        return Pools.get(type, 100);
    }

    public static <T> void set(Class<T> type, Pool<T> pool) {
        typePools.put(type, pool);
    }

    public static <T> T obtain(Class<T> type) {
        return Pools.get(type).obtain();
    }

    public static void free(Object object) {
        if (object == null) {
            throw new IllegalArgumentException("Object cannot be null.");
        }
        Pool pool = typePools.get(object.getClass());
        if (pool == null) {
            return;
        }
        pool.free(object);
    }

    public static void freeAll(Array objects) {
        Pools.freeAll(objects, false);
    }

    public static void freeAll(Array objects, boolean samePool) {
        if (objects == null) {
            throw new IllegalArgumentException("Objects cannot be null.");
        }
        Pool pool = null;
        int n = objects.size;
        for (int i = 0; i < n; ++i) {
            Object object = objects.get(i);
            if (object == null || pool == null && (pool = typePools.get(object.getClass())) == null) continue;
            pool.free(object);
            if (samePool) continue;
            pool = null;
        }
    }

    private Pools() {
    }
}

