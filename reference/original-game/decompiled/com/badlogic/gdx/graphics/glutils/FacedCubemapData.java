/*
 * Decompiled with CFR 0.152.
 */
package com.badlogic.gdx.graphics.glutils;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Cubemap;
import com.badlogic.gdx.graphics.CubemapData;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.TextureData;
import com.badlogic.gdx.graphics.glutils.PixmapTextureData;
import com.badlogic.gdx.utils.GdxRuntimeException;

public class FacedCubemapData
implements CubemapData {
    protected final TextureData[] data = new TextureData[6];

    public FacedCubemapData() {
        this((TextureData)null, (TextureData)null, (TextureData)null, (TextureData)null, (TextureData)null, (TextureData)null);
    }

    public FacedCubemapData(FileHandle positiveX, FileHandle negativeX, FileHandle positiveY, FileHandle negativeY, FileHandle positiveZ, FileHandle negativeZ) {
        this(TextureData.Factory.loadFromFile(positiveX, false), TextureData.Factory.loadFromFile(negativeX, false), TextureData.Factory.loadFromFile(positiveY, false), TextureData.Factory.loadFromFile(negativeY, false), TextureData.Factory.loadFromFile(positiveZ, false), TextureData.Factory.loadFromFile(negativeZ, false));
    }

    public FacedCubemapData(FileHandle positiveX, FileHandle negativeX, FileHandle positiveY, FileHandle negativeY, FileHandle positiveZ, FileHandle negativeZ, boolean useMipMaps) {
        this(TextureData.Factory.loadFromFile(positiveX, useMipMaps), TextureData.Factory.loadFromFile(negativeX, useMipMaps), TextureData.Factory.loadFromFile(positiveY, useMipMaps), TextureData.Factory.loadFromFile(negativeY, useMipMaps), TextureData.Factory.loadFromFile(positiveZ, useMipMaps), TextureData.Factory.loadFromFile(negativeZ, useMipMaps));
    }

    public FacedCubemapData(Pixmap positiveX, Pixmap negativeX, Pixmap positiveY, Pixmap negativeY, Pixmap positiveZ, Pixmap negativeZ) {
        this(positiveX, negativeX, positiveY, negativeY, positiveZ, negativeZ, false);
    }

    public FacedCubemapData(Pixmap positiveX, Pixmap negativeX, Pixmap positiveY, Pixmap negativeY, Pixmap positiveZ, Pixmap negativeZ, boolean useMipMaps) {
        this(positiveX == null ? null : new PixmapTextureData(positiveX, null, useMipMaps, false), negativeX == null ? null : new PixmapTextureData(negativeX, null, useMipMaps, false), positiveY == null ? null : new PixmapTextureData(positiveY, null, useMipMaps, false), negativeY == null ? null : new PixmapTextureData(negativeY, null, useMipMaps, false), positiveZ == null ? null : new PixmapTextureData(positiveZ, null, useMipMaps, false), negativeZ == null ? null : new PixmapTextureData(negativeZ, null, useMipMaps, false));
    }

    public FacedCubemapData(int width, int height, int depth, Pixmap.Format format) {
        this(new PixmapTextureData(new Pixmap(depth, height, format), null, false, true), new PixmapTextureData(new Pixmap(depth, height, format), null, false, true), new PixmapTextureData(new Pixmap(width, depth, format), null, false, true), new PixmapTextureData(new Pixmap(width, depth, format), null, false, true), new PixmapTextureData(new Pixmap(width, height, format), null, false, true), new PixmapTextureData(new Pixmap(width, height, format), null, false, true));
    }

    public FacedCubemapData(TextureData positiveX, TextureData negativeX, TextureData positiveY, TextureData negativeY, TextureData positiveZ, TextureData negativeZ) {
        this.data[0] = positiveX;
        this.data[1] = negativeX;
        this.data[2] = positiveY;
        this.data[3] = negativeY;
        this.data[4] = positiveZ;
        this.data[5] = negativeZ;
    }

    @Override
    public boolean isManaged() {
        for (TextureData data : this.data) {
            if (data.isManaged()) continue;
            return false;
        }
        return true;
    }

    public void load(Cubemap.CubemapSide side, FileHandle file) {
        this.data[side.index] = TextureData.Factory.loadFromFile(file, false);
    }

    public void load(Cubemap.CubemapSide side, Pixmap pixmap) {
        this.data[side.index] = pixmap == null ? null : new PixmapTextureData(pixmap, null, false, false);
    }

    public boolean isComplete() {
        for (int i = 0; i < this.data.length; ++i) {
            if (this.data[i] != null) continue;
            return false;
        }
        return true;
    }

    public TextureData getTextureData(Cubemap.CubemapSide side) {
        return this.data[side.index];
    }

    @Override
    public int getWidth() {
        int tmp;
        int width = 0;
        if (this.data[Cubemap.CubemapSide.PositiveZ.index] != null && (tmp = this.data[Cubemap.CubemapSide.PositiveZ.index].getWidth()) > width) {
            width = tmp;
        }
        if (this.data[Cubemap.CubemapSide.NegativeZ.index] != null && (tmp = this.data[Cubemap.CubemapSide.NegativeZ.index].getWidth()) > width) {
            width = tmp;
        }
        if (this.data[Cubemap.CubemapSide.PositiveY.index] != null && (tmp = this.data[Cubemap.CubemapSide.PositiveY.index].getWidth()) > width) {
            width = tmp;
        }
        if (this.data[Cubemap.CubemapSide.NegativeY.index] != null && (tmp = this.data[Cubemap.CubemapSide.NegativeY.index].getWidth()) > width) {
            width = tmp;
        }
        return width;
    }

    @Override
    public int getHeight() {
        int tmp;
        int height = 0;
        if (this.data[Cubemap.CubemapSide.PositiveZ.index] != null && (tmp = this.data[Cubemap.CubemapSide.PositiveZ.index].getHeight()) > height) {
            height = tmp;
        }
        if (this.data[Cubemap.CubemapSide.NegativeZ.index] != null && (tmp = this.data[Cubemap.CubemapSide.NegativeZ.index].getHeight()) > height) {
            height = tmp;
        }
        if (this.data[Cubemap.CubemapSide.PositiveX.index] != null && (tmp = this.data[Cubemap.CubemapSide.PositiveX.index].getHeight()) > height) {
            height = tmp;
        }
        if (this.data[Cubemap.CubemapSide.NegativeX.index] != null && (tmp = this.data[Cubemap.CubemapSide.NegativeX.index].getHeight()) > height) {
            height = tmp;
        }
        return height;
    }

    @Override
    public boolean isPrepared() {
        return false;
    }

    @Override
    public void prepare() {
        if (!this.isComplete()) {
            throw new GdxRuntimeException("You need to complete your cubemap data before using it");
        }
        for (int i = 0; i < this.data.length; ++i) {
            if (this.data[i].isPrepared()) continue;
            this.data[i].prepare();
        }
    }

    @Override
    public void consumeCubemapData() {
        for (int i = 0; i < this.data.length; ++i) {
            if (this.data[i].getType() == TextureData.TextureDataType.Custom) {
                this.data[i].consumeCustomData(34069 + i);
                continue;
            }
            Pixmap pixmap = this.data[i].consumePixmap();
            boolean disposePixmap = this.data[i].disposePixmap();
            if (this.data[i].getFormat() != pixmap.getFormat()) {
                Pixmap tmp = new Pixmap(pixmap.getWidth(), pixmap.getHeight(), this.data[i].getFormat());
                Pixmap.Blending blend = Pixmap.getBlending();
                Pixmap.setBlending(Pixmap.Blending.None);
                tmp.drawPixmap(pixmap, 0, 0, 0, 0, pixmap.getWidth(), pixmap.getHeight());
                Pixmap.setBlending(blend);
                if (this.data[i].disposePixmap()) {
                    pixmap.dispose();
                }
                pixmap = tmp;
                disposePixmap = true;
            }
            Gdx.gl.glPixelStorei(3317, 1);
            Gdx.gl.glTexImage2D(34069 + i, 0, pixmap.getGLInternalFormat(), pixmap.getWidth(), pixmap.getHeight(), 0, pixmap.getGLFormat(), pixmap.getGLType(), pixmap.getPixels());
            if (!disposePixmap) continue;
            pixmap.dispose();
        }
    }
}

