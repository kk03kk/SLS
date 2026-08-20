/*
 * Decompiled with CFR 0.152.
 */
package com.badlogic.gdx.graphics.glutils;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.glutils.IndexData;
import com.badlogic.gdx.utils.BufferUtils;
import com.badlogic.gdx.utils.GdxRuntimeException;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ShortBuffer;

public class IndexBufferObject
implements IndexData {
    final ShortBuffer buffer;
    final ByteBuffer byteBuffer;
    int bufferHandle;
    final boolean isDirect;
    boolean isDirty = true;
    boolean isBound = false;
    final int usage;
    private final boolean empty;

    public IndexBufferObject(int maxIndices) {
        this(true, maxIndices);
    }

    public IndexBufferObject(boolean isStatic, int maxIndices) {
        boolean bl = this.empty = maxIndices == 0;
        if (this.empty) {
            maxIndices = 1;
        }
        this.byteBuffer = BufferUtils.newUnsafeByteBuffer(maxIndices * 2);
        this.isDirect = true;
        this.buffer = this.byteBuffer.asShortBuffer();
        this.buffer.flip();
        this.byteBuffer.flip();
        this.bufferHandle = Gdx.gl20.glGenBuffer();
        this.usage = isStatic ? 35044 : 35048;
    }

    @Override
    public int getNumIndices() {
        return this.empty ? 0 : this.buffer.limit();
    }

    @Override
    public int getNumMaxIndices() {
        return this.empty ? 0 : this.buffer.capacity();
    }

    @Override
    public void setIndices(short[] indices, int offset, int count) {
        this.isDirty = true;
        this.buffer.clear();
        this.buffer.put(indices, offset, count);
        this.buffer.flip();
        this.byteBuffer.position(0);
        this.byteBuffer.limit(count << 1);
        if (this.isBound) {
            Gdx.gl20.glBufferData(34963, this.byteBuffer.limit(), this.byteBuffer, this.usage);
            this.isDirty = false;
        }
    }

    @Override
    public void setIndices(ShortBuffer indices) {
        this.isDirty = true;
        int pos = indices.position();
        this.buffer.clear();
        this.buffer.put(indices);
        this.buffer.flip();
        indices.position(pos);
        this.byteBuffer.position(0);
        this.byteBuffer.limit(this.buffer.limit() << 1);
        if (this.isBound) {
            Gdx.gl20.glBufferData(34963, this.byteBuffer.limit(), this.byteBuffer, this.usage);
            this.isDirty = false;
        }
    }

    @Override
    public void updateIndices(int targetOffset, short[] indices, int offset, int count) {
        this.isDirty = true;
        int pos = this.byteBuffer.position();
        this.byteBuffer.position(targetOffset * 2);
        BufferUtils.copy(indices, offset, (Buffer)this.byteBuffer, count);
        this.byteBuffer.position(pos);
        this.buffer.position(0);
        if (this.isBound) {
            Gdx.gl20.glBufferData(34963, this.byteBuffer.limit(), this.byteBuffer, this.usage);
            this.isDirty = false;
        }
    }

    @Override
    public ShortBuffer getBuffer() {
        this.isDirty = true;
        return this.buffer;
    }

    @Override
    public void bind() {
        if (this.bufferHandle == 0) {
            throw new GdxRuntimeException("No buffer allocated!");
        }
        Gdx.gl20.glBindBuffer(34963, this.bufferHandle);
        if (this.isDirty) {
            this.byteBuffer.limit(this.buffer.limit() * 2);
            Gdx.gl20.glBufferData(34963, this.byteBuffer.limit(), this.byteBuffer, this.usage);
            this.isDirty = false;
        }
        this.isBound = true;
    }

    @Override
    public void unbind() {
        Gdx.gl20.glBindBuffer(34963, 0);
        this.isBound = false;
    }

    @Override
    public void invalidate() {
        this.bufferHandle = Gdx.gl20.glGenBuffer();
        this.isDirty = true;
    }

    @Override
    public void dispose() {
        Gdx.gl20.glBindBuffer(34963, 0);
        Gdx.gl20.glDeleteBuffer(this.bufferHandle);
        this.bufferHandle = 0;
        BufferUtils.disposeUnsafeByteBuffer(this.byteBuffer);
    }
}

