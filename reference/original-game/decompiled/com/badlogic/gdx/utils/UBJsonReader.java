/*
 * Decompiled with CFR 0.152.
 */
package com.badlogic.gdx.utils;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.BaseJsonReader;
import com.badlogic.gdx.utils.GdxRuntimeException;
import com.badlogic.gdx.utils.JsonValue;
import com.badlogic.gdx.utils.SerializationException;
import com.badlogic.gdx.utils.StreamUtils;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;

public class UBJsonReader
implements BaseJsonReader {
    public boolean oldFormat = true;

    @Override
    public JsonValue parse(InputStream input) {
        JsonValue jsonValue;
        DataInputStream din = null;
        try {
            din = new DataInputStream(input);
            jsonValue = this.parse(din);
        }
        catch (IOException ex) {
            try {
                throw new SerializationException(ex);
            }
            catch (Throwable throwable) {
                StreamUtils.closeQuietly(din);
                throw throwable;
            }
        }
        StreamUtils.closeQuietly(din);
        return jsonValue;
    }

    @Override
    public JsonValue parse(FileHandle file) {
        try {
            return this.parse(file.read(8192));
        }
        catch (Exception ex) {
            throw new SerializationException("Error parsing file: " + file, ex);
        }
    }

    public JsonValue parse(DataInputStream din) throws IOException {
        try {
            JsonValue jsonValue = this.parse(din, din.readByte());
            return jsonValue;
        }
        finally {
            StreamUtils.closeQuietly(din);
        }
    }

    protected JsonValue parse(DataInputStream din, byte type) throws IOException {
        if (type == 91) {
            return this.parseArray(din);
        }
        if (type == 123) {
            return this.parseObject(din);
        }
        if (type == 90) {
            return new JsonValue(JsonValue.ValueType.nullValue);
        }
        if (type == 84) {
            return new JsonValue(true);
        }
        if (type == 70) {
            return new JsonValue(false);
        }
        if (type == 66) {
            return new JsonValue(this.readUChar(din));
        }
        if (type == 85) {
            return new JsonValue(this.readUChar(din));
        }
        if (type == 105) {
            return new JsonValue(this.oldFormat ? (long)din.readShort() : (long)din.readByte());
        }
        if (type == 73) {
            return new JsonValue(this.oldFormat ? (long)din.readInt() : (long)din.readShort());
        }
        if (type == 108) {
            return new JsonValue(din.readInt());
        }
        if (type == 76) {
            return new JsonValue(din.readLong());
        }
        if (type == 100) {
            return new JsonValue(din.readFloat());
        }
        if (type == 68) {
            return new JsonValue(din.readDouble());
        }
        if (type == 115 || type == 83) {
            return new JsonValue(this.parseString(din, type));
        }
        if (type == 97 || type == 65) {
            return this.parseData(din, type);
        }
        if (type == 67) {
            return new JsonValue(din.readChar());
        }
        throw new GdxRuntimeException("Unrecognized data type");
    }

    protected JsonValue parseArray(DataInputStream din) throws IOException {
        JsonValue result = new JsonValue(JsonValue.ValueType.array);
        byte type = din.readByte();
        byte valueType = 0;
        if (type == 36) {
            valueType = din.readByte();
            type = din.readByte();
        }
        long size = -1L;
        if (type == 35) {
            size = this.parseSize(din, false, -1L);
            if (size < 0L) {
                throw new GdxRuntimeException("Unrecognized data type");
            }
            if (size == 0L) {
                return result;
            }
            type = valueType == 0 ? din.readByte() : valueType;
        }
        JsonValue prev = null;
        long c = 0L;
        while (din.available() > 0 && type != 93) {
            JsonValue val = this.parse(din, type);
            val.parent = result;
            if (prev != null) {
                val.prev = prev;
                prev.next = val;
                ++result.size;
            } else {
                result.child = val;
                result.size = 1;
            }
            prev = val;
            if (size > 0L && ++c >= size) break;
            type = valueType == 0 ? din.readByte() : valueType;
        }
        return result;
    }

    protected JsonValue parseObject(DataInputStream din) throws IOException {
        JsonValue result = new JsonValue(JsonValue.ValueType.object);
        byte type = din.readByte();
        byte valueType = 0;
        if (type == 36) {
            valueType = din.readByte();
            type = din.readByte();
        }
        long size = -1L;
        if (type == 35) {
            size = this.parseSize(din, false, -1L);
            if (size < 0L) {
                throw new GdxRuntimeException("Unrecognized data type");
            }
            if (size == 0L) {
                return result;
            }
            type = din.readByte();
        }
        JsonValue prev = null;
        long c = 0L;
        while (din.available() > 0 && type != 125) {
            String key = this.parseString(din, true, type);
            JsonValue child = this.parse(din, valueType == 0 ? din.readByte() : valueType);
            child.setName(key);
            child.parent = result;
            if (prev != null) {
                child.prev = prev;
                prev.next = child;
                ++result.size;
            } else {
                result.child = child;
                result.size = 1;
            }
            prev = child;
            if (size > 0L && ++c >= size) break;
            type = din.readByte();
        }
        return result;
    }

    protected JsonValue parseData(DataInputStream din, byte blockType) throws IOException {
        byte dataType = din.readByte();
        long size = blockType == 65 ? this.readUInt(din) : (long)this.readUChar(din);
        JsonValue result = new JsonValue(JsonValue.ValueType.array);
        JsonValue prev = null;
        for (long i = 0L; i < size; ++i) {
            JsonValue val = this.parse(din, dataType);
            val.parent = result;
            if (prev != null) {
                prev.next = val;
                ++result.size;
            } else {
                result.child = val;
                result.size = 1;
            }
            prev = val;
        }
        return result;
    }

    protected String parseString(DataInputStream din, byte type) throws IOException {
        return this.parseString(din, false, type);
    }

    protected String parseString(DataInputStream din, boolean sOptional, byte type) throws IOException {
        long size = -1L;
        if (type == 83) {
            size = this.parseSize(din, true, -1L);
        } else if (type == 115) {
            size = this.readUChar(din);
        } else if (sOptional) {
            size = this.parseSize(din, type, false, -1L);
        }
        if (size < 0L) {
            throw new GdxRuntimeException("Unrecognized data type, string expected");
        }
        return size > 0L ? this.readString(din, size) : "";
    }

    protected long parseSize(DataInputStream din, boolean useIntOnError, long defaultValue) throws IOException {
        return this.parseSize(din, din.readByte(), useIntOnError, defaultValue);
    }

    protected long parseSize(DataInputStream din, byte type, boolean useIntOnError, long defaultValue) throws IOException {
        if (type == 105) {
            return this.readUChar(din);
        }
        if (type == 73) {
            return this.readUShort(din);
        }
        if (type == 108) {
            return this.readUInt(din);
        }
        if (type == 76) {
            return din.readLong();
        }
        if (useIntOnError) {
            long result = (long)((short)type & 0xFF) << 24;
            result |= (long)((short)din.readByte() & 0xFF) << 16;
            result |= (long)((short)din.readByte() & 0xFF) << 8;
            return result |= (long)((short)din.readByte() & 0xFF);
        }
        return defaultValue;
    }

    protected short readUChar(DataInputStream din) throws IOException {
        return (short)((short)din.readByte() & 0xFF);
    }

    protected int readUShort(DataInputStream din) throws IOException {
        return din.readShort() & 0xFFFF;
    }

    protected long readUInt(DataInputStream din) throws IOException {
        return (long)din.readInt() & 0xFFFFFFFFFFFFFFFFL;
    }

    protected String readString(DataInputStream din, long size) throws IOException {
        byte[] data = new byte[(int)size];
        din.readFully(data);
        return new String(data, "UTF-8");
    }
}

