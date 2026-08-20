/*
 * Decompiled with CFR 0.152.
 */
package com.badlogic.gdx.graphics.glutils;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.VertexAttribute;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.graphics.glutils.VertexData;
import com.badlogic.gdx.utils.BufferUtils;
import com.badlogic.gdx.utils.GdxRuntimeException;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;

public class VertexBufferObjectSubData
implements VertexData {
    final VertexAttributes attributes;
    final FloatBuffer buffer;
    final ByteBuffer byteBuffer;
    int bufferHandle;
    final boolean isDirect;
    final boolean isStatic;
    final int usage;
    boolean isDirty = false;
    boolean isBound = false;

    public VertexBufferObjectSubData(boolean isStatic, int numVertices, VertexAttribute ... attributes) {
        this.isStatic = isStatic;
        this.attributes = new VertexAttributes(attributes);
        this.byteBuffer = BufferUtils.newByteBuffer(this.attributes.vertexSize * numVertices);
        this.isDirect = true;
        this.usage = isStatic ? 35044 : 35048;
        this.buffer = this.byteBuffer.asFloatBuffer();
        this.bufferHandle = this.createBufferObject();
        this.buffer.flip();
        this.byteBuffer.flip();
    }

    private int createBufferObject() {
        int result = Gdx.gl20.glGenBuffer();
        Gdx.gl20.glBindBuffer(34962, result);
        Gdx.gl20.glBufferData(34962, this.byteBuffer.capacity(), null, this.usage);
        Gdx.gl20.glBindBuffer(34962, 0);
        return result;
    }

    @Override
    public VertexAttributes getAttributes() {
        return this.attributes;
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
    public FloatBuffer getBuffer() {
        this.isDirty = true;
        return this.buffer;
    }

    private void bufferChanged() {
        if (this.isBound) {
            Gdx.gl20.glBufferSubData(34962, 0, this.byteBuffer.limit(), this.byteBuffer);
            this.isDirty = false;
        }
    }

    @Override
    public void setVertices(float[] vertices, int offset, int count) {
        this.isDirty = true;
        if (this.isDirect) {
            BufferUtils.copy(vertices, this.byteBuffer, count, offset);
            this.buffer.position(0);
            this.buffer.limit(count);
        } else {
            this.buffer.clear();
            this.buffer.put(vertices, offset, count);
            this.buffer.flip();
            this.byteBuffer.position(0);
            this.byteBuffer.limit(this.buffer.limit() << 2);
        }
        this.bufferChanged();
    }

    @Override
    public void updateVertices(int targetOffset, float[] vertices, int sourceOffset, int count) {
        this.isDirty = true;
        if (!this.isDirect) {
            throw new GdxRuntimeException("Buffer must be allocated direct.");
        }
        int pos = this.byteBuffer.position();
        this.byteBuffer.position(targetOffset * 4);
        BufferUtils.copy(vertices, sourceOffset, count, (Buffer)this.byteBuffer);
        this.byteBuffer.position(pos);
        this.bufferChanged();
    }

    @Override
    public void bind(ShaderProgram shader) {
        this.bind(shader, null);
    }

    @Override
    public void bind(ShaderProgram shader, int[] locations) {
        GL20 gl = Gdx.gl20;
        gl.glBindBuffer(34962, this.bufferHandle);
        if (this.isDirty) {
            this.byteBuffer.limit(this.buffer.limit() * 4);
            gl.glBufferData(34962, this.byteBuffer.limit(), this.byteBuffer, this.usage);
            this.isDirty = false;
        }
        int numAttributes = this.attributes.size();
        if (locations == null) {
            for (int i = 0; i < numAttributes; ++i) {
                VertexAttribute attribute = this.attributes.get(i);
                int location = shader.getAttributeLocation(attribute.alias);
                if (location < 0) continue;
                shader.enableVertexAttribute(location);
                shader.setVertexAttribute(location, attribute.numComponents, attribute.type, attribute.normalized, this.attributes.vertexSize, attribute.offset);
            }
        } else {
            for (int i = 0; i < numAttributes; ++i) {
                VertexAttribute attribute = this.attributes.get(i);
                int location = locations[i];
                if (location < 0) continue;
                shader.enableVertexAttribute(location);
                shader.setVertexAttribute(location, attribute.numComponents, attribute.type, attribute.normalized, this.attributes.vertexSize, attribute.offset);
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
        GL20 gl = Gdx.gl20;
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
        gl.glBindBuffer(34962, 0);
        this.isBound = false;
    }

    @Override
    public void invalidate() {
        this.bufferHandle = this.createBufferObject();
        this.isDirty = true;
    }

    @Override
    public void dispose() {
        GL20 gl = Gdx.gl20;
        gl.glBindBuffer(34962, 0);
        gl.glDeleteBuffer(this.bufferHandle);
        this.bufferHandle = 0;
    }

    public int getBufferHandle() {
        return this.bufferHandle;
    }
}

