/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.vfx;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.helpers.FontHelper;
import com.megacrit.cardcrawl.ui.DialogWord;
import com.megacrit.cardcrawl.ui.SpeechWord;
import com.megacrit.cardcrawl.vfx.AbstractGameEffect;
import java.util.ArrayList;
import java.util.Scanner;

public class MegaDialogTextEffect
extends AbstractGameEffect {
    private static GlyphLayout gl;
    private BitmapFont font;
    private DialogWord.AppearEffect a_effect;
    private static final float DEFAULT_WIDTH;
    private static final float LINE_SPACING;
    private static final float CHAR_SPACING;
    private static final float WORD_TIME = 0.03f;
    private float wordTimer = 0.0f;
    private boolean textDone = false;
    private float x;
    private float y;
    private ArrayList<SpeechWord> words = new ArrayList();
    private int curLine = 0;
    private Scanner s;
    private float curLineWidth = 0.0f;
    private static final float FADE_TIME = 0.3f;

    public MegaDialogTextEffect(float x, float y, float duration, String msg, DialogWord.AppearEffect a_effect) {
        if (gl == null) {
            gl = new GlyphLayout();
        }
        this.duration = duration;
        this.x = x;
        this.y = y;
        this.font = FontHelper.panelNameFont;
        this.a_effect = a_effect;
        this.s = new Scanner(msg);
    }

    @Override
    public void update() {
        this.wordTimer -= Gdx.graphics.getDeltaTime();
        if (this.wordTimer < 0.0f && !this.textDone) {
            this.wordTimer = 0.03f;
            if (Settings.lineBreakViaCharacter) {
                this.addWordCN();
            } else {
                this.addWord();
            }
        }
        for (SpeechWord w : this.words) {
            w.update();
        }
        this.duration -= Gdx.graphics.getDeltaTime();
        if (this.duration < 0.0f) {
            this.words.clear();
            this.isDone = true;
        }
        if (this.duration < 0.3f) {
            for (SpeechWord w : this.words) {
                w.fadeOut();
            }
        }
    }

    private void addWord() {
        this.wordTimer = 0.03f;
        if (this.s.hasNext()) {
            DialogWord.WordEffect effect;
            String word = this.s.next();
            if (word.equals("NL")) {
                ++this.curLine;
                for (SpeechWord w : this.words) {
                    w.shiftY(LINE_SPACING);
                }
                this.curLineWidth = 0.0f;
                return;
            }
            DialogWord.WordColor color = SpeechWord.identifyWordColor(word);
            if (color != DialogWord.WordColor.DEFAULT) {
                word = word.substring(2, word.length());
            }
            if ((effect = SpeechWord.identifyWordEffect(word)) != DialogWord.WordEffect.NONE) {
                word = word.substring(1, word.length() - 1);
            }
            gl.setText(this.font, word);
            float temp = 0.0f;
            if (this.curLineWidth + MegaDialogTextEffect.gl.width > DEFAULT_WIDTH) {
                ++this.curLine;
                for (SpeechWord w : this.words) {
                    w.shiftY(LINE_SPACING);
                }
                this.curLineWidth = MegaDialogTextEffect.gl.width + CHAR_SPACING;
                temp = -this.curLineWidth / 2.0f;
            } else {
                this.curLineWidth += MegaDialogTextEffect.gl.width;
                temp = -this.curLineWidth / 2.0f;
                for (SpeechWord w : this.words) {
                    if (w.line != this.curLine) continue;
                    w.setX(this.x + temp);
                    gl.setText(this.font, w.word);
                    temp += MegaDialogTextEffect.gl.width + CHAR_SPACING;
                }
                this.curLineWidth += CHAR_SPACING;
                gl.setText(this.font, word + " ");
            }
            this.words.add(new SpeechWord(this.font, word, this.a_effect, effect, color, this.x + temp, this.y - LINE_SPACING * (float)this.curLine, this.curLine));
        } else {
            this.textDone = true;
            this.s.close();
        }
    }

    private void addWordCN() {
        this.wordTimer = 0.03f;
        if (this.s.hasNext()) {
            DialogWord.WordEffect effect;
            String word = this.s.next();
            if (word.equals("NL")) {
                ++this.curLine;
                for (SpeechWord w : this.words) {
                    w.shiftY(LINE_SPACING);
                }
                this.curLineWidth = 0.0f;
                return;
            }
            DialogWord.WordColor color = SpeechWord.identifyWordColor(word);
            if (color != DialogWord.WordColor.DEFAULT) {
                word = word.substring(2, word.length());
            }
            if ((effect = SpeechWord.identifyWordEffect(word)) != DialogWord.WordEffect.NONE) {
                word = word.substring(1, word.length() - 1);
            }
            for (int i = 0; i < word.length(); ++i) {
                String tmp = Character.toString(word.charAt(i));
                gl.setText(this.font, tmp);
                float temp = 0.0f;
                if (this.curLineWidth + MegaDialogTextEffect.gl.width > DEFAULT_WIDTH) {
                    ++this.curLine;
                    for (SpeechWord w : this.words) {
                        w.shiftY(LINE_SPACING);
                    }
                    this.curLineWidth = MegaDialogTextEffect.gl.width;
                    temp = -this.curLineWidth / 2.0f;
                } else {
                    this.curLineWidth += MegaDialogTextEffect.gl.width;
                    temp = -this.curLineWidth / 2.0f;
                    for (SpeechWord w : this.words) {
                        if (w.line != this.curLine) continue;
                        w.setX(this.x + temp);
                        gl.setText(this.font, w.word);
                        temp += MegaDialogTextEffect.gl.width;
                    }
                    gl.setText(this.font, tmp + " ");
                }
                this.words.add(new SpeechWord(this.font, tmp, this.a_effect, effect, color, this.x + temp, this.y - LINE_SPACING * (float)this.curLine, this.curLine));
            }
        } else {
            this.textDone = true;
            this.s.close();
        }
    }

    @Override
    public void render(SpriteBatch sb) {
        for (SpeechWord w : this.words) {
            w.render(sb);
        }
    }

    @Override
    public void dispose() {
    }

    static {
        DEFAULT_WIDTH = 320.0f * Settings.scale;
        LINE_SPACING = Settings.isMobile ? 33.0f * Settings.scale : 30.0f * Settings.scale;
        CHAR_SPACING = 8.0f * Settings.scale;
    }
}

