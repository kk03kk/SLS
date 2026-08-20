/*
 * Decompiled with CFR 0.152.
 */
package com.badlogic.gdx.utils;

import com.badlogic.gdx.utils.JsonWriter;
import com.badlogic.gdx.utils.SerializationException;
import com.badlogic.gdx.utils.StringBuilder;
import java.util.Iterator;
import java.util.NoSuchElementException;

public class JsonValue
implements Iterable<JsonValue> {
    private ValueType type;
    private String stringValue;
    private double doubleValue;
    private long longValue;
    public String name;
    public JsonValue child;
    public JsonValue next;
    public JsonValue prev;
    public JsonValue parent;
    public int size;

    public JsonValue(ValueType type) {
        this.type = type;
    }

    public JsonValue(String value) {
        this.set(value);
    }

    public JsonValue(double value) {
        this.set(value, null);
    }

    public JsonValue(long value) {
        this.set(value, (String)null);
    }

    public JsonValue(double value, String stringValue) {
        this.set(value, stringValue);
    }

    public JsonValue(long value, String stringValue) {
        this.set(value, stringValue);
    }

    public JsonValue(boolean value) {
        this.set(value);
    }

    public JsonValue get(int index) {
        JsonValue current = this.child;
        while (current != null && index > 0) {
            --index;
            current = current.next;
        }
        return current;
    }

    public JsonValue get(String name) {
        JsonValue current = this.child;
        while (current != null && !current.name.equalsIgnoreCase(name)) {
            current = current.next;
        }
        return current;
    }

    public boolean has(String name) {
        return this.get(name) != null;
    }

    public JsonValue require(int index) {
        JsonValue current = this.child;
        while (current != null && index > 0) {
            --index;
            current = current.next;
        }
        if (current == null) {
            throw new IllegalArgumentException("Child not found with index: " + index);
        }
        return current;
    }

    public JsonValue require(String name) {
        JsonValue current = this.child;
        while (current != null && !current.name.equalsIgnoreCase(name)) {
            current = current.next;
        }
        if (current == null) {
            throw new IllegalArgumentException("Child not found with name: " + name);
        }
        return current;
    }

    public JsonValue remove(int index) {
        JsonValue child = this.get(index);
        if (child == null) {
            return null;
        }
        if (child.prev == null) {
            this.child = child.next;
            if (this.child != null) {
                this.child.prev = null;
            }
        } else {
            child.prev.next = child.next;
            if (child.next != null) {
                child.next.prev = child.prev;
            }
        }
        --this.size;
        return child;
    }

    public JsonValue remove(String name) {
        JsonValue child = this.get(name);
        if (child == null) {
            return null;
        }
        if (child.prev == null) {
            this.child = child.next;
            if (this.child != null) {
                this.child.prev = null;
            }
        } else {
            child.prev.next = child.next;
            if (child.next != null) {
                child.next.prev = child.prev;
            }
        }
        --this.size;
        return child;
    }

    @Deprecated
    public int size() {
        return this.size;
    }

    public String asString() {
        switch (this.type) {
            case stringValue: {
                return this.stringValue;
            }
            case doubleValue: {
                return this.stringValue != null ? this.stringValue : Double.toString(this.doubleValue);
            }
            case longValue: {
                return this.stringValue != null ? this.stringValue : Long.toString(this.longValue);
            }
            case booleanValue: {
                return this.longValue != 0L ? "true" : "false";
            }
            case nullValue: {
                return null;
            }
        }
        throw new IllegalStateException("Value cannot be converted to string: " + (Object)((Object)this.type));
    }

    public float asFloat() {
        switch (this.type) {
            case stringValue: {
                return Float.parseFloat(this.stringValue);
            }
            case doubleValue: {
                return (float)this.doubleValue;
            }
            case longValue: {
                return this.longValue;
            }
            case booleanValue: {
                return this.longValue != 0L ? 1.0f : 0.0f;
            }
        }
        throw new IllegalStateException("Value cannot be converted to float: " + (Object)((Object)this.type));
    }

    public double asDouble() {
        switch (this.type) {
            case stringValue: {
                return Double.parseDouble(this.stringValue);
            }
            case doubleValue: {
                return this.doubleValue;
            }
            case longValue: {
                return this.longValue;
            }
            case booleanValue: {
                return this.longValue != 0L ? 1.0 : 0.0;
            }
        }
        throw new IllegalStateException("Value cannot be converted to double: " + (Object)((Object)this.type));
    }

    public long asLong() {
        switch (this.type) {
            case stringValue: {
                return Long.parseLong(this.stringValue);
            }
            case doubleValue: {
                return (long)this.doubleValue;
            }
            case longValue: {
                return this.longValue;
            }
            case booleanValue: {
                return this.longValue != 0L ? 1L : 0L;
            }
        }
        throw new IllegalStateException("Value cannot be converted to long: " + (Object)((Object)this.type));
    }

    public int asInt() {
        switch (this.type) {
            case stringValue: {
                return Integer.parseInt(this.stringValue);
            }
            case doubleValue: {
                return (int)this.doubleValue;
            }
            case longValue: {
                return (int)this.longValue;
            }
            case booleanValue: {
                return this.longValue != 0L ? 1 : 0;
            }
        }
        throw new IllegalStateException("Value cannot be converted to int: " + (Object)((Object)this.type));
    }

    public boolean asBoolean() {
        switch (this.type) {
            case stringValue: {
                return this.stringValue.equalsIgnoreCase("true");
            }
            case doubleValue: {
                return this.doubleValue != 0.0;
            }
            case longValue: {
                return this.longValue != 0L;
            }
            case booleanValue: {
                return this.longValue != 0L;
            }
        }
        throw new IllegalStateException("Value cannot be converted to boolean: " + (Object)((Object)this.type));
    }

    public byte asByte() {
        switch (this.type) {
            case stringValue: {
                return Byte.parseByte(this.stringValue);
            }
            case doubleValue: {
                return (byte)this.doubleValue;
            }
            case longValue: {
                return (byte)this.longValue;
            }
            case booleanValue: {
                return this.longValue != 0L ? (byte)1 : 0;
            }
        }
        throw new IllegalStateException("Value cannot be converted to byte: " + (Object)((Object)this.type));
    }

    public short asShort() {
        switch (this.type) {
            case stringValue: {
                return Short.parseShort(this.stringValue);
            }
            case doubleValue: {
                return (short)this.doubleValue;
            }
            case longValue: {
                return (short)this.longValue;
            }
            case booleanValue: {
                return this.longValue != 0L ? (short)1 : 0;
            }
        }
        throw new IllegalStateException("Value cannot be converted to short: " + (Object)((Object)this.type));
    }

    public char asChar() {
        switch (this.type) {
            case stringValue: {
                return this.stringValue.length() == 0 ? (char)'\u0000' : this.stringValue.charAt(0);
            }
            case doubleValue: {
                return (char)this.doubleValue;
            }
            case longValue: {
                return (char)this.longValue;
            }
            case booleanValue: {
                return this.longValue != 0L ? (char)'\u0001' : '\u0000';
            }
        }
        throw new IllegalStateException("Value cannot be converted to char: " + (Object)((Object)this.type));
    }

    public String[] asStringArray() {
        if (this.type != ValueType.array) {
            throw new IllegalStateException("Value is not an array: " + (Object)((Object)this.type));
        }
        String[] array = new String[this.size];
        int i = 0;
        JsonValue value = this.child;
        while (value != null) {
            String v;
            switch (value.type) {
                case stringValue: {
                    v = value.stringValue;
                    break;
                }
                case doubleValue: {
                    v = this.stringValue != null ? this.stringValue : Double.toString(value.doubleValue);
                    break;
                }
                case longValue: {
                    v = this.stringValue != null ? this.stringValue : Long.toString(value.longValue);
                    break;
                }
                case booleanValue: {
                    v = value.longValue != 0L ? "true" : "false";
                    break;
                }
                case nullValue: {
                    v = null;
                    break;
                }
                default: {
                    throw new IllegalStateException("Value cannot be converted to string: " + (Object)((Object)value.type));
                }
            }
            array[i] = v;
            value = value.next;
            ++i;
        }
        return array;
    }

    public float[] asFloatArray() {
        if (this.type != ValueType.array) {
            throw new IllegalStateException("Value is not an array: " + (Object)((Object)this.type));
        }
        float[] array = new float[this.size];
        int i = 0;
        JsonValue value = this.child;
        while (value != null) {
            float v;
            switch (value.type) {
                case stringValue: {
                    v = Float.parseFloat(value.stringValue);
                    break;
                }
                case doubleValue: {
                    v = (float)value.doubleValue;
                    break;
                }
                case longValue: {
                    v = value.longValue;
                    break;
                }
                case booleanValue: {
                    v = value.longValue != 0L ? 1.0f : 0.0f;
                    break;
                }
                default: {
                    throw new IllegalStateException("Value cannot be converted to float: " + (Object)((Object)value.type));
                }
            }
            array[i] = v;
            value = value.next;
            ++i;
        }
        return array;
    }

    public double[] asDoubleArray() {
        if (this.type != ValueType.array) {
            throw new IllegalStateException("Value is not an array: " + (Object)((Object)this.type));
        }
        double[] array = new double[this.size];
        int i = 0;
        JsonValue value = this.child;
        while (value != null) {
            double v;
            switch (value.type) {
                case stringValue: {
                    v = Double.parseDouble(value.stringValue);
                    break;
                }
                case doubleValue: {
                    v = value.doubleValue;
                    break;
                }
                case longValue: {
                    v = value.longValue;
                    break;
                }
                case booleanValue: {
                    v = value.longValue != 0L ? 1.0 : 0.0;
                    break;
                }
                default: {
                    throw new IllegalStateException("Value cannot be converted to double: " + (Object)((Object)value.type));
                }
            }
            array[i] = v;
            value = value.next;
            ++i;
        }
        return array;
    }

    public long[] asLongArray() {
        if (this.type != ValueType.array) {
            throw new IllegalStateException("Value is not an array: " + (Object)((Object)this.type));
        }
        long[] array = new long[this.size];
        int i = 0;
        JsonValue value = this.child;
        while (value != null) {
            long v;
            switch (value.type) {
                case stringValue: {
                    v = Long.parseLong(value.stringValue);
                    break;
                }
                case doubleValue: {
                    v = (long)value.doubleValue;
                    break;
                }
                case longValue: {
                    v = value.longValue;
                    break;
                }
                case booleanValue: {
                    v = value.longValue != 0L ? 1L : 0L;
                    break;
                }
                default: {
                    throw new IllegalStateException("Value cannot be converted to long: " + (Object)((Object)value.type));
                }
            }
            array[i] = v;
            value = value.next;
            ++i;
        }
        return array;
    }

    public int[] asIntArray() {
        if (this.type != ValueType.array) {
            throw new IllegalStateException("Value is not an array: " + (Object)((Object)this.type));
        }
        int[] array = new int[this.size];
        int i = 0;
        JsonValue value = this.child;
        while (value != null) {
            int v;
            switch (value.type) {
                case stringValue: {
                    v = Integer.parseInt(value.stringValue);
                    break;
                }
                case doubleValue: {
                    v = (int)value.doubleValue;
                    break;
                }
                case longValue: {
                    v = (int)value.longValue;
                    break;
                }
                case booleanValue: {
                    v = value.longValue != 0L ? 1 : 0;
                    break;
                }
                default: {
                    throw new IllegalStateException("Value cannot be converted to int: " + (Object)((Object)value.type));
                }
            }
            array[i] = v;
            value = value.next;
            ++i;
        }
        return array;
    }

    public boolean[] asBooleanArray() {
        if (this.type != ValueType.array) {
            throw new IllegalStateException("Value is not an array: " + (Object)((Object)this.type));
        }
        boolean[] array = new boolean[this.size];
        int i = 0;
        JsonValue value = this.child;
        while (value != null) {
            boolean v;
            switch (value.type) {
                case stringValue: {
                    v = Boolean.parseBoolean(value.stringValue);
                    break;
                }
                case doubleValue: {
                    v = value.doubleValue == 0.0;
                    break;
                }
                case longValue: {
                    v = value.longValue == 0L;
                    break;
                }
                case booleanValue: {
                    v = value.longValue != 0L;
                    break;
                }
                default: {
                    throw new IllegalStateException("Value cannot be converted to boolean: " + (Object)((Object)value.type));
                }
            }
            array[i] = v;
            value = value.next;
            ++i;
        }
        return array;
    }

    public byte[] asByteArray() {
        if (this.type != ValueType.array) {
            throw new IllegalStateException("Value is not an array: " + (Object)((Object)this.type));
        }
        byte[] array = new byte[this.size];
        int i = 0;
        JsonValue value = this.child;
        while (value != null) {
            byte v;
            switch (value.type) {
                case stringValue: {
                    v = Byte.parseByte(value.stringValue);
                    break;
                }
                case doubleValue: {
                    v = (byte)value.doubleValue;
                    break;
                }
                case longValue: {
                    v = (byte)value.longValue;
                    break;
                }
                case booleanValue: {
                    v = value.longValue != 0L ? (byte)1 : 0;
                    break;
                }
                default: {
                    throw new IllegalStateException("Value cannot be converted to byte: " + (Object)((Object)value.type));
                }
            }
            array[i] = v;
            value = value.next;
            ++i;
        }
        return array;
    }

    public short[] asShortArray() {
        if (this.type != ValueType.array) {
            throw new IllegalStateException("Value is not an array: " + (Object)((Object)this.type));
        }
        short[] array = new short[this.size];
        int i = 0;
        JsonValue value = this.child;
        while (value != null) {
            short v;
            switch (value.type) {
                case stringValue: {
                    v = Short.parseShort(value.stringValue);
                    break;
                }
                case doubleValue: {
                    v = (short)value.doubleValue;
                    break;
                }
                case longValue: {
                    v = (short)value.longValue;
                    break;
                }
                case booleanValue: {
                    v = value.longValue != 0L ? (short)1 : 0;
                    break;
                }
                default: {
                    throw new IllegalStateException("Value cannot be converted to short: " + (Object)((Object)value.type));
                }
            }
            array[i] = v;
            value = value.next;
            ++i;
        }
        return array;
    }

    public char[] asCharArray() {
        if (this.type != ValueType.array) {
            throw new IllegalStateException("Value is not an array: " + (Object)((Object)this.type));
        }
        char[] array = new char[this.size];
        int i = 0;
        JsonValue value = this.child;
        while (value != null) {
            char v;
            switch (value.type) {
                case stringValue: {
                    v = value.stringValue.length() == 0 ? (char)'\u0000' : value.stringValue.charAt(0);
                    break;
                }
                case doubleValue: {
                    v = (char)value.doubleValue;
                    break;
                }
                case longValue: {
                    v = (char)value.longValue;
                    break;
                }
                case booleanValue: {
                    v = value.longValue != 0L ? (char)'\u0001' : '\u0000';
                    break;
                }
                default: {
                    throw new IllegalStateException("Value cannot be converted to char: " + (Object)((Object)value.type));
                }
            }
            array[i] = v;
            value = value.next;
            ++i;
        }
        return array;
    }

    public boolean hasChild(String name) {
        return this.getChild(name) != null;
    }

    public JsonValue getChild(String name) {
        JsonValue child = this.get(name);
        return child == null ? null : child.child;
    }

    public String getString(String name, String defaultValue) {
        JsonValue child = this.get(name);
        return child == null || !child.isValue() || child.isNull() ? defaultValue : child.asString();
    }

    public float getFloat(String name, float defaultValue) {
        JsonValue child = this.get(name);
        return child == null || !child.isValue() ? defaultValue : child.asFloat();
    }

    public double getDouble(String name, double defaultValue) {
        JsonValue child = this.get(name);
        return child == null || !child.isValue() ? defaultValue : child.asDouble();
    }

    public long getLong(String name, long defaultValue) {
        JsonValue child = this.get(name);
        return child == null || !child.isValue() ? defaultValue : child.asLong();
    }

    public int getInt(String name, int defaultValue) {
        JsonValue child = this.get(name);
        return child == null || !child.isValue() ? defaultValue : child.asInt();
    }

    public boolean getBoolean(String name, boolean defaultValue) {
        JsonValue child = this.get(name);
        return child == null || !child.isValue() ? defaultValue : child.asBoolean();
    }

    public byte getByte(String name, byte defaultValue) {
        JsonValue child = this.get(name);
        return child == null || !child.isValue() ? defaultValue : child.asByte();
    }

    public short getShort(String name, short defaultValue) {
        JsonValue child = this.get(name);
        return child == null || !child.isValue() ? defaultValue : child.asShort();
    }

    public char getChar(String name, char defaultValue) {
        JsonValue child = this.get(name);
        return child == null || !child.isValue() ? defaultValue : child.asChar();
    }

    public String getString(String name) {
        JsonValue child = this.get(name);
        if (child == null) {
            throw new IllegalArgumentException("Named value not found: " + name);
        }
        return child.asString();
    }

    public float getFloat(String name) {
        JsonValue child = this.get(name);
        if (child == null) {
            throw new IllegalArgumentException("Named value not found: " + name);
        }
        return child.asFloat();
    }

    public double getDouble(String name) {
        JsonValue child = this.get(name);
        if (child == null) {
            throw new IllegalArgumentException("Named value not found: " + name);
        }
        return child.asDouble();
    }

    public long getLong(String name) {
        JsonValue child = this.get(name);
        if (child == null) {
            throw new IllegalArgumentException("Named value not found: " + name);
        }
        return child.asLong();
    }

    public int getInt(String name) {
        JsonValue child = this.get(name);
        if (child == null) {
            throw new IllegalArgumentException("Named value not found: " + name);
        }
        return child.asInt();
    }

    public boolean getBoolean(String name) {
        JsonValue child = this.get(name);
        if (child == null) {
            throw new IllegalArgumentException("Named value not found: " + name);
        }
        return child.asBoolean();
    }

    public byte getByte(String name) {
        JsonValue child = this.get(name);
        if (child == null) {
            throw new IllegalArgumentException("Named value not found: " + name);
        }
        return child.asByte();
    }

    public short getShort(String name) {
        JsonValue child = this.get(name);
        if (child == null) {
            throw new IllegalArgumentException("Named value not found: " + name);
        }
        return child.asShort();
    }

    public char getChar(String name) {
        JsonValue child = this.get(name);
        if (child == null) {
            throw new IllegalArgumentException("Named value not found: " + name);
        }
        return child.asChar();
    }

    public String getString(int index) {
        JsonValue child = this.get(index);
        if (child == null) {
            throw new IllegalArgumentException("Indexed value not found: " + this.name);
        }
        return child.asString();
    }

    public float getFloat(int index) {
        JsonValue child = this.get(index);
        if (child == null) {
            throw new IllegalArgumentException("Indexed value not found: " + this.name);
        }
        return child.asFloat();
    }

    public double getDouble(int index) {
        JsonValue child = this.get(index);
        if (child == null) {
            throw new IllegalArgumentException("Indexed value not found: " + this.name);
        }
        return child.asDouble();
    }

    public long getLong(int index) {
        JsonValue child = this.get(index);
        if (child == null) {
            throw new IllegalArgumentException("Indexed value not found: " + this.name);
        }
        return child.asLong();
    }

    public int getInt(int index) {
        JsonValue child = this.get(index);
        if (child == null) {
            throw new IllegalArgumentException("Indexed value not found: " + this.name);
        }
        return child.asInt();
    }

    public boolean getBoolean(int index) {
        JsonValue child = this.get(index);
        if (child == null) {
            throw new IllegalArgumentException("Indexed value not found: " + this.name);
        }
        return child.asBoolean();
    }

    public byte getByte(int index) {
        JsonValue child = this.get(index);
        if (child == null) {
            throw new IllegalArgumentException("Indexed value not found: " + this.name);
        }
        return child.asByte();
    }

    public short getShort(int index) {
        JsonValue child = this.get(index);
        if (child == null) {
            throw new IllegalArgumentException("Indexed value not found: " + this.name);
        }
        return child.asShort();
    }

    public char getChar(int index) {
        JsonValue child = this.get(index);
        if (child == null) {
            throw new IllegalArgumentException("Indexed value not found: " + this.name);
        }
        return child.asChar();
    }

    public ValueType type() {
        return this.type;
    }

    public void setType(ValueType type) {
        if (type == null) {
            throw new IllegalArgumentException("type cannot be null.");
        }
        this.type = type;
    }

    public boolean isArray() {
        return this.type == ValueType.array;
    }

    public boolean isObject() {
        return this.type == ValueType.object;
    }

    public boolean isString() {
        return this.type == ValueType.stringValue;
    }

    public boolean isNumber() {
        return this.type == ValueType.doubleValue || this.type == ValueType.longValue;
    }

    public boolean isDouble() {
        return this.type == ValueType.doubleValue;
    }

    public boolean isLong() {
        return this.type == ValueType.longValue;
    }

    public boolean isBoolean() {
        return this.type == ValueType.booleanValue;
    }

    public boolean isNull() {
        return this.type == ValueType.nullValue;
    }

    public boolean isValue() {
        switch (this.type) {
            case stringValue: 
            case doubleValue: 
            case longValue: 
            case booleanValue: 
            case nullValue: {
                return true;
            }
        }
        return false;
    }

    public String name() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public JsonValue parent() {
        return this.parent;
    }

    public JsonValue child() {
        return this.child;
    }

    public void addChild(String name, JsonValue value) {
        value.name = name;
        this.addChild(value);
    }

    public void addChild(JsonValue value) {
        value.parent = this;
        JsonValue current = this.child;
        if (current != null) {
            while (true) {
                if (current.next == null) {
                    current.next = value;
                    return;
                }
                current = current.next;
            }
        }
        this.child = value;
    }

    public JsonValue next() {
        return this.next;
    }

    public void setNext(JsonValue next) {
        this.next = next;
    }

    public JsonValue prev() {
        return this.prev;
    }

    public void setPrev(JsonValue prev) {
        this.prev = prev;
    }

    public void set(String value) {
        this.stringValue = value;
        this.type = value == null ? ValueType.nullValue : ValueType.stringValue;
    }

    public void set(double value, String stringValue) {
        this.doubleValue = value;
        this.longValue = (long)value;
        this.stringValue = stringValue;
        this.type = ValueType.doubleValue;
    }

    public void set(long value, String stringValue) {
        this.longValue = value;
        this.doubleValue = value;
        this.stringValue = stringValue;
        this.type = ValueType.longValue;
    }

    public void set(boolean value) {
        this.longValue = value ? 1L : 0L;
        this.type = ValueType.booleanValue;
    }

    public String toJson(JsonWriter.OutputType outputType) {
        if (this.isValue()) {
            return this.asString();
        }
        StringBuilder buffer = new StringBuilder(512);
        this.json(this, buffer, outputType);
        return buffer.toString();
    }

    private void json(JsonValue object, StringBuilder buffer, JsonWriter.OutputType outputType) {
        if (object.isObject()) {
            if (object.child == null) {
                buffer.append("{}");
            } else {
                int start = buffer.length();
                buffer.append('{');
                boolean i = false;
                JsonValue child = object.child;
                while (child != null) {
                    buffer.append(outputType.quoteName(child.name));
                    buffer.append(':');
                    this.json(child, buffer, outputType);
                    if (child.next != null) {
                        buffer.append(',');
                    }
                    child = child.next;
                }
                buffer.append('}');
            }
        } else if (object.isArray()) {
            if (object.child == null) {
                buffer.append("[]");
            } else {
                int start = buffer.length();
                buffer.append('[');
                JsonValue child = object.child;
                while (child != null) {
                    this.json(child, buffer, outputType);
                    if (child.next != null) {
                        buffer.append(',');
                    }
                    child = child.next;
                }
                buffer.append(']');
            }
        } else if (object.isString()) {
            buffer.append(outputType.quoteValue(object.asString()));
        } else if (object.isDouble()) {
            long longValue;
            double doubleValue = object.asDouble();
            buffer.append(doubleValue == (double)(longValue = object.asLong()) ? (double)longValue : doubleValue);
        } else if (object.isLong()) {
            buffer.append(object.asLong());
        } else if (object.isBoolean()) {
            buffer.append(object.asBoolean());
        } else if (object.isNull()) {
            buffer.append("null");
        } else {
            throw new SerializationException("Unknown object type: " + object);
        }
    }

    public String toString() {
        if (this.isValue()) {
            return this.name == null ? this.asString() : this.name + ": " + this.asString();
        }
        return (this.name == null ? "" : this.name + ": ") + this.prettyPrint(JsonWriter.OutputType.minimal, 0);
    }

    public String prettyPrint(JsonWriter.OutputType outputType, int singleLineColumns) {
        PrettyPrintSettings settings = new PrettyPrintSettings();
        settings.outputType = outputType;
        settings.singleLineColumns = singleLineColumns;
        return this.prettyPrint(settings);
    }

    public String prettyPrint(PrettyPrintSettings settings) {
        StringBuilder buffer = new StringBuilder(512);
        this.prettyPrint(this, buffer, 0, settings);
        return buffer.toString();
    }

    private void prettyPrint(JsonValue object, StringBuilder buffer, int indent, PrettyPrintSettings settings) {
        JsonWriter.OutputType outputType = settings.outputType;
        if (object.isObject()) {
            if (object.child == null) {
                buffer.append("{}");
            } else {
                boolean newLines = !JsonValue.isFlat(object);
                int start = buffer.length();
                block0: while (true) {
                    buffer.append(newLines ? "{\n" : "{ ");
                    boolean i = false;
                    JsonValue child = object.child;
                    while (child != null) {
                        if (newLines) {
                            JsonValue.indent(indent, buffer);
                        }
                        buffer.append(outputType.quoteName(child.name));
                        buffer.append(": ");
                        this.prettyPrint(child, buffer, indent + 1, settings);
                        if (!(newLines && outputType == JsonWriter.OutputType.minimal || child.next == null)) {
                            buffer.append(',');
                        }
                        buffer.append(newLines ? (char)'\n' : ' ');
                        if (!newLines && buffer.length() - start > settings.singleLineColumns) {
                            buffer.setLength(start);
                            newLines = true;
                            continue block0;
                        }
                        child = child.next;
                    }
                    break;
                }
                if (newLines) {
                    JsonValue.indent(indent - 1, buffer);
                }
                buffer.append('}');
            }
        } else if (object.isArray()) {
            if (object.child == null) {
                buffer.append("[]");
            } else {
                boolean newLines = !JsonValue.isFlat(object);
                boolean wrap = settings.wrapNumericArrays || !JsonValue.isNumeric(object);
                int start = buffer.length();
                block2: while (true) {
                    buffer.append(newLines ? "[\n" : "[ ");
                    JsonValue child = object.child;
                    while (child != null) {
                        if (newLines) {
                            JsonValue.indent(indent, buffer);
                        }
                        this.prettyPrint(child, buffer, indent + 1, settings);
                        if (!(newLines && outputType == JsonWriter.OutputType.minimal || child.next == null)) {
                            buffer.append(',');
                        }
                        buffer.append(newLines ? (char)'\n' : ' ');
                        if (wrap && !newLines && buffer.length() - start > settings.singleLineColumns) {
                            buffer.setLength(start);
                            newLines = true;
                            continue block2;
                        }
                        child = child.next;
                    }
                    break;
                }
                if (newLines) {
                    JsonValue.indent(indent - 1, buffer);
                }
                buffer.append(']');
            }
        } else if (object.isString()) {
            buffer.append(outputType.quoteValue(object.asString()));
        } else if (object.isDouble()) {
            long longValue;
            double doubleValue = object.asDouble();
            buffer.append(doubleValue == (double)(longValue = object.asLong()) ? (double)longValue : doubleValue);
        } else if (object.isLong()) {
            buffer.append(object.asLong());
        } else if (object.isBoolean()) {
            buffer.append(object.asBoolean());
        } else if (object.isNull()) {
            buffer.append("null");
        } else {
            throw new SerializationException("Unknown object type: " + object);
        }
    }

    private static boolean isFlat(JsonValue object) {
        JsonValue child = object.child;
        while (child != null) {
            if (child.isObject() || child.isArray()) {
                return false;
            }
            child = child.next;
        }
        return true;
    }

    private static boolean isNumeric(JsonValue object) {
        JsonValue child = object.child;
        while (child != null) {
            if (!child.isNumber()) {
                return false;
            }
            child = child.next;
        }
        return true;
    }

    private static void indent(int count, StringBuilder buffer) {
        for (int i = 0; i < count; ++i) {
            buffer.append('\t');
        }
    }

    public JsonIterator iterator() {
        return new JsonIterator();
    }

    public String trace() {
        String trace;
        if (this.parent == null) {
            if (this.type == ValueType.array) {
                return "[]";
            }
            if (this.type == ValueType.object) {
                return "{}";
            }
            return "";
        }
        if (this.parent.type == ValueType.array) {
            trace = "[]";
            int i = 0;
            JsonValue child = this.parent.child;
            while (child != null) {
                if (child == this) {
                    trace = "[" + i + "]";
                    break;
                }
                child = child.next;
                ++i;
            }
        } else {
            trace = this.name.indexOf(46) != -1 ? ".\"" + this.name.replace("\"", "\\\"") + "\"" : '.' + this.name;
        }
        return this.parent.trace() + trace;
    }

    public static class PrettyPrintSettings {
        public JsonWriter.OutputType outputType;
        public int singleLineColumns;
        public boolean wrapNumericArrays;
    }

    public class JsonIterator
    implements Iterator<JsonValue>,
    Iterable<JsonValue> {
        JsonValue entry;
        JsonValue current;

        public JsonIterator() {
            this.entry = JsonValue.this.child;
        }

        @Override
        public boolean hasNext() {
            return this.entry != null;
        }

        @Override
        public JsonValue next() {
            this.current = this.entry;
            if (this.current == null) {
                throw new NoSuchElementException();
            }
            this.entry = this.current.next;
            return this.current;
        }

        @Override
        public void remove() {
            if (this.current.prev == null) {
                JsonValue.this.child = this.current.next;
                if (JsonValue.this.child != null) {
                    JsonValue.this.child.prev = null;
                }
            } else {
                this.current.prev.next = this.current.next;
                if (this.current.next != null) {
                    this.current.next.prev = this.current.prev;
                }
            }
            --JsonValue.this.size;
        }

        @Override
        public Iterator<JsonValue> iterator() {
            return this;
        }
    }

    public static enum ValueType {
        object,
        array,
        stringValue,
        doubleValue,
        longValue,
        booleanValue,
        nullValue;

    }
}

