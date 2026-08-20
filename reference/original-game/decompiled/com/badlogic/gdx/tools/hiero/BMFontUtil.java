/*
 * Decompiled with CFR 0.152.
 */
package com.badlogic.gdx.tools.hiero;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.tools.hiero.Kerning;
import com.badlogic.gdx.tools.hiero.unicodefont.Glyph;
import com.badlogic.gdx.tools.hiero.unicodefont.GlyphPage;
import com.badlogic.gdx.tools.hiero.unicodefont.UnicodeFont;
import com.badlogic.gdx.utils.IntIntMap;
import java.awt.Font;
import java.awt.Rectangle;
import java.awt.font.GlyphVector;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.awt.image.WritableRaster;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import javax.imageio.ImageIO;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;

public class BMFontUtil {
    private final UnicodeFont unicodeFont;

    public BMFontUtil(UnicodeFont unicodeFont) {
        this.unicodeFont = unicodeFont;
    }

    public void save(File outputBMFontFile) throws IOException {
        File outputDir = outputBMFontFile.getParentFile();
        String outputName = outputBMFontFile.getName();
        if (outputName.endsWith(".fnt")) {
            outputName = outputName.substring(0, outputName.length() - 4);
        }
        this.getGlyph(' ');
        this.getGlyph('\u0000');
        this.unicodeFont.loadGlyphs();
        PrintStream out = new PrintStream(new FileOutputStream(new File(outputDir, outputName + ".fnt")));
        Font font = this.unicodeFont.getFont();
        int pageWidth = this.unicodeFont.getGlyphPageWidth();
        int pageHeight = this.unicodeFont.getGlyphPageHeight();
        out.println("info face=\"" + font.getFontName() + "\" size=" + font.getSize() + " bold=" + (font.isBold() ? 1 : 0) + " italic=" + (font.isItalic() ? 1 : 0) + " charset=\"\" unicode=0 stretchH=100 smooth=1 aa=1 padding=" + this.unicodeFont.getPaddingTop() + "," + this.unicodeFont.getPaddingRight() + "," + this.unicodeFont.getPaddingBottom() + "," + this.unicodeFont.getPaddingLeft() + " spacing=" + this.unicodeFont.getPaddingAdvanceX() + "," + this.unicodeFont.getPaddingAdvanceY());
        out.println("common lineHeight=" + this.unicodeFont.getLineHeight() + " base=" + this.unicodeFont.getAscent() + " scaleW=" + pageWidth + " scaleH=" + pageHeight + " pages=" + this.unicodeFont.getGlyphPages().size() + " packed=0");
        int pageIndex = 0;
        int glyphCount = 0;
        Iterator pageIter = this.unicodeFont.getGlyphPages().iterator();
        while (pageIter.hasNext()) {
            GlyphPage page = (GlyphPage)pageIter.next();
            String fileName = pageIndex == 0 && !pageIter.hasNext() ? outputName + ".png" : outputName + (pageIndex + 1) + ".png";
            out.println("page id=" + pageIndex + " file=\"" + fileName + "\"");
            glyphCount += page.getGlyphs().size();
            ++pageIndex;
        }
        out.println("chars count=" + glyphCount);
        pageIndex = 0;
        ArrayList<Glyph> allGlyphs = new ArrayList<Glyph>(512);
        for (GlyphPage page : this.unicodeFont.getGlyphPages()) {
            List<Glyph> glyphs = page.getGlyphs();
            Collections.sort(glyphs, new Comparator<Glyph>(){

                @Override
                public int compare(Glyph o1, Glyph o2) {
                    return o1.getCodePoint() - o2.getCodePoint();
                }
            });
            for (Glyph glyph : page.getGlyphs()) {
                this.writeGlyph(out, pageWidth, pageHeight, pageIndex, glyph);
            }
            allGlyphs.addAll(page.getGlyphs());
            ++pageIndex;
        }
        String ttfFileRef = this.unicodeFont.getFontFile();
        if (ttfFileRef == null) {
            System.out.println("Kerning information could not be output because a TTF font file was not specified.");
        } else {
            Kerning kerning = new Kerning();
            try {
                kerning.load(Gdx.files.internal(ttfFileRef).read(), font.getSize());
            }
            catch (IOException ex) {
                System.out.println("Unable to read kerning information from font: " + ttfFileRef);
                ex.printStackTrace();
            }
            IntIntMap glyphCodeToCodePoint = new IntIntMap();
            for (Glyph glyph : allGlyphs) {
                glyphCodeToCodePoint.put(new Integer(this.getGlyphCode(font, glyph.getCodePoint())), new Integer(glyph.getCodePoint()));
            }
            class KerningPair {
                public int firstCodePoint;
                public int secondCodePoint;
                public int offset;

                KerningPair() {
                }
            }
            ArrayList<KerningPair> kernings = new ArrayList<KerningPair>(256);
            for (IntIntMap.Entry entry : kerning.getKernings()) {
                int firstGlyphCode = entry.key >> 16;
                int secondGlyphCode = entry.key & 0xFFFF;
                int offset = entry.value;
                int firstCodePoint = glyphCodeToCodePoint.get(firstGlyphCode, -1);
                int secondCodePoint = glyphCodeToCodePoint.get(secondGlyphCode, -1);
                if (firstCodePoint == -1 || secondCodePoint == -1 || offset == 0) continue;
                KerningPair pair = new KerningPair();
                pair.firstCodePoint = firstCodePoint;
                pair.secondCodePoint = secondCodePoint;
                pair.offset = offset;
                kernings.add(pair);
            }
            out.println("kernings count=" + kernings.size());
            for (KerningPair pair : kernings) {
                out.println("kerning first=" + pair.firstCodePoint + " second=" + pair.secondCodePoint + " amount=" + pair.offset);
            }
        }
        out.close();
        int width = this.unicodeFont.getGlyphPageWidth();
        int height = this.unicodeFont.getGlyphPageHeight();
        IntBuffer buffer = BufferUtils.createIntBuffer(width * height);
        BufferedImage bufferedImage = new BufferedImage(width, height, 2);
        int[] row = new int[width];
        pageIndex = 0;
        Iterator pageIter2 = this.unicodeFont.getGlyphPages().iterator();
        while (pageIter2.hasNext()) {
            GlyphPage page = (GlyphPage)pageIter2.next();
            String fileName = pageIndex == 0 && !pageIter2.hasNext() ? outputName + ".png" : outputName + (pageIndex + 1) + ".png";
            page.getTexture().bind();
            buffer.clear();
            GL11.glGetTexImage(3553, 0, 32993, 5121, buffer);
            WritableRaster raster = bufferedImage.getRaster();
            for (int y = 0; y < height; ++y) {
                buffer.get(row);
                raster.setDataElements(0, y, width, 1, row);
            }
            File imageOutputFile = new File(outputDir, fileName);
            ImageIO.write((RenderedImage)bufferedImage, "png", imageOutputFile);
            ++pageIndex;
        }
    }

    private Glyph getGlyph(char c) {
        char[] chars = new char[]{c};
        GlyphVector vector = this.unicodeFont.getFont().layoutGlyphVector(GlyphPage.renderContext, chars, 0, chars.length, 0);
        Rectangle bounds = vector.getGlyphPixelBounds(0, GlyphPage.renderContext, 0.0f, 0.0f);
        return this.unicodeFont.getGlyph(vector.getGlyphCode(0), c, bounds, vector, 0);
    }

    void writeGlyph(PrintStream out, int pageWidth, int pageHeight, int pageIndex, Glyph glyph) {
        out.println("char id=" + String.format("%-7s ", glyph.getCodePoint()) + "x=" + String.format("%-5s", (int)(glyph.getU() * (float)pageWidth)) + "y=" + String.format("%-5s", (int)(glyph.getV() * (float)pageHeight)) + "width=" + String.format("%-5s", glyph.getWidth()) + "height=" + String.format("%-5s", glyph.getHeight()) + "xoffset=" + String.format("%-5s", glyph.getXOffset()) + "yoffset=" + String.format("%-5s", glyph.getYOffset()) + "xadvance=" + String.format("%-5s", glyph.getXAdvance()) + "page=" + String.format("%-5s", pageIndex) + "chnl=0 ");
    }

    private int getGlyphCode(Font font, int codePoint) {
        char[] chars = Character.toChars(codePoint);
        GlyphVector vector = font.layoutGlyphVector(GlyphPage.renderContext, chars, 0, chars.length, 0);
        return vector.getGlyphCode(0);
    }
}

