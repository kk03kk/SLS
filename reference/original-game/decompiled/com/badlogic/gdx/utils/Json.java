/*
 * Decompiled with CFR 0.152.
 */
package com.badlogic.gdx.utils;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ArrayMap;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;
import com.badlogic.gdx.utils.JsonWriter;
import com.badlogic.gdx.utils.ObjectMap;
import com.badlogic.gdx.utils.OrderedMap;
import com.badlogic.gdx.utils.Queue;
import com.badlogic.gdx.utils.SerializationException;
import com.badlogic.gdx.utils.StreamUtils;
import com.badlogic.gdx.utils.reflect.ArrayReflection;
import com.badlogic.gdx.utils.reflect.ClassReflection;
import com.badlogic.gdx.utils.reflect.Constructor;
import com.badlogic.gdx.utils.reflect.Field;
import com.badlogic.gdx.utils.reflect.ReflectionException;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.security.AccessControlException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class Json {
    private static final boolean debug = false;
    private JsonWriter writer;
    private String typeName = "class";
    private boolean usePrototypes = true;
    private JsonWriter.OutputType outputType;
    private boolean quoteLongValues;
    private boolean ignoreUnknownFields;
    private boolean ignoreDeprecated;
    private boolean enumNames = true;
    private Serializer defaultSerializer;
    private final ObjectMap<Class, OrderedMap<String, FieldMetadata>> typeToFields = new ObjectMap();
    private final ObjectMap<String, Class> tagToClass = new ObjectMap();
    private final ObjectMap<Class, String> classToTag = new ObjectMap();
    private final ObjectMap<Class, Serializer> classToSerializer = new ObjectMap();
    private final ObjectMap<Class, Object[]> classToDefaultValues = new ObjectMap();
    private final Object[] equals1 = new Object[]{null};
    private final Object[] equals2 = new Object[]{null};

    public Json() {
        this.outputType = JsonWriter.OutputType.minimal;
    }

    public Json(JsonWriter.OutputType outputType) {
        this.outputType = outputType;
    }

    public void setIgnoreUnknownFields(boolean ignoreUnknownFields) {
        this.ignoreUnknownFields = ignoreUnknownFields;
    }

    public void setIgnoreDeprecated(boolean ignoreDeprecated) {
        this.ignoreDeprecated = ignoreDeprecated;
    }

    public void setOutputType(JsonWriter.OutputType outputType) {
        this.outputType = outputType;
    }

    public void setQuoteLongValues(boolean quoteLongValues) {
        this.quoteLongValues = quoteLongValues;
    }

    public void setEnumNames(boolean enumNames) {
        this.enumNames = enumNames;
    }

    public void addClassTag(String tag, Class type) {
        this.tagToClass.put(tag, type);
        this.classToTag.put(type, tag);
    }

    public Class getClass(String tag) {
        return this.tagToClass.get(tag);
    }

    public String getTag(Class type) {
        return this.classToTag.get(type);
    }

    public void setTypeName(String typeName) {
        this.typeName = typeName;
    }

    public void setDefaultSerializer(Serializer defaultSerializer) {
        this.defaultSerializer = defaultSerializer;
    }

    public <T> void setSerializer(Class<T> type, Serializer<T> serializer) {
        this.classToSerializer.put(type, serializer);
    }

    public <T> Serializer<T> getSerializer(Class<T> type) {
        return this.classToSerializer.get(type);
    }

    public void setUsePrototypes(boolean usePrototypes) {
        this.usePrototypes = usePrototypes;
    }

    public void setElementType(Class type, String fieldName, Class elementType) {
        OrderedMap<String, FieldMetadata> fields = this.getFields(type);
        FieldMetadata metadata = (FieldMetadata)fields.get(fieldName);
        if (metadata == null) {
            throw new SerializationException("Field not found: " + fieldName + " (" + type.getName() + ")");
        }
        metadata.elementType = elementType;
    }

    private OrderedMap<String, FieldMetadata> getFields(Class type) {
        OrderedMap<String, FieldMetadata> fields = this.typeToFields.get(type);
        if (fields != null) {
            return fields;
        }
        Array classHierarchy = new Array();
        for (Class nextClass = type; nextClass != Object.class; nextClass = nextClass.getSuperclass()) {
            classHierarchy.add(nextClass);
        }
        ArrayList allFields = new ArrayList();
        for (int i = classHierarchy.size - 1; i >= 0; --i) {
            Collections.addAll(allFields, ClassReflection.getDeclaredFields((Class)classHierarchy.get(i)));
        }
        OrderedMap<String, FieldMetadata> nameToField = new OrderedMap<String, FieldMetadata>(allFields.size());
        int n = allFields.size();
        for (int i = 0; i < n; ++i) {
            Field field = (Field)allFields.get(i);
            if (field.isTransient() || field.isStatic() || field.isSynthetic()) continue;
            if (!field.isAccessible()) {
                try {
                    field.setAccessible(true);
                }
                catch (AccessControlException ex) {
                    continue;
                }
            }
            if (this.ignoreDeprecated && field.isAnnotationPresent(Deprecated.class)) continue;
            nameToField.put(field.getName(), new FieldMetadata(field));
        }
        this.typeToFields.put(type, nameToField);
        return nameToField;
    }

    public String toJson(Object object) {
        return this.toJson(object, object == null ? null : object.getClass(), (Class)null);
    }

    public String toJson(Object object, Class knownType) {
        return this.toJson(object, knownType, (Class)null);
    }

    public String toJson(Object object, Class knownType, Class elementType) {
        StringWriter buffer = new StringWriter();
        this.toJson(object, knownType, elementType, buffer);
        return buffer.toString();
    }

    public void toJson(Object object, FileHandle file) {
        this.toJson(object, object == null ? null : object.getClass(), null, file);
    }

    public void toJson(Object object, Class knownType, FileHandle file) {
        this.toJson(object, knownType, null, file);
    }

    public void toJson(Object object, Class knownType, Class elementType, FileHandle file) {
        Writer writer = null;
        try {
            writer = file.writer(false, "UTF-8");
            this.toJson(object, knownType, elementType, writer);
        }
        catch (Exception ex) {
            try {
                throw new SerializationException("Error writing file: " + file, ex);
            }
            catch (Throwable throwable) {
                StreamUtils.closeQuietly(writer);
                throw throwable;
            }
        }
        StreamUtils.closeQuietly(writer);
    }

    public void toJson(Object object, Writer writer) {
        this.toJson(object, object == null ? null : object.getClass(), null, writer);
    }

    public void toJson(Object object, Class knownType, Writer writer) {
        this.toJson(object, knownType, null, writer);
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public void toJson(Object object, Class knownType, Class elementType, Writer writer) {
        this.setWriter(writer);
        try {
            this.writeValue(object, knownType, elementType);
        }
        finally {
            StreamUtils.closeQuietly(this.writer);
            this.writer = null;
        }
    }

    public void setWriter(Writer writer) {
        if (!(writer instanceof JsonWriter)) {
            writer = new JsonWriter(writer);
        }
        this.writer = (JsonWriter)writer;
        this.writer.setOutputType(this.outputType);
        this.writer.setQuoteLongValues(this.quoteLongValues);
    }

    public JsonWriter getWriter() {
        return this.writer;
    }

    public void writeFields(Object object) {
        Class<?> type = object.getClass();
        Object[] defaultValues = this.getDefaultValues(type);
        OrderedMap<String, FieldMetadata> fields = this.getFields(type);
        int i = 0;
        for (FieldMetadata metadata : new OrderedMap.OrderedMapValues<FieldMetadata>(fields)) {
            Field field = metadata.field;
            try {
                Object value = field.get(object);
                if (defaultValues != null) {
                    Object defaultValue = defaultValues[i++];
                    if (value == null && defaultValue == null) continue;
                    if (value != null && defaultValue != null) {
                        if (value.equals(defaultValue)) continue;
                        if (value.getClass().isArray() && defaultValue.getClass().isArray()) {
                            this.equals1[0] = value;
                            this.equals2[0] = defaultValue;
                            if (Arrays.deepEquals(this.equals1, this.equals2)) continue;
                        }
                    }
                }
                this.writer.name(field.getName());
                this.writeValue(value, field.getType(), metadata.elementType);
            }
            catch (ReflectionException ex) {
                throw new SerializationException("Error accessing field: " + field.getName() + " (" + type.getName() + ")", ex);
            }
            catch (SerializationException ex) {
                ex.addTrace(field + " (" + type.getName() + ")");
                throw ex;
            }
            catch (Exception runtimeEx) {
                SerializationException ex = new SerializationException(runtimeEx);
                ex.addTrace(field + " (" + type.getName() + ")");
                throw ex;
            }
        }
    }

    private Object[] getDefaultValues(Class type) {
        Object object;
        if (!this.usePrototypes) {
            return null;
        }
        if (this.classToDefaultValues.containsKey(type)) {
            return this.classToDefaultValues.get(type);
        }
        try {
            object = this.newInstance(type);
        }
        catch (Exception ex) {
            this.classToDefaultValues.put(type, null);
            return null;
        }
        OrderedMap<String, FieldMetadata> fields = this.getFields(type);
        Object[] values = new Object[fields.size];
        this.classToDefaultValues.put(type, values);
        int i = 0;
        for (FieldMetadata metadata : ((ObjectMap)fields).values()) {
            Field field = metadata.field;
            try {
                values[i++] = field.get(object);
            }
            catch (ReflectionException ex) {
                throw new SerializationException("Error accessing field: " + field.getName() + " (" + type.getName() + ")", ex);
            }
            catch (SerializationException ex) {
                ex.addTrace(field + " (" + type.getName() + ")");
                throw ex;
            }
            catch (RuntimeException runtimeEx) {
                SerializationException ex = new SerializationException(runtimeEx);
                ex.addTrace(field + " (" + type.getName() + ")");
                throw ex;
            }
        }
        return values;
    }

    public void writeField(Object object, String name) {
        this.writeField(object, name, name, null);
    }

    public void writeField(Object object, String name, Class elementType) {
        this.writeField(object, name, name, elementType);
    }

    public void writeField(Object object, String fieldName, String jsonName) {
        this.writeField(object, fieldName, jsonName, null);
    }

    public void writeField(Object object, String fieldName, String jsonName, Class elementType) {
        Class<?> type = object.getClass();
        OrderedMap<String, FieldMetadata> fields = this.getFields(type);
        FieldMetadata metadata = (FieldMetadata)fields.get(fieldName);
        if (metadata == null) {
            throw new SerializationException("Field not found: " + fieldName + " (" + type.getName() + ")");
        }
        Field field = metadata.field;
        if (elementType == null) {
            elementType = metadata.elementType;
        }
        try {
            this.writer.name(jsonName);
            this.writeValue(field.get(object), field.getType(), elementType);
        }
        catch (ReflectionException ex) {
            throw new SerializationException("Error accessing field: " + field.getName() + " (" + type.getName() + ")", ex);
        }
        catch (SerializationException ex) {
            ex.addTrace(field + " (" + type.getName() + ")");
            throw ex;
        }
        catch (Exception runtimeEx) {
            SerializationException ex = new SerializationException(runtimeEx);
            ex.addTrace(field + " (" + type.getName() + ")");
            throw ex;
        }
    }

    public void writeValue(String name, Object value) {
        try {
            this.writer.name(name);
        }
        catch (IOException ex) {
            throw new SerializationException(ex);
        }
        if (value == null) {
            this.writeValue(value, null, null);
        } else {
            this.writeValue(value, value.getClass(), null);
        }
    }

    public void writeValue(String name, Object value, Class knownType) {
        try {
            this.writer.name(name);
        }
        catch (IOException ex) {
            throw new SerializationException(ex);
        }
        this.writeValue(value, knownType, null);
    }

    public void writeValue(String name, Object value, Class knownType, Class elementType) {
        try {
            this.writer.name(name);
        }
        catch (IOException ex) {
            throw new SerializationException(ex);
        }
        this.writeValue(value, knownType, elementType);
    }

    public void writeValue(Object value) {
        if (value == null) {
            this.writeValue(value, null, null);
        } else {
            this.writeValue(value, value.getClass(), null);
        }
    }

    public void writeValue(Object value, Class knownType) {
        this.writeValue(value, knownType, null);
    }

    /*
     * WARNING - void declaration
     */
    public void writeValue(Object value, Class knownType, Class elementType) {
        try {
            if (value == null) {
                this.writer.value(null);
                return;
            }
            if (knownType != null && knownType.isPrimitive() || knownType == String.class || knownType == Integer.class || knownType == Boolean.class || knownType == Float.class || knownType == Long.class || knownType == Double.class || knownType == Short.class || knownType == Byte.class || knownType == Character.class) {
                this.writer.value(value);
                return;
            }
            Class<?> actualType = value.getClass();
            if (actualType.isPrimitive() || actualType == String.class || actualType == Integer.class || actualType == Boolean.class || actualType == Float.class || actualType == Long.class || actualType == Double.class || actualType == Short.class || actualType == Byte.class || actualType == Character.class) {
                this.writeObjectStart(actualType, null);
                this.writeValue("value", value);
                this.writeObjectEnd();
                return;
            }
            if (value instanceof Serializable) {
                this.writeObjectStart(actualType, knownType);
                ((Serializable)value).write(this);
                this.writeObjectEnd();
                return;
            }
            Serializer serializer = this.classToSerializer.get(actualType);
            if (serializer != null) {
                serializer.write(this, value, knownType);
                return;
            }
            if (value instanceof Array) {
                void var7_16;
                if (knownType != null && actualType != knownType && actualType != Array.class) {
                    throw new SerializationException("Serialization of an Array other than the known type is not supported.\nKnown type: " + knownType + "\nActual type: " + actualType);
                }
                this.writeArrayStart();
                Array array = (Array)value;
                boolean bl = false;
                int n = array.size;
                while (var7_16 < n) {
                    this.writeValue(array.get((int)var7_16), elementType, null);
                    ++var7_16;
                }
                this.writeArrayEnd();
                return;
            }
            if (value instanceof Queue) {
                void var7_18;
                if (knownType != null && actualType != knownType && actualType != Queue.class) {
                    throw new SerializationException("Serialization of a Queue other than the known type is not supported.\nKnown type: " + knownType + "\nActual type: " + actualType);
                }
                this.writeArrayStart();
                Queue queue = (Queue)value;
                boolean bl = false;
                int n = queue.size;
                while (var7_18 < n) {
                    this.writeValue(queue.get((int)var7_18), elementType, null);
                    ++var7_18;
                }
                this.writeArrayEnd();
                return;
            }
            if (value instanceof Collection) {
                if (this.typeName != null && actualType != ArrayList.class && (knownType == null || knownType != actualType)) {
                    this.writeObjectStart(actualType, knownType);
                    this.writeArrayStart("items");
                    for (Object e : (Collection)value) {
                        this.writeValue(e, elementType, null);
                    }
                    this.writeArrayEnd();
                    this.writeObjectEnd();
                } else {
                    this.writeArrayStart();
                    for (Object e : (Collection)value) {
                        this.writeValue(e, elementType, null);
                    }
                    this.writeArrayEnd();
                }
                return;
            }
            if (actualType.isArray()) {
                void var7_22;
                if (elementType == null) {
                    elementType = actualType.getComponentType();
                }
                int length = ArrayReflection.getLength(value);
                this.writeArrayStart();
                boolean bl = false;
                while (var7_22 < length) {
                    this.writeValue(ArrayReflection.get(value, (int)var7_22), elementType, null);
                    ++var7_22;
                }
                this.writeArrayEnd();
                return;
            }
            if (value instanceof ObjectMap) {
                if (knownType == null) {
                    knownType = ObjectMap.class;
                }
                this.writeObjectStart(actualType, knownType);
                for (ObjectMap.Entry entry : ((ObjectMap)value).entries()) {
                    this.writer.name(this.convertToString(entry.key));
                    this.writeValue(entry.value, elementType, null);
                }
                this.writeObjectEnd();
                return;
            }
            if (value instanceof ArrayMap) {
                void var7_25;
                if (knownType == null) {
                    knownType = ArrayMap.class;
                }
                this.writeObjectStart(actualType, knownType);
                ArrayMap map = (ArrayMap)value;
                boolean bl = false;
                int n = map.size;
                while (var7_25 < n) {
                    this.writer.name(this.convertToString(map.keys[var7_25]));
                    this.writeValue(map.values[var7_25], elementType, null);
                    ++var7_25;
                }
                this.writeObjectEnd();
                return;
            }
            if (value instanceof Map) {
                if (knownType == null) {
                    knownType = HashMap.class;
                }
                this.writeObjectStart(actualType, knownType);
                for (Map.Entry entry : ((Map)value).entrySet()) {
                    this.writer.name(this.convertToString(entry.getKey()));
                    this.writeValue(entry.getValue(), elementType, null);
                }
                this.writeObjectEnd();
                return;
            }
            if (ClassReflection.isAssignableFrom(Enum.class, actualType)) {
                if (this.typeName != null && (knownType == null || knownType != actualType)) {
                    if (actualType.getEnumConstants() == null) {
                        actualType = actualType.getSuperclass();
                    }
                    this.writeObjectStart(actualType, null);
                    this.writer.name("value");
                    this.writer.value(this.convertToString((Enum)value));
                    this.writeObjectEnd();
                } else {
                    this.writer.value(this.convertToString((Enum)value));
                }
                return;
            }
            this.writeObjectStart(actualType, knownType);
            this.writeFields(value);
            this.writeObjectEnd();
        }
        catch (IOException ex) {
            throw new SerializationException(ex);
        }
    }

    public void writeObjectStart(String name) {
        try {
            this.writer.name(name);
        }
        catch (IOException ex) {
            throw new SerializationException(ex);
        }
        this.writeObjectStart();
    }

    public void writeObjectStart(String name, Class actualType, Class knownType) {
        try {
            this.writer.name(name);
        }
        catch (IOException ex) {
            throw new SerializationException(ex);
        }
        this.writeObjectStart(actualType, knownType);
    }

    public void writeObjectStart() {
        try {
            this.writer.object();
        }
        catch (IOException ex) {
            throw new SerializationException(ex);
        }
    }

    public void writeObjectStart(Class actualType, Class knownType) {
        try {
            this.writer.object();
        }
        catch (IOException ex) {
            throw new SerializationException(ex);
        }
        if (knownType == null || knownType != actualType) {
            this.writeType(actualType);
        }
    }

    public void writeObjectEnd() {
        try {
            this.writer.pop();
        }
        catch (IOException ex) {
            throw new SerializationException(ex);
        }
    }

    public void writeArrayStart(String name) {
        try {
            this.writer.name(name);
            this.writer.array();
        }
        catch (IOException ex) {
            throw new SerializationException(ex);
        }
    }

    public void writeArrayStart() {
        try {
            this.writer.array();
        }
        catch (IOException ex) {
            throw new SerializationException(ex);
        }
    }

    public void writeArrayEnd() {
        try {
            this.writer.pop();
        }
        catch (IOException ex) {
            throw new SerializationException(ex);
        }
    }

    public void writeType(Class type) {
        if (this.typeName == null) {
            return;
        }
        String className = this.getTag(type);
        if (className == null) {
            className = type.getName();
        }
        try {
            this.writer.set(this.typeName, className);
        }
        catch (IOException ex) {
            throw new SerializationException(ex);
        }
    }

    public <T> T fromJson(Class<T> type, Reader reader) {
        return this.readValue(type, null, new JsonReader().parse(reader));
    }

    public <T> T fromJson(Class<T> type, Class elementType, Reader reader) {
        return this.readValue(type, elementType, new JsonReader().parse(reader));
    }

    public <T> T fromJson(Class<T> type, InputStream input) {
        return this.readValue(type, null, new JsonReader().parse(input));
    }

    public <T> T fromJson(Class<T> type, Class elementType, InputStream input) {
        return this.readValue(type, elementType, new JsonReader().parse(input));
    }

    public <T> T fromJson(Class<T> type, FileHandle file) {
        try {
            return this.readValue(type, null, new JsonReader().parse(file));
        }
        catch (Exception ex) {
            throw new SerializationException("Error reading file: " + file, ex);
        }
    }

    public <T> T fromJson(Class<T> type, Class elementType, FileHandle file) {
        try {
            return this.readValue(type, elementType, new JsonReader().parse(file));
        }
        catch (Exception ex) {
            throw new SerializationException("Error reading file: " + file, ex);
        }
    }

    public <T> T fromJson(Class<T> type, char[] data, int offset, int length) {
        return this.readValue(type, null, new JsonReader().parse(data, offset, length));
    }

    public <T> T fromJson(Class<T> type, Class elementType, char[] data, int offset, int length) {
        return this.readValue(type, elementType, new JsonReader().parse(data, offset, length));
    }

    public <T> T fromJson(Class<T> type, String json) {
        return this.readValue(type, null, new JsonReader().parse(json));
    }

    public <T> T fromJson(Class<T> type, Class elementType, String json) {
        return this.readValue(type, elementType, new JsonReader().parse(json));
    }

    public void readField(Object object, String name, JsonValue jsonData) {
        this.readField(object, name, name, null, jsonData);
    }

    public void readField(Object object, String name, Class elementType, JsonValue jsonData) {
        this.readField(object, name, name, elementType, jsonData);
    }

    public void readField(Object object, String fieldName, String jsonName, JsonValue jsonData) {
        this.readField(object, fieldName, jsonName, null, jsonData);
    }

    public void readField(Object object, String fieldName, String jsonName, Class elementType, JsonValue jsonMap) {
        Class<?> type = object.getClass();
        OrderedMap<String, FieldMetadata> fields = this.getFields(type);
        FieldMetadata metadata = (FieldMetadata)fields.get(fieldName);
        if (metadata == null) {
            throw new SerializationException("Field not found: " + fieldName + " (" + type.getName() + ")");
        }
        Field field = metadata.field;
        if (elementType == null) {
            elementType = metadata.elementType;
        }
        this.readField(object, field, jsonName, elementType, jsonMap);
    }

    public void readField(Object object, Field field, String jsonName, Class elementType, JsonValue jsonMap) {
        JsonValue jsonValue = jsonMap.get(jsonName);
        if (jsonValue == null) {
            return;
        }
        try {
            field.set(object, this.readValue(field.getType(), elementType, jsonValue));
        }
        catch (ReflectionException ex) {
            throw new SerializationException("Error accessing field: " + field.getName() + " (" + field.getDeclaringClass().getName() + ")", ex);
        }
        catch (SerializationException ex) {
            ex.addTrace(field.getName() + " (" + field.getDeclaringClass().getName() + ")");
            throw ex;
        }
        catch (RuntimeException runtimeEx) {
            SerializationException ex = new SerializationException(runtimeEx);
            ex.addTrace(jsonValue.trace());
            ex.addTrace(field.getName() + " (" + field.getDeclaringClass().getName() + ")");
            throw ex;
        }
    }

    public void readFields(Object object, JsonValue jsonMap) {
        Class<?> type = object.getClass();
        OrderedMap<String, FieldMetadata> fields = this.getFields(type);
        JsonValue child = jsonMap.child;
        while (child != null) {
            FieldMetadata metadata = (FieldMetadata)fields.get(child.name);
            if (metadata == null) {
                if (!child.name.equals(this.typeName) && !this.ignoreUnknownFields) {
                    SerializationException ex = new SerializationException("Field not found: " + child.name + " (" + type.getName() + ")");
                    ex.addTrace(child.trace());
                    throw ex;
                }
            } else {
                Field field = metadata.field;
                try {
                    field.set(object, this.readValue(field.getType(), metadata.elementType, child));
                }
                catch (ReflectionException ex) {
                    throw new SerializationException("Error accessing field: " + field.getName() + " (" + type.getName() + ")", ex);
                }
                catch (SerializationException ex) {
                    ex.addTrace(field.getName() + " (" + type.getName() + ")");
                    throw ex;
                }
                catch (RuntimeException runtimeEx) {
                    SerializationException ex = new SerializationException(runtimeEx);
                    ex.addTrace(child.trace());
                    ex.addTrace(field.getName() + " (" + type.getName() + ")");
                    throw ex;
                }
            }
            child = child.next;
        }
    }

    public <T> T readValue(String name, Class<T> type, JsonValue jsonMap) {
        return this.readValue(type, null, jsonMap.get(name));
    }

    public <T> T readValue(String name, Class<T> type, T defaultValue, JsonValue jsonMap) {
        JsonValue jsonValue = jsonMap.get(name);
        if (jsonValue == null) {
            return defaultValue;
        }
        return this.readValue(type, null, jsonValue);
    }

    public <T> T readValue(String name, Class<T> type, Class elementType, JsonValue jsonMap) {
        return this.readValue(type, elementType, jsonMap.get(name));
    }

    public <T> T readValue(String name, Class<T> type, Class elementType, T defaultValue, JsonValue jsonMap) {
        JsonValue jsonValue = jsonMap.get(name);
        return this.readValue(type, elementType, defaultValue, jsonValue);
    }

    public <T> T readValue(Class<T> type, Class elementType, T defaultValue, JsonValue jsonData) {
        if (jsonData == null) {
            return defaultValue;
        }
        return this.readValue(type, elementType, jsonData);
    }

    public <T> T readValue(Class<T> type, JsonValue jsonData) {
        return this.readValue(type, null, jsonData);
    }

    public <T> T readValue(Class<T> type, Class elementType, JsonValue jsonData) {
        Serializer serializer;
        if (jsonData == null) {
            return null;
        }
        if (jsonData.isObject()) {
            String className;
            String string = className = this.typeName == null ? null : jsonData.getString(this.typeName, null);
            if (className != null && (type = this.getClass(className)) == null) {
                try {
                    type = ClassReflection.forName(className);
                }
                catch (ReflectionException ex) {
                    throw new SerializationException(ex);
                }
            }
            if (type == null) {
                if (this.defaultSerializer != null) {
                    return this.defaultSerializer.read(this, jsonData, type);
                }
                return (T)jsonData;
            }
            if (this.typeName != null && ClassReflection.isAssignableFrom(Collection.class, type)) {
                jsonData = jsonData.get("items");
            } else {
                Serializer serializer2 = this.classToSerializer.get(type);
                if (serializer2 != null) {
                    return serializer2.read(this, jsonData, type);
                }
                if (type == String.class || type == Integer.class || type == Boolean.class || type == Float.class || type == Long.class || type == Double.class || type == Short.class || type == Byte.class || type == Character.class || ClassReflection.isAssignableFrom(Enum.class, type)) {
                    return this.readValue("value", type, jsonData);
                }
                Object object = this.newInstance(type);
                if (object instanceof Serializable) {
                    ((Serializable)object).read(this, jsonData);
                    return (T)object;
                }
                if (object instanceof ObjectMap) {
                    ObjectMap result = (ObjectMap)object;
                    JsonValue child = jsonData.child;
                    while (child != null) {
                        result.put(child.name, this.readValue(elementType, null, child));
                        child = child.next;
                    }
                    return (T)result;
                }
                if (object instanceof ArrayMap) {
                    ArrayMap result = (ArrayMap)object;
                    JsonValue child = jsonData.child;
                    while (child != null) {
                        result.put(child.name, this.readValue(elementType, null, child));
                        child = child.next;
                    }
                    return (T)result;
                }
                if (object instanceof Map) {
                    Map result = (Map)object;
                    JsonValue child = jsonData.child;
                    while (child != null) {
                        if (!child.name.equals(this.typeName)) {
                            result.put(child.name, this.readValue(elementType, null, child));
                        }
                        child = child.next;
                    }
                    return (T)result;
                }
                this.readFields(object, jsonData);
                return (T)object;
            }
        }
        if (type != null && (serializer = this.classToSerializer.get(type)) != null) {
            return serializer.read(this, jsonData, type);
        }
        if (jsonData.isArray()) {
            Iterable<Object> result;
            if (type == null || type == Object.class) {
                type = Array.class;
            }
            if (ClassReflection.isAssignableFrom(Array.class, type)) {
                result = type == Array.class ? new Array() : (Array)this.newInstance(type);
                JsonValue child = jsonData.child;
                while (child != null) {
                    ((Array)result).add(this.readValue(elementType, null, child));
                    child = child.next;
                }
                return (T)result;
            }
            if (ClassReflection.isAssignableFrom(Queue.class, type)) {
                result = type == Queue.class ? new Queue() : (Queue)this.newInstance(type);
                JsonValue child = jsonData.child;
                while (child != null) {
                    ((Queue)result).addLast(this.readValue(elementType, null, child));
                    child = child.next;
                }
                return (T)result;
            }
            if (ClassReflection.isAssignableFrom(Collection.class, type)) {
                result = type.isInterface() ? new ArrayList() : (Collection)this.newInstance(type);
                JsonValue child = jsonData.child;
                while (child != null) {
                    result.add(this.readValue(elementType, null, child));
                    child = child.next;
                }
                return (T)result;
            }
            if (type.isArray()) {
                Class<?> componentType = type.getComponentType();
                if (elementType == null) {
                    elementType = componentType;
                }
                Object result2 = ArrayReflection.newInstance(componentType, jsonData.size);
                int i = 0;
                JsonValue child = jsonData.child;
                while (child != null) {
                    ArrayReflection.set(result2, i++, this.readValue(elementType, null, child));
                    child = child.next;
                }
                return (T)result2;
            }
            throw new SerializationException("Unable to convert value to required type: " + jsonData + " (" + type.getName() + ")");
        }
        if (jsonData.isNumber()) {
            try {
                if (type == null || type == Float.TYPE || type == Float.class) {
                    return (T)Float.valueOf(jsonData.asFloat());
                }
                if (type == Integer.TYPE || type == Integer.class) {
                    return (T)Integer.valueOf(jsonData.asInt());
                }
                if (type == Long.TYPE || type == Long.class) {
                    return (T)Long.valueOf(jsonData.asLong());
                }
                if (type == Double.TYPE || type == Double.class) {
                    return (T)Double.valueOf(jsonData.asDouble());
                }
                if (type == String.class) {
                    return (T)jsonData.asString();
                }
                if (type == Short.TYPE || type == Short.class) {
                    return (T)Short.valueOf(jsonData.asShort());
                }
                if (type == Byte.TYPE || type == Byte.class) {
                    return (T)Byte.valueOf(jsonData.asByte());
                }
            }
            catch (NumberFormatException componentType) {
                // empty catch block
            }
            jsonData = new JsonValue(jsonData.asString());
        }
        if (jsonData.isBoolean()) {
            try {
                if (type == null || type == Boolean.TYPE || type == Boolean.class) {
                    return (T)Boolean.valueOf(jsonData.asBoolean());
                }
            }
            catch (NumberFormatException componentType) {
                // empty catch block
            }
            jsonData = new JsonValue(jsonData.asString());
        }
        if (jsonData.isString()) {
            String string = jsonData.asString();
            if (type == null || type == String.class) {
                return (T)string;
            }
            try {
                if (type == Integer.TYPE || type == Integer.class) {
                    return (T)Integer.valueOf(string);
                }
                if (type == Float.TYPE || type == Float.class) {
                    return (T)Float.valueOf(string);
                }
                if (type == Long.TYPE || type == Long.class) {
                    return (T)Long.valueOf(string);
                }
                if (type == Double.TYPE || type == Double.class) {
                    return (T)Double.valueOf(string);
                }
                if (type == Short.TYPE || type == Short.class) {
                    return (T)Short.valueOf(string);
                }
                if (type == Byte.TYPE || type == Byte.class) {
                    return (T)Byte.valueOf(string);
                }
            }
            catch (NumberFormatException result2) {
                // empty catch block
            }
            if (type == Boolean.TYPE || type == Boolean.class) {
                return (T)Boolean.valueOf(string);
            }
            if (type == Character.TYPE || type == Character.class) {
                return (T)Character.valueOf(string.charAt(0));
            }
            if (ClassReflection.isAssignableFrom(Enum.class, type)) {
                for (Enum e : (Enum[])type.getEnumConstants()) {
                    if (!string.equals(this.convertToString(e))) continue;
                    return (T)e;
                }
            }
            if (type == CharSequence.class) {
                return (T)string;
            }
            throw new SerializationException("Unable to convert value to required type: " + jsonData + " (" + type.getName() + ")");
        }
        return null;
    }

    private String convertToString(Enum e) {
        return this.enumNames ? e.name() : e.toString();
    }

    private String convertToString(Object object) {
        if (object instanceof Enum) {
            return this.convertToString((Enum)object);
        }
        if (object instanceof Class) {
            return ((Class)object).getName();
        }
        return String.valueOf(object);
    }

    protected Object newInstance(Class type) {
        try {
            return ClassReflection.newInstance(type);
        }
        catch (Exception ex) {
            try {
                Constructor constructor = ClassReflection.getDeclaredConstructor(type, new Class[0]);
                constructor.setAccessible(true);
                return constructor.newInstance(new Object[0]);
            }
            catch (SecurityException constructor) {
            }
            catch (ReflectionException ignored) {
                if (ClassReflection.isAssignableFrom(Enum.class, type)) {
                    if (type.getEnumConstants() == null) {
                        type = type.getSuperclass();
                    }
                    return type.getEnumConstants()[0];
                }
                if (type.isArray()) {
                    throw new SerializationException("Encountered JSON object when expected array of type: " + type.getName(), ex);
                }
                if (ClassReflection.isMemberClass(type) && !ClassReflection.isStaticClass(type)) {
                    throw new SerializationException("Class cannot be created (non-static member class): " + type.getName(), ex);
                }
                throw new SerializationException("Class cannot be created (missing no-arg constructor): " + type.getName(), ex);
            }
            catch (Exception privateConstructorException) {
                ex = privateConstructorException;
            }
            throw new SerializationException("Error constructing instance of class: " + type.getName(), ex);
        }
    }

    public String prettyPrint(Object object) {
        return this.prettyPrint(object, 0);
    }

    public String prettyPrint(String json) {
        return this.prettyPrint(json, 0);
    }

    public String prettyPrint(Object object, int singleLineColumns) {
        return this.prettyPrint(this.toJson(object), singleLineColumns);
    }

    public String prettyPrint(String json, int singleLineColumns) {
        return new JsonReader().parse(json).prettyPrint(this.outputType, singleLineColumns);
    }

    public String prettyPrint(Object object, JsonValue.PrettyPrintSettings settings) {
        return this.prettyPrint(this.toJson(object), settings);
    }

    public String prettyPrint(String json, JsonValue.PrettyPrintSettings settings) {
        return new JsonReader().parse(json).prettyPrint(settings);
    }

    public static interface Serializable {
        public void write(Json var1);

        public void read(Json var1, JsonValue var2);
    }

    public static abstract class ReadOnlySerializer<T>
    implements Serializer<T> {
        @Override
        public void write(Json json, T object, Class knownType) {
        }

        @Override
        public abstract T read(Json var1, JsonValue var2, Class var3);
    }

    public static interface Serializer<T> {
        public void write(Json var1, T var2, Class var3);

        public T read(Json var1, JsonValue var2, Class var3);
    }

    private static class FieldMetadata {
        Field field;
        Class elementType;

        public FieldMetadata(Field field) {
            this.field = field;
            int index = ClassReflection.isAssignableFrom(ObjectMap.class, field.getType()) || ClassReflection.isAssignableFrom(Map.class, field.getType()) ? 1 : 0;
            this.elementType = field.getElementType(index);
        }
    }
}

