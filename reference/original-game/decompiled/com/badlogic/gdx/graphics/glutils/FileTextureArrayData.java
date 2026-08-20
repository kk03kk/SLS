/*
 * Decompiled with CFR 0.152.
 */
package com.badlogic.gdx.graphics.glutils;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.TextureArrayData;
import com.badlogic.gdx.graphics.TextureData;
import com.badlogic.gdx.utils.GdxRuntimeException;

public class FileTextureArrayData
implements TextureArrayData {
    private TextureData[] textureDatas;
    private boolean prepared;
    private Pixmap.Format format;
    private int depth;
    boolean useMipMaps;

    public FileTextureArrayData(Pixmap.Format format, boolean useMipMaps, FileHandle[] files) {
        this.format = format;
        this.useMipMaps = useMipMaps;
        this.depth = files.length;
        this.textureDatas = new TextureData[files.length];
        for (int i = 0; i < files.length; ++i) {
            this.textureDatas[i] = TextureData.Factory.loadFromFile(files[i], format, useMipMaps);
        }
    }

    @Override
    public boolean isPrepared() {
        return this.prepared;
    }

    @Override
    public void prepare() {
        int width = -1;
        int height = -1;
        for (TextureData data : this.textureDatas) {
            data.prepare();
            if (width == -1) {
                width = data.getWidth();
                height = data.getHeight();
                continue;
            }
            if (width == data.getWidth() && height == data.getHeight()) continue;
            throw new GdxRuntimeException("Error whilst preparing TextureArray: TextureArray Textures must have equal dimensions.");
        }
        this.prepared = true;
    }

    @Override
    public void consumeTextureArrayData() {
        for (int i = 0; i < this.textureDatas.length; ++i) {
            if (this.textureDatas[i].getType() == TextureData.TextureDataType.Custom) {
                this.textureDatas[i].consumeCustomData(35866);
                continue;
            }
            TextureData texData = this.textureDatas[i];
            Pixmap pixmap = texData.consumePixmap();
            boolean disposePixmap = texData.disposePixmap();
            if (texData.getFormat() != pixmap.getFormat()) {
                Pixmap temp = new Pixmap(pixmap.getWidth(), pixmap.getHeight(), texData.getFormat());
                Pixmap.Blending blendmode = Pixmap.getBlending();
                Pixmap.setBlending(Pixmap.Blending.None);
                temp.drawPixmap(pixmap, 0, 0, 0, 0, pixmap.getWidth(), pixmap.getHeight());
                Pixmap.setBlending(blendmode);
                if (texData.disposePixmap()) {
                    pixmap.dispose();
                }
                pixmap = temp;
                disposePixmap = true;
            }
            Gdx.gl30.glTexSubImage3D(35866, 0, 0, 0, i, pixmap.getWidth(), pixmap.getHeight(), 1, pixmap.getGLInternalFormat(), pixmap.getGLType(), pixmap.getPixels());
            if (!disposePixmap) continue;
            pixmap.dispose();
        }
    }

    @Override
    public int getWidth() {
        return this.textureDatas[0].getWidth();
    }

    @Override
    public int getHeight() {
        return this.textureDatas[0].getHeight();
    }

    @Override
    public int getDepth() {
        return this.depth;
    }

    @Override
    public int getInternalFormat() {
        return Pixmap.Format.toGlFormat(this.format);
    }

    @Override
    public int getGLType() {
        return Pixmap.Format.toGlType(this.format);
    }

    @Override
    public boolean isManaged() {
        for (TextureData data : this.textureDatas) {
            if (data.isManaged()) continue;
            return false;
        }
        return true;
    }
}

