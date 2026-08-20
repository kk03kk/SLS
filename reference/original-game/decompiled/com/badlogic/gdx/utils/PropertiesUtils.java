/*
 * Decompiled with CFR 0.152.
 */
package com.badlogic.gdx.utils;

import com.badlogic.gdx.utils.ObjectMap;
import com.badlogic.gdx.utils.StringBuilder;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.Date;

public final class PropertiesUtils {
    private static final int NONE = 0;
    private static final int SLASH = 1;
    private static final int UNICODE = 2;
    private static final int CONTINUE = 3;
    private static final int KEY_DONE = 4;
    private static final int IGNORE = 5;
    private static final String LINE_SEPARATOR = "\n";

    private PropertiesUtils() {
    }

    public static void load(ObjectMap<String, String> properties, Reader reader) throws IOException {
        int intVal;
        if (properties == null) {
            throw new NullPointerException("ObjectMap cannot be null");
        }
        if (reader == null) {
            throw new NullPointerException("Reader cannot be null");
        }
        int mode = 0;
        int unicode = 0;
        int count = 0;
        char[] buf = new char[40];
        int offset = 0;
        int keyLength = -1;
        boolean firstChar = true;
        BufferedReader br = new BufferedReader(reader);
        block17: while ((intVal = br.read()) != -1) {
            int nextChar = intVal;
            if (offset == buf.length) {
                char[] newBuf = new char[buf.length * 2];
                System.arraycopy(buf, 0, newBuf, 0, offset);
                buf = newBuf;
            }
            if (mode == 2) {
                int digit = Character.digit((char)nextChar, 16);
                if (digit >= 0) {
                    unicode = (unicode << 4) + digit;
                    if (++count < 4) {
                        continue;
                    }
                } else if (count <= 4) {
                    throw new IllegalArgumentException("Invalid Unicode sequence: illegal character");
                }
                mode = 0;
                buf[offset++] = (char)unicode;
                if (nextChar != 10) continue;
            }
            if (mode == 1) {
                mode = 0;
                switch (nextChar) {
                    case 13: {
                        mode = 3;
                        continue block17;
                    }
                    case 10: {
                        mode = 5;
                        continue block17;
                    }
                    case 98: {
                        nextChar = 8;
                        break;
                    }
                    case 102: {
                        nextChar = 12;
                        break;
                    }
                    case 110: {
                        nextChar = 10;
                        break;
                    }
                    case 114: {
                        nextChar = 13;
                        break;
                    }
                    case 116: {
                        nextChar = 9;
                        break;
                    }
                    case 117: {
                        mode = 2;
                        count = 0;
                        unicode = 0;
                        continue block17;
                    }
                }
            } else {
                switch (nextChar) {
                    case 33: 
                    case 35: {
                        if (!firstChar) break;
                        while ((intVal = br.read()) != -1 && (nextChar = (int)((char)intVal)) != 13 && nextChar != 10) {
                        }
                        continue block17;
                    }
                    case 10: {
                        if (mode == 3) {
                            mode = 5;
                            continue block17;
                        }
                    }
                    case 13: {
                        mode = 0;
                        firstChar = true;
                        if (offset > 0 || offset == 0 && keyLength == 0) {
                            if (keyLength == -1) {
                                keyLength = offset;
                            }
                            String temp = new String(buf, 0, offset);
                            properties.put(temp.substring(0, keyLength), temp.substring(keyLength));
                        }
                        keyLength = -1;
                        offset = 0;
                        continue block17;
                    }
                    case 92: {
                        if (mode == 4) {
                            keyLength = offset;
                        }
                        mode = 1;
                        continue block17;
                    }
                    case 58: 
                    case 61: {
                        if (keyLength != -1) break;
                        mode = 0;
                        keyLength = offset;
                        continue block17;
                    }
                }
                if (Character.isSpace((char)nextChar)) {
                    if (mode == 3) {
                        mode = 5;
                    }
                    if (offset == 0 || offset == keyLength || mode == 5) continue;
                    if (keyLength == -1) {
                        mode = 4;
                        continue;
                    }
                }
                if (mode == 5 || mode == 3) {
                    mode = 0;
                }
            }
            firstChar = false;
            if (mode == 4) {
                keyLength = offset;
                mode = 0;
            }
            buf[offset++] = nextChar;
        }
        if (mode == 2 && count <= 4) {
            throw new IllegalArgumentException("Invalid Unicode sequence: expected format \\uxxxx");
        }
        if (keyLength == -1 && offset > 0) {
            keyLength = offset;
        }
        if (keyLength >= 0) {
            String temp = new String(buf, 0, offset);
            String key = temp.substring(0, keyLength);
            String value = temp.substring(keyLength);
            if (mode == 1) {
                value = value + "\u0000";
            }
            properties.put(key, value);
        }
    }

    public static void store(ObjectMap<String, String> properties, Writer writer, String comment) throws IOException {
        PropertiesUtils.storeImpl(properties, writer, comment, false);
    }

    private static void storeImpl(ObjectMap<String, String> properties, Writer writer, String comment, boolean escapeUnicode) throws IOException {
        if (comment != null) {
            PropertiesUtils.writeComment(writer, comment);
        }
        writer.write("#");
        writer.write(new Date().toString());
        writer.write(LINE_SEPARATOR);
        StringBuilder sb = new StringBuilder(200);
        for (ObjectMap.Entry entry : properties.entries()) {
            PropertiesUtils.dumpString(sb, (String)entry.key, true, escapeUnicode);
            sb.append('=');
            PropertiesUtils.dumpString(sb, (String)entry.value, false, escapeUnicode);
            writer.write(LINE_SEPARATOR);
            writer.write(sb.toString());
            sb.setLength(0);
        }
        writer.flush();
    }

    private static void dumpString(StringBuilder outBuffer, String string, boolean escapeSpace, boolean escapeUnicode) {
        int len = string.length();
        block8: for (int i = 0; i < len; ++i) {
            char ch = string.charAt(i);
            if (ch > '=' && ch < '\u007f') {
                outBuffer.append(ch == '\\' ? "\\\\" : Character.valueOf(ch));
                continue;
            }
            switch (ch) {
                case ' ': {
                    if (i != 0 && !escapeSpace) continue block8;
                    outBuffer.append("\\ ");
                    continue block8;
                }
                case '\n': {
                    outBuffer.append("\\n");
                    continue block8;
                }
                case '\r': {
                    outBuffer.append("\\r");
                    continue block8;
                }
                case '\t': {
                    outBuffer.append("\\t");
                    continue block8;
                }
                case '\f': {
                    outBuffer.append("\\f");
                    continue block8;
                }
                case '!': 
                case '#': 
                case ':': 
                case '=': {
                    outBuffer.append('\\').append(ch);
                    continue block8;
                }
                default: {
                    if ((ch < ' ' || ch > '~') & escapeUnicode) {
                        String hex = Integer.toHexString(ch);
                        outBuffer.append("\\u");
                        for (int j = 0; j < 4 - hex.length(); ++j) {
                            outBuffer.append('0');
                        }
                        outBuffer.append(hex);
                        continue block8;
                    }
                    outBuffer.append(ch);
                }
            }
        }
    }

    private static void writeComment(Writer writer, String comment) throws IOException {
        int curIndex;
        writer.write("#");
        int len = comment.length();
        int lastIndex = 0;
        for (curIndex = 0; curIndex < len; ++curIndex) {
            char c = comment.charAt(curIndex);
            if (c <= '\u00ff' && c != '\n' && c != '\r') continue;
            if (lastIndex != curIndex) {
                writer.write(comment.substring(lastIndex, curIndex));
            }
            if (c > '\u00ff') {
                String hex = Integer.toHexString(c);
                writer.write("\\u");
                for (int j = 0; j < 4 - hex.length(); ++j) {
                    writer.write(48);
                }
                writer.write(hex);
            } else {
                writer.write(LINE_SEPARATOR);
                if (c == '\r' && curIndex != len - 1 && comment.charAt(curIndex + 1) == '\n') {
                    ++curIndex;
                }
                if (curIndex == len - 1 || comment.charAt(curIndex + 1) != '#' && comment.charAt(curIndex + 1) != '!') {
                    writer.write("#");
                }
            }
            lastIndex = curIndex + 1;
        }
        if (lastIndex != curIndex) {
            writer.write(comment.substring(lastIndex, curIndex));
        }
        writer.write(LINE_SEPARATOR);
    }
}

