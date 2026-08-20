/*
 * Decompiled with CFR 0.152.
 */
package com.badlogic.gdx.scenes.scene2d.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.utils.IntArray;
import com.badlogic.gdx.utils.Pool;
import com.badlogic.gdx.utils.Pools;

public class TextArea
extends TextField {
    IntArray linesBreak;
    private String lastText;
    int cursorLine;
    int firstLineShowing;
    private int linesShowing;
    float moveOffset;
    private float prefRows;

    public TextArea(String text, Skin skin) {
        super(text, skin);
    }

    public TextArea(String text, Skin skin, String styleName) {
        super(text, skin, styleName);
    }

    public TextArea(String text, TextField.TextFieldStyle style) {
        super(text, style);
    }

    @Override
    protected void initialize() {
        super.initialize();
        this.writeEnters = true;
        this.linesBreak = new IntArray();
        this.cursorLine = 0;
        this.firstLineShowing = 0;
        this.moveOffset = -1.0f;
        this.linesShowing = 0;
    }

    @Override
    protected int letterUnderCursor(float x) {
        if (this.linesBreak.size > 0) {
            int i;
            if (this.cursorLine * 2 >= this.linesBreak.size) {
                return this.text.length();
            }
            float[] glyphPositions = this.glyphPositions.items;
            int start = this.linesBreak.items[this.cursorLine * 2];
            x += glyphPositions[start];
            int end = this.linesBreak.items[this.cursorLine * 2 + 1];
            for (i = start; i < end && !(glyphPositions[i] > x); ++i) {
            }
            if (glyphPositions[i] - x <= x - glyphPositions[i - 1]) {
                return i;
            }
            return Math.max(0, i - 1);
        }
        return 0;
    }

    public void setPrefRows(float prefRows) {
        this.prefRows = prefRows;
    }

    @Override
    public float getPrefHeight() {
        if (this.prefRows <= 0.0f) {
            return super.getPrefHeight();
        }
        float prefHeight = this.textHeight * this.prefRows;
        if (this.style.background != null) {
            prefHeight = Math.max(prefHeight + this.style.background.getBottomHeight() + this.style.background.getTopHeight(), this.style.background.getMinHeight());
        }
        return prefHeight;
    }

    public int getLines() {
        return this.linesBreak.size / 2 + (this.newLineAtEnd() ? 1 : 0);
    }

    public boolean newLineAtEnd() {
        return this.text.length() != 0 && (this.text.charAt(this.text.length() - 1) == '\n' || this.text.charAt(this.text.length() - 1) == '\r');
    }

    public void moveCursorLine(int line) {
        if (line < 0) {
            this.cursorLine = 0;
            this.cursor = 0;
            this.moveOffset = -1.0f;
        } else if (line >= this.getLines()) {
            int newLine = this.getLines() - 1;
            this.cursor = this.text.length();
            if (line > this.getLines() || newLine == this.cursorLine) {
                this.moveOffset = -1.0f;
            }
            this.cursorLine = newLine;
        } else if (line != this.cursorLine) {
            if (this.moveOffset < 0.0f) {
                this.moveOffset = this.linesBreak.size <= this.cursorLine * 2 ? 0.0f : this.glyphPositions.get(this.cursor) - this.glyphPositions.get(this.linesBreak.get(this.cursorLine * 2));
            }
            this.cursorLine = line;
            int n = this.cursor = this.cursorLine * 2 >= this.linesBreak.size ? this.text.length() : this.linesBreak.get(this.cursorLine * 2);
            while (this.cursor < this.text.length() && this.cursor <= this.linesBreak.get(this.cursorLine * 2 + 1) - 1 && this.glyphPositions.get(this.cursor) - this.glyphPositions.get(this.linesBreak.get(this.cursorLine * 2)) < this.moveOffset) {
                ++this.cursor;
            }
            this.showCursor();
        }
    }

    void updateCurrentLine() {
        int index = this.calculateCurrentLineIndex(this.cursor);
        int line = index / 2;
        if (!(index % 2 != 0 && index + 1 < this.linesBreak.size && this.cursor == this.linesBreak.items[index] && this.linesBreak.items[index + 1] == this.linesBreak.items[index] || line >= this.linesBreak.size / 2 && this.text.length() != 0 && this.text.charAt(this.text.length() - 1) != '\n' && this.text.charAt(this.text.length() - 1) != '\r')) {
            this.cursorLine = line;
        }
    }

    void showCursor() {
        this.updateCurrentLine();
        if (this.cursorLine != this.firstLineShowing) {
            int step;
            int n = step = this.cursorLine >= this.firstLineShowing ? 1 : -1;
            while (this.firstLineShowing > this.cursorLine || this.firstLineShowing + this.linesShowing - 1 < this.cursorLine) {
                this.firstLineShowing += step;
            }
        }
    }

    private int calculateCurrentLineIndex(int cursor) {
        int index;
        for (index = 0; index < this.linesBreak.size && cursor > this.linesBreak.items[index]; ++index) {
        }
        return index;
    }

    @Override
    protected void sizeChanged() {
        this.lastText = null;
        BitmapFont font = this.style.font;
        Drawable background = this.style.background;
        float availableHeight = this.getHeight() - (background == null ? 0.0f : background.getBottomHeight() + background.getTopHeight());
        this.linesShowing = (int)Math.floor(availableHeight / font.getLineHeight());
    }

    @Override
    protected float getTextY(BitmapFont font, Drawable background) {
        float textY = this.getHeight();
        if (background != null) {
            textY = (int)(textY - background.getTopHeight());
        }
        return textY;
    }

    @Override
    protected void drawSelection(Drawable selection, Batch batch, BitmapFont font, float x, float y) {
        float offsetY = 0.0f;
        int minIndex = Math.min(this.cursor, this.selectionStart);
        int maxIndex = Math.max(this.cursor, this.selectionStart);
        for (int i = this.firstLineShowing * 2; i + 1 < this.linesBreak.size && i < (this.firstLineShowing + this.linesShowing) * 2; i += 2) {
            int lineStart = this.linesBreak.get(i);
            int lineEnd = this.linesBreak.get(i + 1);
            if (!(minIndex < lineStart && minIndex < lineEnd && maxIndex < lineStart && maxIndex < lineEnd || minIndex > lineStart && minIndex > lineEnd && maxIndex > lineStart && maxIndex > lineEnd)) {
                int start = Math.max(this.linesBreak.get(i), minIndex);
                int end = Math.min(this.linesBreak.get(i + 1), maxIndex);
                float selectionX = this.glyphPositions.get(start) - this.glyphPositions.get(this.linesBreak.get(i));
                float selectionWidth = this.glyphPositions.get(end) - this.glyphPositions.get(start);
                selection.draw(batch, x + selectionX + this.fontOffset, y - this.textHeight - font.getDescent() - offsetY, selectionWidth, font.getLineHeight());
            }
            offsetY += font.getLineHeight();
        }
    }

    @Override
    protected void drawText(Batch batch, BitmapFont font, float x, float y) {
        float offsetY = 0.0f;
        for (int i = this.firstLineShowing * 2; i < (this.firstLineShowing + this.linesShowing) * 2 && i < this.linesBreak.size; i += 2) {
            font.draw(batch, this.displayText, x, y + offsetY, this.linesBreak.items[i], this.linesBreak.items[i + 1], 0.0f, 8, false);
            offsetY -= font.getLineHeight();
        }
    }

    @Override
    protected void drawCursor(Drawable cursorPatch, Batch batch, BitmapFont font, float x, float y) {
        float textOffset = this.cursor >= this.glyphPositions.size || this.cursorLine * 2 >= this.linesBreak.size ? 0.0f : this.glyphPositions.get(this.cursor) - this.glyphPositions.get(this.linesBreak.items[this.cursorLine * 2]);
        cursorPatch.draw(batch, x + textOffset + this.fontOffset + font.getData().cursorX, y - font.getDescent() / 2.0f - (float)(this.cursorLine - this.firstLineShowing + 1) * font.getLineHeight(), cursorPatch.getMinWidth(), font.getLineHeight());
    }

    @Override
    protected void calculateOffsets() {
        super.calculateOffsets();
        if (!this.text.equals(this.lastText)) {
            this.lastText = this.text;
            BitmapFont font = this.style.font;
            float maxWidthLine = this.getWidth() - (this.style.background != null ? this.style.background.getLeftWidth() + this.style.background.getRightWidth() : 0.0f);
            this.linesBreak.clear();
            int lineStart = 0;
            int lastSpace = 0;
            Pool<GlyphLayout> layoutPool = Pools.get(GlyphLayout.class);
            GlyphLayout layout = layoutPool.obtain();
            for (int i = 0; i < this.text.length(); ++i) {
                char lastCharacter = this.text.charAt(i);
                if (lastCharacter == '\r' || lastCharacter == '\n') {
                    this.linesBreak.add(lineStart);
                    this.linesBreak.add(i);
                    lineStart = i + 1;
                    continue;
                }
                lastSpace = this.continueCursor(i, 0) ? lastSpace : i;
                layout.setText(font, this.text.subSequence(lineStart, i + 1));
                if (!(layout.width > maxWidthLine)) continue;
                if (lineStart >= lastSpace) {
                    lastSpace = i - 1;
                }
                this.linesBreak.add(lineStart);
                this.linesBreak.add(lastSpace + 1);
                lastSpace = lineStart = lastSpace + 1;
            }
            layoutPool.free(layout);
            if (lineStart < this.text.length()) {
                this.linesBreak.add(lineStart);
                this.linesBreak.add(this.text.length());
            }
            this.showCursor();
        }
    }

    @Override
    protected InputListener createInputListener() {
        return new TextAreaListener();
    }

    @Override
    public void setSelection(int selectionStart, int selectionEnd) {
        super.setSelection(selectionStart, selectionEnd);
        this.updateCurrentLine();
    }

    @Override
    protected void moveCursor(boolean forward, boolean jump) {
        int count = forward ? 1 : -1;
        int index = this.cursorLine * 2 + count;
        if (index >= 0 && index + 1 < this.linesBreak.size && this.linesBreak.items[index] == this.cursor && this.linesBreak.items[index + 1] == this.cursor) {
            this.cursorLine += count;
            if (jump) {
                super.moveCursor(forward, jump);
            }
            this.showCursor();
        } else {
            super.moveCursor(forward, jump);
        }
        this.updateCurrentLine();
    }

    @Override
    protected boolean continueCursor(int index, int offset) {
        int pos = this.calculateCurrentLineIndex(index + offset);
        return super.continueCursor(index, offset) && (pos < 0 || pos >= this.linesBreak.size - 2 || this.linesBreak.items[pos + 1] != index || this.linesBreak.items[pos + 1] == this.linesBreak.items[pos + 2]);
    }

    public int getCursorLine() {
        return this.cursorLine;
    }

    public int getFirstLineShowing() {
        return this.firstLineShowing;
    }

    public int getLinesShowing() {
        return this.linesShowing;
    }

    public float getCursorX() {
        return this.textOffset + this.fontOffset + this.style.font.getData().cursorX;
    }

    public float getCursorY() {
        BitmapFont font = this.style.font;
        return -(-font.getDescent() / 2.0f - (float)(this.cursorLine - this.firstLineShowing + 1) * font.getLineHeight());
    }

    public class TextAreaListener
    extends TextField.TextFieldClickListener {
        public TextAreaListener() {
            super(TextArea.this);
        }

        @Override
        protected void setCursorPosition(float x, float y) {
            TextArea.this.moveOffset = -1.0f;
            Drawable background = TextArea.this.style.background;
            BitmapFont font = TextArea.this.style.font;
            float height = TextArea.this.getHeight();
            if (background != null) {
                height -= background.getTopHeight();
                x -= background.getLeftWidth();
            }
            x = Math.max(0.0f, x);
            if (background != null) {
                y -= background.getTopHeight();
            }
            TextArea.this.cursorLine = (int)Math.floor((height - y) / font.getLineHeight()) + TextArea.this.firstLineShowing;
            TextArea.this.cursorLine = Math.max(0, Math.min(TextArea.this.cursorLine, TextArea.this.getLines() - 1));
            super.setCursorPosition(x, y);
            TextArea.this.updateCurrentLine();
        }

        @Override
        public boolean keyDown(InputEvent event, int keycode) {
            boolean result = super.keyDown(event, keycode);
            Stage stage = TextArea.this.getStage();
            if (stage != null && stage.getKeyboardFocus() == TextArea.this) {
                boolean shift;
                boolean repeat = false;
                boolean bl = shift = Gdx.input.isKeyPressed(59) || Gdx.input.isKeyPressed(60);
                if (keycode == 20) {
                    if (shift) {
                        if (!TextArea.this.hasSelection) {
                            TextArea.this.selectionStart = TextArea.this.cursor;
                            TextArea.this.hasSelection = true;
                        }
                    } else {
                        TextArea.this.clearSelection();
                    }
                    TextArea.this.moveCursorLine(TextArea.this.cursorLine + 1);
                    repeat = true;
                } else if (keycode == 19) {
                    if (shift) {
                        if (!TextArea.this.hasSelection) {
                            TextArea.this.selectionStart = TextArea.this.cursor;
                            TextArea.this.hasSelection = true;
                        }
                    } else {
                        TextArea.this.clearSelection();
                    }
                    TextArea.this.moveCursorLine(TextArea.this.cursorLine - 1);
                    repeat = true;
                } else {
                    TextArea.this.moveOffset = -1.0f;
                }
                if (repeat) {
                    this.scheduleKeyRepeatTask(keycode);
                }
                TextArea.this.showCursor();
                return true;
            }
            return result;
        }

        @Override
        public boolean keyTyped(InputEvent event, char character) {
            boolean result = super.keyTyped(event, character);
            TextArea.this.showCursor();
            return result;
        }

        @Override
        protected void goHome(boolean jump) {
            if (jump) {
                TextArea.this.cursor = 0;
            } else if (TextArea.this.cursorLine * 2 < TextArea.this.linesBreak.size) {
                TextArea.this.cursor = TextArea.this.linesBreak.get(TextArea.this.cursorLine * 2);
            }
        }

        @Override
        protected void goEnd(boolean jump) {
            if (jump || TextArea.this.cursorLine >= TextArea.this.getLines()) {
                TextArea.this.cursor = TextArea.this.text.length();
            } else if (TextArea.this.cursorLine * 2 + 1 < TextArea.this.linesBreak.size) {
                TextArea.this.cursor = TextArea.this.linesBreak.get(TextArea.this.cursorLine * 2 + 1);
            }
        }
    }
}

