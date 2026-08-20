/*
 * Decompiled with CFR 0.152.
 */
package com.badlogic.gdx.tools.hiero.unicodefont;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.tools.hiero.unicodefont.GlyphPage;
import com.badlogic.gdx.tools.hiero.unicodefont.UnicodeFont;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.font.FontRenderContext;
import java.awt.font.GlyphMetrics;
import java.awt.font.GlyphVector;

public class Glyph {
    private int codePoint;
    private short width;
    private short height;
    private short yOffset;
    private boolean isMissing;
    private Shape shape;
    float u;
    float v;
    float u2;
    float v2;
    private int xOffset;
    private int xAdvance;
    Texture texture;

    Glyph(int codePoint, Rectangle bounds, GlyphVector vector, int index, UnicodeFont unicodeFont) {
        this.codePoint = codePoint;
        int padTop = unicodeFont.getPaddingTop();
        int padBottom = unicodeFont.getPaddingBottom();
        int padLeft = unicodeFont.getPaddingLeft();
        int padRight = unicodeFont.getPaddingRight();
        if (unicodeFont.renderType == UnicodeFont.RenderType.FreeType && unicodeFont.bitmapFont != null) {
            BitmapFont.Glyph g = unicodeFont.bitmapFont.getData().getGlyph((char)codePoint);
            if (g == null) {
                this.isMissing = true;
            } else {
                boolean empty = g.width == 0 || g.height == 0;
                this.width = empty ? (short)0 : (short)(g.width + padLeft + padRight);
                this.height = empty ? (short)0 : (short)(g.height + padTop + padBottom);
                this.yOffset = (short)(g.yoffset - padTop);
                this.xOffset = g.xoffset - unicodeFont.getPaddingLeft();
                this.xAdvance = g.xadvance + unicodeFont.getPaddingAdvanceX() + unicodeFont.getPaddingLeft() + unicodeFont.getPaddingRight();
                this.isMissing = codePoint == 0;
            }
        } else {
            int rsb;
            GlyphMetrics metrics = vector.getGlyphMetrics(index);
            int lsb = (int)metrics.getLSB();
            if (lsb > 0) {
                lsb = 0;
            }
            if ((rsb = (int)metrics.getRSB()) > 0) {
                rsb = 0;
            }
            int glyphWidth = bounds.width - lsb - rsb;
            int glyphHeight = bounds.height;
            if (glyphWidth > 0 && glyphHeight > 0) {
                this.width = (short)(glyphWidth + padLeft + padRight);
                this.height = (short)(glyphHeight + padTop + padBottom);
                this.yOffset = (short)(unicodeFont.getAscent() + bounds.y - padTop);
            }
            char[] chars = Character.toChars(codePoint);
            GlyphVector charVector = unicodeFont.getFont().layoutGlyphVector(GlyphPage.renderContext, chars, 0, chars.length, 0);
            GlyphMetrics charMetrics = charVector.getGlyphMetrics(0);
            this.xOffset = charVector.getGlyphPixelBounds((int)0, (FontRenderContext)GlyphPage.renderContext, (float)0.0f, (float)0.0f).x - unicodeFont.getPaddingLeft();
            this.xAdvance = (int)(metrics.getAdvanceX() + (float)unicodeFont.getPaddingAdvanceX() + (float)unicodeFont.getPaddingLeft() + (float)unicodeFont.getPaddingRight());
            this.shape = vector.getGlyphOutline(index, -bounds.x + unicodeFont.getPaddingLeft(), -bounds.y + unicodeFont.getPaddingTop());
            this.isMissing = !unicodeFont.getFont().canDisplay((char)codePoint);
        }
    }

    public int getCodePoint() {
        return this.codePoint;
    }

    public boolean isMissing() {
        return this.isMissing;
    }

    public int getWidth() {
        return this.width;
    }

    public int getHeight() {
        return this.height;
    }

    public Shape getShape() {
        return this.shape;
    }

    public void setShape(Shape shape) {
        this.shape = shape;
    }

    public void setTexture(Texture texture, float u, float v, float u2, float v2) {
        this.texture = texture;
        this.u = u;
        this.v = v;
        this.u2 = u2;
        this.v2 = v2;
    }

    public Texture getTexture() {
        return this.texture;
    }

    public float getU() {
        return this.u;
    }

    public float getV() {
        return this.v;
    }

    public float getU2() {
        return this.u2;
    }

    public float getV2() {
        return this.v2;
    }

    public int getYOffset() {
        return this.yOffset;
    }

    public int getXOffset() {
        return this.xOffset;
    }

    public int getXAdvance() {
        return this.xAdvance;
    }
}

