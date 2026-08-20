/*
 * Decompiled with CFR 0.152.
 */
package com.badlogic.gdx.graphics;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Gdx2DPixmap;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.GdxRuntimeException;
import java.io.IOException;
import java.nio.ByteBuffer;

public class Pixmap
implements Disposable {
    private static Blending blending = Blending.SourceOver;
    final Gdx2DPixmap pixmap;
    int color = 0;
    private boolean disposed;

    public static void setBlending(Blending blending) {
        Pixmap.blending = blending;
        Gdx2DPixmap.setBlend(blending == Blending.None ? 0 : 1);
    }

    public static void setFilter(Filter filter) {
        Gdx2DPixmap.setScale(filter == Filter.NearestNeighbour ? 0 : 1);
    }

    public Pixmap(int width, int height, Format format) {
        this.pixmap = new Gdx2DPixmap(width, height, Format.toGdx2DPixmapFormat(format));
        this.setColor(0.0f, 0.0f, 0.0f, 0.0f);
        this.fill();
    }

    public Pixmap(byte[] encodedData, int offset, int len) {
        try {
            this.pixmap = new Gdx2DPixmap(encodedData, offset, len, 0);
        }
        catch (IOException e) {
            throw new GdxRuntimeException("Couldn't load pixmap from image data", e);
        }
    }

    public Pixmap(FileHandle file) {
        try {
            byte[] bytes = file.readBytes();
            this.pixmap = new Gdx2DPixmap(bytes, 0, bytes.length, 0);
        }
        catch (Exception e) {
            throw new GdxRuntimeException("Couldn't load file: " + file, e);
        }
    }

    public Pixmap(Gdx2DPixmap pixmap) {
        this.pixmap = pixmap;
    }

    public void setColor(int color) {
        this.color = color;
    }

    public void setColor(float r, float g, float b, float a) {
        this.color = Color.rgba8888(r, g, b, a);
    }

    public void setColor(Color color) {
        this.color = Color.rgba8888(color.r, color.g, color.b, color.a);
    }

    public void fill() {
        this.pixmap.clear(this.color);
    }

    public void drawLine(int x, int y, int x2, int y2) {
        this.pixmap.drawLine(x, y, x2, y2, this.color);
    }

    public void drawRectangle(int x, int y, int width, int height) {
        this.pixmap.drawRect(x, y, width, height, this.color);
    }

    public void drawPixmap(Pixmap pixmap, int x, int y) {
        this.drawPixmap(pixmap, x, y, 0, 0, pixmap.getWidth(), pixmap.getHeight());
    }

    public void drawPixmap(Pixmap pixmap, int x, int y, int srcx, int srcy, int srcWidth, int srcHeight) {
        this.pixmap.drawPixmap(pixmap.pixmap, srcx, srcy, x, y, srcWidth, srcHeight);
    }

    public void drawPixmap(Pixmap pixmap, int srcx, int srcy, int srcWidth, int srcHeight, int dstx, int dsty, int dstWidth, int dstHeight) {
        this.pixmap.drawPixmap(pixmap.pixmap, srcx, srcy, srcWidth, srcHeight, dstx, dsty, dstWidth, dstHeight);
    }

    public void fillRectangle(int x, int y, int width, int height) {
        this.pixmap.fillRect(x, y, width, height, this.color);
    }

    public void drawCircle(int x, int y, int radius) {
        this.pixmap.drawCircle(x, y, radius, this.color);
    }

    public void fillCircle(int x, int y, int radius) {
        this.pixmap.fillCircle(x, y, radius, this.color);
    }

    public void fillTriangle(int x1, int y1, int x2, int y2, int x3, int y3) {
        this.pixmap.fillTriangle(x1, y1, x2, y2, x3, y3, this.color);
    }

    public int getPixel(int x, int y) {
        return this.pixmap.getPixel(x, y);
    }

    public int getWidth() {
        return this.pixmap.getWidth();
    }

    public int getHeight() {
        return this.pixmap.getHeight();
    }

    @Override
    public void dispose() {
        if (this.disposed) {
            throw new GdxRuntimeException("Pixmap already disposed!");
        }
        this.pixmap.dispose();
        this.disposed = true;
    }

    public void drawPixel(int x, int y) {
        this.pixmap.setPixel(x, y, this.color);
    }

    public void drawPixel(int x, int y, int color) {
        this.pixmap.setPixel(x, y, color);
    }

    public int getGLFormat() {
        return this.pixmap.getGLFormat();
    }

    public int getGLInternalFormat() {
        return this.pixmap.getGLInternalFormat();
    }

    public int getGLType() {
        return this.pixmap.getGLType();
    }

    public ByteBuffer getPixels() {
        if (this.disposed) {
            throw new GdxRuntimeException("Pixmap already disposed");
        }
        return this.pixmap.getPixels();
    }

    public Format getFormat() {
        return Format.fromGdx2DPixmapFormat(this.pixmap.getFormat());
    }

    public static Blending getBlending() {
        return blending;
    }

    public static enum Filter {
        NearestNeighbour,
        BiLinear;

    }

    public static enum Blending {
        None,
        SourceOver;

    }

    public static enum Format {
        Alpha,
        Intensity,
        LuminanceAlpha,
        RGB565,
        RGBA4444,
        RGB888,
        RGBA8888;


        public static int toGdx2DPixmapFormat(Format format) {
            if (format == Alpha) {
                return 1;
            }
            if (format == Intensity) {
                return 1;
            }
            if (format == LuminanceAlpha) {
                return 2;
            }
            if (format == RGB565) {
                return 5;
            }
            if (format == RGBA4444) {
                return 6;
            }
            if (format == RGB888) {
                return 3;
            }
            if (format == RGBA8888) {
                return 4;
            }
            throw new GdxRuntimeException("Unknown Format: " + (Object)((Object)format));
        }

        public static Format fromGdx2DPixmapFormat(int format) {
            if (format == 1) {
                return Alpha;
            }
            if (format == 2) {
                return LuminanceAlpha;
            }
            if (format == 5) {
                return RGB565;
            }
            if (format == 6) {
                return RGBA4444;
            }
            if (format == 3) {
                return RGB888;
            }
            if (format == 4) {
                return RGBA8888;
            }
            throw new GdxRuntimeException("Unknown Gdx2DPixmap Format: " + format);
        }

        public static int toGlFormat(Format format) {
            return Gdx2DPixmap.toGlFormat(Format.toGdx2DPixmapFormat(format));
        }

        public static int toGlType(Format format) {
            return Gdx2DPixmap.toGlType(Format.toGdx2DPixmapFormat(format));
        }
    }
}

