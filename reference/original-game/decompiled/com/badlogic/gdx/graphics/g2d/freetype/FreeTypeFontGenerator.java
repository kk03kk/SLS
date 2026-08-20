/*
 * Decompiled with CFR 0.152.
 */
package com.badlogic.gdx.graphics.g2d.freetype;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.PixmapPacker;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.g2d.freetype.FreeType;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.BufferUtils;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.GdxRuntimeException;
import com.badlogic.gdx.utils.StreamUtils;
import java.io.IOException;
import java.io.InputStream;
import java.nio.Buffer;
import java.nio.ByteBuffer;

public class FreeTypeFontGenerator
implements Disposable {
    public static final String DEFAULT_CHARS = "\u0000ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz1234567890\"!`?'.,;:()[]{}<>|/@\\^$\u20ac-%+=#_&~*\u007f\u0080\u0081\u0082\u0083\u0084\u0085\u0086\u0087\u0088\u0089\u008a\u008b\u008c\u008d\u008e\u008f\u0090\u0091\u0092\u0093\u0094\u0095\u0096\u0097\u0098\u0099\u009a\u009b\u009c\u009d\u009e\u009f\u00a0\u00a1\u00a2\u00a3\u00a4\u00a5\u00a6\u00a7\u00a8\u00a9\u00aa\u00ab\u00ac\u00ad\u00ae\u00af\u00b0\u00b1\u00b2\u00b3\u00b4\u00b5\u00b6\u00b7\u00b8\u00b9\u00ba\u00bb\u00bc\u00bd\u00be\u00bf\u00c0\u00c1\u00c2\u00c3\u00c4\u00c5\u00c6\u00c7\u00c8\u00c9\u00ca\u00cb\u00cc\u00cd\u00ce\u00cf\u00d0\u00d1\u00d2\u00d3\u00d4\u00d5\u00d6\u00d7\u00d8\u00d9\u00da\u00db\u00dc\u00dd\u00de\u00df\u00e0\u00e1\u00e2\u00e3\u00e4\u00e5\u00e6\u00e7\u00e8\u00e9\u00ea\u00eb\u00ec\u00ed\u00ee\u00ef\u00f0\u00f1\u00f2\u00f3\u00f4\u00f5\u00f6\u00f7\u00f8\u00f9\u00fa\u00fb\u00fc\u00fd\u00fe\u00ff";
    public static final int NO_MAXIMUM = -1;
    private static int maxTextureSize = 1024;
    final FreeType.Library library;
    final FreeType.Face face;
    final String name;
    boolean bitmapped = false;
    private int pixelWidth;
    private int pixelHeight;

    public FreeTypeFontGenerator(FileHandle fontFile) {
        ByteBuffer buffer;
        this.name = fontFile.pathWithoutExtension();
        int fileSize = (int)fontFile.length();
        this.library = FreeType.initFreeType();
        if (this.library == null) {
            throw new GdxRuntimeException("Couldn't initialize FreeType");
        }
        InputStream input = fontFile.read();
        try {
            if (fileSize == 0) {
                byte[] data = StreamUtils.copyStreamToByteArray(input, fileSize > 0 ? (int)((float)fileSize * 1.5f) : 16384);
                buffer = BufferUtils.newUnsafeByteBuffer(data.length);
                BufferUtils.copy(data, 0, (Buffer)buffer, data.length);
            } else {
                buffer = BufferUtils.newUnsafeByteBuffer(fileSize);
                StreamUtils.copyStream(input, buffer);
            }
        }
        catch (IOException ex) {
            throw new GdxRuntimeException(ex);
        }
        finally {
            StreamUtils.closeQuietly(input);
        }
        this.face = this.library.newMemoryFace(buffer, 0);
        if (this.face == null) {
            throw new GdxRuntimeException("Couldn't create face for font: " + fontFile);
        }
        if (this.checkForBitmapFont()) {
            return;
        }
        this.setPixelSizes(0, 15);
    }

    private int getLoadingFlags(FreeTypeFontParameter parameter) {
        int loadingFlags = FreeType.FT_LOAD_DEFAULT;
        switch (parameter.hinting) {
            case None: {
                loadingFlags |= FreeType.FT_LOAD_NO_HINTING;
                break;
            }
            case Slight: {
                loadingFlags |= FreeType.FT_LOAD_TARGET_LIGHT;
                break;
            }
            case Medium: {
                loadingFlags |= FreeType.FT_LOAD_TARGET_NORMAL;
                break;
            }
            case Full: {
                loadingFlags |= FreeType.FT_LOAD_TARGET_MONO;
                break;
            }
            case AutoSlight: {
                loadingFlags |= FreeType.FT_LOAD_FORCE_AUTOHINT | FreeType.FT_LOAD_TARGET_LIGHT;
                break;
            }
            case AutoMedium: {
                loadingFlags |= FreeType.FT_LOAD_FORCE_AUTOHINT | FreeType.FT_LOAD_TARGET_NORMAL;
                break;
            }
            case AutoFull: {
                loadingFlags |= FreeType.FT_LOAD_FORCE_AUTOHINT | FreeType.FT_LOAD_TARGET_MONO;
            }
        }
        return loadingFlags;
    }

    private boolean loadChar(int c) {
        return this.loadChar(c, FreeType.FT_LOAD_DEFAULT | FreeType.FT_LOAD_FORCE_AUTOHINT);
    }

    private boolean loadChar(int c, int flags) {
        return this.face.loadChar(c, flags);
    }

    private boolean checkForBitmapFont() {
        FreeType.GlyphSlot slot;
        int faceFlags = this.face.getFaceFlags();
        if ((faceFlags & FreeType.FT_FACE_FLAG_FIXED_SIZES) == FreeType.FT_FACE_FLAG_FIXED_SIZES && (faceFlags & FreeType.FT_FACE_FLAG_HORIZONTAL) == FreeType.FT_FACE_FLAG_HORIZONTAL && this.loadChar(32) && (slot = this.face.getGlyph()).getFormat() == 1651078259) {
            this.bitmapped = true;
        }
        return this.bitmapped;
    }

    public BitmapFont generateFont(FreeTypeFontParameter parameter) {
        return this.generateFont(parameter, new FreeTypeBitmapFontData());
    }

    public BitmapFont generateFont(FreeTypeFontParameter parameter, FreeTypeBitmapFontData data) {
        this.generateData(parameter, data);
        if (data.regions == null && parameter.packer != null) {
            data.regions = new Array();
            parameter.packer.updateTextureRegions(data.regions, parameter.minFilter, parameter.magFilter, parameter.genMipMaps);
        }
        BitmapFont font = new BitmapFont((BitmapFont.BitmapFontData)data, data.regions, true);
        font.setOwnsTexture(parameter.packer == null);
        return font;
    }

    public int scaleForPixelHeight(int height) {
        this.setPixelSizes(0, height);
        FreeType.SizeMetrics fontMetrics = this.face.getSize().getMetrics();
        int ascent = FreeType.toInt(fontMetrics.getAscender());
        int descent = FreeType.toInt(fontMetrics.getDescender());
        return height * height / (ascent - descent);
    }

    public int scaleForPixelWidth(int width, int numChars) {
        FreeType.SizeMetrics fontMetrics = this.face.getSize().getMetrics();
        int advance = FreeType.toInt(fontMetrics.getMaxAdvance());
        int ascent = FreeType.toInt(fontMetrics.getAscender());
        int descent = FreeType.toInt(fontMetrics.getDescender());
        int unscaledHeight = ascent - descent;
        int height = unscaledHeight * width / (advance * numChars);
        this.setPixelSizes(0, height);
        return height;
    }

    public int scaleToFitSquare(int width, int height, int numChars) {
        return Math.min(this.scaleForPixelHeight(height), this.scaleForPixelWidth(width, numChars));
    }

    public GlyphAndBitmap generateGlyphAndBitmap(int c, int size, boolean flip) {
        this.setPixelSizes(0, size);
        FreeType.SizeMetrics fontMetrics = this.face.getSize().getMetrics();
        int baseline = FreeType.toInt(fontMetrics.getAscender());
        if (this.face.getCharIndex(c) == 0) {
            return null;
        }
        if (!this.loadChar(c)) {
            throw new GdxRuntimeException("Unable to load character!");
        }
        FreeType.GlyphSlot slot = this.face.getGlyph();
        Object bitmap = this.bitmapped ? slot.getBitmap() : (!slot.renderGlyph(FreeType.FT_RENDER_MODE_NORMAL) ? null : slot.getBitmap());
        FreeType.GlyphMetrics metrics = slot.getMetrics();
        BitmapFont.Glyph glyph = new BitmapFont.Glyph();
        if (bitmap != null) {
            glyph.width = ((FreeType.Bitmap)bitmap).getWidth();
            glyph.height = ((FreeType.Bitmap)bitmap).getRows();
        } else {
            glyph.width = 0;
            glyph.height = 0;
        }
        glyph.xoffset = slot.getBitmapLeft();
        glyph.yoffset = flip ? -slot.getBitmapTop() + baseline : -(glyph.height - slot.getBitmapTop()) - baseline;
        glyph.xadvance = FreeType.toInt(metrics.getHoriAdvance());
        glyph.srcX = 0;
        glyph.srcY = 0;
        glyph.id = c;
        GlyphAndBitmap result = new GlyphAndBitmap();
        result.glyph = glyph;
        result.bitmap = bitmap;
        return result;
    }

    public FreeTypeBitmapFontData generateData(int size) {
        FreeTypeFontParameter parameter = new FreeTypeFontParameter();
        parameter.size = size;
        return this.generateData(parameter);
    }

    public FreeTypeBitmapFontData generateData(FreeTypeFontParameter parameter) {
        return this.generateData(parameter, new FreeTypeBitmapFontData());
    }

    void setPixelSizes(int pixelWidth, int pixelHeight) {
        this.pixelWidth = pixelWidth;
        this.pixelHeight = pixelHeight;
        if (!this.bitmapped && !this.face.setPixelSizes(pixelWidth, pixelHeight)) {
            throw new GdxRuntimeException("Couldn't set size for font");
        }
    }

    public FreeTypeBitmapFontData generateData(FreeTypeFontParameter parameter, FreeTypeBitmapFontData data) {
        BitmapFont.Glyph spaceGlyph;
        BitmapFont.Glyph missingGlyph;
        parameter = parameter == null ? new FreeTypeFontParameter() : parameter;
        char[] characters = parameter.characters.toCharArray();
        int charactersLength = characters.length;
        boolean incremental = parameter.incremental;
        int flags = this.getLoadingFlags(parameter);
        this.setPixelSizes(0, parameter.size);
        FreeType.SizeMetrics fontMetrics = this.face.getSize().getMetrics();
        data.flipped = parameter.flip;
        data.ascent = FreeType.toInt(fontMetrics.getAscender());
        data.descent = FreeType.toInt(fontMetrics.getDescender());
        data.lineHeight = FreeType.toInt(fontMetrics.getHeight());
        float baseLine = data.ascent;
        if (this.bitmapped && data.lineHeight == 0.0f) {
            for (int c = 32; c < 32 + this.face.getNumGlyphs(); ++c) {
                if (!this.loadChar(c, flags)) continue;
                int lh = FreeType.toInt(this.face.getGlyph().getMetrics().getHeight());
                data.lineHeight = (float)lh > data.lineHeight ? (float)lh : data.lineHeight;
            }
        }
        data.lineHeight += (float)parameter.spaceY;
        data.spaceWidth = this.loadChar(32, flags) || this.loadChar(108, flags) ? (float)FreeType.toInt(this.face.getGlyph().getMetrics().getHoriAdvance()) : (float)this.face.getMaxAdvanceWidth();
        for (char xChar : data.xChars) {
            if (!this.loadChar(xChar, flags)) continue;
            data.xHeight = FreeType.toInt(this.face.getGlyph().getMetrics().getHeight());
            break;
        }
        if (data.xHeight == 0.0f) {
            throw new GdxRuntimeException("No x-height character found in font");
        }
        for (char capChar : data.capChars) {
            if (!this.loadChar(capChar, flags)) continue;
            data.capHeight = FreeType.toInt(this.face.getGlyph().getMetrics().getHeight());
            break;
        }
        if (!this.bitmapped && data.capHeight == 1.0f) {
            throw new GdxRuntimeException("No cap character found in font");
        }
        data.ascent -= data.capHeight;
        data.down = -data.lineHeight;
        if (parameter.flip) {
            data.ascent = -data.ascent;
            data.down = -data.down;
        }
        boolean ownsAtlas = false;
        PixmapPacker packer = parameter.packer;
        if (packer == null) {
            PixmapPacker.PackStrategy packStrategy;
            int size;
            if (incremental) {
                size = maxTextureSize;
                packStrategy = new PixmapPacker.GuillotineStrategy();
            } else {
                int maxGlyphHeight = (int)Math.ceil(data.lineHeight);
                size = MathUtils.nextPowerOfTwo((int)Math.sqrt(maxGlyphHeight * maxGlyphHeight * charactersLength));
                if (maxTextureSize > 0) {
                    size = Math.min(size, maxTextureSize);
                }
                packStrategy = new PixmapPacker.SkylineStrategy();
            }
            ownsAtlas = true;
            packer = new PixmapPacker(size, size, Pixmap.Format.RGBA8888, 1, false, packStrategy);
            packer.setTransparentColor(parameter.color);
            packer.getTransparentColor().a = 0.0f;
            if (parameter.borderWidth > 0.0f) {
                packer.setTransparentColor(parameter.borderColor);
                packer.getTransparentColor().a = 0.0f;
            }
        }
        if (incremental) {
            data.glyphs = new Array(charactersLength + 32);
        }
        FreeType.Stroker stroker = null;
        if (parameter.borderWidth > 0.0f) {
            stroker = this.library.createStroker();
            stroker.set((int)(parameter.borderWidth * 64.0f), parameter.borderStraight ? FreeType.FT_STROKER_LINECAP_BUTT : FreeType.FT_STROKER_LINECAP_ROUND, parameter.borderStraight ? FreeType.FT_STROKER_LINEJOIN_MITER_FIXED : FreeType.FT_STROKER_LINEJOIN_ROUND, 0);
        }
        if ((missingGlyph = this.createGlyph('\u0000', data, parameter, stroker, baseLine, packer)) != null && missingGlyph.width != 0 && missingGlyph.height != 0) {
            data.setGlyph(0, missingGlyph);
            if (incremental) {
                data.glyphs.add(missingGlyph);
            }
        }
        int[] heights = new int[charactersLength];
        int n = charactersLength;
        for (int i = 0; i < n; ++i) {
            int height;
            heights[i] = height = this.loadChar(characters[i], flags) ? FreeType.toInt(this.face.getGlyph().getMetrics().getHeight()) : 0;
        }
        int heightsCount = heights.length;
        while (heightsCount > 0) {
            int best = 0;
            int maxHeight = heights[0];
            for (int i = 1; i < heightsCount; ++i) {
                int height = heights[i];
                if (height <= maxHeight) continue;
                maxHeight = height;
                best = i;
            }
            char c = characters[best];
            BitmapFont.Glyph glyph = this.createGlyph(c, data, parameter, stroker, baseLine, packer);
            if (glyph != null) {
                data.setGlyph(c, glyph);
                if (incremental) {
                    data.glyphs.add(glyph);
                }
            }
            heights[best] = heights[--heightsCount];
            char tmpChar = characters[best];
            characters[best] = characters[heightsCount];
            characters[heightsCount] = tmpChar;
        }
        if (stroker != null && !incremental) {
            stroker.dispose();
        }
        if (incremental) {
            data.generator = this;
            data.parameter = parameter;
            data.stroker = stroker;
            data.packer = packer;
        }
        parameter.kerning &= this.face.hasKerning();
        if (parameter.kerning) {
            for (int i = 0; i < charactersLength; ++i) {
                char firstChar = characters[i];
                BitmapFont.Glyph first = data.getGlyph(firstChar);
                if (first == null) continue;
                int firstIndex = this.face.getCharIndex(firstChar);
                for (int ii = i; ii < charactersLength; ++ii) {
                    char secondChar = characters[ii];
                    BitmapFont.Glyph second = data.getGlyph(secondChar);
                    if (second == null) continue;
                    int secondIndex = this.face.getCharIndex(secondChar);
                    int kerning = this.face.getKerning(firstIndex, secondIndex, 0);
                    if (kerning != 0) {
                        first.setKerning(secondChar, FreeType.toInt(kerning));
                    }
                    if ((kerning = this.face.getKerning(secondIndex, firstIndex, 0)) == 0) continue;
                    second.setKerning(firstChar, FreeType.toInt(kerning));
                }
            }
        }
        if (ownsAtlas) {
            data.regions = new Array();
            packer.updateTextureRegions(data.regions, parameter.minFilter, parameter.magFilter, parameter.genMipMaps);
        }
        if ((spaceGlyph = data.getGlyph(' ')) == null) {
            spaceGlyph = new BitmapFont.Glyph();
            spaceGlyph.xadvance = (int)data.spaceWidth + parameter.spaceX;
            spaceGlyph.id = 32;
            data.setGlyph(32, spaceGlyph);
        }
        if (spaceGlyph.width == 0) {
            spaceGlyph.width = (int)((float)spaceGlyph.xadvance + data.padRight);
        }
        return data;
    }

    BitmapFont.Glyph createGlyph(char c, FreeTypeBitmapFontData data, FreeTypeFontParameter parameter, FreeType.Stroker stroker, float baseLine, PixmapPacker packer) {
        boolean missing;
        boolean bl = missing = this.face.getCharIndex(c) == 0 && c != '\u0000';
        if (missing) {
            return null;
        }
        if (!this.loadChar(c, this.getLoadingFlags(parameter))) {
            return null;
        }
        FreeType.GlyphSlot slot = this.face.getGlyph();
        FreeType.Glyph mainGlyph = slot.getGlyph();
        try {
            mainGlyph.toBitmap(parameter.mono ? FreeType.FT_RENDER_MODE_MONO : FreeType.FT_RENDER_MODE_NORMAL);
        }
        catch (GdxRuntimeException e) {
            mainGlyph.dispose();
            Gdx.app.log("FreeTypeFontGenerator", "Couldn't render char: " + c);
            return null;
        }
        FreeType.Bitmap mainBitmap = mainGlyph.getBitmap();
        Pixmap mainPixmap = mainBitmap.getPixmap(Pixmap.Format.RGBA8888, parameter.color, parameter.gamma);
        if (mainBitmap.getWidth() != 0 && mainBitmap.getRows() != 0) {
            int offsetX = 0;
            int offsetY = 0;
            if (parameter.borderWidth > 0.0f) {
                int top = mainGlyph.getTop();
                int left = mainGlyph.getLeft();
                FreeType.Glyph borderGlyph = slot.getGlyph();
                borderGlyph.strokeBorder(stroker, false);
                borderGlyph.toBitmap(parameter.mono ? FreeType.FT_RENDER_MODE_MONO : FreeType.FT_RENDER_MODE_NORMAL);
                offsetX = left - borderGlyph.getLeft();
                offsetY = -(top - borderGlyph.getTop());
                FreeType.Bitmap borderBitmap = borderGlyph.getBitmap();
                Pixmap borderPixmap = borderBitmap.getPixmap(Pixmap.Format.RGBA8888, parameter.borderColor, parameter.borderGamma);
                int n = parameter.renderCount;
                for (int i = 0; i < n; ++i) {
                    borderPixmap.drawPixmap(mainPixmap, offsetX, offsetY);
                }
                mainPixmap.dispose();
                mainGlyph.dispose();
                mainPixmap = borderPixmap;
                mainGlyph = borderGlyph;
            }
            if (parameter.shadowOffsetX != 0 || parameter.shadowOffsetY != 0) {
                int mainW = mainPixmap.getWidth();
                int mainH = mainPixmap.getHeight();
                int shadowOffsetX = Math.max(parameter.shadowOffsetX, 0);
                int shadowOffsetY = Math.max(parameter.shadowOffsetY, 0);
                int shadowW = mainW + Math.abs(parameter.shadowOffsetX);
                int shadowH = mainH + Math.abs(parameter.shadowOffsetY);
                Pixmap shadowPixmap = new Pixmap(shadowW, shadowH, mainPixmap.getFormat());
                Color shadowColor = parameter.shadowColor;
                byte r = (byte)(shadowColor.r * 255.0f);
                byte g = (byte)(shadowColor.g * 255.0f);
                byte b = (byte)(shadowColor.b * 255.0f);
                float a = shadowColor.a;
                ByteBuffer mainPixels = mainPixmap.getPixels();
                ByteBuffer shadowPixels = shadowPixmap.getPixels();
                for (int y = 0; y < mainH; ++y) {
                    int shadowRow = shadowW * (y + shadowOffsetY) + shadowOffsetX;
                    for (int x = 0; x < mainW; ++x) {
                        int mainPixel = (mainW * y + x) * 4;
                        byte mainA = mainPixels.get(mainPixel + 3);
                        if (mainA == 0) continue;
                        int shadowPixel = (shadowRow + x) * 4;
                        shadowPixels.put(shadowPixel, r);
                        shadowPixels.put(shadowPixel + 1, g);
                        shadowPixels.put(shadowPixel + 2, b);
                        shadowPixels.put(shadowPixel + 3, (byte)((float)(mainA & 0xFF) * a));
                    }
                }
                int n = parameter.renderCount;
                for (int i = 0; i < n; ++i) {
                    shadowPixmap.drawPixmap(mainPixmap, Math.max(-parameter.shadowOffsetX, 0), Math.max(-parameter.shadowOffsetY, 0));
                }
                mainPixmap.dispose();
                mainPixmap = shadowPixmap;
            } else if (parameter.borderWidth == 0.0f) {
                int n = parameter.renderCount - 1;
                for (int i = 0; i < n; ++i) {
                    mainPixmap.drawPixmap(mainPixmap, 0, 0);
                }
            }
        }
        FreeType.GlyphMetrics metrics = slot.getMetrics();
        BitmapFont.Glyph glyph = new BitmapFont.Glyph();
        glyph.id = c;
        glyph.width = mainPixmap.getWidth();
        glyph.height = mainPixmap.getHeight();
        glyph.xoffset = mainGlyph.getLeft();
        glyph.yoffset = parameter.flip ? -mainGlyph.getTop() + (int)baseLine : -(glyph.height - mainGlyph.getTop()) - (int)baseLine;
        glyph.xadvance = FreeType.toInt(metrics.getHoriAdvance()) + (int)parameter.borderWidth + parameter.spaceX;
        if (this.bitmapped) {
            mainPixmap.setColor(Color.CLEAR);
            mainPixmap.fill();
            ByteBuffer buf = mainBitmap.getBuffer();
            int whiteIntBits = Color.WHITE.toIntBits();
            int clearIntBits = Color.CLEAR.toIntBits();
            for (int h = 0; h < glyph.height; ++h) {
                int idx = h * mainBitmap.getPitch();
                for (int w = 0; w < glyph.width + glyph.xoffset; ++w) {
                    int bit = buf.get(idx + w / 8) >>> 7 - w % 8 & 1;
                    mainPixmap.drawPixel(w, h, bit == 1 ? whiteIntBits : clearIntBits);
                }
            }
        }
        Rectangle rect = packer.pack(mainPixmap);
        glyph.page = packer.getPages().size - 1;
        glyph.srcX = (int)rect.x;
        glyph.srcY = (int)rect.y;
        if (parameter.incremental && data.regions != null && data.regions.size <= glyph.page) {
            packer.updateTextureRegions(data.regions, parameter.minFilter, parameter.magFilter, parameter.genMipMaps);
        }
        mainPixmap.dispose();
        mainGlyph.dispose();
        return glyph;
    }

    @Override
    public void dispose() {
        this.face.dispose();
        this.library.dispose();
    }

    public static void setMaxTextureSize(int texSize) {
        maxTextureSize = texSize;
    }

    public static int getMaxTextureSize() {
        return maxTextureSize;
    }

    public static class FreeTypeFontParameter {
        public int size = 16;
        public boolean mono;
        public Hinting hinting = Hinting.AutoMedium;
        public Color color = Color.WHITE;
        public float gamma = 1.8f;
        public int renderCount = 2;
        public float borderWidth = 0.0f;
        public Color borderColor = Color.BLACK;
        public boolean borderStraight = false;
        public float borderGamma = 1.8f;
        public int shadowOffsetX = 0;
        public int shadowOffsetY = 0;
        public Color shadowColor = new Color(0.0f, 0.0f, 0.0f, 0.75f);
        public int spaceX;
        public int spaceY;
        public String characters = "\u0000ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz1234567890\"!`?'.,;:()[]{}<>|/@\\^$\u20ac-%+=#_&~*\u007f\u0080\u0081\u0082\u0083\u0084\u0085\u0086\u0087\u0088\u0089\u008a\u008b\u008c\u008d\u008e\u008f\u0090\u0091\u0092\u0093\u0094\u0095\u0096\u0097\u0098\u0099\u009a\u009b\u009c\u009d\u009e\u009f\u00a0\u00a1\u00a2\u00a3\u00a4\u00a5\u00a6\u00a7\u00a8\u00a9\u00aa\u00ab\u00ac\u00ad\u00ae\u00af\u00b0\u00b1\u00b2\u00b3\u00b4\u00b5\u00b6\u00b7\u00b8\u00b9\u00ba\u00bb\u00bc\u00bd\u00be\u00bf\u00c0\u00c1\u00c2\u00c3\u00c4\u00c5\u00c6\u00c7\u00c8\u00c9\u00ca\u00cb\u00cc\u00cd\u00ce\u00cf\u00d0\u00d1\u00d2\u00d3\u00d4\u00d5\u00d6\u00d7\u00d8\u00d9\u00da\u00db\u00dc\u00dd\u00de\u00df\u00e0\u00e1\u00e2\u00e3\u00e4\u00e5\u00e6\u00e7\u00e8\u00e9\u00ea\u00eb\u00ec\u00ed\u00ee\u00ef\u00f0\u00f1\u00f2\u00f3\u00f4\u00f5\u00f6\u00f7\u00f8\u00f9\u00fa\u00fb\u00fc\u00fd\u00fe\u00ff";
        public boolean kerning = true;
        public PixmapPacker packer = null;
        public boolean flip = false;
        public boolean genMipMaps = false;
        public Texture.TextureFilter minFilter = Texture.TextureFilter.Nearest;
        public Texture.TextureFilter magFilter = Texture.TextureFilter.Nearest;
        public boolean incremental;
    }

    public static enum Hinting {
        None,
        Slight,
        Medium,
        Full,
        AutoSlight,
        AutoMedium,
        AutoFull;

    }

    public static class FreeTypeBitmapFontData
    extends BitmapFont.BitmapFontData
    implements Disposable {
        Array<TextureRegion> regions;
        FreeTypeFontGenerator generator;
        FreeTypeFontParameter parameter;
        FreeType.Stroker stroker;
        PixmapPacker packer;
        Array<BitmapFont.Glyph> glyphs;
        private boolean dirty;

        @Override
        public BitmapFont.Glyph getGlyph(char ch) {
            BitmapFont.Glyph glyph = super.getGlyph(ch);
            if (glyph == null && this.generator != null) {
                this.generator.setPixelSizes(0, this.parameter.size);
                float baseline = ((this.flipped ? -this.ascent : this.ascent) + this.capHeight) / this.scaleY;
                glyph = this.generator.createGlyph(ch, this, this.parameter, this.stroker, baseline, this.packer);
                if (glyph == null) {
                    return this.missingGlyph;
                }
                this.setGlyphRegion(glyph, this.regions.get(glyph.page));
                this.setGlyph(ch, glyph);
                this.glyphs.add(glyph);
                this.dirty = true;
                FreeType.Face face = this.generator.face;
                if (this.parameter.kerning) {
                    int glyphIndex = face.getCharIndex(ch);
                    int n = this.glyphs.size;
                    for (int i = 0; i < n; ++i) {
                        BitmapFont.Glyph other = this.glyphs.get(i);
                        int otherIndex = face.getCharIndex(other.id);
                        int kerning = face.getKerning(glyphIndex, otherIndex, 0);
                        if (kerning != 0) {
                            glyph.setKerning(other.id, FreeType.toInt(kerning));
                        }
                        if ((kerning = face.getKerning(otherIndex, glyphIndex, 0)) == 0) continue;
                        other.setKerning(ch, FreeType.toInt(kerning));
                    }
                }
            }
            return glyph;
        }

        @Override
        public void getGlyphs(GlyphLayout.GlyphRun run, CharSequence str, int start, int end, boolean tightBounds) {
            if (this.packer != null) {
                this.packer.setPackToTexture(true);
            }
            super.getGlyphs(run, str, start, end, tightBounds);
            if (this.dirty) {
                this.dirty = false;
                this.packer.updateTextureRegions(this.regions, this.parameter.minFilter, this.parameter.magFilter, this.parameter.genMipMaps);
            }
        }

        @Override
        public void dispose() {
            if (this.stroker != null) {
                this.stroker.dispose();
            }
            if (this.packer != null) {
                this.packer.dispose();
            }
        }
    }

    public class GlyphAndBitmap {
        public BitmapFont.Glyph glyph;
        public FreeType.Bitmap bitmap;
    }
}

