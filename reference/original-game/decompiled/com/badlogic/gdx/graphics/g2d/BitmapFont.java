/*
 * Decompiled with CFR 0.152.
 */
package com.badlogic.gdx.graphics.g2d;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.BitmapFontCache;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.FloatArray;
import com.badlogic.gdx.utils.GdxRuntimeException;
import com.badlogic.gdx.utils.StreamUtils;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BitmapFont
implements Disposable {
    private static final int LOG2_PAGE_SIZE = 9;
    private static final int PAGE_SIZE = 512;
    private static final int PAGES = 128;
    final BitmapFontData data;
    Array<TextureRegion> regions;
    private final BitmapFontCache cache;
    private boolean flipped;
    boolean integer;
    private boolean ownsTexture;

    public BitmapFont() {
        this(Gdx.files.classpath("com/badlogic/gdx/utils/arial-15.fnt"), Gdx.files.classpath("com/badlogic/gdx/utils/arial-15.png"), false, true);
    }

    public BitmapFont(boolean flip) {
        this(Gdx.files.classpath("com/badlogic/gdx/utils/arial-15.fnt"), Gdx.files.classpath("com/badlogic/gdx/utils/arial-15.png"), flip, true);
    }

    public BitmapFont(FileHandle fontFile, TextureRegion region) {
        this(fontFile, region, false);
    }

    public BitmapFont(FileHandle fontFile, TextureRegion region, boolean flip) {
        this(new BitmapFontData(fontFile, flip), region, true);
    }

    public BitmapFont(FileHandle fontFile) {
        this(fontFile, false);
    }

    public BitmapFont(FileHandle fontFile, boolean flip) {
        this(new BitmapFontData(fontFile, flip), (TextureRegion)null, true);
    }

    public BitmapFont(FileHandle fontFile, FileHandle imageFile, boolean flip) {
        this(fontFile, imageFile, flip, true);
    }

    public BitmapFont(FileHandle fontFile, FileHandle imageFile, boolean flip, boolean integer) {
        this(new BitmapFontData(fontFile, flip), new TextureRegion(new Texture(imageFile, false)), integer);
        this.ownsTexture = true;
    }

    public BitmapFont(BitmapFontData data, TextureRegion region, boolean integer) {
        this(data, region != null ? Array.with(region) : null, integer);
    }

    public BitmapFont(BitmapFontData data, Array<TextureRegion> pageRegions, boolean integer) {
        this.flipped = data.flipped;
        this.data = data;
        this.integer = integer;
        if (pageRegions == null || pageRegions.size == 0) {
            int n = data.imagePaths.length;
            this.regions = new Array(n);
            for (int i = 0; i < n; ++i) {
                FileHandle file = data.fontFile == null ? Gdx.files.internal(data.imagePaths[i]) : Gdx.files.getFileHandle(data.imagePaths[i], data.fontFile.type());
                this.regions.add(new TextureRegion(new Texture(file, false)));
            }
            this.ownsTexture = true;
        } else {
            this.regions = pageRegions;
            this.ownsTexture = false;
        }
        this.cache = this.newFontCache();
        this.load(data);
    }

    protected void load(BitmapFontData data) {
        for (Glyph[] page : data.glyphs) {
            if (page == null) continue;
            for (Glyph glyph : page) {
                if (glyph == null) continue;
                data.setGlyphRegion(glyph, this.regions.get(glyph.page));
            }
        }
        if (data.missingGlyph != null) {
            data.setGlyphRegion(data.missingGlyph, this.regions.get(data.missingGlyph.page));
        }
    }

    public GlyphLayout draw(Batch batch, CharSequence str, float x, float y) {
        this.cache.clear();
        GlyphLayout layout = this.cache.addText(str, x, y);
        this.cache.draw(batch);
        return layout;
    }

    public GlyphLayout draw(Batch batch, CharSequence str, float x, float y, float targetWidth, int halign, boolean wrap) {
        this.cache.clear();
        GlyphLayout layout = this.cache.addText(str, x, y, targetWidth, halign, wrap);
        this.cache.draw(batch);
        return layout;
    }

    public GlyphLayout draw(Batch batch, CharSequence str, float x, float y, int start, int end, float targetWidth, int halign, boolean wrap) {
        this.cache.clear();
        GlyphLayout layout = this.cache.addText(str, x, y, start, end, targetWidth, halign, wrap);
        this.cache.draw(batch);
        return layout;
    }

    public GlyphLayout draw(Batch batch, CharSequence str, float x, float y, int start, int end, float targetWidth, int halign, boolean wrap, String truncate) {
        this.cache.clear();
        GlyphLayout layout = this.cache.addText(str, x, y, start, end, targetWidth, halign, wrap, truncate);
        this.cache.draw(batch);
        return layout;
    }

    public void draw(Batch batch, GlyphLayout layout, float x, float y) {
        this.cache.clear();
        this.cache.addText(layout, x, y);
        this.cache.draw(batch);
    }

    public Color getColor() {
        return this.cache.getColor();
    }

    public void setColor(Color color) {
        this.cache.getColor().set(color);
    }

    public void setColor(float r, float g, float b, float a) {
        this.cache.getColor().set(r, g, b, a);
    }

    public float getScaleX() {
        return this.data.scaleX;
    }

    public float getScaleY() {
        return this.data.scaleY;
    }

    public TextureRegion getRegion() {
        return this.regions.first();
    }

    public Array<TextureRegion> getRegions() {
        return this.regions;
    }

    public TextureRegion getRegion(int index) {
        return this.regions.get(index);
    }

    public float getLineHeight() {
        return this.data.lineHeight;
    }

    public float getSpaceWidth() {
        return this.data.spaceWidth;
    }

    public float getXHeight() {
        return this.data.xHeight;
    }

    public float getCapHeight() {
        return this.data.capHeight;
    }

    public float getAscent() {
        return this.data.ascent;
    }

    public float getDescent() {
        return this.data.descent;
    }

    public boolean isFlipped() {
        return this.flipped;
    }

    @Override
    public void dispose() {
        if (this.ownsTexture) {
            for (int i = 0; i < this.regions.size; ++i) {
                this.regions.get(i).getTexture().dispose();
            }
        }
    }

    public void setFixedWidthGlyphs(CharSequence glyphs) {
        Glyph g;
        int index;
        BitmapFontData data = this.data;
        int maxAdvance = 0;
        int end = glyphs.length();
        for (index = 0; index < end; ++index) {
            g = data.getGlyph(glyphs.charAt(index));
            if (g == null || g.xadvance <= maxAdvance) continue;
            maxAdvance = g.xadvance;
        }
        end = glyphs.length();
        for (index = 0; index < end; ++index) {
            g = data.getGlyph(glyphs.charAt(index));
            if (g == null) continue;
            g.xoffset += Math.round((maxAdvance - g.xadvance) / 2);
            g.xadvance = maxAdvance;
            g.kerning = null;
            g.fixedWidth = true;
        }
    }

    public void setUseIntegerPositions(boolean integer) {
        this.integer = integer;
        this.cache.setUseIntegerPositions(integer);
    }

    public boolean usesIntegerPositions() {
        return this.integer;
    }

    public BitmapFontCache getCache() {
        return this.cache;
    }

    public BitmapFontData getData() {
        return this.data;
    }

    public boolean ownsTexture() {
        return this.ownsTexture;
    }

    public void setOwnsTexture(boolean ownsTexture) {
        this.ownsTexture = ownsTexture;
    }

    public BitmapFontCache newFontCache() {
        return new BitmapFontCache(this, this.integer);
    }

    public String toString() {
        if (this.data.fontFile != null) {
            return this.data.fontFile.nameWithoutExtension();
        }
        return super.toString();
    }

    static int indexOf(CharSequence text, char ch, int start) {
        int n = text.length();
        while (start < n) {
            if (text.charAt(start) == ch) {
                return start;
            }
            ++start;
        }
        return n;
    }

    public static class BitmapFontData {
        public String[] imagePaths;
        public FileHandle fontFile;
        public boolean flipped;
        public float padTop;
        public float padRight;
        public float padBottom;
        public float padLeft;
        public float lineHeight;
        public float capHeight = 1.0f;
        public float ascent;
        public float descent;
        public float down;
        public float blankLineScale = 1.0f;
        public float scaleX = 1.0f;
        public float scaleY = 1.0f;
        public boolean markupEnabled;
        public float cursorX;
        public final Glyph[][] glyphs = new Glyph[128][];
        public Glyph missingGlyph;
        public float spaceWidth;
        public float xHeight = 1.0f;
        public char[] breakChars;
        public char[] xChars = new char[]{'x', 'e', 'a', 'o', 'n', 's', 'r', 'c', 'u', 'm', 'v', 'w', 'z'};
        public char[] capChars = new char[]{'M', 'N', 'B', 'D', 'C', 'E', 'F', 'K', 'A', 'G', 'H', 'I', 'J', 'L', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z'};

        public BitmapFontData() {
        }

        public BitmapFontData(FileHandle fontFile, boolean flip) {
            this.fontFile = fontFile;
            this.flipped = flip;
            this.load(fontFile, flip);
        }

        public void load(FileHandle fontFile, boolean flip) {
            if (this.imagePaths != null) {
                throw new IllegalStateException("Already loaded.");
            }
            BufferedReader reader = new BufferedReader(new InputStreamReader(fontFile.read()), 512);
            try {
                char capChar;
                char xChar;
                int n;
                String line = reader.readLine();
                if (line == null) {
                    throw new GdxRuntimeException("File is empty.");
                }
                String[] padding = (line = line.substring(line.indexOf("padding=") + 8)).substring(0, line.indexOf(32)).split(",", 4);
                if (padding.length != 4) {
                    throw new GdxRuntimeException("Invalid padding.");
                }
                this.padTop = Integer.parseInt(padding[0]);
                this.padRight = Integer.parseInt(padding[1]);
                this.padBottom = Integer.parseInt(padding[2]);
                this.padLeft = Integer.parseInt(padding[3]);
                float padY = this.padTop + this.padBottom;
                line = reader.readLine();
                if (line == null) {
                    throw new GdxRuntimeException("Missing common header.");
                }
                String[] common = line.split(" ", 7);
                if (common.length < 3) {
                    throw new GdxRuntimeException("Invalid common header.");
                }
                if (!common[1].startsWith("lineHeight=")) {
                    throw new GdxRuntimeException("Missing: lineHeight");
                }
                this.lineHeight = Integer.parseInt(common[1].substring(11));
                if (!common[2].startsWith("base=")) {
                    throw new GdxRuntimeException("Missing: base");
                }
                float baseLine = Integer.parseInt(common[2].substring(5));
                int pageCount = 1;
                if (common.length >= 6 && common[5] != null && common[5].startsWith("pages=")) {
                    try {
                        pageCount = Math.max(1, Integer.parseInt(common[5].substring(6)));
                    }
                    catch (NumberFormatException numberFormatException) {
                        // empty catch block
                    }
                }
                this.imagePaths = new String[pageCount];
                for (int p = 0; p < pageCount; ++p) {
                    line = reader.readLine();
                    if (line == null) {
                        throw new GdxRuntimeException("Missing additional page definitions.");
                    }
                    Matcher matcher = Pattern.compile(".*id=(\\d+)").matcher(line);
                    if (matcher.matches()) {
                        String id = matcher.group(1);
                        try {
                            int n2 = Integer.parseInt(id.substring(3));
                            if (n2 != p) {
                                throw new GdxRuntimeException("Page IDs must be indices starting at 0: " + id.substring(3));
                            }
                        }
                        catch (NumberFormatException numberFormatException) {
                            throw new GdxRuntimeException("Invalid page id: " + id.substring(3), numberFormatException);
                        }
                    }
                    if (!(matcher = Pattern.compile(".*file=\"?([^\"]*+)\"?").matcher(line)).matches()) {
                        throw new GdxRuntimeException("Missing: file");
                    }
                    String fileName = matcher.group(1);
                    this.imagePaths[p] = fontFile.parent().child(fileName).path().replaceAll("\\\\", "/");
                }
                this.descent = 0.0f;
                while ((line = reader.readLine()) != null && !line.startsWith("kernings ")) {
                    if (!line.startsWith("char ")) continue;
                    Glyph glyph = new Glyph();
                    StringTokenizer tokens = new StringTokenizer(line, " =");
                    tokens.nextToken();
                    tokens.nextToken();
                    int ch = Integer.parseInt(tokens.nextToken());
                    if (ch <= 0) {
                        this.missingGlyph = glyph;
                    } else {
                        if (ch > 65535) continue;
                        this.setGlyph(ch, glyph);
                    }
                    glyph.id = ch;
                    tokens.nextToken();
                    glyph.srcX = Integer.parseInt(tokens.nextToken());
                    tokens.nextToken();
                    glyph.srcY = Integer.parseInt(tokens.nextToken());
                    tokens.nextToken();
                    glyph.width = Integer.parseInt(tokens.nextToken());
                    tokens.nextToken();
                    glyph.height = Integer.parseInt(tokens.nextToken());
                    tokens.nextToken();
                    glyph.xoffset = Integer.parseInt(tokens.nextToken());
                    tokens.nextToken();
                    glyph.yoffset = flip ? Integer.parseInt(tokens.nextToken()) : -(glyph.height + Integer.parseInt(tokens.nextToken()));
                    tokens.nextToken();
                    glyph.xadvance = Integer.parseInt(tokens.nextToken());
                    if (tokens.hasMoreTokens()) {
                        tokens.nextToken();
                    }
                    if (tokens.hasMoreTokens()) {
                        try {
                            glyph.page = Integer.parseInt(tokens.nextToken());
                        }
                        catch (NumberFormatException numberFormatException) {
                            // empty catch block
                        }
                    }
                    if (glyph.width <= 0 || glyph.height <= 0) continue;
                    this.descent = Math.min(baseLine + (float)glyph.yoffset, this.descent);
                }
                this.descent += this.padBottom;
                while ((line = reader.readLine()) != null && line.startsWith("kerning ")) {
                    StringTokenizer tokens = new StringTokenizer(line, " =");
                    tokens.nextToken();
                    tokens.nextToken();
                    int first = Integer.parseInt(tokens.nextToken());
                    tokens.nextToken();
                    int second = Integer.parseInt(tokens.nextToken());
                    if (first < 0 || first > 65535 || second < 0 || second > 65535) continue;
                    Glyph glyph = this.getGlyph((char)first);
                    tokens.nextToken();
                    int amount = Integer.parseInt(tokens.nextToken());
                    if (glyph == null) continue;
                    glyph.setKerning(second, amount);
                }
                Glyph spaceGlyph = this.getGlyph(' ');
                if (spaceGlyph == null) {
                    spaceGlyph = new Glyph();
                    spaceGlyph.id = 32;
                    Glyph xadvanceGlyph = this.getGlyph('l');
                    if (xadvanceGlyph == null) {
                        xadvanceGlyph = this.getFirstGlyph();
                    }
                    spaceGlyph.xadvance = xadvanceGlyph.xadvance;
                    this.setGlyph(32, spaceGlyph);
                }
                if (spaceGlyph.width == 0) {
                    spaceGlyph.width = (int)(this.padLeft + (float)spaceGlyph.xadvance + this.padRight);
                    spaceGlyph.xoffset = (int)(-this.padLeft);
                }
                this.spaceWidth = spaceGlyph.width;
                Glyph xGlyph = null;
                char[] second = this.xChars;
                int n2 = second.length;
                for (n = 0; n < n2 && (xGlyph = this.getGlyph(xChar = second[n])) == null; ++n) {
                }
                if (xGlyph == null) {
                    xGlyph = this.getFirstGlyph();
                }
                this.xHeight = (float)xGlyph.height - padY;
                Glyph capGlyph = null;
                char[] object = this.capChars;
                n = object.length;
                for (int i = 0; i < n && (capGlyph = this.getGlyph(capChar = object[i])) == null; ++i) {
                }
                if (capGlyph == null) {
                    for (Glyph[] page : this.glyphs) {
                        if (page == null) continue;
                        for (Glyph glyph : page) {
                            if (glyph == null || glyph.height == 0 || glyph.width == 0) continue;
                            this.capHeight = Math.max(this.capHeight, (float)glyph.height);
                        }
                    }
                } else {
                    this.capHeight = capGlyph.height;
                }
                this.capHeight -= padY;
                this.ascent = baseLine - this.capHeight;
                this.down = -this.lineHeight;
                if (flip) {
                    this.ascent = -this.ascent;
                    this.down = -this.down;
                }
            }
            catch (Exception ex) {
                throw new GdxRuntimeException("Error loading font file: " + fontFile, ex);
            }
            finally {
                StreamUtils.closeQuietly(reader);
            }
        }

        public void setGlyphRegion(Glyph glyph, TextureRegion region) {
            Texture texture = region.getTexture();
            float invTexWidth = 1.0f / (float)texture.getWidth();
            float invTexHeight = 1.0f / (float)texture.getHeight();
            float offsetX = 0.0f;
            float offsetY = 0.0f;
            float u = region.u;
            float v = region.v;
            float regionWidth = region.getRegionWidth();
            float regionHeight = region.getRegionHeight();
            if (region instanceof TextureAtlas.AtlasRegion) {
                TextureAtlas.AtlasRegion atlasRegion = (TextureAtlas.AtlasRegion)region;
                offsetX = atlasRegion.offsetX;
                offsetY = (float)(atlasRegion.originalHeight - atlasRegion.packedHeight) - atlasRegion.offsetY;
            }
            float x = glyph.srcX;
            float x2 = glyph.srcX + glyph.width;
            float y = glyph.srcY;
            float y2 = glyph.srcY + glyph.height;
            if (offsetX > 0.0f) {
                if ((x -= offsetX) < 0.0f) {
                    glyph.width = (int)((float)glyph.width + x);
                    glyph.xoffset = (int)((float)glyph.xoffset - x);
                    x = 0.0f;
                }
                if ((x2 -= offsetX) > regionWidth) {
                    glyph.width = (int)((float)glyph.width - (x2 - regionWidth));
                    x2 = regionWidth;
                }
            }
            if (offsetY > 0.0f) {
                if ((y -= offsetY) < 0.0f) {
                    glyph.height = (int)((float)glyph.height + y);
                    y = 0.0f;
                }
                if ((y2 -= offsetY) > regionHeight) {
                    float amount = y2 - regionHeight;
                    glyph.height = (int)((float)glyph.height - amount);
                    glyph.yoffset = (int)((float)glyph.yoffset + amount);
                    y2 = regionHeight;
                }
            }
            glyph.u = u + x * invTexWidth;
            glyph.u2 = u + x2 * invTexWidth;
            if (this.flipped) {
                glyph.v = v + y * invTexHeight;
                glyph.v2 = v + y2 * invTexHeight;
            } else {
                glyph.v2 = v + y * invTexHeight;
                glyph.v = v + y2 * invTexHeight;
            }
        }

        public void setLineHeight(float height) {
            this.lineHeight = height * this.scaleY;
            this.down = this.flipped ? this.lineHeight : -this.lineHeight;
        }

        public void setGlyph(int ch, Glyph glyph) {
            Glyph[] page = this.glyphs[ch / 512];
            if (page == null) {
                page = new Glyph[512];
                this.glyphs[ch / 512] = page;
            }
            page[ch & 0x1FF] = glyph;
        }

        public Glyph getFirstGlyph() {
            for (Glyph[] page : this.glyphs) {
                if (page == null) continue;
                for (Glyph glyph : page) {
                    if (glyph == null || glyph.height == 0 || glyph.width == 0) continue;
                    return glyph;
                }
            }
            throw new GdxRuntimeException("No glyphs found.");
        }

        public boolean hasGlyph(char ch) {
            if (this.missingGlyph != null) {
                return true;
            }
            return this.getGlyph(ch) != null;
        }

        public Glyph getGlyph(char ch) {
            Glyph[] page = this.glyphs[ch / 512];
            if (page != null) {
                return page[ch & 0x1FF];
            }
            return null;
        }

        public void getGlyphs(GlyphLayout.GlyphRun run, CharSequence str, int start, int end, boolean tightBounds) {
            boolean markupEnabled = this.markupEnabled;
            float scaleX = this.scaleX;
            Glyph missingGlyph = this.missingGlyph;
            Array<Glyph> glyphs = run.glyphs;
            FloatArray xAdvances = run.xAdvances;
            glyphs.ensureCapacity(end - start);
            xAdvances.ensureCapacity(end - start + 1);
            Glyph lastGlyph = null;
            while (start < end) {
                char ch;
                Glyph glyph;
                if ((glyph = this.getGlyph(ch = str.charAt(start++))) == null) {
                    if (missingGlyph == null) continue;
                    glyph = missingGlyph;
                }
                glyphs.add(glyph);
                if (lastGlyph == null) {
                    xAdvances.add(!tightBounds || glyph.fixedWidth ? 0.0f : (float)(-glyph.xoffset) * scaleX - this.padLeft);
                } else {
                    xAdvances.add((float)(lastGlyph.xadvance + lastGlyph.getKerning(ch)) * scaleX);
                }
                lastGlyph = glyph;
                if (!markupEnabled || ch != '[' || start >= end || str.charAt(start) != '[') continue;
                ++start;
            }
            if (lastGlyph != null) {
                float lastGlyphWidth = !tightBounds || lastGlyph.fixedWidth ? (float)lastGlyph.xadvance : (float)(lastGlyph.xoffset + lastGlyph.width) - this.padRight;
                xAdvances.add(lastGlyphWidth * scaleX);
            }
        }

        public int getWrapIndex(Array<Glyph> glyphs, int start) {
            int i;
            for (i = start - 1; i >= 1 && this.isWhitespace((char)glyphs.get((int)i).id); --i) {
            }
            while (i >= 1) {
                char ch = (char)glyphs.get((int)i).id;
                if (this.isWhitespace(ch) || this.isBreakChar(ch)) {
                    return i + 1;
                }
                --i;
            }
            return 0;
        }

        public boolean isBreakChar(char c) {
            if (this.breakChars == null) {
                return false;
            }
            for (char br : this.breakChars) {
                if (c != br) continue;
                return true;
            }
            return false;
        }

        public boolean isWhitespace(char c) {
            switch (c) {
                case '\t': 
                case '\n': 
                case '\r': 
                case ' ': {
                    return true;
                }
            }
            return false;
        }

        public String getImagePath(int index) {
            return this.imagePaths[index];
        }

        public String[] getImagePaths() {
            return this.imagePaths;
        }

        public FileHandle getFontFile() {
            return this.fontFile;
        }

        public void setScale(float scaleX, float scaleY) {
            if (scaleX == 0.0f) {
                throw new IllegalArgumentException("scaleX cannot be 0.");
            }
            if (scaleY == 0.0f) {
                throw new IllegalArgumentException("scaleY cannot be 0.");
            }
            float x = scaleX / this.scaleX;
            float y = scaleY / this.scaleY;
            this.lineHeight *= y;
            this.spaceWidth *= x;
            this.xHeight *= y;
            this.capHeight *= y;
            this.ascent *= y;
            this.descent *= y;
            this.down *= y;
            this.padTop *= y;
            this.padLeft *= y;
            this.padBottom *= y;
            this.padRight *= y;
            this.scaleX = scaleX;
            this.scaleY = scaleY;
        }

        public void setScale(float scaleXY) {
            this.setScale(scaleXY, scaleXY);
        }

        public void scale(float amount) {
            this.setScale(this.scaleX + amount, this.scaleY + amount);
        }
    }

    public static class Glyph {
        public int id;
        public int srcX;
        public int srcY;
        public int width;
        public int height;
        public float u;
        public float v;
        public float u2;
        public float v2;
        public int xoffset;
        public int yoffset;
        public int xadvance;
        public byte[][] kerning;
        public boolean fixedWidth;
        public int page = 0;

        public int getKerning(char ch) {
            byte[] page;
            if (this.kerning != null && (page = this.kerning[ch >>> 9]) != null) {
                return page[ch & 0x1FF];
            }
            return 0;
        }

        public void setKerning(int ch, int value) {
            byte[] page;
            if (this.kerning == null) {
                this.kerning = new byte[128][];
            }
            if ((page = this.kerning[ch >>> 9]) == null) {
                page = new byte[512];
                this.kerning[ch >>> 9] = page;
            }
            page[ch & 0x1FF] = (byte)value;
        }

        public String toString() {
            return Character.toString((char)this.id);
        }
    }
}

