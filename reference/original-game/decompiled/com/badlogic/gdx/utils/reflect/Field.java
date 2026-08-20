/*
 * Decompiled with CFR 0.152.
 */
package com.badlogic.gdx.utils.reflect;

import com.badlogic.gdx.utils.reflect.Annotation;
import com.badlogic.gdx.utils.reflect.ArrayReflection;
import com.badlogic.gdx.utils.reflect.ReflectionException;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

public final class Field {
    private final java.lang.reflect.Field field;

    Field(java.lang.reflect.Field field) {
        this.field = field;
    }

    public String getName() {
        return this.field.getName();
    }

    public Class getType() {
        return this.field.getType();
    }

    public Class getDeclaringClass() {
        return this.field.getDeclaringClass();
    }

    public boolean isAccessible() {
        return this.field.isAccessible();
    }

    public void setAccessible(boolean accessible) {
        this.field.setAccessible(accessible);
    }

    public boolean isDefaultAccess() {
        return !this.isPrivate() && !this.isProtected() && !this.isPublic();
    }

    public boolean isFinal() {
        return Modifier.isFinal(this.field.getModifiers());
    }

    public boolean isPrivate() {
        return Modifier.isPrivate(this.field.getModifiers());
    }

    public boolean isProtected() {
        return Modifier.isProtected(this.field.getModifiers());
    }

    public boolean isPublic() {
        return Modifier.isPublic(this.field.getModifiers());
    }

    public boolean isStatic() {
        return Modifier.isStatic(this.field.getModifiers());
    }

    public boolean isTransient() {
        return Modifier.isTransient(this.field.getModifiers());
    }

    public boolean isVolatile() {
        return Modifier.isVolatile(this.field.getModifiers());
    }

    public boolean isSynthetic() {
        return this.field.isSynthetic();
    }

    public Class getElementType(int index) {
        Type[] actualTypes;
        Type genericType = this.field.getGenericType();
        if (genericType instanceof ParameterizedType && (actualTypes = ((ParameterizedType)genericType).getActualTypeArguments()).length - 1 >= index) {
            Type componentType;
            Type actualType = actualTypes[index];
            if (actualType instanceof Class) {
                return (Class)actualType;
            }
            if (actualType instanceof ParameterizedType) {
                return (Class)((ParameterizedType)actualType).getRawType();
            }
            if (actualType instanceof GenericArrayType && (componentType = ((GenericArrayType)actualType).getGenericComponentType()) instanceof Class) {
                return ArrayReflection.newInstance((Class)componentType, 0).getClass();
            }
        }
        return null;
    }

    public boolean isAnnotationPresent(Class<? extends java.lang.annotation.Annotation> annotationType) {
        return this.field.isAnnotationPresent(annotationType);
    }

    public Annotation[] getDeclaredAnnotations() {
        java.lang.annotation.Annotation[] annotations = this.field.getDeclaredAnnotations();
        Annotation[] result = new Annotation[annotations.length];
        for (int i = 0; i < annotations.length; ++i) {
            result[i] = new Annotation(annotations[i]);
        }
        return result;
    }

    public Annotation getDeclaredAnnotation(Class<? extends java.lang.annotation.Annotation> annotationType) {
        java.lang.annotation.Annotation[] annotations = this.field.getDeclaredAnnotations();
        if (annotations == null) {
            return null;
        }
        for (java.lang.annotation.Annotation annotation : annotations) {
            if (!annotation.annotationType().equals(annotationType)) continue;
            return new Annotation(annotation);
        }
        return null;
    }

    public Object get(Object obj) throws ReflectionException {
        try {
            return this.field.get(obj);
        }
        catch (IllegalArgumentException e) {
            throw new ReflectionException("Object is not an instance of " + this.getDeclaringClass(), e);
        }
        catch (IllegalAccessException e) {
            throw new ReflectionException("Illegal access to field: " + this.getName(), e);
        }
    }

    public void set(Object obj, Object value) throws ReflectionException {
        try {
            this.field.set(obj, value);
        }
        catch (IllegalArgumentException e) {
            throw new ReflectionException("Argument not valid for field: " + this.getName(), e);
        }
        catch (IllegalAccessException e) {
            throw new ReflectionException("Illegal access to field: " + this.getName(), e);
        }
    }
}

