/*
 * Decompiled with CFR 0.152.
 */
package com.badlogic.gdx.graphics.glutils;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.GL30;
import com.badlogic.gdx.graphics.VertexAttribute;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.graphics.glutils.VertexData;
import com.badlogic.gdx.utils.BufferUtils;
import com.badlogic.gdx.utils.IntArray;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

public class VertexBufferObjectWithVAO
implements VertexData {
    static final IntBuffer tmpHandle = BufferUtils.newIntBuffer(1);
    final VertexAttributes attributes;
    final FloatBuffer buffer;
    final ByteBuffer byteBuffer;
    int bufferHandle;
    final boolean isStatic;
    final int usage;
    boolean isDirty = false;
    boolean isBound = false;
    int vaoHandle = -1;
    IntArray cachedLocations = new IntArray();

    public VertexBufferObjectWithVAO(boolean isStatic, int numVertices, VertexAttribute ... attributes) {
        this(isStatic, numVertices, new VertexAttributes(attributes));
    }

    public VertexBufferObjectWithVAO(boolean isStatic, int numVertices, VertexAttributes attributes) {
        this.isStatic = isStatic;
        this.attributes = attributes;
        this.byteBuffer = BufferUtils.newUnsafeByteBuffer(this.attributes.vertexSize * numVertices);
        this.buffer = this.byteBuffer.asFloatBuffer();
        this.buffer.flip();
        this.byteBuffer.flip();
        this.bufferHandle = Gdx.gl20.glGenBuffer();
        this.usage = isStatic ? 35044 : 35048;
        this.createVAO();
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
            Gdx.gl20.glBufferData(34962, this.byteBuffer.limit(), this.byteBuffer, this.usage);
            this.isDirty = false;
        }
    }

    @Override
    public void setVertices(float[] vertices, int offset, int count) {
        this.isDirty = true;
        BufferUtils.copy(vertices, this.byteBuffer, count, offset);
        this.buffer.position(0);
        this.buffer.limit(count);
        this.bufferChanged();
    }

    @Override
    public void updateVertices(int targetOffset, float[] vertices, int sourceOffset, int count) {
        this.isDirty = true;
        int pos = this.byteBuffer.position();
        this.byteBuffer.position(targetOffset * 4);
        BufferUtils.copy(vertices, sourceOffset, count, (Buffer)this.byteBuffer);
        this.byteBuffer.position(pos);
        this.buffer.position(0);
        this.bufferChanged();
    }

    @Override
    public void bind(ShaderProgram shader) {
        this.bind(shader, null);
    }

    @Override
    public void bind(ShaderProgram shader, int[] locations) {
        GL30 gl = Gdx.gl30;
        gl.glBindVertexArray(this.vaoHandle);
        this.bindAttributes(shader, locations);
        this.bindData(gl);
        this.isBound = true;
    }

    private void bindAttributes(ShaderProgram shader, int[] locations) {
        int location;
        VertexAttribute attribute;
        int i;
        boolean stillValid = this.cachedLocations.size != 0;
        int numAttributes = this.attributes.size();
        if (stillValid) {
            if (locations == null) {
                for (i = 0; stillValid && i < numAttributes; ++i) {
                    attribute = this.attributes.get(i);
                    location = shader.getAttributeLocation(attribute.alias);
                    stillValid = location == this.cachedLocations.get(i);
                }
            } else {
                stillValid = locations.length == this.cachedLocations.size;
                for (i = 0; stillValid && i < numAttributes; ++i) {
                    stillValid = locations[i] == this.cachedLocations.get(i);
                }
            }
        }
        if (!stillValid) {
            Gdx.gl.glBindBuffer(34962, this.bufferHandle);
            this.unbindAttributes(shader);
            this.cachedLocations.clear();
            for (i = 0; i < numAttributes; ++i) {
                attribute = this.attributes.get(i);
                if (locations == null) {
                    this.cachedLocations.add(shader.getAttributeLocation(attribute.alias));
                } else {
                    this.cachedLocations.add(locations[i]);
                }
                location = this.cachedLocations.get(i);
                if (location < 0) continue;
                shader.enableVertexAttribute(location);
                shader.setVertexAttribute(location, attribute.numComponents, attribute.type, attribute.normalized, this.attributes.vertexSize, attribute.offset);
            }
        }
    }

    private void unbindAttributes(ShaderProgram shaderProgram) {
        if (this.cachedLocations.size == 0) {
            return;
        }
        int numAttributes = this.attributes.size();
        for (int i = 0; i < numAttributes; ++i) {
            int location = this.cachedLocations.get(i);
            if (location < 0) continue;
            shaderProgram.disableVertexAttribute(location);
        }
    }

    private void bindData(GL20 gl) {
        if (this.isDirty) {
            gl.glBindBuffer(34962, this.bufferHandle);
            this.byteBuffer.limit(this.buffer.limit() * 4);
            gl.glBufferData(34962, this.byteBuffer.limit(), this.byteBuffer, this.usage);
            this.isDirty = false;
        }
    }

    @Override
    public void unbind(ShaderProgram shader) {
        this.unbind(shader, null);
    }

    @Override
    public void unbind(ShaderProgram shader, int[] locations) {
        GL30 gl = Gdx.gl30;
        gl.glBindVertexArray(0);
        this.isBound = false;
    }

    @Override
    public void invalidate() {
        this.bufferHandle = Gdx.gl30.glGenBuffer();
        this.createVAO();
        this.isDirty = true;
    }

    @Override
    public void dispose() {
        GL30 gl = Gdx.gl30;
        gl.glBindBuffer(34962, 0);
        gl.glDeleteBuffer(this.bufferHandle);
        this.bufferHandle = 0;
        BufferUtils.disposeUnsafeByteBuffer(this.byteBuffer);
        this.deleteVAO();
    }

    private void createVAO() {
        tmpHandle.clear();
        Gdx.gl30.glGenVertexArrays(1, tmpHandle);
        this.vaoHandle = tmpHandle.get();
    }

    private void deleteVAO() {
        if (this.vaoHandle != -1) {
            tmpHandle.clear();
            tmpHandle.put(this.vaoHandle);
            tmpHandle.flip();
            Gdx.gl30.glDeleteVertexArrays(1, tmpHandle);
            this.vaoHandle = -1;
        }
    }
}

