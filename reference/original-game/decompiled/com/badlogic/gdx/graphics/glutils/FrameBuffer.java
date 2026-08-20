/*
 * Decompiled with CFR 0.152.
 */
package com.badlogic.gdx.graphics.glutils;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.glutils.GLFrameBuffer;
import com.badlogic.gdx.graphics.glutils.GLOnlyTextureData;

public class FrameBuffer
extends GLFrameBuffer<Texture> {
    public FrameBuffer(Pixmap.Format format, int width, int height, boolean hasDepth) {
        this(format, width, height, hasDepth, false);
    }

    public FrameBuffer(Pixmap.Format format, int width, int height, boolean hasDepth, boolean hasStencil) {
        super(format, width, height, hasDepth, hasStencil);
    }

    @Override
    protected Texture createColorTexture() {
        int glFormat = Pixmap.Format.toGlFormat(this.format);
        int glType = Pixmap.Format.toGlType(this.format);
        GLOnlyTextureData data = new GLOnlyTextureData(this.width, this.height, 0, glFormat, glFormat, glType);
        Texture result = new Texture(data);
        result.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        result.setWrap(Texture.TextureWrap.ClampToEdge, Texture.TextureWrap.ClampToEdge);
        return result;
    }

    @Override
    protected void disposeColorTexture(Texture colorTexture) {
        colorTexture.dispose();
    }

    @Override
    protected void attachFrameBufferColorTexture() {
        Gdx.gl20.glFramebufferTexture2D(36160, 36064, 3553, ((Texture)this.colorTexture).getTextureObjectHandle(), 0);
    }

    public static void unbind() {
        GLFrameBuffer.unbind();
    }
}

