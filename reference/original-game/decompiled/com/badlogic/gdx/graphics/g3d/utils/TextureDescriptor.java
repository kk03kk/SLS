/*
 * Decompiled with CFR 0.152.
 */
package com.badlogic.gdx.graphics.g3d.utils;

import com.badlogic.gdx.graphics.GLTexture;
import com.badlogic.gdx.graphics.Texture;

public class TextureDescriptor<T extends GLTexture>
implements Comparable<TextureDescriptor<T>> {
    public T texture = null;
    public Texture.TextureFilter minFilter;
    public Texture.TextureFilter magFilter;
    public Texture.TextureWrap uWrap;
    public Texture.TextureWrap vWrap;

    public TextureDescriptor(T texture, Texture.TextureFilter minFilter, Texture.TextureFilter magFilter, Texture.TextureWrap uWrap, Texture.TextureWrap vWrap) {
        this.set(texture, minFilter, magFilter, uWrap, vWrap);
    }

    public TextureDescriptor(T texture) {
        this(texture, null, null, null, null);
    }

    public TextureDescriptor() {
    }

    public void set(T texture, Texture.TextureFilter minFilter, Texture.TextureFilter magFilter, Texture.TextureWrap uWrap, Texture.TextureWrap vWrap) {
        this.texture = texture;
        this.minFilter = minFilter;
        this.magFilter = magFilter;
        this.uWrap = uWrap;
        this.vWrap = vWrap;
    }

    public <V extends T> void set(TextureDescriptor<V> other) {
        this.texture = other.texture;
        this.minFilter = other.minFilter;
        this.magFilter = other.magFilter;
        this.uWrap = other.uWrap;
        this.vWrap = other.vWrap;
    }

    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof TextureDescriptor)) {
            return false;
        }
        TextureDescriptor other = (TextureDescriptor)obj;
        return other.texture == this.texture && other.minFilter == this.minFilter && other.magFilter == this.magFilter && other.uWrap == this.uWrap && other.vWrap == this.vWrap;
    }

    public int hashCode() {
        long result = this.texture == null ? 0 : ((GLTexture)this.texture).glTarget;
        result = 811L * result + (long)(this.texture == null ? 0 : ((GLTexture)this.texture).getTextureObjectHandle());
        result = 811L * result + (long)(this.minFilter == null ? 0 : this.minFilter.getGLEnum());
        result = 811L * result + (long)(this.magFilter == null ? 0 : this.magFilter.getGLEnum());
        result = 811L * result + (long)(this.uWrap == null ? 0 : this.uWrap.getGLEnum());
        result = 811L * result + (long)(this.vWrap == null ? 0 : this.vWrap.getGLEnum());
        return (int)(result ^ result >> 32);
    }

    @Override
    public int compareTo(TextureDescriptor<T> o) {
        int h2;
        int t2;
        if (o == this) {
            return 0;
        }
        int t1 = this.texture == null ? 0 : ((GLTexture)this.texture).glTarget;
        int n = t2 = o.texture == null ? 0 : ((GLTexture)o.texture).glTarget;
        if (t1 != t2) {
            return t1 - t2;
        }
        int h1 = this.texture == null ? 0 : ((GLTexture)this.texture).getTextureObjectHandle();
        int n2 = h2 = o.texture == null ? 0 : ((GLTexture)o.texture).getTextureObjectHandle();
        if (h1 != h2) {
            return h1 - h2;
        }
        if (this.minFilter != o.minFilter) {
            return (this.minFilter == null ? 0 : this.minFilter.getGLEnum()) - (o.minFilter == null ? 0 : o.minFilter.getGLEnum());
        }
        if (this.magFilter != o.magFilter) {
            return (this.magFilter == null ? 0 : this.magFilter.getGLEnum()) - (o.magFilter == null ? 0 : o.magFilter.getGLEnum());
        }
        if (this.uWrap != o.uWrap) {
            return (this.uWrap == null ? 0 : this.uWrap.getGLEnum()) - (o.uWrap == null ? 0 : o.uWrap.getGLEnum());
        }
        if (this.vWrap != o.vWrap) {
            return (this.vWrap == null ? 0 : this.vWrap.getGLEnum()) - (o.vWrap == null ? 0 : o.vWrap.getGLEnum());
        }
        return 0;
    }
}

