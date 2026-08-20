/*
 * Decompiled with CFR 0.152.
 */
package com.badlogic.gdx.tools.hiero.unicodefont;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.tools.hiero.unicodefont.Glyph;
import com.badlogic.gdx.tools.hiero.unicodefont.UnicodeFont;
import com.badlogic.gdx.tools.hiero.unicodefont.effects.ColorEffect;
import com.badlogic.gdx.tools.hiero.unicodefont.effects.Effect;
import com.badlogic.gdx.utils.Array;
import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.font.FontRenderContext;
import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.lwjgl.opengl.GL11;

public class GlyphPage {
    private final UnicodeFont unicodeFont;
    private final int pageWidth;
    private final int pageHeight;
    private final Texture texture;
    private final List<Glyph> pageGlyphs = new ArrayList<Glyph>(32);
    private final List<String> hashes = new ArrayList<String>(32);
    Array<Row> rows = new Array();
    public static final int MAX_GLYPH_SIZE = 256;
    private static ByteBuffer scratchByteBuffer = ByteBuffer.allocateDirect(262144);
    private static IntBuffer scratchIntBuffer;
    private static BufferedImage scratchImage;
    static Graphics2D scratchGraphics;
    public static FontRenderContext renderContext;

    GlyphPage(UnicodeFont unicodeFont, int pageWidth, int pageHeight) {
        this.unicodeFont = unicodeFont;
        this.pageWidth = pageWidth;
        this.pageHeight = pageHeight;
        this.texture = new Texture(pageWidth, pageHeight, Pixmap.Format.RGBA8888);
        this.rows.add(new Row());
    }

    int loadGlyphs(List glyphs, int maxGlyphsToLoad) {
        GL11.glColor4f(1.0f, 1.0f, 1.0f, 1.0f);
        this.texture.bind();
        int loadedCount = 0;
        Iterator iter = glyphs.iterator();
        while (iter.hasNext()) {
            Glyph glyph = (Glyph)iter.next();
            int width = Math.min(256, glyph.getWidth());
            int height = Math.min(256, glyph.getHeight());
            if (width == 0 || height == 0) {
                this.pageGlyphs.add(glyph);
            } else {
                Row bestRow = null;
                int nn = this.rows.size - 1;
                for (int ii = 0; ii < nn; ++ii) {
                    Row row = this.rows.get(ii);
                    if (row.x + width >= this.pageWidth || row.y + height >= this.pageHeight || height > row.height || bestRow != null && row.height >= bestRow.height) continue;
                    bestRow = row;
                }
                if (bestRow == null) {
                    Row row = this.rows.peek();
                    if (row.y + height >= this.pageHeight) continue;
                    if (row.x + width < this.pageWidth) {
                        row.height = Math.max(row.height, height);
                        bestRow = row;
                    } else {
                        bestRow = new Row();
                        bestRow.y = row.y + row.height;
                        bestRow.height = height;
                        this.rows.add(bestRow);
                    }
                }
                if (bestRow == null) continue;
                if (this.renderGlyph(glyph, bestRow.x, bestRow.y, width, height)) {
                    bestRow.x += width;
                }
            }
            iter.remove();
            if (++loadedCount != maxGlyphsToLoad) continue;
            break;
        }
        return loadedCount;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private boolean renderGlyph(Glyph glyph, int pageX, int pageY, int width, int height) {
        int format;
        scratchGraphics.setComposite(AlphaComposite.Clear);
        scratchGraphics.fillRect(0, 0, 256, 256);
        scratchGraphics.setComposite(AlphaComposite.SrcOver);
        ByteBuffer glyphPixels = scratchByteBuffer;
        if (this.unicodeFont.getRenderType() == UnicodeFont.RenderType.FreeType && this.unicodeFont.bitmapFont != null) {
            int i;
            BitmapFont.BitmapFontData data = this.unicodeFont.bitmapFont.getData();
            BitmapFont.Glyph g = data.getGlyph((char)glyph.getCodePoint());
            Pixmap fontPixmap = this.unicodeFont.bitmapFont.getRegions().get(g.page).getTexture().getTextureData().consumePixmap();
            int fontWidth = fontPixmap.getWidth();
            int padTop = this.unicodeFont.getPaddingTop();
            int padBottom = this.unicodeFont.getPaddingBottom();
            int padLeftBytes = this.unicodeFont.getPaddingLeft() * 4;
            int padXBytes = padLeftBytes + this.unicodeFont.getPaddingRight() * 4;
            int glyphRowBytes = width * 4;
            int fontRowBytes = g.width * 4;
            ByteBuffer fontPixels = fontPixmap.getPixels();
            byte[] row = new byte[glyphRowBytes];
            glyphPixels.position(0);
            for (i = 0; i < padTop; ++i) {
                glyphPixels.put(row);
            }
            glyphPixels.position((height - padBottom) * glyphRowBytes);
            for (i = 0; i < padBottom; ++i) {
                glyphPixels.put(row);
            }
            glyphPixels.position(padTop * glyphRowBytes);
            int n = g.height;
            for (int y = 0; y < n; ++y) {
                fontPixels.position(((g.srcY + y) * fontWidth + g.srcX) * 4);
                fontPixels.get(row, padLeftBytes, fontRowBytes);
                glyphPixels.put(row);
            }
            fontPixels.position(0);
            glyphPixels.position(height * glyphRowBytes);
            glyphPixels.flip();
            format = 6408;
        } else {
            if (this.unicodeFont.getRenderType() == UnicodeFont.RenderType.Native) {
                for (Effect effect : this.unicodeFont.getEffects()) {
                    if (!(effect instanceof ColorEffect)) continue;
                    scratchGraphics.setColor(((ColorEffect)effect).getColor());
                }
                scratchGraphics.setColor(Color.white);
                scratchGraphics.setFont(this.unicodeFont.getFont());
                scratchGraphics.drawString("" + (char)glyph.getCodePoint(), 0, this.unicodeFont.getAscent());
            } else if (this.unicodeFont.getRenderType() == UnicodeFont.RenderType.Java) {
                scratchGraphics.setColor(Color.white);
                Iterator iter = this.unicodeFont.getEffects().iterator();
                while (iter.hasNext()) {
                    ((Effect)iter.next()).draw(scratchImage, scratchGraphics, this.unicodeFont, glyph);
                }
                glyph.setShape(null);
            }
            width = Math.min(width, this.texture.getWidth());
            height = Math.min(height, this.texture.getHeight());
            WritableRaster raster = scratchImage.getRaster();
            int[] row = new int[width];
            for (int y = 0; y < height; ++y) {
                raster.getDataElements(0, y, width, 1, row);
                scratchIntBuffer.put(row);
            }
            format = 32993;
        }
        String hash = "";
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(glyphPixels);
            BigInteger bigInt = new BigInteger(1, md.digest());
            hash = bigInt.toString(16);
        }
        catch (NoSuchAlgorithmException md) {
            // empty catch block
        }
        scratchByteBuffer.clear();
        scratchIntBuffer.clear();
        try {
            int n = this.hashes.size();
            for (int i = 0; i < n; ++i) {
                String other = this.hashes.get(i);
                if (!other.equals(hash)) continue;
                Glyph dupe = this.pageGlyphs.get(i);
                glyph.setTexture(dupe.texture, dupe.u, dupe.v, dupe.u2, dupe.v2);
                boolean bl = false;
                return bl;
            }
        }
        finally {
            this.hashes.add(hash);
            this.pageGlyphs.add(glyph);
        }
        Gdx.gl.glTexSubImage2D(this.texture.glTarget, 0, pageX, pageY, width, height, format, 5121, glyphPixels);
        float u = (float)pageX / (float)this.texture.getWidth();
        float v = (float)pageY / (float)this.texture.getHeight();
        float u2 = (float)(pageX + width) / (float)this.texture.getWidth();
        float v2 = (float)(pageY + height) / (float)this.texture.getHeight();
        glyph.setTexture(this.texture, u, v, u2, v2);
        return true;
    }

    public List<Glyph> getGlyphs() {
        return this.pageGlyphs;
    }

    public Texture getTexture() {
        return this.texture;
    }

    static {
        scratchByteBuffer.order(ByteOrder.LITTLE_ENDIAN);
        scratchIntBuffer = scratchByteBuffer.asIntBuffer();
        scratchImage = new BufferedImage(256, 256, 2);
        scratchGraphics = (Graphics2D)scratchImage.getGraphics();
        scratchGraphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        scratchGraphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        renderContext = scratchGraphics.getFontRenderContext();
    }

    static class Row {
        int x;
        int y;
        int height;

        Row() {
        }
    }
}

