/*
 * Decompiled with CFR 0.152.
 */
package com.badlogic.gdx.graphics;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.TextureData;
import com.badlogic.gdx.graphics.glutils.MipMapGenerator;
import com.badlogic.gdx.utils.Disposable;

public abstract class GLTexture
implements Disposable {
    public final int glTarget;
    protected int glHandle;
    protected Texture.TextureFilter minFilter = Texture.TextureFilter.Nearest;
    protected Texture.TextureFilter magFilter = Texture.TextureFilter.Nearest;
    protected Texture.TextureWrap uWrap = Texture.TextureWrap.ClampToEdge;
    protected Texture.TextureWrap vWrap = Texture.TextureWrap.ClampToEdge;

    public abstract int getWidth();

    public abstract int getHeight();

    public abstract int getDepth();

    public GLTexture(int glTarget) {
        this(glTarget, Gdx.gl.glGenTexture());
    }

    public GLTexture(int glTarget, int glHandle) {
        this.glTarget = glTarget;
        this.glHandle = glHandle;
    }

    public abstract boolean isManaged();

    protected abstract void reload();

    public void bind() {
        Gdx.gl.glBindTexture(this.glTarget, this.glHandle);
    }

    public void bind(int unit) {
        Gdx.gl.glActiveTexture(33984 + unit);
        Gdx.gl.glBindTexture(this.glTarget, this.glHandle);
    }

    public Texture.TextureFilter getMinFilter() {
        return this.minFilter;
    }

    public Texture.TextureFilter getMagFilter() {
        return this.magFilter;
    }

    public Texture.TextureWrap getUWrap() {
        return this.uWrap;
    }

    public Texture.TextureWrap getVWrap() {
        return this.vWrap;
    }

    public int getTextureObjectHandle() {
        return this.glHandle;
    }

    public void unsafeSetWrap(Texture.TextureWrap u, Texture.TextureWrap v) {
        this.unsafeSetWrap(u, v, false);
    }

    public void unsafeSetWrap(Texture.TextureWrap u, Texture.TextureWrap v, boolean force) {
        if (u != null && (force || this.uWrap != u)) {
            Gdx.gl.glTexParameterf(this.glTarget, 10242, u.getGLEnum());
            this.uWrap = u;
        }
        if (v != null && (force || this.vWrap != v)) {
            Gdx.gl.glTexParameterf(this.glTarget, 10243, v.getGLEnum());
            this.vWrap = v;
        }
    }

    public void setWrap(Texture.TextureWrap u, Texture.TextureWrap v) {
        this.uWrap = u;
        this.vWrap = v;
        this.bind();
        Gdx.gl.glTexParameterf(this.glTarget, 10242, u.getGLEnum());
        Gdx.gl.glTexParameterf(this.glTarget, 10243, v.getGLEnum());
    }

    public void unsafeSetFilter(Texture.TextureFilter minFilter, Texture.TextureFilter magFilter) {
        this.unsafeSetFilter(minFilter, magFilter, false);
    }

    public void unsafeSetFilter(Texture.TextureFilter minFilter, Texture.TextureFilter magFilter, boolean force) {
        if (minFilter != null && (force || this.minFilter != minFilter)) {
            Gdx.gl.glTexParameterf(this.glTarget, 10241, minFilter.getGLEnum());
            this.minFilter = minFilter;
        }
        if (magFilter != null && (force || this.magFilter != magFilter)) {
            Gdx.gl.glTexParameterf(this.glTarget, 10240, magFilter.getGLEnum());
            this.magFilter = magFilter;
        }
    }

    public void setFilter(Texture.TextureFilter minFilter, Texture.TextureFilter magFilter) {
        this.minFilter = minFilter;
        this.magFilter = magFilter;
        this.bind();
        Gdx.gl.glTexParameterf(this.glTarget, 10241, minFilter.getGLEnum());
        Gdx.gl.glTexParameterf(this.glTarget, 10240, magFilter.getGLEnum());
    }

    protected void delete() {
        if (this.glHandle != 0) {
            Gdx.gl.glDeleteTexture(this.glHandle);
            this.glHandle = 0;
        }
    }

    @Override
    public void dispose() {
        this.delete();
    }

    protected static void uploadImageData(int target, TextureData data) {
        GLTexture.uploadImageData(target, data, 0);
    }

    public static void uploadImageData(int target, TextureData data, int miplevel) {
        TextureData.TextureDataType type;
        if (data == null) {
            return;
        }
        if (!data.isPrepared()) {
            data.prepare();
        }
        if ((type = data.getType()) == TextureData.TextureDataType.Custom) {
            data.consumeCustomData(target);
            return;
        }
        Pixmap pixmap = data.consumePixmap();
        boolean disposePixmap = data.disposePixmap();
        if (data.getFormat() != pixmap.getFormat()) {
            Pixmap tmp = new Pixmap(pixmap.getWidth(), pixmap.getHeight(), data.getFormat());
            Pixmap.Blending blend = Pixmap.getBlending();
            Pixmap.setBlending(Pixmap.Blending.None);
            tmp.drawPixmap(pixmap, 0, 0, 0, 0, pixmap.getWidth(), pixmap.getHeight());
            Pixmap.setBlending(blend);
            if (data.disposePixmap()) {
                pixmap.dispose();
            }
            pixmap = tmp;
            disposePixmap = true;
        }
        Gdx.gl.glPixelStorei(3317, 1);
        if (data.useMipMaps()) {
            MipMapGenerator.generateMipMap(target, pixmap, pixmap.getWidth(), pixmap.getHeight());
        } else {
            Gdx.gl.glTexImage2D(target, miplevel, pixmap.getGLInternalFormat(), pixmap.getWidth(), pixmap.getHeight(), 0, pixmap.getGLFormat(), pixmap.getGLType(), pixmap.getPixels());
        }
        if (disposePixmap) {
            pixmap.dispose();
        }
    }
}

