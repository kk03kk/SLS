/*
 * Decompiled with CFR 0.152.
 */
package com.badlogic.gdx.utils;

import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.StringBuilder;
import java.io.IOException;
import java.io.Writer;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.regex.Pattern;

public class JsonWriter
extends Writer {
    final Writer writer;
    private final Array<JsonObject> stack = new Array();
    private JsonObject current;
    private boolean named;
    private OutputType outputType = OutputType.json;
    private boolean quoteLongValues = false;

    public JsonWriter(Writer writer) {
        this.writer = writer;
    }

    public Writer getWriter() {
        return this.writer;
    }

    public void setOutputType(OutputType outputType) {
        this.outputType = outputType;
    }

    public void setQuoteLongValues(boolean quoteLongValues) {
        this.quoteLongValues = quoteLongValues;
    }

    public JsonWriter name(String name) throws IOException {
        if (this.current == null || this.current.array) {
            throw new IllegalStateException("Current item must be an object.");
        }
        if (!this.current.needsComma) {
            this.current.needsComma = true;
        } else {
            this.writer.write(44);
        }
        this.writer.write(this.outputType.quoteName(name));
        this.writer.write(58);
        this.named = true;
        return this;
    }

    public JsonWriter object() throws IOException {
        this.requireCommaOrName();
        this.current = new JsonObject(false);
        this.stack.add(this.current);
        return this;
    }

    public JsonWriter array() throws IOException {
        this.requireCommaOrName();
        this.current = new JsonObject(true);
        this.stack.add(this.current);
        return this;
    }

    public JsonWriter value(Object value) throws IOException {
        if (this.quoteLongValues && (value instanceof Long || value instanceof Double || value instanceof BigDecimal || value instanceof BigInteger)) {
            value = value.toString();
        } else if (value instanceof Number) {
            Number number = (Number)value;
            long longValue = number.longValue();
            if (number.doubleValue() == (double)longValue) {
                value = longValue;
            }
        }
        this.requireCommaOrName();
        this.writer.write(this.outputType.quoteValue(value));
        return this;
    }

    public JsonWriter json(String json) throws IOException {
        this.requireCommaOrName();
        this.writer.write(json);
        return this;
    }

    private void requireCommaOrName() throws IOException {
        if (this.current == null) {
            return;
        }
        if (this.current.array) {
            if (!this.current.needsComma) {
                this.current.needsComma = true;
            } else {
                this.writer.write(44);
            }
        } else {
            if (!this.named) {
                throw new IllegalStateException("Name must be set.");
            }
            this.named = false;
        }
    }

    public JsonWriter object(String name) throws IOException {
        return this.name(name).object();
    }

    public JsonWriter array(String name) throws IOException {
        return this.name(name).array();
    }

    public JsonWriter set(String name, Object value) throws IOException {
        return this.name(name).value(value);
    }

    public JsonWriter json(String name, String json) throws IOException {
        return this.name(name).json(json);
    }

    public JsonWriter pop() throws IOException {
        if (this.named) {
            throw new IllegalStateException("Expected an object, array, or value since a name was set.");
        }
        this.stack.pop().close();
        this.current = this.stack.size == 0 ? null : this.stack.peek();
        return this;
    }

    @Override
    public void write(char[] cbuf, int off, int len) throws IOException {
        this.writer.write(cbuf, off, len);
    }

    @Override
    public void flush() throws IOException {
        this.writer.flush();
    }

    @Override
    public void close() throws IOException {
        while (this.stack.size > 0) {
            this.pop();
        }
        this.writer.close();
    }

    public static enum OutputType {
        json,
        javascript,
        minimal;

        private static Pattern javascriptPattern;
        private static Pattern minimalNamePattern;
        private static Pattern minimalValuePattern;

        public String quoteValue(Object value) {
            int length;
            if (value == null) {
                return "null";
            }
            String string = value.toString();
            if (value instanceof Number || value instanceof Boolean) {
                return string;
            }
            StringBuilder buffer = new StringBuilder(string);
            buffer.replace('\\', "\\\\").replace('\r', "\\r").replace('\n', "\\n").replace('\t', "\\t");
            if (!(this != minimal || string.equals("true") || string.equals("false") || string.equals("null") || string.contains("//") || string.contains("/*") || (length = buffer.length()) <= 0 || buffer.charAt(length - 1) == ' ' || !minimalValuePattern.matcher(buffer).matches())) {
                return buffer.toString();
            }
            return '\"' + buffer.replace('\"', "\\\"").toString() + '\"';
        }

        public String quoteName(String value) {
            StringBuilder buffer = new StringBuilder(value);
            buffer.replace('\\', "\\\\").replace('\r', "\\r").replace('\n', "\\n").replace('\t', "\\t");
            switch (this) {
                case minimal: {
                    if (!value.contains("//") && !value.contains("/*") && minimalNamePattern.matcher(buffer).matches()) {
                        return buffer.toString();
                    }
                }
                case javascript: {
                    if (!javascriptPattern.matcher(buffer).matches()) break;
                    return buffer.toString();
                }
            }
            return '\"' + buffer.replace('\"', "\\\"").toString() + '\"';
        }

        static {
            javascriptPattern = Pattern.compile("^[a-zA-Z_$][a-zA-Z_$0-9]*$");
            minimalNamePattern = Pattern.compile("^[^\":,}/ ][^:]*$");
            minimalValuePattern = Pattern.compile("^[^\":,{\\[\\]/ ][^}\\],]*$");
        }
    }

    private class JsonObject {
        final boolean array;
        boolean needsComma;

        JsonObject(boolean array) throws IOException {
            this.array = array;
            JsonWriter.this.writer.write(array ? 91 : 123);
        }

        void close() throws IOException {
            JsonWriter.this.writer.write(this.array ? 93 : 125);
        }
    }
}

