/*
 * Decompiled with CFR 0.152.
 */
package com.badlogic.gdx.graphics;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.PixmapIO;
import com.badlogic.gdx.graphics.glutils.ETC1TextureData;
import com.badlogic.gdx.graphics.glutils.FileTextureData;
import com.badlogic.gdx.graphics.glutils.KTXTextureData;

public interface TextureData {
    public TextureDataType getType();

    public boolean isPrepared();

    public void prepare();

    public Pixmap consumePixmap();

    public boolean disposePixmap();

    public void consumeCustomData(int var1);

    public int getWidth();

    public int getHeight();

    public Pixmap.Format getFormat();

    public boolean useMipMaps();

    public boolean isManaged();

    public static class Factory {
        public static TextureData loadFromFile(FileHandle file, boolean useMipMaps) {
            return Factory.loadFromFile(file, null, useMipMaps);
        }

        public static TextureData loadFromFile(FileHandle file, Pixmap.Format format, boolean useMipMaps) {
            if (file == null) {
                return null;
            }
            if (file.name().endsWith(".cim")) {
                return new FileTextureData(file, PixmapIO.readCIM(file), format, useMipMaps);
            }
            if (file.name().endsWith(".etc1")) {
                return new ETC1TextureData(file, useMipMaps);
            }
            if (file.name().endsWith(".ktx") || file.name().endsWith(".zktx")) {
                return new KTXTextureData(file, useMipMaps);
            }
            return new FileTextureData(file, new Pixmap(file), format, useMipMaps);
        }
    }

    public static enum TextureDataType {
        Pixmap,
        Custom;

    }
}

