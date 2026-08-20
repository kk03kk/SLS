/*
 * Decompiled with CFR 0.152.
 */
package com.badlogic.gdx.utils.reflect;

import com.badlogic.gdx.utils.reflect.ReflectionException;
import java.lang.reflect.InvocationTargetException;

public final class Constructor {
    private final java.lang.reflect.Constructor constructor;

    Constructor(java.lang.reflect.Constructor constructor) {
        this.constructor = constructor;
    }

    public Class[] getParameterTypes() {
        return this.constructor.getParameterTypes();
    }

    public Class getDeclaringClass() {
        return this.constructor.getDeclaringClass();
    }

    public boolean isAccessible() {
        return this.constructor.isAccessible();
    }

    public void setAccessible(boolean accessible) {
        this.constructor.setAccessible(accessible);
    }

    public Object newInstance(Object ... args) throws ReflectionException {
        try {
            return this.constructor.newInstance(args);
        }
        catch (IllegalArgumentException e) {
            throw new ReflectionException("Illegal argument(s) supplied to constructor for class: " + this.getDeclaringClass().getName(), e);
        }
        catch (InstantiationException e) {
            throw new ReflectionException("Could not instantiate instance of class: " + this.getDeclaringClass().getName(), e);
        }
        catch (IllegalAccessException e) {
            throw new ReflectionException("Could not instantiate instance of class: " + this.getDeclaringClass().getName(), e);
        }
        catch (InvocationTargetException e) {
            throw new ReflectionException("Exception occurred in constructor for class: " + this.getDeclaringClass().getName(), e);
        }
    }
}

