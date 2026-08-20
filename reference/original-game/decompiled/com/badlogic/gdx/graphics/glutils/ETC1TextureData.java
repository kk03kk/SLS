/*
 * Decompiled with CFR 0.152.
 */
package com.badlogic.gdx.graphics.glutils;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.TextureData;
import com.badlogic.gdx.graphics.glutils.ETC1;
import com.badlogic.gdx.graphics.glutils.MipMapGenerator;
import com.badlogic.gdx.utils.GdxRuntimeException;

public class ETC1TextureData
implements TextureData {
    FileHandle file;
    ETC1.ETC1Data data;
    boolean useMipMaps;
    int width = 0;
    int height = 0;
    boolean isPrepared = false;

    public ETC1TextureData(FileHandle file) {
        this(file, false);
    }

    public ETC1TextureData(FileHandle file, boolean useMipMaps) {
        this.file = file;
        this.useMipMaps = useMipMaps;
    }

    public ETC1TextureData(ETC1.ETC1Data encodedImage, boolean useMipMaps) {
        this.data = encodedImage;
        this.useMipMaps = useMipMaps;
    }

    @Override
    public TextureData.TextureDataType getType() {
        return TextureData.TextureDataType.Custom;
    }

    @Override
    public boolean isPrepared() {
        return this.isPrepared;
    }

    @Override
    public void prepare() {
        if (this.isPrepared) {
            throw new GdxRuntimeException("Already prepared");
        }
        if (this.file == null && this.data == null) {
            throw new GdxRuntimeException("Can only load once from ETC1Data");
        }
        if (this.file != null) {
            this.data = new ETC1.ETC1Data(this.file);
        }
        this.width = this.data.width;
        this.height = this.data.height;
        this.isPrepared = true;
    }

    @Override
    public void consumeCustomData(int target) {
        if (!this.isPrepared) {
            throw new GdxRuntimeException("Call prepare() before calling consumeCompressedData()");
        }
        if (!Gdx.graphics.supportsExtension("GL_OES_compressed_ETC1_RGB8_texture")) {
            Pixmap pixmap = ETC1.decodeImage(this.data, Pixmap.Format.RGB565);
            Gdx.gl.glTexImage2D(target, 0, pixmap.getGLInternalFormat(), pixmap.getWidth(), pixmap.getHeight(), 0, pixmap.getGLFormat(), pixmap.getGLType(), pixmap.getPixels());
            if (this.useMipMaps) {
                MipMapGenerator.generateMipMap(target, pixmap, pixmap.getWidth(), pixmap.getHeight());
            }
            pixmap.dispose();
            this.useMipMaps = false;
        } else {
            Gdx.gl.glCompressedTexImage2D(target, 0, ETC1.ETC1_RGB8_OES, this.width, this.height, 0, this.data.compressedData.capacity() - this.data.dataOffset, this.data.compressedData);
            if (this.useMipMaps()) {
                Gdx.gl20.glGenerateMipmap(3553);
            }
        }
        this.data.dispose();
        this.data = null;
        this.isPrepared = false;
    }

    @Override
    public Pixmap consumePixmap() {
        throw new GdxRuntimeException("This TextureData implementation does not return a Pixmap");
    }

    @Override
    public boolean disposePixmap() {
        throw new GdxRuntimeException("This TextureData implementation does not return a Pixmap");
    }

    @Override
    public int getWidth() {
        return this.width;
    }

    @Override
    public int getHeight() {
        return this.height;
    }

    @Override
    public Pixmap.Format getFormat() {
        return Pixmap.Format.RGB565;
    }

    @Override
    public boolean useMipMaps() {
        return this.useMipMaps;
    }

    @Override
    public boolean isManaged() {
        return true;
    }
}

