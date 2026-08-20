/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.cutscenes;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.math.MathUtils;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.core.GameCursor;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.cutscenes.NeowEye;
import com.megacrit.cardcrawl.helpers.FontHelper;
import com.megacrit.cardcrawl.helpers.ImageMaster;
import com.megacrit.cardcrawl.helpers.MathHelper;
import com.megacrit.cardcrawl.helpers.controller.CInputActionSet;
import com.megacrit.cardcrawl.helpers.input.InputHelper;
import com.megacrit.cardcrawl.localization.CharacterStrings;
import com.megacrit.cardcrawl.screens.mainMenu.MainMenuScreen;
import com.megacrit.cardcrawl.ui.DialogWord;
import com.megacrit.cardcrawl.ui.SpeechWord;
import java.util.ArrayList;
import java.util.Scanner;

public class NeowNarrationScreen {
    private Color bgColor;
    private Color eyeColor;
    private NeowEye eye1;
    private NeowEye eye2;
    private NeowEye eye3;
    private int currentDialog = 0;
    private int clickCount = 0;
    private static final CharacterStrings charStrings = CardCrawlGame.languagePack.getCharacterString("PostCreditsNeow");
    private static float curLineWidth = 0.0f;
    private static int curLine = 0;
    private static Scanner s;
    private static GlyphLayout gl;
    private static ArrayList<SpeechWord> words;
    private static final float CHAR_SPACING;
    private static final float LINE_SPACING;
    private BitmapFont font = FontHelper.panelNameFont;
    private float x = (float)Settings.WIDTH / 2.0f;
    private float y = (float)Settings.HEIGHT / 2.0f;
    private float wordTimer = 1.0f;
    private boolean textDone = false;
    private boolean fadingOut = false;
    private float fadeOutTimer = 3.0f;

    public void open() {
        this.fadingOut = false;
        this.fadeOutTimer = 3.0f;
        this.playSfx();
        s = new Scanner(NeowNarrationScreen.charStrings.TEXT[0]);
        this.textDone = false;
        this.wordTimer = 1.0f;
        words.clear();
        curLineWidth = 0.0f;
        curLine = 0;
        this.currentDialog = 0;
        this.clickCount = 0;
        this.eye1 = new NeowEye(0);
        this.eye2 = new NeowEye(1);
        this.eye3 = new NeowEye(2);
        this.bgColor = new Color(320149504);
        this.eyeColor = new Color(1.0f, 1.0f, 1.0f, 0.0f);
    }

    public void update() {
        this.eyeColor.a = this.bgColor.a = MathHelper.slowColorLerpSnap(this.bgColor.a, 1.0f);
        this.eye1.update();
        this.eye2.update();
        this.eye3.update();
        this.wordTimer -= Gdx.graphics.getDeltaTime();
        if (this.wordTimer < 0.0f && !this.textDone) {
            this.wordTimer = this.clickCount > 4 ? 0.1f : (this.wordTimer += 0.4f);
            if (Settings.lineBreakViaCharacter) {
                this.addWordCN();
            } else {
                this.addWord();
            }
        }
        for (SpeechWord w : words) {
            w.update();
        }
        if (InputHelper.justClickedLeft || CInputActionSet.select.isJustPressed()) {
            ++this.clickCount;
        }
        if (this.fadingOut) {
            this.fadeOutTimer = this.clickCount > 4 ? (this.fadeOutTimer -= Gdx.graphics.getDeltaTime() * 3.0f) : (this.fadeOutTimer -= Gdx.graphics.getDeltaTime());
            if (this.fadeOutTimer < 0.0f) {
                this.fadeOutTimer = 0.0f;
                this.exit();
                return;
            }
        } else if ((InputHelper.justClickedLeft || CInputActionSet.select.isJustPressed() || this.clickCount > 3) && this.textDone) {
            ++this.currentDialog;
            if (this.currentDialog > 2) {
                this.fadingOut = true;
                return;
            }
            this.playSfx();
            s = new Scanner(NeowNarrationScreen.charStrings.TEXT[this.currentDialog]);
            this.textDone = false;
            this.wordTimer = this.clickCount > 4 ? 0.1f : 0.3f;
            words.clear();
            curLineWidth = 0.0f;
            curLine = 0;
        }
    }

    private void playSfx() {
        int roll = MathUtils.random(3);
        if (roll == 0) {
            CardCrawlGame.sound.playA("VO_NEOW_1A", -0.2f);
            CardCrawlGame.sound.playA("VO_NEOW_1A", -0.4f);
        } else if (roll == 1) {
            CardCrawlGame.sound.playA("VO_NEOW_1B", -0.2f);
            CardCrawlGame.sound.playA("VO_NEOW_1B", -0.4f);
        } else if (roll == 2) {
            CardCrawlGame.sound.playA("VO_NEOW_2A", -0.2f);
            CardCrawlGame.sound.playA("VO_NEOW_2A", -0.4f);
        } else {
            CardCrawlGame.sound.playA("VO_NEOW_2B", -0.2f);
            CardCrawlGame.sound.playA("VO_NEOW_2B", -0.4f);
        }
    }

    private void addWord() {
        if (s.hasNext()) {
            String word = s.next();
            if (word.equals("NL")) {
                ++curLine;
                for (SpeechWord w : words) {
                    w.shiftY(LINE_SPACING);
                }
                curLineWidth = 0.0f;
                return;
            }
            gl.setText(this.font, word);
            float temp = 0.0f;
            if (curLineWidth + NeowNarrationScreen.gl.width > 9999.0f) {
                ++curLine;
                for (SpeechWord w : words) {
                    w.shiftY(LINE_SPACING);
                }
                curLineWidth = NeowNarrationScreen.gl.width + CHAR_SPACING;
                temp = -curLineWidth / 2.0f;
            } else {
                temp = -(curLineWidth += NeowNarrationScreen.gl.width) / 2.0f;
                for (SpeechWord w : words) {
                    if (w.line != curLine) continue;
                    w.setX(this.x + temp);
                    gl.setText(this.font, w.word);
                    temp += NeowNarrationScreen.gl.width + CHAR_SPACING;
                }
                curLineWidth += CHAR_SPACING;
                gl.setText(this.font, word + " ");
            }
            words.add(new SpeechWord(this.font, word, DialogWord.AppearEffect.FADE_IN, DialogWord.WordEffect.SLOW_WAVY, DialogWord.WordColor.WHITE, this.x + temp, this.y - LINE_SPACING * (float)curLine, curLine));
        } else {
            this.textDone = true;
            s.close();
        }
    }

    private void addWordCN() {
        if (s.hasNext()) {
            String word = s.next();
            if (word.equals("NL")) {
                ++curLine;
                for (SpeechWord w : words) {
                    w.shiftY(LINE_SPACING);
                }
                curLineWidth = 0.0f;
                return;
            }
            for (int i = 0; i < word.length(); ++i) {
                String tmp = Character.toString(word.charAt(i));
                gl.setText(this.font, tmp);
                float temp = 0.0f;
                if (curLineWidth + NeowNarrationScreen.gl.width > 9999.0f) {
                    ++curLine;
                    for (SpeechWord w : words) {
                        w.shiftY(LINE_SPACING);
                    }
                    curLineWidth = NeowNarrationScreen.gl.width;
                    temp = -curLineWidth / 2.0f;
                } else {
                    temp = -(curLineWidth += NeowNarrationScreen.gl.width) / 2.0f;
                    for (SpeechWord w : words) {
                        if (w.line != curLine) continue;
                        w.setX(this.x + temp);
                        gl.setText(this.font, w.word);
                        temp += NeowNarrationScreen.gl.width;
                    }
                    gl.setText(this.font, tmp + " ");
                }
                words.add(new SpeechWord(this.font, tmp, DialogWord.AppearEffect.FADE_IN, DialogWord.WordEffect.SLOW_WAVY, DialogWord.WordColor.WHITE, this.x + temp, this.y - LINE_SPACING * (float)curLine, curLine));
            }
        } else {
            this.textDone = true;
            s.close();
        }
    }

    private void exit() {
        GameCursor.hidden = false;
        CardCrawlGame.mainMenuScreen.lighten();
        CardCrawlGame.mainMenuScreen.screen = MainMenuScreen.CurScreen.MAIN_MENU;
        CardCrawlGame.music.changeBGM("MENU");
    }

    public void render(SpriteBatch sb) {
        sb.setColor(this.bgColor);
        sb.draw(ImageMaster.WHITE_SQUARE_IMG, 0.0f, 0.0f, (float)Settings.WIDTH, (float)Settings.HEIGHT);
        sb.setColor(this.eyeColor);
        this.eye1.renderRight(sb);
        this.eye1.renderLeft(sb);
        this.eye2.renderRight(sb);
        this.eye2.renderLeft(sb);
        this.eye3.renderRight(sb);
        this.eye3.renderLeft(sb);
        for (SpeechWord w : words) {
            w.render(sb);
        }
        if (this.fadingOut) {
            sb.setColor(new Color(0.0f, 0.0f, 0.0f, Interpolation.fade.apply(1.0f, 0.0f, this.fadeOutTimer / 3.0f)));
            sb.draw(ImageMaster.WHITE_SQUARE_IMG, 0.0f, 0.0f, (float)Settings.WIDTH, (float)Settings.HEIGHT);
        }
    }

    static {
        gl = new GlyphLayout();
        words = new ArrayList();
        CHAR_SPACING = 8.0f * Settings.scale;
        LINE_SPACING = 38.0f * Settings.scale;
    }
}

