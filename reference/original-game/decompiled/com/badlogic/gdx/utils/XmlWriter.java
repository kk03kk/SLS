/*
 * Decompiled with CFR 0.152.
 */
package com.badlogic.gdx.utils;

import com.badlogic.gdx.utils.Array;
import java.io.IOException;
import java.io.Writer;

public class XmlWriter
extends Writer {
    private final Writer writer;
    private final Array<String> stack = new Array();
    private String currentElement;
    private boolean indentNextClose;
    public int indent;

    public XmlWriter(Writer writer) {
        this.writer = writer;
    }

    private void indent() throws IOException {
        int count = this.indent;
        if (this.currentElement != null) {
            ++count;
        }
        for (int i = 0; i < count; ++i) {
            this.writer.write(9);
        }
    }

    public XmlWriter element(String name) throws IOException {
        if (this.startElementContent()) {
            this.writer.write(10);
        }
        this.indent();
        this.writer.write(60);
        this.writer.write(name);
        this.currentElement = name;
        return this;
    }

    public XmlWriter element(String name, Object text) throws IOException {
        return this.element(name).text(text).pop();
    }

    private boolean startElementContent() throws IOException {
        if (this.currentElement == null) {
            return false;
        }
        ++this.indent;
        this.stack.add(this.currentElement);
        this.currentElement = null;
        this.writer.write(">");
        return true;
    }

    public XmlWriter attribute(String name, Object value) throws IOException {
        if (this.currentElement == null) {
            throw new IllegalStateException();
        }
        this.writer.write(32);
        this.writer.write(name);
        this.writer.write("=\"");
        this.writer.write(value == null ? "null" : value.toString());
        this.writer.write(34);
        return this;
    }

    public XmlWriter text(Object text) throws IOException {
        this.startElementContent();
        String string = text == null ? "null" : text.toString();
        boolean bl = this.indentNextClose = string.length() > 64;
        if (this.indentNextClose) {
            this.writer.write(10);
            this.indent();
        }
        this.writer.write(string);
        if (this.indentNextClose) {
            this.writer.write(10);
        }
        return this;
    }

    public XmlWriter pop() throws IOException {
        if (this.currentElement != null) {
            this.writer.write("/>\n");
            this.currentElement = null;
        } else {
            this.indent = Math.max(this.indent - 1, 0);
            if (this.indentNextClose) {
                this.indent();
            }
            this.writer.write("</");
            this.writer.write(this.stack.pop());
            this.writer.write(">\n");
        }
        this.indentNextClose = true;
        return this;
    }

    @Override
    public void close() throws IOException {
        while (this.stack.size != 0) {
            this.pop();
        }
        this.writer.close();
    }

    @Override
    public void write(char[] cbuf, int off, int len) throws IOException {
        this.startElementContent();
        this.writer.write(cbuf, off, len);
    }

    @Override
    public void flush() throws IOException {
        this.writer.flush();
    }
}

