/*
 * Decompiled with CFR 0.152.
 */
package com.badlogic.gdx.graphics.glutils;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Cubemap;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.glutils.GLFrameBuffer;
import com.badlogic.gdx.graphics.glutils.GLOnlyTextureData;
import com.badlogic.gdx.utils.GdxRuntimeException;

public class FrameBufferCubemap
extends GLFrameBuffer<Cubemap> {
    private int currentSide;

    public FrameBufferCubemap(Pixmap.Format format, int width, int height, boolean hasDepth) {
        this(format, width, height, hasDepth, false);
    }

    public FrameBufferCubemap(Pixmap.Format format, int width, int height, boolean hasDepth, boolean hasStencil) {
        super(format, width, height, hasDepth, hasStencil);
    }

    @Override
    protected Cubemap createColorTexture() {
        int glFormat = Pixmap.Format.toGlFormat(this.format);
        int glType = Pixmap.Format.toGlType(this.format);
        GLOnlyTextureData data = new GLOnlyTextureData(this.width, this.height, 0, glFormat, glFormat, glType);
        Cubemap result = new Cubemap(data, data, data, data, data, data);
        result.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        result.setWrap(Texture.TextureWrap.ClampToEdge, Texture.TextureWrap.ClampToEdge);
        return result;
    }

    @Override
    protected void disposeColorTexture(Cubemap colorTexture) {
        colorTexture.dispose();
    }

    @Override
    protected void attachFrameBufferColorTexture() {
        Cubemap.CubemapSide[] sides;
        GL20 gl = Gdx.gl20;
        int glHandle = ((Cubemap)this.colorTexture).getTextureObjectHandle();
        for (Cubemap.CubemapSide side : sides = Cubemap.CubemapSide.values()) {
            gl.glFramebufferTexture2D(36160, 36064, side.glEnum, glHandle, 0);
        }
    }

    @Override
    public void bind() {
        this.currentSide = -1;
        super.bind();
    }

    public boolean nextSide() {
        if (this.currentSide > 5) {
            throw new GdxRuntimeException("No remaining sides.");
        }
        if (this.currentSide == 5) {
            return false;
        }
        ++this.currentSide;
        this.bindSide(this.getSide());
        return true;
    }

    protected void bindSide(Cubemap.CubemapSide side) {
        Gdx.gl20.glFramebufferTexture2D(36160, 36064, side.glEnum, ((Cubemap)this.colorTexture).getTextureObjectHandle(), 0);
    }

    public Cubemap.CubemapSide getSide() {
        return this.currentSide < 0 ? null : Cubemap.CubemapSide.values()[this.currentSide];
    }
}

