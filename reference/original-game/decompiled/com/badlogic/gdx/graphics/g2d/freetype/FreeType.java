/*
 * Decompiled with CFR 0.152.
 */
package com.badlogic.gdx.graphics.g2d.freetype;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.utils.BufferUtils;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.GdxRuntimeException;
import com.badlogic.gdx.utils.LongMap;
import com.badlogic.gdx.utils.SharedLibraryLoader;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;

public class FreeType {
    public static int FT_PIXEL_MODE_NONE = 0;
    public static int FT_PIXEL_MODE_MONO = 1;
    public static int FT_PIXEL_MODE_GRAY = 2;
    public static int FT_PIXEL_MODE_GRAY2 = 3;
    public static int FT_PIXEL_MODE_GRAY4 = 4;
    public static int FT_PIXEL_MODE_LCD = 5;
    public static int FT_PIXEL_MODE_LCD_V = 6;
    public static int FT_ENCODING_NONE = 0;
    public static int FT_ENCODING_MS_SYMBOL = FreeType.encode('s', 'y', 'm', 'b');
    public static int FT_ENCODING_UNICODE = FreeType.encode('u', 'n', 'i', 'c');
    public static int FT_ENCODING_SJIS = FreeType.encode('s', 'j', 'i', 's');
    public static int FT_ENCODING_GB2312 = FreeType.encode('g', 'b', ' ', ' ');
    public static int FT_ENCODING_BIG5 = FreeType.encode('b', 'i', 'g', '5');
    public static int FT_ENCODING_WANSUNG = FreeType.encode('w', 'a', 'n', 's');
    public static int FT_ENCODING_JOHAB = FreeType.encode('j', 'o', 'h', 'a');
    public static int FT_ENCODING_ADOBE_STANDARD = FreeType.encode('A', 'D', 'O', 'B');
    public static int FT_ENCODING_ADOBE_EXPERT = FreeType.encode('A', 'D', 'B', 'E');
    public static int FT_ENCODING_ADOBE_CUSTOM = FreeType.encode('A', 'D', 'B', 'C');
    public static int FT_ENCODING_ADOBE_LATIN_1 = FreeType.encode('l', 'a', 't', '1');
    public static int FT_ENCODING_OLD_LATIN_2 = FreeType.encode('l', 'a', 't', '2');
    public static int FT_ENCODING_APPLE_ROMAN = FreeType.encode('a', 'r', 'm', 'n');
    public static int FT_FACE_FLAG_SCALABLE = 1;
    public static int FT_FACE_FLAG_FIXED_SIZES = 2;
    public static int FT_FACE_FLAG_FIXED_WIDTH = 4;
    public static int FT_FACE_FLAG_SFNT = 8;
    public static int FT_FACE_FLAG_HORIZONTAL = 16;
    public static int FT_FACE_FLAG_VERTICAL = 32;
    public static int FT_FACE_FLAG_KERNING = 64;
    public static int FT_FACE_FLAG_FAST_GLYPHS = 128;
    public static int FT_FACE_FLAG_MULTIPLE_MASTERS = 256;
    public static int FT_FACE_FLAG_GLYPH_NAMES = 512;
    public static int FT_FACE_FLAG_EXTERNAL_STREAM = 1024;
    public static int FT_FACE_FLAG_HINTER = 2048;
    public static int FT_FACE_FLAG_CID_KEYED = 4096;
    public static int FT_FACE_FLAG_TRICKY = 8192;
    public static int FT_STYLE_FLAG_ITALIC = 1;
    public static int FT_STYLE_FLAG_BOLD = 2;
    public static int FT_LOAD_DEFAULT = 0;
    public static int FT_LOAD_NO_SCALE = 1;
    public static int FT_LOAD_NO_HINTING = 2;
    public static int FT_LOAD_RENDER = 4;
    public static int FT_LOAD_NO_BITMAP = 8;
    public static int FT_LOAD_VERTICAL_LAYOUT = 16;
    public static int FT_LOAD_FORCE_AUTOHINT = 32;
    public static int FT_LOAD_CROP_BITMAP = 64;
    public static int FT_LOAD_PEDANTIC = 128;
    public static int FT_LOAD_IGNORE_GLOBAL_ADVANCE_WIDTH = 512;
    public static int FT_LOAD_NO_RECURSE = 1024;
    public static int FT_LOAD_IGNORE_TRANSFORM = 2048;
    public static int FT_LOAD_MONOCHROME = 4096;
    public static int FT_LOAD_LINEAR_DESIGN = 8192;
    public static int FT_LOAD_NO_AUTOHINT = 32768;
    public static int FT_LOAD_TARGET_NORMAL = 0;
    public static int FT_LOAD_TARGET_LIGHT = 65536;
    public static int FT_LOAD_TARGET_MONO = 131072;
    public static int FT_LOAD_TARGET_LCD = 196608;
    public static int FT_LOAD_TARGET_LCD_V = 262144;
    public static int FT_RENDER_MODE_NORMAL = 0;
    public static int FT_RENDER_MODE_LIGHT = 1;
    public static int FT_RENDER_MODE_MONO = 2;
    public static int FT_RENDER_MODE_LCD = 3;
    public static int FT_RENDER_MODE_LCD_V = 4;
    public static int FT_RENDER_MODE_MAX = 5;
    public static int FT_KERNING_DEFAULT = 0;
    public static int FT_KERNING_UNFITTED = 1;
    public static int FT_KERNING_UNSCALED = 2;
    public static int FT_STROKER_LINECAP_BUTT = 0;
    public static int FT_STROKER_LINECAP_ROUND = 1;
    public static int FT_STROKER_LINECAP_SQUARE = 2;
    public static int FT_STROKER_LINEJOIN_ROUND = 0;
    public static int FT_STROKER_LINEJOIN_BEVEL = 1;
    public static int FT_STROKER_LINEJOIN_MITER_VARIABLE;
    public static int FT_STROKER_LINEJOIN_MITER;
    public static int FT_STROKER_LINEJOIN_MITER_FIXED;

    static native int getLastErrorCode();

    private static int encode(char a, char b, char c, char d) {
        return a << 24 | b << 16 | c << 8 | d;
    }

    public static Library initFreeType() {
        new SharedLibraryLoader().load("gdx-freetype");
        long address = FreeType.initFreeTypeJni();
        if (address == 0L) {
            throw new GdxRuntimeException("Couldn't initialize FreeType library, FreeType error code: " + FreeType.getLastErrorCode());
        }
        return new Library(address);
    }

    private static native long initFreeTypeJni();

    public static int toInt(int value) {
        return (value + 63 & 0xFFFFFFC0) >> 6;
    }

    static {
        FT_STROKER_LINEJOIN_MITER = FT_STROKER_LINEJOIN_MITER_VARIABLE = 2;
        FT_STROKER_LINEJOIN_MITER_FIXED = 3;
    }

    public static class Stroker
    extends Pointer
    implements Disposable {
        Stroker(long address) {
            super(address);
        }

        public void set(int radius, int lineCap, int lineJoin, int miterLimit) {
            Stroker.set(this.address, radius, lineCap, lineJoin, miterLimit);
        }

        private static native void set(long var0, int var2, int var3, int var4, int var5);

        @Override
        public void dispose() {
            Stroker.done(this.address);
        }

        private static native void done(long var0);
    }

    public static class GlyphMetrics
    extends Pointer {
        GlyphMetrics(long address) {
            super(address);
        }

        public int getWidth() {
            return GlyphMetrics.getWidth(this.address);
        }

        private static native int getWidth(long var0);

        public int getHeight() {
            return GlyphMetrics.getHeight(this.address);
        }

        private static native int getHeight(long var0);

        public int getHoriBearingX() {
            return GlyphMetrics.getHoriBearingX(this.address);
        }

        private static native int getHoriBearingX(long var0);

        public int getHoriBearingY() {
            return GlyphMetrics.getHoriBearingY(this.address);
        }

        private static native int getHoriBearingY(long var0);

        public int getHoriAdvance() {
            return GlyphMetrics.getHoriAdvance(this.address);
        }

        private static native int getHoriAdvance(long var0);

        public int getVertBearingX() {
            return GlyphMetrics.getVertBearingX(this.address);
        }

        private static native int getVertBearingX(long var0);

        public int getVertBearingY() {
            return GlyphMetrics.getVertBearingY(this.address);
        }

        private static native int getVertBearingY(long var0);

        public int getVertAdvance() {
            return GlyphMetrics.getVertAdvance(this.address);
        }

        private static native int getVertAdvance(long var0);
    }

    public static class Bitmap
    extends Pointer {
        Bitmap(long address) {
            super(address);
        }

        public int getRows() {
            return Bitmap.getRows(this.address);
        }

        private static native int getRows(long var0);

        public int getWidth() {
            return Bitmap.getWidth(this.address);
        }

        private static native int getWidth(long var0);

        public int getPitch() {
            return Bitmap.getPitch(this.address);
        }

        private static native int getPitch(long var0);

        public ByteBuffer getBuffer() {
            if (this.getRows() == 0) {
                return BufferUtils.newByteBuffer(1);
            }
            return Bitmap.getBuffer(this.address);
        }

        private static native ByteBuffer getBuffer(long var0);

        public Pixmap getPixmap(Pixmap.Format format, Color color, float gamma) {
            Pixmap pixmap;
            int width = this.getWidth();
            int rows = this.getRows();
            ByteBuffer src = this.getBuffer();
            int pixelMode = this.getPixelMode();
            int rowBytes = Math.abs(this.getPitch());
            if (color == Color.WHITE && pixelMode == FT_PIXEL_MODE_GRAY && rowBytes == width && gamma == 1.0f) {
                pixmap = new Pixmap(width, rows, Pixmap.Format.Alpha);
                BufferUtils.copy(src, pixmap.getPixels(), pixmap.getPixels().capacity());
            } else {
                pixmap = new Pixmap(width, rows, Pixmap.Format.RGBA8888);
                int rgba = Color.rgba8888(color);
                byte[] srcRow = new byte[rowBytes];
                int[] dstRow = new int[width];
                IntBuffer dst = pixmap.getPixels().asIntBuffer();
                if (pixelMode == FT_PIXEL_MODE_MONO) {
                    for (int y = 0; y < rows; ++y) {
                        src.get(srcRow);
                        int i = 0;
                        for (int x = 0; x < width; x += 8) {
                            byte b = srcRow[i];
                            int n = Math.min(8, width - x);
                            for (int ii = 0; ii < n; ++ii) {
                                dstRow[x + ii] = (b & 1 << 7 - ii) != 0 ? rgba : 0;
                            }
                            ++i;
                        }
                        dst.put(dstRow);
                    }
                } else {
                    int rgb = rgba & 0xFFFFFF00;
                    int a = rgba & 0xFF;
                    for (int y = 0; y < rows; ++y) {
                        src.get(srcRow);
                        for (int x = 0; x < width; ++x) {
                            float alpha = (float)(srcRow[x] & 0xFF) / 255.0f;
                            alpha = (float)Math.pow(alpha, gamma);
                            dstRow[x] = rgb | (int)((float)a * alpha);
                        }
                        dst.put(dstRow);
                    }
                }
            }
            Pixmap converted = pixmap;
            if (format != pixmap.getFormat()) {
                converted = new Pixmap(pixmap.getWidth(), pixmap.getHeight(), format);
                Pixmap.Blending blending = Pixmap.getBlending();
                Pixmap.setBlending(Pixmap.Blending.None);
                converted.drawPixmap(pixmap, 0, 0);
                Pixmap.setBlending(blending);
                pixmap.dispose();
            }
            return converted;
        }

        public int getNumGray() {
            return Bitmap.getNumGray(this.address);
        }

        private static native int getNumGray(long var0);

        public int getPixelMode() {
            return Bitmap.getPixelMode(this.address);
        }

        private static native int getPixelMode(long var0);
    }

    public static class Glyph
    extends Pointer
    implements Disposable {
        private boolean rendered;

        Glyph(long address) {
            super(address);
        }

        @Override
        public void dispose() {
            Glyph.done(this.address);
        }

        private static native void done(long var0);

        public void strokeBorder(Stroker stroker, boolean inside) {
            this.address = Glyph.strokeBorder(this.address, stroker.address, inside);
        }

        private static native long strokeBorder(long var0, long var2, boolean var4);

        public void toBitmap(int renderMode) {
            long bitmap = Glyph.toBitmap(this.address, renderMode);
            if (bitmap == 0L) {
                throw new GdxRuntimeException("Couldn't render glyph, FreeType error code: " + FreeType.getLastErrorCode());
            }
            this.address = bitmap;
            this.rendered = true;
        }

        private static native long toBitmap(long var0, int var2);

        public Bitmap getBitmap() {
            if (!this.rendered) {
                throw new GdxRuntimeException("Glyph is not yet rendered");
            }
            return new Bitmap(Glyph.getBitmap(this.address));
        }

        private static native long getBitmap(long var0);

        public int getLeft() {
            if (!this.rendered) {
                throw new GdxRuntimeException("Glyph is not yet rendered");
            }
            return Glyph.getLeft(this.address);
        }

        private static native int getLeft(long var0);

        public int getTop() {
            if (!this.rendered) {
                throw new GdxRuntimeException("Glyph is not yet rendered");
            }
            return Glyph.getTop(this.address);
        }

        private static native int getTop(long var0);
    }

    public static class GlyphSlot
    extends Pointer {
        GlyphSlot(long address) {
            super(address);
        }

        public GlyphMetrics getMetrics() {
            return new GlyphMetrics(GlyphSlot.getMetrics(this.address));
        }

        private static native long getMetrics(long var0);

        public int getLinearHoriAdvance() {
            return GlyphSlot.getLinearHoriAdvance(this.address);
        }

        private static native int getLinearHoriAdvance(long var0);

        public int getLinearVertAdvance() {
            return GlyphSlot.getLinearVertAdvance(this.address);
        }

        private static native int getLinearVertAdvance(long var0);

        public int getAdvanceX() {
            return GlyphSlot.getAdvanceX(this.address);
        }

        private static native int getAdvanceX(long var0);

        public int getAdvanceY() {
            return GlyphSlot.getAdvanceY(this.address);
        }

        private static native int getAdvanceY(long var0);

        public int getFormat() {
            return GlyphSlot.getFormat(this.address);
        }

        private static native int getFormat(long var0);

        public Bitmap getBitmap() {
            return new Bitmap(GlyphSlot.getBitmap(this.address));
        }

        private static native long getBitmap(long var0);

        public int getBitmapLeft() {
            return GlyphSlot.getBitmapLeft(this.address);
        }

        private static native int getBitmapLeft(long var0);

        public int getBitmapTop() {
            return GlyphSlot.getBitmapTop(this.address);
        }

        private static native int getBitmapTop(long var0);

        public boolean renderGlyph(int renderMode) {
            return GlyphSlot.renderGlyph(this.address, renderMode);
        }

        private static native boolean renderGlyph(long var0, int var2);

        public Glyph getGlyph() {
            long glyph = GlyphSlot.getGlyph(this.address);
            if (glyph == 0L) {
                throw new GdxRuntimeException("Couldn't get glyph, FreeType error code: " + FreeType.getLastErrorCode());
            }
            return new Glyph(glyph);
        }

        private static native long getGlyph(long var0);
    }

    public static class SizeMetrics
    extends Pointer {
        SizeMetrics(long address) {
            super(address);
        }

        public int getXppem() {
            return SizeMetrics.getXppem(this.address);
        }

        private static native int getXppem(long var0);

        public int getYppem() {
            return SizeMetrics.getYppem(this.address);
        }

        private static native int getYppem(long var0);

        public int getXScale() {
            return SizeMetrics.getXscale(this.address);
        }

        private static native int getXscale(long var0);

        public int getYscale() {
            return SizeMetrics.getYscale(this.address);
        }

        private static native int getYscale(long var0);

        public int getAscender() {
            return SizeMetrics.getAscender(this.address);
        }

        private static native int getAscender(long var0);

        public int getDescender() {
            return SizeMetrics.getDescender(this.address);
        }

        private static native int getDescender(long var0);

        public int getHeight() {
            return SizeMetrics.getHeight(this.address);
        }

        private static native int getHeight(long var0);

        public int getMaxAdvance() {
            return SizeMetrics.getMaxAdvance(this.address);
        }

        private static native int getMaxAdvance(long var0);
    }

    public static class Size
    extends Pointer {
        Size(long address) {
            super(address);
        }

        public SizeMetrics getMetrics() {
            return new SizeMetrics(Size.getMetrics(this.address));
        }

        private static native long getMetrics(long var0);
    }

    public static class Face
    extends Pointer
    implements Disposable {
        Library library;

        public Face(long address, Library library) {
            super(address);
            this.library = library;
        }

        @Override
        public void dispose() {
            Face.doneFace(this.address);
            ByteBuffer buffer = this.library.fontData.get(this.address);
            if (buffer != null) {
                this.library.fontData.remove(this.address);
                BufferUtils.disposeUnsafeByteBuffer(buffer);
            }
        }

        private static native void doneFace(long var0);

        public int getFaceFlags() {
            return Face.getFaceFlags(this.address);
        }

        private static native int getFaceFlags(long var0);

        public int getStyleFlags() {
            return Face.getStyleFlags(this.address);
        }

        private static native int getStyleFlags(long var0);

        public int getNumGlyphs() {
            return Face.getNumGlyphs(this.address);
        }

        private static native int getNumGlyphs(long var0);

        public int getAscender() {
            return Face.getAscender(this.address);
        }

        private static native int getAscender(long var0);

        public int getDescender() {
            return Face.getDescender(this.address);
        }

        private static native int getDescender(long var0);

        public int getHeight() {
            return Face.getHeight(this.address);
        }

        private static native int getHeight(long var0);

        public int getMaxAdvanceWidth() {
            return Face.getMaxAdvanceWidth(this.address);
        }

        private static native int getMaxAdvanceWidth(long var0);

        public int getMaxAdvanceHeight() {
            return Face.getMaxAdvanceHeight(this.address);
        }

        private static native int getMaxAdvanceHeight(long var0);

        public int getUnderlinePosition() {
            return Face.getUnderlinePosition(this.address);
        }

        private static native int getUnderlinePosition(long var0);

        public int getUnderlineThickness() {
            return Face.getUnderlineThickness(this.address);
        }

        private static native int getUnderlineThickness(long var0);

        public boolean selectSize(int strikeIndex) {
            return Face.selectSize(this.address, strikeIndex);
        }

        private static native boolean selectSize(long var0, int var2);

        public boolean setCharSize(int charWidth, int charHeight, int horzResolution, int vertResolution) {
            return Face.setCharSize(this.address, charWidth, charHeight, horzResolution, vertResolution);
        }

        private static native boolean setCharSize(long var0, int var2, int var3, int var4, int var5);

        public boolean setPixelSizes(int pixelWidth, int pixelHeight) {
            return Face.setPixelSizes(this.address, pixelWidth, pixelHeight);
        }

        private static native boolean setPixelSizes(long var0, int var2, int var3);

        public boolean loadGlyph(int glyphIndex, int loadFlags) {
            return Face.loadGlyph(this.address, glyphIndex, loadFlags);
        }

        private static native boolean loadGlyph(long var0, int var2, int var3);

        public boolean loadChar(int charCode, int loadFlags) {
            return Face.loadChar(this.address, charCode, loadFlags);
        }

        private static native boolean loadChar(long var0, int var2, int var3);

        public GlyphSlot getGlyph() {
            return new GlyphSlot(Face.getGlyph(this.address));
        }

        private static native long getGlyph(long var0);

        public Size getSize() {
            return new Size(Face.getSize(this.address));
        }

        private static native long getSize(long var0);

        public boolean hasKerning() {
            return Face.hasKerning(this.address);
        }

        private static native boolean hasKerning(long var0);

        public int getKerning(int leftGlyph, int rightGlyph, int kernMode) {
            return Face.getKerning(this.address, leftGlyph, rightGlyph, kernMode);
        }

        private static native int getKerning(long var0, int var2, int var3, int var4);

        public int getCharIndex(int charCode) {
            return Face.getCharIndex(this.address, charCode);
        }

        private static native int getCharIndex(long var0, int var2);
    }

    public static class Library
    extends Pointer
    implements Disposable {
        LongMap<ByteBuffer> fontData = new LongMap();

        Library(long address) {
            super(address);
        }

        @Override
        public void dispose() {
            Library.doneFreeType(this.address);
            for (ByteBuffer buffer : this.fontData.values()) {
                BufferUtils.disposeUnsafeByteBuffer(buffer);
            }
        }

        private static native void doneFreeType(long var0);

        public Face newFace(FileHandle font, int faceIndex) {
            byte[] data = font.readBytes();
            return this.newMemoryFace(data, data.length, faceIndex);
        }

        public Face newMemoryFace(byte[] data, int dataSize, int faceIndex) {
            ByteBuffer buffer = BufferUtils.newUnsafeByteBuffer(data.length);
            BufferUtils.copy(data, 0, (Buffer)buffer, data.length);
            return this.newMemoryFace(buffer, faceIndex);
        }

        public Face newMemoryFace(ByteBuffer buffer, int faceIndex) {
            long face = Library.newMemoryFace(this.address, buffer, buffer.remaining(), faceIndex);
            if (face == 0L) {
                BufferUtils.disposeUnsafeByteBuffer(buffer);
                throw new GdxRuntimeException("Couldn't load font, FreeType error code: " + FreeType.getLastErrorCode());
            }
            this.fontData.put(face, buffer);
            return new Face(face, this);
        }

        private static native long newMemoryFace(long var0, ByteBuffer var2, int var3, int var4);

        public Stroker createStroker() {
            long stroker = Library.strokerNew(this.address);
            if (stroker == 0L) {
                throw new GdxRuntimeException("Couldn't create FreeType stroker, FreeType error code: " + FreeType.getLastErrorCode());
            }
            return new Stroker(stroker);
        }

        private static native long strokerNew(long var0);
    }

    private static class Pointer {
        long address;

        Pointer(long address) {
            this.address = address;
        }
    }
}

