/*
 * Decompiled with CFR 0.152.
 */
package com.badlogic.gdx.utils.reflect;

import com.badlogic.gdx.utils.reflect.Annotation;
import com.badlogic.gdx.utils.reflect.Constructor;
import com.badlogic.gdx.utils.reflect.Field;
import com.badlogic.gdx.utils.reflect.Method;
import com.badlogic.gdx.utils.reflect.ReflectionException;
import java.lang.reflect.Modifier;

public final class ClassReflection {
    public static Class forName(String name) throws ReflectionException {
        try {
            return Class.forName(name);
        }
        catch (ClassNotFoundException e) {
            throw new ReflectionException("Class not found: " + name, e);
        }
    }

    public static String getSimpleName(Class c) {
        return c.getSimpleName();
    }

    public static boolean isInstance(Class c, Object obj) {
        return c.isInstance(obj);
    }

    public static boolean isAssignableFrom(Class c1, Class c2) {
        return c1.isAssignableFrom(c2);
    }

    public static boolean isMemberClass(Class c) {
        return c.isMemberClass();
    }

    public static boolean isStaticClass(Class c) {
        return Modifier.isStatic(c.getModifiers());
    }

    public static boolean isArray(Class c) {
        return c.isArray();
    }

    public static <T> T newInstance(Class<T> c) throws ReflectionException {
        try {
            return c.newInstance();
        }
        catch (InstantiationException e) {
            throw new ReflectionException("Could not instantiate instance of class: " + c.getName(), e);
        }
        catch (IllegalAccessException e) {
            throw new ReflectionException("Could not instantiate instance of class: " + c.getName(), e);
        }
    }

    public static Constructor[] getConstructors(Class c) {
        java.lang.reflect.Constructor<?>[] constructors = c.getConstructors();
        Constructor[] result = new Constructor[constructors.length];
        int j = constructors.length;
        for (int i = 0; i < j; ++i) {
            result[i] = new Constructor(constructors[i]);
        }
        return result;
    }

    public static Constructor getConstructor(Class c, Class ... parameterTypes) throws ReflectionException {
        try {
            return new Constructor(c.getConstructor(parameterTypes));
        }
        catch (SecurityException e) {
            throw new ReflectionException("Security violation occurred while getting constructor for class: '" + c.getName() + "'.", e);
        }
        catch (NoSuchMethodException e) {
            throw new ReflectionException("Constructor not found for class: " + c.getName(), e);
        }
    }

    public static Constructor getDeclaredConstructor(Class c, Class ... parameterTypes) throws ReflectionException {
        try {
            return new Constructor(c.getDeclaredConstructor(parameterTypes));
        }
        catch (SecurityException e) {
            throw new ReflectionException("Security violation while getting constructor for class: " + c.getName(), e);
        }
        catch (NoSuchMethodException e) {
            throw new ReflectionException("Constructor not found for class: " + c.getName(), e);
        }
    }

    public static Method[] getMethods(Class c) {
        java.lang.reflect.Method[] methods = c.getMethods();
        Method[] result = new Method[methods.length];
        int j = methods.length;
        for (int i = 0; i < j; ++i) {
            result[i] = new Method(methods[i]);
        }
        return result;
    }

    public static Method getMethod(Class c, String name, Class ... parameterTypes) throws ReflectionException {
        try {
            return new Method(c.getMethod(name, parameterTypes));
        }
        catch (SecurityException e) {
            throw new ReflectionException("Security violation while getting method: " + name + ", for class: " + c.getName(), e);
        }
        catch (NoSuchMethodException e) {
            throw new ReflectionException("Method not found: " + name + ", for class: " + c.getName(), e);
        }
    }

    public static Method[] getDeclaredMethods(Class c) {
        java.lang.reflect.Method[] methods = c.getDeclaredMethods();
        Method[] result = new Method[methods.length];
        int j = methods.length;
        for (int i = 0; i < j; ++i) {
            result[i] = new Method(methods[i]);
        }
        return result;
    }

    public static Method getDeclaredMethod(Class c, String name, Class ... parameterTypes) throws ReflectionException {
        try {
            return new Method(c.getDeclaredMethod(name, parameterTypes));
        }
        catch (SecurityException e) {
            throw new ReflectionException("Security violation while getting method: " + name + ", for class: " + c.getName(), e);
        }
        catch (NoSuchMethodException e) {
            throw new ReflectionException("Method not found: " + name + ", for class: " + c.getName(), e);
        }
    }

    public static Field[] getFields(Class c) {
        java.lang.reflect.Field[] fields = c.getFields();
        Field[] result = new Field[fields.length];
        int j = fields.length;
        for (int i = 0; i < j; ++i) {
            result[i] = new Field(fields[i]);
        }
        return result;
    }

    public static Field getField(Class c, String name) throws ReflectionException {
        try {
            return new Field(c.getField(name));
        }
        catch (SecurityException e) {
            throw new ReflectionException("Security violation while getting field: " + name + ", for class: " + c.getName(), e);
        }
        catch (NoSuchFieldException e) {
            throw new ReflectionException("Field not found: " + name + ", for class: " + c.getName(), e);
        }
    }

    public static Field[] getDeclaredFields(Class c) {
        java.lang.reflect.Field[] fields = c.getDeclaredFields();
        Field[] result = new Field[fields.length];
        int j = fields.length;
        for (int i = 0; i < j; ++i) {
            result[i] = new Field(fields[i]);
        }
        return result;
    }

    public static Field getDeclaredField(Class c, String name) throws ReflectionException {
        try {
            return new Field(c.getDeclaredField(name));
        }
        catch (SecurityException e) {
            throw new ReflectionException("Security violation while getting field: " + name + ", for class: " + c.getName(), e);
        }
        catch (NoSuchFieldException e) {
            throw new ReflectionException("Field not found: " + name + ", for class: " + c.getName(), e);
        }
    }

    public static boolean isAnnotationPresent(Class c, Class<? extends java.lang.annotation.Annotation> annotationType) {
        return c.isAnnotationPresent(annotationType);
    }

    public static Annotation[] getAnnotations(Class c) {
        java.lang.annotation.Annotation[] annotations = c.getAnnotations();
        Annotation[] result = new Annotation[annotations.length];
        for (int i = 0; i < annotations.length; ++i) {
            result[i] = new Annotation(annotations[i]);
        }
        return result;
    }

    public static Annotation getAnnotation(Class c, Class<? extends java.lang.annotation.Annotation> annotationType) {
        java.lang.annotation.Annotation annotation = c.getAnnotation(annotationType);
        if (annotation != null) {
            return new Annotation(annotation);
        }
        return null;
    }

    public static Annotation[] getDeclaredAnnotations(Class c) {
        java.lang.annotation.Annotation[] annotations = c.getDeclaredAnnotations();
        Annotation[] result = new Annotation[annotations.length];
        for (int i = 0; i < annotations.length; ++i) {
            result[i] = new Annotation(annotations[i]);
        }
        return result;
    }

    public static Annotation getDeclaredAnnotation(Class c, Class<? extends java.lang.annotation.Annotation> annotationType) {
        java.lang.annotation.Annotation[] annotations;
        for (java.lang.annotation.Annotation annotation : annotations = c.getDeclaredAnnotations()) {
            if (!annotation.annotationType().equals(annotationType)) continue;
            return new Annotation(annotation);
        }
        return null;
    }

    public static Class[] getInterfaces(Class c) {
        return c.getInterfaces();
    }
}

