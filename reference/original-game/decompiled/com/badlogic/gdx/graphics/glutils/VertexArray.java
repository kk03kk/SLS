/*
 * Decompiled with CFR 0.152.
 */
package com.badlogic.gdx.graphics.glutils;

import com.badlogic.gdx.graphics.VertexAttribute;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.graphics.glutils.VertexData;
import com.badlogic.gdx.utils.BufferUtils;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;

public class VertexArray
implements VertexData {
    final VertexAttributes attributes;
    final FloatBuffer buffer;
    final ByteBuffer byteBuffer;
    boolean isBound = false;

    public VertexArray(int numVertices, VertexAttribute ... attributes) {
        this(numVertices, new VertexAttributes(attributes));
    }

    public VertexArray(int numVertices, VertexAttributes attributes) {
        this.attributes = attributes;
        this.byteBuffer = BufferUtils.newUnsafeByteBuffer(this.attributes.vertexSize * numVertices);
        this.buffer = this.byteBuffer.asFloatBuffer();
        this.buffer.flip();
        this.byteBuffer.flip();
    }

    @Override
    public void dispose() {
        BufferUtils.disposeUnsafeByteBuffer(this.byteBuffer);
    }

    @Override
    public FloatBuffer getBuffer() {
        return this.buffer;
    }

    @Override
    public int getNumVertices() {
        return this.buffer.limit() * 4 / this.attributes.vertexSize;
    }

    @Override
    public int getNumMaxVertices() {
        return this.byteBuffer.capacity() / this.attributes.vertexSize;
    }

    @Override
    public void setVertices(float[] vertices, int offset, int count) {
        BufferUtils.copy(vertices, this.byteBuffer, count, offset);
        this.buffer.position(0);
        this.buffer.limit(count);
    }

    @Override
    public void updateVertices(int targetOffset, float[] vertices, int sourceOffset, int count) {
        int pos = this.byteBuffer.position();
        this.byteBuffer.position(targetOffset * 4);
        BufferUtils.copy(vertices, sourceOffset, count, (Buffer)this.byteBuffer);
        this.byteBuffer.position(pos);
    }

    @Override
    public void bind(ShaderProgram shader) {
        this.bind(shader, null);
    }

    @Override
    public void bind(ShaderProgram shader, int[] locations) {
        int numAttributes = this.attributes.size();
        this.byteBuffer.limit(this.buffer.limit() * 4);
        if (locations == null) {
            for (int i = 0; i < numAttributes; ++i) {
                VertexAttribute attribute = this.attributes.get(i);
                int location = shader.getAttributeLocation(attribute.alias);
                if (location < 0) continue;
                shader.enableVertexAttribute(location);
                if (attribute.type == 5126) {
                    this.buffer.position(attribute.offset / 4);
                    shader.setVertexAttribute(location, attribute.numComponents, attribute.type, attribute.normalized, this.attributes.vertexSize, (Buffer)this.buffer);
                    continue;
                }
                this.byteBuffer.position(attribute.offset);
                shader.setVertexAttribute(location, attribute.numComponents, attribute.type, attribute.normalized, this.attributes.vertexSize, (Buffer)this.byteBuffer);
            }
        } else {
            for (int i = 0; i < numAttributes; ++i) {
                VertexAttribute attribute = this.attributes.get(i);
                int location = locations[i];
                if (location < 0) continue;
                shader.enableVertexAttribute(location);
                if (attribute.type == 5126) {
                    this.buffer.position(attribute.offset / 4);
                    shader.setVertexAttribute(location, attribute.numComponents, attribute.type, attribute.normalized, this.attributes.vertexSize, (Buffer)this.buffer);
                    continue;
                }
                this.byteBuffer.position(attribute.offset);
                shader.setVertexAttribute(location, attribute.numComponents, attribute.type, attribute.normalized, this.attributes.vertexSize, (Buffer)this.byteBuffer);
            }
        }
        this.isBound = true;
    }

    @Override
    public void unbind(ShaderProgram shader) {
        this.unbind(shader, null);
    }

    @Override
    public void unbind(ShaderProgram shader, int[] locations) {
        int numAttributes = this.attributes.size();
        if (locations == null) {
            for (int i = 0; i < numAttributes; ++i) {
                shader.disableVertexAttribute(this.attributes.get((int)i).alias);
            }
        } else {
            for (int i = 0; i < numAttributes; ++i) {
                int location = locations[i];
                if (location < 0) continue;
                shader.disableVertexAttribute(location);
            }
        }
        this.isBound = false;
    }

    @Override
    public VertexAttributes getAttributes() {
        return this.attributes;
    }

    @Override
    public void invalidate() {
    }
}

