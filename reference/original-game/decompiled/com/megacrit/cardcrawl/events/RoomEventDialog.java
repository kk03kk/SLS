/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.events;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.helpers.FontHelper;
import com.megacrit.cardcrawl.relics.AbstractRelic;
import com.megacrit.cardcrawl.ui.DialogWord;
import com.megacrit.cardcrawl.ui.buttons.LargeDialogOptionButton;
import java.util.ArrayList;
import java.util.Scanner;

public class RoomEventDialog {
    private Color color = new Color(0.0f, 0.0f, 0.0f, 0.0f);
    private Color targetColor = new Color(0.0f, 0.0f, 0.0f, 0.0f);
    private static final Color PANEL_COLOR = new Color(0.0f, 0.0f, 0.0f, 0.5f);
    private static final float COLOR_FADE_SPEED = 8.0f;
    private boolean isMoving = false;
    private float animateTimer = 0.0f;
    private static final float ANIM_SPEED = 0.5f;
    private boolean show = false;
    private float curLineWidth = 0.0f;
    private int curLine = 0;
    private DialogWord.AppearEffect a_effect;
    private Scanner s;
    private GlyphLayout gl = new GlyphLayout();
    private ArrayList<DialogWord> words = new ArrayList();
    private boolean textDone = true;
    private float wordTimer = 0.0f;
    private static final float WORD_TIME = 0.02f;
    private static final float CHAR_SPACING = 8.0f * Settings.scale;
    private static final float LINE_SPACING = 34.0f * Settings.scale;
    private static final float DIALOG_MSG_X = (float)Settings.WIDTH * 0.1f;
    private static final float DIALOG_MSG_Y = 250.0f * Settings.scale;
    private static final float DIALOG_MSG_W = (float)Settings.WIDTH * 0.8f;
    public static ArrayList<LargeDialogOptionButton> optionList = new ArrayList();
    public static int selectedOption = -1;
    public static boolean waitForInput = true;

    public void update() {
        if (this.isMoving) {
            this.animateTimer -= Gdx.graphics.getDeltaTime();
            if (this.animateTimer < 0.0f) {
                this.animateTimer = 0.0f;
                this.isMoving = false;
            }
        }
        this.color = this.color.lerp(this.targetColor, Gdx.graphics.getDeltaTime() * 8.0f);
        if (this.show) {
            for (int i = 0; i < optionList.size(); ++i) {
                optionList.get(i).update(optionList.size());
                if (!RoomEventDialog.optionList.get((int)i).pressed || !waitForInput) continue;
                selectedOption = i;
                RoomEventDialog.optionList.get((int)i).pressed = false;
                waitForInput = false;
            }
        }
        if (Settings.lineBreakViaCharacter) {
            this.bodyTextEffectCN();
        } else {
            this.bodyTextEffect();
        }
        for (DialogWord w : this.words) {
            w.update();
        }
    }

    public int getSelectedOption() {
        waitForInput = true;
        return selectedOption;
    }

    public void clear() {
        optionList.clear();
        this.words.clear();
        waitForInput = true;
    }

    public void show() {
        this.targetColor = PANEL_COLOR;
        this.animateTimer = Settings.FAST_MODE ? 0.125f : 0.5f;
        this.show = true;
        this.isMoving = true;
    }

    public void show(String text) {
        this.updateBodyText(text);
        this.show();
    }

    public void hide() {
        this.targetColor = new Color(0.0f, 0.0f, 0.0f, 0.0f);
        this.animateTimer = Settings.FAST_MODE ? 0.125f : 0.5f;
        this.show = false;
        this.isMoving = true;
        for (DialogWord w : this.words) {
            w.dialogFadeOut();
        }
        optionList.clear();
    }

    public void removeDialogOption(int slot) {
        optionList.remove(slot);
    }

    public void addDialogOption(String text) {
        optionList.add(new LargeDialogOptionButton(optionList.size(), text));
    }

    public void addDialogOption(String text, AbstractCard previewCard) {
        optionList.add(new LargeDialogOptionButton(optionList.size(), text, previewCard));
    }

    public void addDialogOption(String text, AbstractRelic previewRelic) {
        optionList.add(new LargeDialogOptionButton(optionList.size(), text, previewRelic));
    }

    public void addDialogOption(String text, AbstractCard previewCard, AbstractRelic previewRelic) {
        optionList.add(new LargeDialogOptionButton(optionList.size(), text, previewCard, previewRelic));
    }

    public void addDialogOption(String text, boolean isDisabled) {
        optionList.add(new LargeDialogOptionButton(optionList.size(), text, isDisabled));
    }

    public void addDialogOption(String text, boolean isDisabled, AbstractCard previewCard) {
        optionList.add(new LargeDialogOptionButton(optionList.size(), text, isDisabled, previewCard));
    }

    public void addDialogOption(String text, boolean isDisabled, AbstractRelic previewRelic) {
        optionList.add(new LargeDialogOptionButton(optionList.size(), text, isDisabled, previewRelic));
    }

    public void addDialogOption(String text, boolean isDisabled, AbstractCard previewCard, AbstractRelic previewRelic) {
        optionList.add(new LargeDialogOptionButton(optionList.size(), text, isDisabled, previewCard, previewRelic));
    }

    public void updateDialogOption(int slot, String text) {
        optionList.set(slot, new LargeDialogOptionButton(slot, text));
    }

    public void updateBodyText(String text) {
        this.updateBodyText(text, DialogWord.AppearEffect.BUMP_IN);
    }

    public void updateBodyText(String text, DialogWord.AppearEffect ae) {
        this.s = new Scanner(text);
        this.words.clear();
        this.textDone = false;
        this.a_effect = ae;
        this.curLineWidth = 0.0f;
        this.curLine = 0;
    }

    public void clearRemainingOptions() {
        for (int i = optionList.size() - 1; i > 0; --i) {
            optionList.remove(i);
        }
    }

    private void bodyTextEffectCN() {
        this.wordTimer -= Gdx.graphics.getDeltaTime();
        if (this.wordTimer < 0.0f && !this.textDone) {
            this.wordTimer = Settings.FAST_MODE ? 0.005f : 0.02f;
            if (this.s.hasNext()) {
                DialogWord.WordEffect effect;
                String word = this.s.next();
                if (word.equals("NL")) {
                    ++this.curLine;
                    this.curLineWidth = 0.0f;
                    return;
                }
                DialogWord.WordColor color = DialogWord.identifyWordColor(word);
                if (color != DialogWord.WordColor.DEFAULT) {
                    word = word.substring(2, word.length());
                }
                if ((effect = DialogWord.identifyWordEffect(word)) != DialogWord.WordEffect.NONE) {
                    word = word.substring(1, word.length() - 1);
                }
                for (int i = 0; i < word.length(); ++i) {
                    String tmp = Character.toString(word.charAt(i));
                    this.gl.setText(FontHelper.charDescFont, tmp);
                    if (this.curLineWidth + this.gl.width > DIALOG_MSG_W) {
                        ++this.curLine;
                        this.curLineWidth = this.gl.width;
                    } else {
                        this.curLineWidth += this.gl.width;
                    }
                    this.words.add(new DialogWord(FontHelper.charDescFont, tmp, this.a_effect, effect, color, DIALOG_MSG_X + this.curLineWidth - this.gl.width, DIALOG_MSG_Y - LINE_SPACING * (float)this.curLine, this.curLine));
                    if (this.show) continue;
                    this.words.get(this.words.size() - 1).dialogFadeOut();
                }
            } else {
                this.textDone = true;
                this.s.close();
            }
        }
    }

    private void bodyTextEffect() {
        this.wordTimer -= Gdx.graphics.getDeltaTime();
        if (this.wordTimer < 0.0f && !this.textDone) {
            this.wordTimer = Settings.FAST_MODE ? 0.005f : 0.02f;
            if (this.s.hasNext()) {
                DialogWord.WordEffect effect;
                String word = this.s.next();
                if (word.equals("NL")) {
                    ++this.curLine;
                    this.curLineWidth = 0.0f;
                    return;
                }
                DialogWord.WordColor color = DialogWord.identifyWordColor(word);
                if (color != DialogWord.WordColor.DEFAULT) {
                    word = word.substring(2, word.length());
                }
                if ((effect = DialogWord.identifyWordEffect(word)) != DialogWord.WordEffect.NONE) {
                    word = word.substring(1, word.length() - 1);
                }
                this.gl.setText(FontHelper.charDescFont, word);
                if (this.curLineWidth + this.gl.width > DIALOG_MSG_W) {
                    ++this.curLine;
                    this.curLineWidth = this.gl.width + CHAR_SPACING;
                } else {
                    this.curLineWidth += this.gl.width + CHAR_SPACING;
                }
                this.words.add(new DialogWord(FontHelper.charDescFont, word, this.a_effect, effect, color, DIALOG_MSG_X + this.curLineWidth - this.gl.width, DIALOG_MSG_Y - LINE_SPACING * (float)this.curLine, this.curLine));
                if (!this.show) {
                    this.words.get(this.words.size() - 1).dialogFadeOut();
                }
            } else {
                this.textDone = true;
                this.s.close();
            }
        }
    }

    public void render(SpriteBatch sb) {
        if (!AbstractDungeon.player.isDead) {
            for (DialogWord w : this.words) {
                w.render(sb, (float)Settings.HEIGHT - 525.0f * Settings.scale);
            }
            for (LargeDialogOptionButton b : optionList) {
                b.render(sb);
            }
            for (LargeDialogOptionButton b : optionList) {
                b.renderCardPreview(sb);
            }
            for (LargeDialogOptionButton b : optionList) {
                b.renderRelicPreview(sb);
            }
        }
    }
}

