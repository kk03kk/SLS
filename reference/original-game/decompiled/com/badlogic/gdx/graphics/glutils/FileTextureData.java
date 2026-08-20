/*
 * Decompiled with CFR 0.152.
 */
package com.badlogic.gdx.graphics.glutils;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.PixmapIO;
import com.badlogic.gdx.graphics.TextureData;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.GdxRuntimeException;

public class FileTextureData
implements TextureData {
    public static boolean copyToPOT;
    final FileHandle file;
    int width = 0;
    int height = 0;
    Pixmap.Format format;
    Pixmap pixmap;
    boolean useMipMaps;
    boolean isPrepared = false;

    public FileTextureData(FileHandle file, Pixmap preloadedPixmap, Pixmap.Format format, boolean useMipMaps) {
        this.file = file;
        this.pixmap = preloadedPixmap;
        this.format = format;
        this.useMipMaps = useMipMaps;
        if (this.pixmap != null) {
            this.pixmap = this.ensurePot(this.pixmap);
            this.width = this.pixmap.getWidth();
            this.height = this.pixmap.getHeight();
            if (format == null) {
                this.format = this.pixmap.getFormat();
            }
        }
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
        if (this.pixmap == null) {
            this.pixmap = this.file.extension().equals("cim") ? PixmapIO.readCIM(this.file) : this.ensurePot(new Pixmap(this.file));
            this.width = this.pixmap.getWidth();
            this.height = this.pixmap.getHeight();
            if (this.format == null) {
                this.format = this.pixmap.getFormat();
            }
        }
        this.isPrepared = true;
    }

    private Pixmap ensurePot(Pixmap pixmap) {
        if (Gdx.gl20 == null && copyToPOT) {
            int pixmapWidth = pixmap.getWidth();
            int pixmapHeight = pixmap.getHeight();
            int potWidth = MathUtils.nextPowerOfTwo(pixmapWidth);
            int potHeight = MathUtils.nextPowerOfTwo(pixmapHeight);
            if (pixmapWidth != potWidth || pixmapHeight != potHeight) {
                Pixmap tmp = new Pixmap(potWidth, potHeight, pixmap.getFormat());
                tmp.drawPixmap(pixmap, 0, 0, 0, 0, pixmapWidth, pixmapHeight);
                pixmap.dispose();
                return tmp;
            }
        }
        return pixmap;
    }

    @Override
    public Pixmap consumePixmap() {
        if (!this.isPrepared) {
            throw new GdxRuntimeException("Call prepare() before calling getPixmap()");
        }
        this.isPrepared = false;
        Pixmap pixmap = this.pixmap;
        this.pixmap = null;
        return pixmap;
    }

    @Override
    public boolean disposePixmap() {
        return true;
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
        return this.format;
    }

    @Override
    public boolean useMipMaps() {
        return this.useMipMaps;
    }

    @Override
    public boolean isManaged() {
        return true;
    }

    public FileHandle getFileHandle() {
        return this.file;
    }

    @Override
    public TextureData.TextureDataType getType() {
        return TextureData.TextureDataType.Pixmap;
    }

    @Override
    public void consumeCustomData(int target) {
        throw new GdxRuntimeException("This TextureData implementation does not upload data itself");
    }
}

