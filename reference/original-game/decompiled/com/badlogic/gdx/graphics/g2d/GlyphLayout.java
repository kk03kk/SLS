/*
 * Decompiled with CFR 0.152.
 */
package com.badlogic.gdx.graphics.g2d;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Colors;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.FloatArray;
import com.badlogic.gdx.utils.Pool;
import com.badlogic.gdx.utils.Pools;

public class GlyphLayout
implements Pool.Poolable {
    public final Array<GlyphRun> runs = new Array();
    public float width;
    public float height;
    private final Array<Color> colorStack = new Array(4);

    public GlyphLayout() {
    }

    public GlyphLayout(BitmapFont font, CharSequence str) {
        this.setText(font, str);
    }

    public GlyphLayout(BitmapFont font, CharSequence str, Color color, float targetWidth, int halign, boolean wrap) {
        this.setText(font, str, color, targetWidth, halign, wrap);
    }

    public GlyphLayout(BitmapFont font, CharSequence str, int start, int end, Color color, float targetWidth, int halign, boolean wrap, String truncate) {
        this.setText(font, str, start, end, color, targetWidth, halign, wrap, truncate);
    }

    public void setText(BitmapFont font, CharSequence str) {
        this.setText(font, str, 0, str.length(), font.getColor(), 0.0f, 8, false, null);
    }

    public void setText(BitmapFont font, CharSequence str, Color color, float targetWidth, int halign, boolean wrap) {
        this.setText(font, str, 0, str.length(), color, targetWidth, halign, wrap, null);
    }

    public void setText(BitmapFont font, CharSequence str, int start, int end, Color color, float targetWidth, int halign, boolean wrap, String truncate) {
        int i;
        if (truncate != null) {
            wrap = true;
        } else if (targetWidth <= font.data.spaceWidth) {
            wrap = false;
        }
        BitmapFont.BitmapFontData fontData = font.data;
        boolean markupEnabled = fontData.markupEnabled;
        Pool<GlyphRun> glyphRunPool = Pools.get(GlyphRun.class);
        Array<GlyphRun> runs = this.runs;
        glyphRunPool.freeAll(runs);
        runs.clear();
        float x = 0.0f;
        float y = 0.0f;
        float width = 0.0f;
        int lines = 0;
        int blankLines = 0;
        Array<Color> colorStack = this.colorStack;
        Color nextColor = color;
        colorStack.add(color);
        Pool<Color> colorPool = Pools.get(Color.class);
        int runStart = start;
        block4: while (true) {
            int runEnd = -1;
            boolean newline = false;
            boolean colorRun = false;
            if (start == end) {
                if (runStart == end) break;
                runEnd = end;
            } else {
                switch (str.charAt(start++)) {
                    case '\n': {
                        runEnd = start - 1;
                        newline = true;
                        break;
                    }
                    case '[': {
                        if (!markupEnabled) break;
                        int length = this.parseColorMarkup(str, start, end, colorPool);
                        if (length >= 0) {
                            runEnd = start - 1;
                            start += length + 1;
                            nextColor = colorStack.peek();
                            colorRun = true;
                            break;
                        }
                        if (length != -2) break;
                        ++start;
                        continue block4;
                    }
                }
            }
            if (runEnd == -1) continue;
            if (runEnd != runStart) {
                GlyphRun run = glyphRunPool.obtain();
                run.color.set(color);
                run.x = x;
                run.y = y;
                fontData.getGlyphs(run, str, runStart, runEnd, colorRun);
                if (run.glyphs.size == 0) {
                    glyphRunPool.free(run);
                } else {
                    runs.add(run);
                    float[] xAdvances = run.xAdvances.items;
                    int n = run.xAdvances.size;
                    for (i = 0; i < n; ++i) {
                        float xAdvance = xAdvances[i];
                        x += xAdvance;
                        if (wrap && x > targetWidth && i > 1 && x - xAdvance + (float)(run.glyphs.get((int)(i - 1)).xoffset + run.glyphs.get((int)(i - 1)).width) * fontData.scaleX - 1.0E-4f > targetWidth) {
                            GlyphRun next;
                            if (truncate != null) {
                                this.truncate(fontData, run, targetWidth, truncate, i, glyphRunPool);
                                x = run.x + run.width;
                                break block4;
                            }
                            int wrapIndex = fontData.getWrapIndex(run.glyphs, i);
                            if (run.x == 0.0f && wrapIndex == 0 || wrapIndex >= run.glyphs.size) {
                                wrapIndex = i - 1;
                            }
                            if (wrapIndex == 0) {
                                next = run;
                            } else {
                                next = this.wrap(fontData, run, glyphRunPool, wrapIndex, i);
                                runs.add(next);
                            }
                            width = Math.max(width, run.x + run.width);
                            x = 0.0f;
                            ++lines;
                            next.x = 0.0f;
                            next.y = y += fontData.down;
                            i = -1;
                            n = next.xAdvances.size;
                            xAdvances = next.xAdvances.items;
                            run = next;
                            continue;
                        }
                        run.width += xAdvance;
                    }
                }
            }
            if (newline) {
                width = Math.max(width, x);
                x = 0.0f;
                float down = fontData.down;
                if (runEnd == runStart) {
                    down *= fontData.blankLineScale;
                    ++blankLines;
                } else {
                    ++lines;
                }
                y += down;
            }
            runStart = start;
            color = nextColor;
        }
        width = Math.max(width, x);
        int n = colorStack.size;
        for (int i2 = 1; i2 < n; ++i2) {
            colorPool.free(colorStack.get(i2));
        }
        colorStack.clear();
        if ((halign & 8) == 0) {
            boolean center = (halign & 1) != 0;
            float lineWidth = 0.0f;
            float lineY = -2.1474836E9f;
            int lineStart = 0;
            int n2 = runs.size;
            for (i = 0; i < n2; ++i) {
                GlyphRun run = runs.get(i);
                if (run.y != lineY) {
                    lineY = run.y;
                    float shift = targetWidth - lineWidth;
                    if (center) {
                        shift /= 2.0f;
                    }
                    while (lineStart < i) {
                        runs.get((int)lineStart++).x += shift;
                    }
                    lineWidth = 0.0f;
                }
                lineWidth += run.width;
            }
            float shift = targetWidth - lineWidth;
            if (center) {
                shift /= 2.0f;
            }
            while (lineStart < n2) {
                runs.get((int)lineStart++).x += shift;
            }
        }
        this.width = width;
        this.height = fontData.capHeight + (float)lines * fontData.lineHeight + (float)blankLines * fontData.lineHeight * fontData.blankLineScale;
    }

    private void truncate(BitmapFont.BitmapFontData fontData, GlyphRun run, float targetWidth, String truncate, int widthIndex, Pool<GlyphRun> glyphRunPool) {
        int count;
        GlyphRun truncateRun = glyphRunPool.obtain();
        fontData.getGlyphs(truncateRun, truncate, 0, truncate.length(), true);
        float truncateWidth = 0.0f;
        int n = truncateRun.xAdvances.size;
        for (int i = 1; i < n; ++i) {
            truncateWidth += truncateRun.xAdvances.get(i);
        }
        targetWidth -= truncateWidth;
        float width = run.x;
        for (count = 0; count < run.xAdvances.size; ++count) {
            float xAdvance = run.xAdvances.get(count);
            if (!((width += xAdvance) > targetWidth)) continue;
            run.width = width - run.x - xAdvance;
            break;
        }
        if (count > 1) {
            run.glyphs.truncate(count - 1);
            run.xAdvances.truncate(count);
            this.adjustLastGlyph(fontData, run);
            if (truncateRun.xAdvances.size > 0) {
                run.xAdvances.addAll(truncateRun.xAdvances, 1, truncateRun.xAdvances.size - 1);
            }
        } else {
            run.glyphs.clear();
            run.xAdvances.clear();
            run.xAdvances.addAll(truncateRun.xAdvances);
            if (truncateRun.xAdvances.size > 0) {
                run.width += truncateRun.xAdvances.get(0);
            }
        }
        run.glyphs.addAll(truncateRun.glyphs);
        run.width += truncateWidth;
        glyphRunPool.free(truncateRun);
    }

    private GlyphRun wrap(BitmapFont.BitmapFontData fontData, GlyphRun first, Pool<GlyphRun> glyphRunPool, int wrapIndex, int widthIndex) {
        GlyphRun second = glyphRunPool.obtain();
        second.color.set(first.color);
        int glyphCount = first.glyphs.size;
        while (widthIndex < wrapIndex) {
            first.width += first.xAdvances.get(widthIndex++);
        }
        while (widthIndex > wrapIndex + 1) {
            first.width -= first.xAdvances.get(--widthIndex);
        }
        if (wrapIndex < glyphCount) {
            Array<BitmapFont.Glyph> glyphs1 = second.glyphs;
            Array<BitmapFont.Glyph> glyphs2 = first.glyphs;
            glyphs1.addAll(glyphs2, 0, wrapIndex);
            glyphs2.removeRange(0, wrapIndex - 1);
            first.glyphs = glyphs1;
            second.glyphs = glyphs2;
            FloatArray xAdvances1 = second.xAdvances;
            FloatArray xAdvances2 = first.xAdvances;
            xAdvances1.addAll(xAdvances2, 0, wrapIndex + 1);
            xAdvances2.removeRange(1, wrapIndex);
            xAdvances2.set(0, (float)(-glyphs2.first().xoffset) * fontData.scaleX - fontData.padLeft);
            first.xAdvances = xAdvances1;
            second.xAdvances = xAdvances2;
        }
        if (wrapIndex == 0) {
            glyphRunPool.free(first);
            this.runs.pop();
        } else {
            this.adjustLastGlyph(fontData, first);
        }
        return second;
    }

    private void adjustLastGlyph(BitmapFont.BitmapFontData fontData, GlyphRun run) {
        BitmapFont.Glyph last = run.glyphs.peek();
        if (fontData.isWhitespace((char)last.id)) {
            return;
        }
        float width = (float)(last.xoffset + last.width) * fontData.scaleX - fontData.padRight;
        run.width += width - run.xAdvances.peek();
        run.xAdvances.set(run.xAdvances.size - 1, width);
    }

    private int parseColorMarkup(CharSequence str, int start, int end, Pool<Color> colorPool) {
        if (start == end) {
            return -1;
        }
        switch (str.charAt(start)) {
            case '#': {
                int colorInt = 0;
                for (int i = start + 1; i < end; ++i) {
                    char ch = str.charAt(i);
                    if (ch == ']') {
                        if (i < start + 2 || i > start + 9) break;
                        if (i - start <= 7) {
                            int nn = 9 - (i - start);
                            for (int ii = 0; ii < nn; ++ii) {
                                colorInt <<= 4;
                            }
                            colorInt |= 0xFF;
                        }
                        Color color = colorPool.obtain();
                        this.colorStack.add(color);
                        Color.rgba8888ToColor(color, colorInt);
                        return i - start;
                    }
                    if (ch >= '0' && ch <= '9') {
                        colorInt = colorInt * 16 + (ch - 48);
                        continue;
                    }
                    if (ch >= 'a' && ch <= 'f') {
                        colorInt = colorInt * 16 + (ch - 87);
                        continue;
                    }
                    if (ch < 'A' || ch > 'F') break;
                    colorInt = colorInt * 16 + (ch - 55);
                }
                return -1;
            }
            case '[': {
                return -2;
            }
            case ']': {
                if (this.colorStack.size > 1) {
                    colorPool.free(this.colorStack.pop());
                }
                return 0;
            }
        }
        int colorStart = start;
        for (int i = start + 1; i < end; ++i) {
            char ch = str.charAt(i);
            if (ch != ']') continue;
            Color namedColor = Colors.get(str.subSequence(colorStart, i).toString());
            if (namedColor == null) {
                return -1;
            }
            Color color = colorPool.obtain();
            this.colorStack.add(color);
            color.set(namedColor);
            return i - start;
        }
        return -1;
    }

    @Override
    public void reset() {
        Pools.get(GlyphRun.class).freeAll(this.runs);
        this.runs.clear();
        this.width = 0.0f;
        this.height = 0.0f;
    }

    public String toString() {
        if (this.runs.size == 0) {
            return "";
        }
        StringBuilder buffer = new StringBuilder(128);
        buffer.append(this.width);
        buffer.append('x');
        buffer.append(this.height);
        buffer.append('\n');
        int n = this.runs.size;
        for (int i = 0; i < n; ++i) {
            buffer.append(this.runs.get(i).toString());
            buffer.append('\n');
        }
        buffer.setLength(buffer.length() - 1);
        return buffer.toString();
    }

    public static class GlyphRun
    implements Pool.Poolable {
        public Array<BitmapFont.Glyph> glyphs = new Array();
        public FloatArray xAdvances = new FloatArray();
        public float x;
        public float y;
        public float width;
        public final Color color = new Color();

        @Override
        public void reset() {
            this.glyphs.clear();
            this.xAdvances.clear();
            this.width = 0.0f;
        }

        public String toString() {
            StringBuilder buffer = new StringBuilder(this.glyphs.size);
            Array<BitmapFont.Glyph> glyphs = this.glyphs;
            int n = glyphs.size;
            for (int i = 0; i < n; ++i) {
                BitmapFont.Glyph g = glyphs.get(i);
                buffer.append((char)g.id);
            }
            buffer.append(", #");
            buffer.append(this.color);
            buffer.append(", ");
            buffer.append(this.x);
            buffer.append(", ");
            buffer.append(this.y);
            buffer.append(", ");
            buffer.append(this.width);
            return buffer.toString();
        }
    }
}

