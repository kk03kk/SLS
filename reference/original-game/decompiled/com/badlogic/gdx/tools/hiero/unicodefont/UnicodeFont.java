/*
 * Decompiled with CFR 0.152.
 */
package com.badlogic.gdx.tools.hiero.unicodefont;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.BitmapFontCache;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.tools.hiero.HieroSettings;
import com.badlogic.gdx.tools.hiero.unicodefont.Glyph;
import com.badlogic.gdx.tools.hiero.unicodefont.GlyphPage;
import com.badlogic.gdx.tools.hiero.unicodefont.effects.Effect;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.GdxRuntimeException;
import java.awt.Font;
import java.awt.FontFormatException;
import java.awt.FontMetrics;
import java.awt.Rectangle;
import java.awt.font.GlyphVector;
import java.awt.font.TextAttribute;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.lwjgl.opengl.GL11;

public class UnicodeFont {
    private static final int DISPLAY_LIST_CACHE_SIZE = 200;
    private static final int MAX_GLYPH_CODE = 0x10FFFF;
    private static final int PAGE_SIZE = 512;
    private static final int PAGES = 2175;
    private Font font;
    private FontMetrics metrics;
    private String ttfFileRef;
    private int ascent;
    private int descent;
    private int leading;
    private int spaceWidth;
    private final Glyph[][] glyphs = new Glyph[2175][];
    private final List<GlyphPage> glyphPages = new ArrayList<GlyphPage>();
    private final List<Glyph> queuedGlyphs = new ArrayList<Glyph>(256);
    private final List<Effect> effects = new ArrayList<Effect>();
    private int paddingTop;
    private int paddingLeft;
    private int paddingBottom;
    private int paddingRight;
    private int paddingAdvanceX;
    private int paddingAdvanceY;
    private Glyph missingGlyph;
    private int glyphPageWidth = 512;
    private int glyphPageHeight = 512;
    RenderType renderType;
    BitmapFont bitmapFont;
    private FreeTypeFontGenerator generator;
    private BitmapFontCache cache;
    private GlyphLayout layout;
    private boolean mono;
    private float gamma;
    private static final int X = 0;
    private static final int Y = 1;
    private static final int U = 3;
    private static final int V = 4;
    private static final int X2 = 10;
    private static final int Y2 = 11;
    private static final int U2 = 13;
    private static final int V2 = 14;
    private static final Comparator heightComparator = new Comparator(){

        public int compare(Object o1, Object o2) {
            return ((Glyph)o2).getHeight() - ((Glyph)o1).getHeight();
        }
    };

    public UnicodeFont(String ttfFileRef, String hieroFileRef) {
        this(ttfFileRef, new HieroSettings(hieroFileRef));
    }

    public UnicodeFont(String ttfFileRef, HieroSettings settings) {
        this.ttfFileRef = ttfFileRef;
        Font font = UnicodeFont.createFont(ttfFileRef);
        this.initializeFont(font, settings.getFontSize(), settings.isBold(), settings.isItalic());
        this.loadSettings(settings);
    }

    public UnicodeFont(String ttfFileRef, int size, boolean bold, boolean italic) {
        this.ttfFileRef = ttfFileRef;
        this.initializeFont(UnicodeFont.createFont(ttfFileRef), size, bold, italic);
    }

    public UnicodeFont(Font font, String hieroFileRef) {
        this(font, new HieroSettings(hieroFileRef));
    }

    public UnicodeFont(Font font, HieroSettings settings) {
        this.initializeFont(font, settings.getFontSize(), settings.isBold(), settings.isItalic());
        this.loadSettings(settings);
    }

    public UnicodeFont(Font font) {
        this.initializeFont(font, font.getSize(), font.isBold(), font.isItalic());
    }

    public UnicodeFont(Font font, int size, boolean bold, boolean italic) {
        this.initializeFont(font, size, bold, italic);
    }

    private void initializeFont(Font baseFont, int size, boolean bold, boolean italic) {
        Map<TextAttribute, ?> attributes = baseFont.getAttributes();
        attributes.put(TextAttribute.SIZE, new Float(size));
        attributes.put(TextAttribute.WEIGHT, bold ? TextAttribute.WEIGHT_BOLD : TextAttribute.WEIGHT_REGULAR);
        attributes.put(TextAttribute.POSTURE, italic ? TextAttribute.POSTURE_OBLIQUE : TextAttribute.POSTURE_REGULAR);
        try {
            attributes.put((TextAttribute)TextAttribute.class.getDeclaredField("KERNING").get(null), TextAttribute.class.getDeclaredField("KERNING_ON").get(null));
        }
        catch (Throwable throwable) {
            // empty catch block
        }
        this.font = baseFont.deriveFont(attributes);
        this.metrics = GlyphPage.scratchGraphics.getFontMetrics(this.font);
        this.ascent = this.metrics.getAscent();
        this.descent = this.metrics.getDescent();
        this.leading = this.metrics.getLeading();
        char[] chars = " ".toCharArray();
        GlyphVector vector = this.font.layoutGlyphVector(GlyphPage.renderContext, chars, 0, chars.length, 0);
        this.spaceWidth = vector.getGlyphLogicalBounds((int)0).getBounds().width;
    }

    private void loadSettings(HieroSettings settings) {
        this.paddingTop = settings.getPaddingTop();
        this.paddingLeft = settings.getPaddingLeft();
        this.paddingBottom = settings.getPaddingBottom();
        this.paddingRight = settings.getPaddingRight();
        this.paddingAdvanceX = settings.getPaddingAdvanceX();
        this.paddingAdvanceY = settings.getPaddingAdvanceY();
        this.glyphPageWidth = settings.getGlyphPageWidth();
        this.glyphPageHeight = settings.getGlyphPageHeight();
        this.effects.addAll(settings.getEffects());
    }

    public void addGlyphs(int startCodePoint, int endCodePoint) {
        for (int codePoint = startCodePoint; codePoint <= endCodePoint; ++codePoint) {
            this.addGlyphs(new String(Character.toChars(codePoint)));
        }
    }

    public void addGlyphs(String text) {
        if (text == null) {
            throw new IllegalArgumentException("text cannot be null.");
        }
        char[] chars = text.toCharArray();
        GlyphVector vector = this.font.layoutGlyphVector(GlyphPage.renderContext, chars, 0, chars.length, 0);
        int n = vector.getNumGlyphs();
        for (int i = 0; i < n; ++i) {
            int codePoint = text.codePointAt(vector.getGlyphCharIndex(i));
            Rectangle bounds = this.getGlyphBounds(vector, i, codePoint);
            this.getGlyph(vector.getGlyphCode(i), codePoint, bounds, vector, i);
        }
    }

    public void addAsciiGlyphs() {
        this.addGlyphs(32, 255);
    }

    public void addNeheGlyphs() {
        this.addGlyphs(32, 128);
    }

    public boolean loadGlyphs() {
        return this.loadGlyphs(-1);
    }

    public boolean loadGlyphs(int maxGlyphsToLoad) {
        if (this.queuedGlyphs.isEmpty()) {
            return false;
        }
        if (this.effects.isEmpty()) {
            throw new IllegalStateException("The UnicodeFont must have at least one effect before any glyphs can be loaded.");
        }
        Iterator<Object> iter = this.queuedGlyphs.iterator();
        while (iter.hasNext()) {
            Glyph glyph = iter.next();
            int codePoint = glyph.getCodePoint();
            if (!glyph.isMissing()) continue;
            if (this.missingGlyph != null) {
                if (glyph == this.missingGlyph) continue;
                iter.remove();
                continue;
            }
            this.missingGlyph = glyph;
        }
        Collections.sort(this.queuedGlyphs, heightComparator);
        for (GlyphPage glyphPage : this.glyphPages) {
            if ((maxGlyphsToLoad -= glyphPage.loadGlyphs(this.queuedGlyphs, maxGlyphsToLoad)) != 0 && !this.queuedGlyphs.isEmpty()) continue;
            return true;
        }
        while (!this.queuedGlyphs.isEmpty()) {
            GlyphPage glyphPage = new GlyphPage(this, this.glyphPageWidth, this.glyphPageHeight);
            this.glyphPages.add(glyphPage);
            if ((maxGlyphsToLoad -= glyphPage.loadGlyphs(this.queuedGlyphs, maxGlyphsToLoad)) != 0) continue;
            return true;
        }
        return true;
    }

    public void dispose() {
        for (GlyphPage page : this.glyphPages) {
            page.getTexture().dispose();
        }
        if (this.bitmapFont != null) {
            this.bitmapFont.dispose();
            this.generator.dispose();
        }
    }

    public void drawString(float x, float y, String text, Color color, int startIndex, int endIndex) {
        if (text == null) {
            throw new IllegalArgumentException("text cannot be null.");
        }
        if (text.length() == 0) {
            return;
        }
        if (color == null) {
            throw new IllegalArgumentException("color cannot be null.");
        }
        GL11.glColor4f(color.r, color.g, color.b, color.a);
        GL11.glTranslatef(x -= (float)this.paddingLeft, y -= (float)this.paddingTop, 0.0f);
        if (this.renderType == RenderType.FreeType && this.bitmapFont != null) {
            this.drawBitmap(text, startIndex, endIndex);
        } else {
            this.drawUnicode(text, startIndex, endIndex);
        }
        GL11.glTranslatef(-x, -y, 0.0f);
    }

    private void drawBitmap(String text, int startIndex, int endIndex) {
        BitmapFont.BitmapFontData data = this.bitmapFont.getData();
        int padY = this.paddingTop + this.paddingBottom + this.paddingAdvanceY;
        data.setLineHeight(data.lineHeight + (float)padY);
        this.layout.setText(this.bitmapFont, text);
        data.setLineHeight(data.lineHeight - (float)padY);
        for (GlyphLayout.GlyphRun run : this.layout.runs) {
            int n = run.xAdvances.size;
            for (int i = 0; i < n; ++i) {
                run.xAdvances.incr(i, this.paddingAdvanceX + this.paddingLeft + this.paddingRight);
            }
        }
        this.cache.setText(this.layout, (float)this.paddingLeft, (float)this.paddingRight);
        Array<TextureRegion> regions = this.bitmapFont.getRegions();
        int n = regions.size;
        for (int i = 0; i < n; ++i) {
            regions.get(i).getTexture().bind();
            GL11.glBegin(7);
            float[] vertices = this.cache.getVertices(i);
            int nn = vertices.length;
            for (int ii = 0; ii < nn; ii += 20) {
                GL11.glTexCoord2f(vertices[ii + 3], vertices[ii + 4]);
                GL11.glVertex3f(vertices[ii + 0], vertices[ii + 1], 0.0f);
                GL11.glTexCoord2f(vertices[ii + 3], vertices[ii + 14]);
                GL11.glVertex3f(vertices[ii + 0], vertices[ii + 11], 0.0f);
                GL11.glTexCoord2f(vertices[ii + 13], vertices[ii + 14]);
                GL11.glVertex3f(vertices[ii + 10], vertices[ii + 11], 0.0f);
                GL11.glTexCoord2f(vertices[ii + 13], vertices[ii + 4]);
                GL11.glVertex3f(vertices[ii + 10], vertices[ii + 1], 0.0f);
            }
            GL11.glEnd();
        }
    }

    private void drawUnicode(String text, int startIndex, int endIndex) {
        char[] chars = text.substring(0, endIndex).toCharArray();
        GlyphVector vector = this.font.layoutGlyphVector(GlyphPage.renderContext, chars, 0, chars.length, 0);
        int maxWidth = 0;
        int totalHeight = 0;
        int lines = 0;
        int extraX = 0;
        int extraY = this.ascent;
        boolean startNewLine = false;
        Texture lastBind = null;
        int offsetX = 0;
        int n = vector.getNumGlyphs();
        for (int glyphIndex = 0; glyphIndex < n; ++glyphIndex) {
            int charIndex = vector.getGlyphCharIndex(glyphIndex);
            if (charIndex < startIndex) continue;
            if (charIndex > endIndex) break;
            int codePoint = text.codePointAt(charIndex);
            Rectangle bounds = this.getGlyphBounds(vector, glyphIndex, codePoint);
            bounds.x += offsetX;
            Glyph glyph = this.getGlyph(vector.getGlyphCode(glyphIndex), codePoint, bounds, vector, glyphIndex);
            if (startNewLine && codePoint != 10) {
                extraX = -bounds.x;
                startNewLine = false;
            }
            if (glyph.getTexture() == null && this.missingGlyph != null && glyph.isMissing()) {
                glyph = this.missingGlyph;
            }
            if (glyph.getTexture() != null) {
                Texture texture = glyph.getTexture();
                if (lastBind != null && lastBind != texture) {
                    GL11.glEnd();
                    lastBind = null;
                }
                if (lastBind == null) {
                    texture.bind();
                    GL11.glBegin(7);
                    lastBind = texture;
                }
                int glyphX = bounds.x + extraX;
                int glyphY = bounds.y + extraY;
                GL11.glTexCoord2f(glyph.getU(), glyph.getV());
                GL11.glVertex3f(glyphX, glyphY, 0.0f);
                GL11.glTexCoord2f(glyph.getU(), glyph.getV2());
                GL11.glVertex3f(glyphX, glyphY + glyph.getHeight(), 0.0f);
                GL11.glTexCoord2f(glyph.getU2(), glyph.getV2());
                GL11.glVertex3f(glyphX + glyph.getWidth(), glyphY + glyph.getHeight(), 0.0f);
                GL11.glTexCoord2f(glyph.getU2(), glyph.getV());
                GL11.glVertex3f(glyphX + glyph.getWidth(), glyphY, 0.0f);
            }
            if (glyphIndex > 0) {
                extraX += this.paddingRight + this.paddingLeft + this.paddingAdvanceX;
            }
            maxWidth = Math.max(maxWidth, bounds.x + extraX + bounds.width);
            totalHeight = Math.max(totalHeight, this.ascent + bounds.y + bounds.height);
            if (codePoint == 10) {
                startNewLine = true;
                extraY += this.getLineHeight();
                ++lines;
                totalHeight = 0;
                continue;
            }
            if (this.renderType != RenderType.Native) continue;
            offsetX += bounds.width;
        }
        if (lastBind != null) {
            GL11.glEnd();
        }
    }

    public void drawString(float x, float y, String text) {
        this.drawString(x, y, text, Color.WHITE);
    }

    public void drawString(float x, float y, String text, Color col) {
        this.drawString(x, y, text, col, 0, text.length());
    }

    public Glyph getGlyph(int glyphCode, int codePoint, Rectangle bounds, GlyphVector vector, int index) {
        if (glyphCode < 0 || glyphCode >= 0x10FFFF) {
            return new Glyph(codePoint, bounds, vector, index, this){

                @Override
                public boolean isMissing() {
                    return true;
                }
            };
        }
        int pageIndex = glyphCode / 512;
        int glyphIndex = glyphCode & 0x1FF;
        Glyph glyph = null;
        Glyph[] page = this.glyphs[pageIndex];
        if (page != null) {
            glyph = page[glyphIndex];
            if (glyph != null) {
                return glyph;
            }
        } else {
            this.glyphs[pageIndex] = new Glyph[512];
            page = this.glyphs[pageIndex];
        }
        glyph = page[glyphIndex] = new Glyph(codePoint, bounds, vector, index, this);
        this.queuedGlyphs.add(glyph);
        return glyph;
    }

    private Rectangle getGlyphBounds(GlyphVector vector, int index, int codePoint) {
        Rectangle bounds = vector.getGlyphPixelBounds(index, GlyphPage.renderContext, 0.0f, 0.0f);
        if (this.renderType == RenderType.Native) {
            bounds = bounds.width == 0 || bounds.height == 0 ? new Rectangle() : this.metrics.getStringBounds("" + (char)codePoint, GlyphPage.scratchGraphics).getBounds();
        }
        if (codePoint == 32) {
            bounds.width = this.spaceWidth;
        }
        return bounds;
    }

    public int getSpaceWidth() {
        return this.spaceWidth;
    }

    public int getWidth(String text) {
        if (text == null) {
            throw new IllegalArgumentException("text cannot be null.");
        }
        if (text.length() == 0) {
            return 0;
        }
        char[] chars = text.toCharArray();
        GlyphVector vector = this.font.layoutGlyphVector(GlyphPage.renderContext, chars, 0, chars.length, 0);
        int width = 0;
        int extraX = 0;
        boolean startNewLine = false;
        int n = vector.getNumGlyphs();
        for (int glyphIndex = 0; glyphIndex < n; ++glyphIndex) {
            int charIndex = vector.getGlyphCharIndex(glyphIndex);
            int codePoint = text.codePointAt(charIndex);
            Rectangle bounds = this.getGlyphBounds(vector, glyphIndex, codePoint);
            if (startNewLine && codePoint != 10) {
                extraX = -bounds.x;
            }
            if (glyphIndex > 0) {
                extraX += this.paddingLeft + this.paddingRight + this.paddingAdvanceX;
            }
            width = Math.max(width, bounds.x + extraX + bounds.width);
            if (codePoint != 10) continue;
            startNewLine = true;
        }
        return width;
    }

    public int getHeight(String text) {
        if (text == null) {
            throw new IllegalArgumentException("text cannot be null.");
        }
        if (text.length() == 0) {
            return 0;
        }
        char[] chars = text.toCharArray();
        GlyphVector vector = this.font.layoutGlyphVector(GlyphPage.renderContext, chars, 0, chars.length, 0);
        int lines = 0;
        int height = 0;
        int n = vector.getNumGlyphs();
        for (int i = 0; i < n; ++i) {
            int charIndex = vector.getGlyphCharIndex(i);
            int codePoint = text.codePointAt(charIndex);
            if (codePoint == 32) continue;
            Rectangle bounds = this.getGlyphBounds(vector, i, codePoint);
            height = Math.max(height, this.ascent + bounds.y + bounds.height);
            if (codePoint != 10) continue;
            ++lines;
            height = 0;
        }
        return lines * this.getLineHeight() + height;
    }

    public int getYOffset(String text) {
        if (text == null) {
            throw new IllegalArgumentException("text cannot be null.");
        }
        if (this.renderType == RenderType.FreeType && this.bitmapFont != null) {
            return (int)this.bitmapFont.getAscent();
        }
        int index = text.indexOf(10);
        if (index != -1) {
            text = text.substring(0, index);
        }
        char[] chars = text.toCharArray();
        GlyphVector vector = this.font.layoutGlyphVector(GlyphPage.renderContext, chars, 0, chars.length, 0);
        int yOffset = this.ascent + vector.getPixelBounds(null, (float)0.0f, (float)0.0f).y;
        return yOffset;
    }

    public Font getFont() {
        return this.font;
    }

    public int getPaddingTop() {
        return this.paddingTop;
    }

    public void setPaddingTop(int paddingTop) {
        this.paddingTop = paddingTop;
    }

    public int getPaddingLeft() {
        return this.paddingLeft;
    }

    public void setPaddingLeft(int paddingLeft) {
        this.paddingLeft = paddingLeft;
    }

    public int getPaddingBottom() {
        return this.paddingBottom;
    }

    public void setPaddingBottom(int paddingBottom) {
        this.paddingBottom = paddingBottom;
    }

    public int getPaddingRight() {
        return this.paddingRight;
    }

    public void setPaddingRight(int paddingRight) {
        this.paddingRight = paddingRight;
    }

    public int getPaddingAdvanceX() {
        return this.paddingAdvanceX;
    }

    public void setPaddingAdvanceX(int paddingAdvanceX) {
        this.paddingAdvanceX = paddingAdvanceX;
    }

    public int getPaddingAdvanceY() {
        return this.paddingAdvanceY;
    }

    public void setPaddingAdvanceY(int paddingAdvanceY) {
        this.paddingAdvanceY = paddingAdvanceY;
    }

    public int getLineHeight() {
        return this.descent + this.ascent + this.leading + this.paddingTop + this.paddingBottom + this.paddingAdvanceY;
    }

    public int getAscent() {
        return this.ascent;
    }

    public int getDescent() {
        return this.descent;
    }

    public int getLeading() {
        return this.leading;
    }

    public int getGlyphPageWidth() {
        return this.glyphPageWidth;
    }

    public void setGlyphPageWidth(int glyphPageWidth) {
        this.glyphPageWidth = glyphPageWidth;
    }

    public int getGlyphPageHeight() {
        return this.glyphPageHeight;
    }

    public void setGlyphPageHeight(int glyphPageHeight) {
        this.glyphPageHeight = glyphPageHeight;
    }

    public List getGlyphPages() {
        return this.glyphPages;
    }

    public List getEffects() {
        return this.effects;
    }

    public boolean getMono() {
        return this.mono;
    }

    public void setMono(boolean mono) {
        this.mono = mono;
    }

    public float getGamma() {
        return this.gamma;
    }

    public void setGamma(float gamma) {
        this.gamma = gamma;
    }

    public RenderType getRenderType() {
        return this.renderType;
    }

    public void setRenderType(RenderType renderType) {
        this.renderType = renderType;
        if (renderType != RenderType.FreeType) {
            if (this.bitmapFont != null) {
                this.bitmapFont.dispose();
                this.generator.dispose();
            }
        } else {
            String fontFile = this.getFontFile();
            if (fontFile != null) {
                this.generator = new FreeTypeFontGenerator(Gdx.files.absolute(fontFile));
                FreeTypeFontGenerator.FreeTypeFontParameter param = new FreeTypeFontGenerator.FreeTypeFontParameter();
                param.size = this.font.getSize();
                param.incremental = true;
                param.flip = true;
                param.mono = this.mono;
                param.gamma = this.gamma;
                this.bitmapFont = this.generator.generateFont(param);
                if (this.bitmapFont.getData().missingGlyph == null) {
                    this.bitmapFont.getData().missingGlyph = this.bitmapFont.getData().getGlyph('\ufffd');
                }
                this.cache = this.bitmapFont.newFontCache();
                this.layout = new GlyphLayout();
            }
        }
    }

    public String getFontFile() {
        if (this.ttfFileRef == null) {
            try {
                Object font2D;
                try {
                    font2D = Class.forName("sun.font.FontUtilities").getDeclaredMethod("getFont2D", Font.class).invoke(null, this.font);
                }
                catch (Throwable ignored) {
                    font2D = Class.forName("sun.font.FontManager").getDeclaredMethod("getFont2D", Font.class).invoke(null, this.font);
                }
                Field platNameField = Class.forName("sun.font.PhysicalFont").getDeclaredField("platName");
                platNameField.setAccessible(true);
                this.ttfFileRef = (String)platNameField.get(font2D);
            }
            catch (Throwable throwable) {
                // empty catch block
            }
            if (this.ttfFileRef == null) {
                this.ttfFileRef = "";
            }
        }
        if (this.ttfFileRef.length() == 0) {
            return null;
        }
        return this.ttfFileRef;
    }

    private static Font createFont(String ttfFileRef) {
        try {
            return Font.createFont(0, Gdx.files.absolute(ttfFileRef).read());
        }
        catch (FontFormatException ex) {
            throw new GdxRuntimeException("Invalid font: " + ttfFileRef, ex);
        }
        catch (IOException ex) {
            throw new GdxRuntimeException("Error reading font: " + ttfFileRef, ex);
        }
    }

    public static enum RenderType {
        Java,
        Native,
        FreeType;

    }
}

