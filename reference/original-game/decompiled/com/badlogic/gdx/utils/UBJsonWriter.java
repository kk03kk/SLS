/*
 * Decompiled with CFR 0.152.
 */
package com.badlogic.gdx.utils;

import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.JsonValue;
import java.io.Closeable;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public class UBJsonWriter
implements Closeable {
    final DataOutputStream out;
    private JsonObject current;
    private boolean named;
    private final Array<JsonObject> stack = new Array();

    public UBJsonWriter(OutputStream out) {
        if (!(out instanceof DataOutputStream)) {
            out = new DataOutputStream(out);
        }
        this.out = (DataOutputStream)out;
    }

    public UBJsonWriter object() throws IOException {
        if (this.current != null && !this.current.array) {
            if (!this.named) {
                throw new IllegalStateException("Name must be set.");
            }
            this.named = false;
        }
        this.current = new JsonObject(false);
        this.stack.add(this.current);
        return this;
    }

    public UBJsonWriter object(String name) throws IOException {
        this.name(name).object();
        return this;
    }

    public UBJsonWriter array() throws IOException {
        if (this.current != null && !this.current.array) {
            if (!this.named) {
                throw new IllegalStateException("Name must be set.");
            }
            this.named = false;
        }
        this.current = new JsonObject(true);
        this.stack.add(this.current);
        return this;
    }

    public UBJsonWriter array(String name) throws IOException {
        this.name(name).array();
        return this;
    }

    public UBJsonWriter name(String name) throws IOException {
        if (this.current == null || this.current.array) {
            throw new IllegalStateException("Current item must be an object.");
        }
        byte[] bytes = name.getBytes("UTF-8");
        if (bytes.length <= 127) {
            this.out.writeByte(105);
            this.out.writeByte(bytes.length);
        } else if (bytes.length <= Short.MAX_VALUE) {
            this.out.writeByte(73);
            this.out.writeShort(bytes.length);
        } else {
            this.out.writeByte(108);
            this.out.writeInt(bytes.length);
        }
        this.out.write(bytes);
        this.named = true;
        return this;
    }

    public UBJsonWriter value(byte value) throws IOException {
        this.checkName();
        this.out.writeByte(105);
        this.out.writeByte(value);
        return this;
    }

    public UBJsonWriter value(short value) throws IOException {
        this.checkName();
        this.out.writeByte(73);
        this.out.writeShort(value);
        return this;
    }

    public UBJsonWriter value(int value) throws IOException {
        this.checkName();
        this.out.writeByte(108);
        this.out.writeInt(value);
        return this;
    }

    public UBJsonWriter value(long value) throws IOException {
        this.checkName();
        this.out.writeByte(76);
        this.out.writeLong(value);
        return this;
    }

    public UBJsonWriter value(float value) throws IOException {
        this.checkName();
        this.out.writeByte(100);
        this.out.writeFloat(value);
        return this;
    }

    public UBJsonWriter value(double value) throws IOException {
        this.checkName();
        this.out.writeByte(68);
        this.out.writeDouble(value);
        return this;
    }

    public UBJsonWriter value(boolean value) throws IOException {
        this.checkName();
        this.out.writeByte(value ? 84 : 70);
        return this;
    }

    public UBJsonWriter value(char value) throws IOException {
        this.checkName();
        this.out.writeByte(73);
        this.out.writeChar(value);
        return this;
    }

    public UBJsonWriter value(String value) throws IOException {
        this.checkName();
        byte[] bytes = value.getBytes("UTF-8");
        this.out.writeByte(83);
        if (bytes.length <= 127) {
            this.out.writeByte(105);
            this.out.writeByte(bytes.length);
        } else if (bytes.length <= Short.MAX_VALUE) {
            this.out.writeByte(73);
            this.out.writeShort(bytes.length);
        } else {
            this.out.writeByte(108);
            this.out.writeInt(bytes.length);
        }
        this.out.write(bytes);
        return this;
    }

    public UBJsonWriter value(byte[] values) throws IOException {
        this.array();
        this.out.writeByte(36);
        this.out.writeByte(105);
        this.out.writeByte(35);
        this.value(values.length);
        int n = values.length;
        for (int i = 0; i < n; ++i) {
            this.out.writeByte(values[i]);
        }
        this.pop(true);
        return this;
    }

    public UBJsonWriter value(short[] values) throws IOException {
        this.array();
        this.out.writeByte(36);
        this.out.writeByte(73);
        this.out.writeByte(35);
        this.value(values.length);
        int n = values.length;
        for (int i = 0; i < n; ++i) {
            this.out.writeShort(values[i]);
        }
        this.pop(true);
        return this;
    }

    public UBJsonWriter value(int[] values) throws IOException {
        this.array();
        this.out.writeByte(36);
        this.out.writeByte(108);
        this.out.writeByte(35);
        this.value(values.length);
        int n = values.length;
        for (int i = 0; i < n; ++i) {
            this.out.writeInt(values[i]);
        }
        this.pop(true);
        return this;
    }

    public UBJsonWriter value(long[] values) throws IOException {
        this.array();
        this.out.writeByte(36);
        this.out.writeByte(76);
        this.out.writeByte(35);
        this.value(values.length);
        int n = values.length;
        for (int i = 0; i < n; ++i) {
            this.out.writeLong(values[i]);
        }
        this.pop(true);
        return this;
    }

    public UBJsonWriter value(float[] values) throws IOException {
        this.array();
        this.out.writeByte(36);
        this.out.writeByte(100);
        this.out.writeByte(35);
        this.value(values.length);
        int n = values.length;
        for (int i = 0; i < n; ++i) {
            this.out.writeFloat(values[i]);
        }
        this.pop(true);
        return this;
    }

    public UBJsonWriter value(double[] values) throws IOException {
        this.array();
        this.out.writeByte(36);
        this.out.writeByte(68);
        this.out.writeByte(35);
        this.value(values.length);
        int n = values.length;
        for (int i = 0; i < n; ++i) {
            this.out.writeDouble(values[i]);
        }
        this.pop(true);
        return this;
    }

    public UBJsonWriter value(boolean[] values) throws IOException {
        this.array();
        int n = values.length;
        for (int i = 0; i < n; ++i) {
            this.out.writeByte(values[i] ? 84 : 70);
        }
        this.pop();
        return this;
    }

    public UBJsonWriter value(char[] values) throws IOException {
        this.array();
        this.out.writeByte(36);
        this.out.writeByte(67);
        this.out.writeByte(35);
        this.value(values.length);
        int n = values.length;
        for (int i = 0; i < n; ++i) {
            this.out.writeChar(values[i]);
        }
        this.pop(true);
        return this;
    }

    public UBJsonWriter value(String[] values) throws IOException {
        this.array();
        this.out.writeByte(36);
        this.out.writeByte(83);
        this.out.writeByte(35);
        this.value(values.length);
        int n = values.length;
        for (int i = 0; i < n; ++i) {
            byte[] bytes = values[i].getBytes("UTF-8");
            if (bytes.length <= 127) {
                this.out.writeByte(105);
                this.out.writeByte(bytes.length);
            } else if (bytes.length <= Short.MAX_VALUE) {
                this.out.writeByte(73);
                this.out.writeShort(bytes.length);
            } else {
                this.out.writeByte(108);
                this.out.writeInt(bytes.length);
            }
            this.out.write(bytes);
        }
        this.pop(true);
        return this;
    }

    public UBJsonWriter value(JsonValue value) throws IOException {
        if (value.isObject()) {
            if (value.name != null) {
                this.object(value.name);
            } else {
                this.object();
            }
            JsonValue child = value.child;
            while (child != null) {
                this.value(child);
                child = child.next;
            }
            this.pop();
        } else if (value.isArray()) {
            if (value.name != null) {
                this.array(value.name);
            } else {
                this.array();
            }
            JsonValue child = value.child;
            while (child != null) {
                this.value(child);
                child = child.next;
            }
            this.pop();
        } else if (value.isBoolean()) {
            if (value.name != null) {
                this.name(value.name);
            }
            this.value(value.asBoolean());
        } else if (value.isDouble()) {
            if (value.name != null) {
                this.name(value.name);
            }
            this.value(value.asDouble());
        } else if (value.isLong()) {
            if (value.name != null) {
                this.name(value.name);
            }
            this.value(value.asLong());
        } else if (value.isString()) {
            if (value.name != null) {
                this.name(value.name);
            }
            this.value(value.asString());
        } else if (value.isNull()) {
            if (value.name != null) {
                this.name(value.name);
            }
            this.value();
        } else {
            throw new IOException("Unhandled JsonValue type");
        }
        return this;
    }

    public UBJsonWriter value(Object object) throws IOException {
        if (object == null) {
            return this.value();
        }
        if (object instanceof Number) {
            Number number = (Number)object;
            if (object instanceof Byte) {
                return this.value(number.byteValue());
            }
            if (object instanceof Short) {
                return this.value(number.shortValue());
            }
            if (object instanceof Integer) {
                return this.value(number.intValue());
            }
            if (object instanceof Long) {
                return this.value(number.longValue());
            }
            if (object instanceof Float) {
                return this.value(number.floatValue());
            }
            if (object instanceof Double) {
                return this.value(number.doubleValue());
            }
        } else {
            if (object instanceof Character) {
                return this.value(((Character)object).charValue());
            }
            if (object instanceof CharSequence) {
                return this.value(object.toString());
            }
            throw new IOException("Unknown object type.");
        }
        return this;
    }

    public UBJsonWriter value() throws IOException {
        this.checkName();
        this.out.writeByte(90);
        return this;
    }

    public UBJsonWriter set(String name, byte value) throws IOException {
        return this.name(name).value(value);
    }

    public UBJsonWriter set(String name, short value) throws IOException {
        return this.name(name).value(value);
    }

    public UBJsonWriter set(String name, int value) throws IOException {
        return this.name(name).value(value);
    }

    public UBJsonWriter set(String name, long value) throws IOException {
        return this.name(name).value(value);
    }

    public UBJsonWriter set(String name, float value) throws IOException {
        return this.name(name).value(value);
    }

    public UBJsonWriter set(String name, double value) throws IOException {
        return this.name(name).value(value);
    }

    public UBJsonWriter set(String name, boolean value) throws IOException {
        return this.name(name).value(value);
    }

    public UBJsonWriter set(String name, char value) throws IOException {
        return this.name(name).value(value);
    }

    public UBJsonWriter set(String name, String value) throws IOException {
        return this.name(name).value(value);
    }

    public UBJsonWriter set(String name, byte[] value) throws IOException {
        return this.name(name).value(value);
    }

    public UBJsonWriter set(String name, short[] value) throws IOException {
        return this.name(name).value(value);
    }

    public UBJsonWriter set(String name, int[] value) throws IOException {
        return this.name(name).value(value);
    }

    public UBJsonWriter set(String name, long[] value) throws IOException {
        return this.name(name).value(value);
    }

    public UBJsonWriter set(String name, float[] value) throws IOException {
        return this.name(name).value(value);
    }

    public UBJsonWriter set(String name, double[] value) throws IOException {
        return this.name(name).value(value);
    }

    public UBJsonWriter set(String name, boolean[] value) throws IOException {
        return this.name(name).value(value);
    }

    public UBJsonWriter set(String name, char[] value) throws IOException {
        return this.name(name).value(value);
    }

    public UBJsonWriter set(String name, String[] value) throws IOException {
        return this.name(name).value(value);
    }

    public UBJsonWriter set(String name) throws IOException {
        return this.name(name).value();
    }

    private void checkName() {
        if (this.current != null && !this.current.array) {
            if (!this.named) {
                throw new IllegalStateException("Name must be set.");
            }
            this.named = false;
        }
    }

    public UBJsonWriter pop() throws IOException {
        return this.pop(false);
    }

    protected UBJsonWriter pop(boolean silent) throws IOException {
        if (this.named) {
            throw new IllegalStateException("Expected an object, array, or value since a name was set.");
        }
        if (silent) {
            this.stack.pop();
        } else {
            this.stack.pop().close();
        }
        this.current = this.stack.size == 0 ? null : this.stack.peek();
        return this;
    }

    public void flush() throws IOException {
        this.out.flush();
    }

    @Override
    public void close() throws IOException {
        while (this.stack.size > 0) {
            this.pop();
        }
        this.out.close();
    }

    private class JsonObject {
        final boolean array;

        JsonObject(boolean array) throws IOException {
            this.array = array;
            UBJsonWriter.this.out.writeByte(array ? 91 : 123);
        }

        void close() throws IOException {
            UBJsonWriter.this.out.writeByte(this.array ? 93 : 125);
        }
    }
}

