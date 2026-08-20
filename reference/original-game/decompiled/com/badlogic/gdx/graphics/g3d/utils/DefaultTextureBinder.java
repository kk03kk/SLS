/*
 * Decompiled with CFR 0.152.
 */
package com.badlogic.gdx.graphics.g3d.utils;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GLTexture;
import com.badlogic.gdx.graphics.g3d.utils.TextureBinder;
import com.badlogic.gdx.graphics.g3d.utils.TextureDescriptor;
import com.badlogic.gdx.utils.BufferUtils;
import com.badlogic.gdx.utils.GdxRuntimeException;
import java.nio.IntBuffer;

public final class DefaultTextureBinder
implements TextureBinder {
    public static final int ROUNDROBIN = 0;
    public static final int WEIGHTED = 1;
    public static final int MAX_GLES_UNITS = 32;
    private final int offset;
    private final int count;
    private final int reuseWeight;
    private final GLTexture[] textures;
    private final int[] weights;
    private final int method;
    private boolean reused;
    private int reuseCount = 0;
    private int bindCount = 0;
    private final TextureDescriptor tempDesc = new TextureDescriptor();
    private int currentTexture = 0;

    public DefaultTextureBinder(int method) {
        this(method, 0);
    }

    public DefaultTextureBinder(int method, int offset) {
        this(method, offset, -1);
    }

    public DefaultTextureBinder(int method, int offset, int count) {
        this(method, offset, count, 10);
    }

    public DefaultTextureBinder(int method, int offset, int count, int reuseWeight) {
        int max = Math.min(DefaultTextureBinder.getMaxTextureUnits(), 32);
        if (count < 0) {
            count = max - offset;
        }
        if (offset < 0 || count < 0 || offset + count > max || reuseWeight < 1) {
            throw new GdxRuntimeException("Illegal arguments");
        }
        this.method = method;
        this.offset = offset;
        this.count = count;
        this.textures = new GLTexture[count];
        this.reuseWeight = reuseWeight;
        this.weights = method == 1 ? new int[count] : null;
    }

    private static int getMaxTextureUnits() {
        IntBuffer buffer = BufferUtils.newIntBuffer(16);
        Gdx.gl.glGetIntegerv(34930, buffer);
        return buffer.get(0);
    }

    @Override
    public void begin() {
        for (int i = 0; i < this.count; ++i) {
            this.textures[i] = null;
            if (this.weights == null) continue;
            this.weights[i] = 0;
        }
    }

    @Override
    public void end() {
        Gdx.gl.glActiveTexture(33984);
    }

    @Override
    public final int bind(TextureDescriptor textureDesc) {
        return this.bindTexture(textureDesc, false);
    }

    @Override
    public final int bind(GLTexture texture) {
        this.tempDesc.set(texture, null, null, null, null);
        return this.bindTexture(this.tempDesc, false);
    }

    private final int bindTexture(TextureDescriptor textureDesc, boolean rebind) {
        int result;
        Object texture = textureDesc.texture;
        this.reused = false;
        switch (this.method) {
            case 0: {
                int idx = this.bindTextureRoundRobin((GLTexture)texture);
                result = this.offset + idx;
                break;
            }
            case 1: {
                int idx = this.bindTextureWeighted((GLTexture)texture);
                result = this.offset + idx;
                break;
            }
            default: {
                return -1;
            }
        }
        if (this.reused) {
            ++this.reuseCount;
            if (rebind) {
                ((GLTexture)texture).bind(result);
            } else {
                Gdx.gl.glActiveTexture(33984 + result);
            }
        } else {
            ++this.bindCount;
        }
        ((GLTexture)texture).unsafeSetWrap(textureDesc.uWrap, textureDesc.vWrap);
        ((GLTexture)texture).unsafeSetFilter(textureDesc.minFilter, textureDesc.magFilter);
        return result;
    }

    private final int bindTextureRoundRobin(GLTexture texture) {
        for (int i = 0; i < this.count; ++i) {
            int idx = (this.currentTexture + i) % this.count;
            if (this.textures[idx] != texture) continue;
            this.reused = true;
            return idx;
        }
        this.currentTexture = (this.currentTexture + 1) % this.count;
        this.textures[this.currentTexture] = texture;
        texture.bind(this.offset + this.currentTexture);
        return this.currentTexture;
    }

    private final int bindTextureWeighted(GLTexture texture) {
        int result = -1;
        int weight = this.weights[0];
        int windex = 0;
        for (int i = 0; i < this.count; ++i) {
            if (this.textures[i] == texture) {
                result = i;
                int n = i;
                this.weights[n] = this.weights[n] + this.reuseWeight;
                continue;
            }
            if (this.weights[i] >= 0) {
                int n = i;
                this.weights[n] = this.weights[n] - 1;
                if (this.weights[n] >= weight) continue;
            }
            weight = this.weights[i];
            windex = i;
        }
        if (result < 0) {
            this.textures[windex] = texture;
            this.weights[windex] = 100;
            result = windex;
            texture.bind(this.offset + result);
        } else {
            this.reused = true;
        }
        return result;
    }

    @Override
    public final int getBindCount() {
        return this.bindCount;
    }

    @Override
    public final int getReuseCount() {
        return this.reuseCount;
    }

    @Override
    public final void resetCounts() {
        this.reuseCount = 0;
        this.bindCount = 0;
    }
}

