/*
 * Decompiled with CFR 0.152.
 */
package com.badlogic.gdx.tools.bmfont;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.PixmapIO;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.PixmapPacker;
import com.badlogic.gdx.utils.Array;

public class BitmapFontWriter {
    private static OutputFormat format = OutputFormat.Text;

    public static void setOutputFormat(OutputFormat fmt) {
        if (fmt == null) {
            throw new NullPointerException("format cannot be null");
        }
        format = fmt;
    }

    public static OutputFormat getOutputFormat() {
        return format;
    }

    private static String quote(Object params) {
        return BitmapFontWriter.quote(params, false);
    }

    private static String quote(Object params, boolean spaceAfter) {
        if (BitmapFontWriter.getOutputFormat() == OutputFormat.XML) {
            return "\"" + params.toString().trim() + "\"" + (spaceAfter ? " " : "");
        }
        return params.toString();
    }

    public static void writeFont(BitmapFont.BitmapFontData fontData, String[] pageRefs, FileHandle outFntFile, FontInfo info, int scaleW, int scaleH) {
        String charset;
        if (info == null) {
            info = new FontInfo();
            info.face = outFntFile.nameWithoutExtension();
        }
        int lineHeight = (int)fontData.lineHeight;
        int pages = pageRefs.length;
        int packed = 0;
        int base = (int)(fontData.capHeight + (fontData.flipped ? -fontData.ascent : fontData.ascent));
        OutputFormat fmt = BitmapFontWriter.getOutputFormat();
        boolean xml = fmt == OutputFormat.XML;
        StringBuilder buf = new StringBuilder();
        if (xml) {
            buf.append("<font>\n");
        }
        String xmlOpen = xml ? "\t<" : "";
        String xmlCloseSelf = xml ? "/>" : "";
        String xmlTab = xml ? "\t" : "";
        String xmlClose = xml ? ">" : "";
        String xmlQuote = xml ? "\"" : "";
        String alphaChnlParams = xml ? " alphaChnl=\"0\" redChnl=\"0\" greenChnl=\"0\" blueChnl=\"0\"" : " alphaChnl=0 redChnl=0 greenChnl=0 blueChnl=0";
        buf.append(xmlOpen).append("info face=\"").append(info.face == null ? "" : info.face.replaceAll("\"", "'")).append("\" size=").append(BitmapFontWriter.quote(info.size)).append(" bold=").append(BitmapFontWriter.quote(info.bold ? 1 : 0)).append(" italic=").append(BitmapFontWriter.quote(info.italic ? 1 : 0)).append(" charset=\"").append(info.charset == null ? "" : info.charset).append("\" unicode=").append(BitmapFontWriter.quote(info.unicode ? 1 : 0)).append(" stretchH=").append(BitmapFontWriter.quote(info.stretchH)).append(" smooth=").append(BitmapFontWriter.quote(info.smooth ? 1 : 0)).append(" aa=").append(BitmapFontWriter.quote(info.aa)).append(" padding=").append(xmlQuote).append(info.padding.up).append(",").append(info.padding.right).append(",").append(info.padding.down).append(",").append(info.padding.left).append(xmlQuote).append(" spacing=").append(xmlQuote).append(info.spacing.horizontal).append(",").append(info.spacing.vertical).append(xmlQuote).append(xmlCloseSelf).append("\n");
        buf.append(xmlOpen).append("common lineHeight=").append(BitmapFontWriter.quote(lineHeight)).append(" base=").append(BitmapFontWriter.quote(base)).append(" scaleW=").append(BitmapFontWriter.quote(scaleW)).append(" scaleH=").append(BitmapFontWriter.quote(scaleH)).append(" pages=").append(BitmapFontWriter.quote(pages)).append(" packed=").append(BitmapFontWriter.quote(packed)).append(alphaChnlParams).append(xmlCloseSelf).append("\n");
        if (xml) {
            buf.append("\t<pages>\n");
        }
        for (int i = 0; i < pageRefs.length; ++i) {
            buf.append(xmlTab).append(xmlOpen).append("page id=").append(BitmapFontWriter.quote(i)).append(" file=\"").append(pageRefs[i]).append("\"").append(xmlCloseSelf).append("\n");
        }
        if (xml) {
            buf.append("\t</pages>\n");
        }
        Array<BitmapFont.Glyph> glyphs = new Array<BitmapFont.Glyph>(256);
        for (int i = 0; i < fontData.glyphs.length; ++i) {
            if (fontData.glyphs[i] == null) continue;
            for (int j = 0; j < fontData.glyphs[i].length; ++j) {
                if (fontData.glyphs[i][j] == null) continue;
                glyphs.add(fontData.glyphs[i][j]);
            }
        }
        buf.append(xmlOpen).append("chars count=").append(BitmapFontWriter.quote(glyphs.size)).append(xmlClose).append("\n");
        int padLeft = 0;
        int padRight = 0;
        int padTop = 0;
        int padX = 0;
        int padY = 0;
        if (info != null) {
            padTop = info.padding.up;
            padLeft = info.padding.left;
            padRight = info.padding.right;
            padX = padLeft + padRight;
            padY = info.padding.up + info.padding.down;
        }
        for (int i = 0; i < glyphs.size; ++i) {
            BitmapFont.Glyph g = (BitmapFont.Glyph)glyphs.get(i);
            boolean empty = g.width == 0 || g.height == 0;
            buf.append(xmlTab).append(xmlOpen).append("char id=").append(BitmapFontWriter.quote(String.format("%-6s", g.id), true)).append("x=").append(BitmapFontWriter.quote(String.format("%-5s", empty ? 0 : g.srcX - padLeft), true)).append("y=").append(BitmapFontWriter.quote(String.format("%-5s", empty ? 0 : g.srcY - padRight), true)).append("width=").append(BitmapFontWriter.quote(String.format("%-5s", empty ? 0 : g.width + padX), true)).append("height=").append(BitmapFontWriter.quote(String.format("%-5s", empty ? 0 : g.height + padY), true)).append("xoffset=").append(BitmapFontWriter.quote(String.format("%-5s", g.xoffset - padLeft), true)).append("yoffset=").append(BitmapFontWriter.quote(String.format("%-5s", fontData.flipped ? g.yoffset + padTop : -(g.height + (g.yoffset + padTop))), true)).append("xadvance=").append(BitmapFontWriter.quote(String.format("%-5s", g.xadvance), true)).append("page=").append(BitmapFontWriter.quote(String.format("%-5s", g.page), true)).append("chnl=").append(BitmapFontWriter.quote(0, true)).append(xmlCloseSelf).append("\n");
        }
        if (xml) {
            buf.append("\t</chars>\n");
        }
        int kernCount = 0;
        StringBuilder kernBuf = new StringBuilder();
        for (int i = 0; i < glyphs.size; ++i) {
            for (int j = 0; j < glyphs.size; ++j) {
                BitmapFont.Glyph first = (BitmapFont.Glyph)glyphs.get(i);
                BitmapFont.Glyph second = (BitmapFont.Glyph)glyphs.get(j);
                int kern = first.getKerning((char)second.id);
                if (kern == 0) continue;
                ++kernCount;
                kernBuf.append(xmlTab).append(xmlOpen).append("kerning first=").append(BitmapFontWriter.quote(first.id)).append(" second=").append(BitmapFontWriter.quote(second.id)).append(" amount=").append(BitmapFontWriter.quote(kern, true)).append(xmlCloseSelf).append("\n");
            }
        }
        buf.append(xmlOpen).append("kernings count=").append(BitmapFontWriter.quote(kernCount)).append(xmlClose).append("\n");
        buf.append((CharSequence)kernBuf);
        if (xml) {
            buf.append("\t</kernings>\n");
            buf.append("</font>");
        }
        if ((charset = info.charset) != null && charset.length() == 0) {
            charset = null;
        }
        outFntFile.writeString(buf.toString(), false, charset);
    }

    public static void writeFont(BitmapFont.BitmapFontData fontData, Pixmap[] pages, FileHandle outFntFile, FontInfo info) {
        String[] pageRefs = BitmapFontWriter.writePixmaps(pages, outFntFile.parent(), outFntFile.nameWithoutExtension());
        BitmapFontWriter.writeFont(fontData, pageRefs, outFntFile, info, pages[0].getWidth(), pages[0].getHeight());
    }

    public static String[] writePixmaps(Pixmap[] pages, FileHandle outputDir, String fileName) {
        if (pages == null || pages.length == 0) {
            throw new IllegalArgumentException("no pixmaps supplied to BitmapFontWriter.write");
        }
        String[] pageRefs = new String[pages.length];
        for (int i = 0; i < pages.length; ++i) {
            String ref;
            pageRefs[i] = ref = pages.length == 1 ? fileName + ".png" : fileName + "_" + i + ".png";
            PixmapIO.writePNG(outputDir.child(ref), pages[i]);
        }
        return pageRefs;
    }

    public static String[] writePixmaps(Array<PixmapPacker.Page> pages, FileHandle outputDir, String fileName) {
        Pixmap[] pix = new Pixmap[pages.size];
        for (int i = 0; i < pages.size; ++i) {
            pix[i] = pages.get(i).getPixmap();
        }
        return BitmapFontWriter.writePixmaps(pix, outputDir, fileName);
    }

    public static class FontInfo {
        public String face;
        public int size = 12;
        public boolean bold;
        public boolean italic;
        public String charset;
        public boolean unicode = true;
        public int stretchH = 100;
        public boolean smooth = true;
        public int aa = 2;
        public Padding padding = new Padding();
        public Spacing spacing = new Spacing();
        public int outline = 0;

        public FontInfo() {
        }

        public FontInfo(String face, int size) {
            this.face = face;
            this.size = size;
        }
    }

    public static class Spacing {
        public int horizontal;
        public int vertical;
    }

    public static class Padding {
        public int up;
        public int down;
        public int left;
        public int right;

        public Padding() {
        }

        public Padding(int up, int down, int left, int right) {
            this.up = up;
            this.down = down;
            this.left = left;
            this.right = right;
        }
    }

    public static enum OutputFormat {
        Text,
        XML;

    }
}

