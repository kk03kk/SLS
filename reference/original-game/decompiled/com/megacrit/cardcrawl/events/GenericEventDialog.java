/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.events;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.helpers.FontHelper;
import com.megacrit.cardcrawl.helpers.ImageMaster;
import com.megacrit.cardcrawl.helpers.MathHelper;
import com.megacrit.cardcrawl.relics.AbstractRelic;
import com.megacrit.cardcrawl.ui.DialogWord;
import com.megacrit.cardcrawl.ui.buttons.LargeDialogOptionButton;
import java.util.ArrayList;
import java.util.Scanner;

public class GenericEventDialog {
    private Texture img = null;
    private static final float DIALOG_MSG_X_IMAGE = 816.0f * Settings.xScale;
    private static final float DIALOG_MSG_W_IMAGE = 900.0f * Settings.scale;
    private Color panelColor = new Color(1.0f, 1.0f, 1.0f, 0.0f);
    private Color titleColor = new Color(1.0f, 0.835f, 0.39f, 0.0f);
    private Color borderColor = new Color(0.0f, 0.0f, 0.0f, 0.0f);
    private Color imgColor = new Color(1.0f, 1.0f, 1.0f, 0.0f);
    private float animateTimer = 0.0f;
    private static final float ANIM_SPEED = 0.5f;
    private static boolean show = false;
    private String title = "";
    private static final float TITLE_X = 570.0f * Settings.xScale;
    private static final float TITLE_Y = Settings.EVENT_Y + 408.0f * Settings.scale;
    private static float curLineWidth = 0.0f;
    private static int curLine = 0;
    private static DialogWord.AppearEffect a_effect;
    private static Scanner s;
    private static GlyphLayout gl;
    private static ArrayList<DialogWord> words;
    private static boolean textDone;
    private static float wordTimer;
    private static final float WORD_TIME = 0.02f;
    private static final float CHAR_SPACING;
    private static final float LINE_SPACING;
    private static final float DIALOG_MSG_X_TEXT;
    private static final float DIALOG_MSG_Y_TEXT;
    private static final float DIALOG_MSG_W_TEXT;
    private static float DIALOG_MSG_X;
    private static float DIALOG_MSG_Y;
    private static float DIALOG_MSG_W;
    public ArrayList<LargeDialogOptionButton> optionList = new ArrayList();
    public static int selectedOption;
    public static boolean waitForInput;

    public void loadImage(String imgUrl) {
        if (this.img != null) {
            this.img.dispose();
            this.img = null;
        }
        this.img = ImageMaster.loadImage(imgUrl);
        DIALOG_MSG_X = DIALOG_MSG_X_IMAGE;
        DIALOG_MSG_W = DIALOG_MSG_W_IMAGE;
    }

    private void clearImage() {
        this.dispose();
        DIALOG_MSG_X = DIALOG_MSG_X_TEXT;
        DIALOG_MSG_Y = DIALOG_MSG_Y_TEXT;
        DIALOG_MSG_W = DIALOG_MSG_W_TEXT;
    }

    public void update() {
        this.animateIn();
        if (show && this.animateTimer == 0.0f) {
            for (int i = 0; i < this.optionList.size(); ++i) {
                this.optionList.get(i).update(this.optionList.size());
                if (!this.optionList.get((int)i).pressed || !waitForInput) continue;
                selectedOption = i;
                this.optionList.get((int)i).pressed = false;
                waitForInput = false;
            }
        }
        if (!Settings.lineBreakViaCharacter) {
            this.bodyTextEffect();
        } else {
            this.bodyTextEffectCN();
        }
        for (DialogWord w : words) {
            w.update();
        }
    }

    private void animateIn() {
        if (show) {
            this.animateTimer -= Gdx.graphics.getDeltaTime();
            if (this.animateTimer < 0.0f) {
                this.animateTimer = 0.0f;
            }
            this.panelColor.a = MathHelper.slowColorLerpSnap(this.panelColor.a, 1.0f);
            if (this.panelColor.a > 0.8f) {
                this.borderColor.a = this.titleColor.a = MathHelper.slowColorLerpSnap(this.titleColor.a, 1.0f);
                if (this.borderColor.a > 0.8f) {
                    this.imgColor.a = MathHelper.slowColorLerpSnap(this.imgColor.a, 1.0f);
                }
            }
        }
    }

    public static int getSelectedOption() {
        waitForInput = true;
        return selectedOption;
    }

    public static void hide() {
        show = false;
    }

    public static void show() {
        show = true;
    }

    public void clear() {
        show = false;
        this.clearImage();
        this.animateTimer = 0.0f;
        this.panelColor.a = 0.0f;
        this.titleColor.a = 0.0f;
        this.imgColor.a = 0.0f;
        this.borderColor.a = 0.0f;
        this.optionList.clear();
        words.clear();
        waitForInput = true;
    }

    public static void cleanUp() {
        words.clear();
        show = false;
        waitForInput = true;
    }

    public void show(String title, String text) {
        this.title = title;
        this.updateBodyText(text);
        this.animateTimer = Settings.FAST_MODE ? 0.125f : 0.5f;
        show = true;
    }

    public void clearAllDialogs() {
        this.optionList.clear();
    }

    public void removeDialogOption(int slot) {
        if (slot < this.optionList.size()) {
            this.optionList.remove(slot);
        }
        for (LargeDialogOptionButton b : this.optionList) {
            b.calculateY(this.optionList.size());
        }
    }

    public void clearRemainingOptions() {
        for (int i = this.optionList.size() - 1; i > 0; --i) {
            this.optionList.remove(i);
        }
        for (LargeDialogOptionButton b : this.optionList) {
            b.calculateY(this.optionList.size());
        }
    }

    public void setDialogOption(String text) {
        this.optionList.add(new LargeDialogOptionButton(this.optionList.size(), text));
        for (LargeDialogOptionButton b : this.optionList) {
            b.calculateY(this.optionList.size());
        }
    }

    public void setDialogOption(String text, AbstractCard previewCard) {
        this.optionList.add(new LargeDialogOptionButton(this.optionList.size(), text, previewCard));
        for (LargeDialogOptionButton b : this.optionList) {
            b.calculateY(this.optionList.size());
        }
    }

    public void setDialogOption(String text, AbstractRelic previewRelic) {
        this.optionList.add(new LargeDialogOptionButton(this.optionList.size(), text, previewRelic));
        for (LargeDialogOptionButton b : this.optionList) {
            b.calculateY(this.optionList.size());
        }
    }

    public void setDialogOption(String text, AbstractCard previewCard, AbstractRelic previewRelic) {
        this.optionList.add(new LargeDialogOptionButton(this.optionList.size(), text, previewCard, previewRelic));
        for (LargeDialogOptionButton b : this.optionList) {
            b.calculateY(this.optionList.size());
        }
    }

    public void setDialogOption(String text, boolean isDisabled) {
        this.optionList.add(new LargeDialogOptionButton(this.optionList.size(), text, isDisabled));
        for (LargeDialogOptionButton b : this.optionList) {
            b.calculateY(this.optionList.size());
        }
    }

    public void setDialogOption(String text, boolean isDisabled, AbstractCard previewCard) {
        this.optionList.add(new LargeDialogOptionButton(this.optionList.size(), text, isDisabled, previewCard));
        for (LargeDialogOptionButton b : this.optionList) {
            b.calculateY(this.optionList.size());
        }
    }

    public void setDialogOption(String text, boolean isDisabled, AbstractRelic previewRelic) {
        this.optionList.add(new LargeDialogOptionButton(this.optionList.size(), text, isDisabled, previewRelic));
        for (LargeDialogOptionButton b : this.optionList) {
            b.calculateY(this.optionList.size());
        }
    }

    public void setDialogOption(String text, boolean isDisabled, AbstractCard previewCard, AbstractRelic previewRelic) {
        this.optionList.add(new LargeDialogOptionButton(this.optionList.size(), text, isDisabled, previewCard, previewRelic));
        for (LargeDialogOptionButton b : this.optionList) {
            b.calculateY(this.optionList.size());
        }
    }

    public void updateDialogOption(int slot, String text) {
        if (!this.optionList.isEmpty()) {
            if (this.optionList.size() > slot) {
                this.optionList.set(slot, new LargeDialogOptionButton(slot, text));
            } else {
                this.optionList.add(new LargeDialogOptionButton(slot, text));
            }
        } else {
            this.optionList.add(new LargeDialogOptionButton(slot, text));
        }
    }

    public void updateDialogOption(int slot, String text, boolean isDisabled) {
        if (!this.optionList.isEmpty()) {
            if (this.optionList.size() > slot) {
                this.optionList.set(slot, new LargeDialogOptionButton(slot, text, isDisabled));
            } else {
                this.optionList.add(new LargeDialogOptionButton(slot, text, isDisabled));
            }
        } else {
            this.optionList.add(new LargeDialogOptionButton(slot, text, isDisabled));
        }
    }

    public void updateDialogOption(int slot, String text, AbstractCard previewCard) {
        if (!this.optionList.isEmpty()) {
            if (this.optionList.size() > slot) {
                this.optionList.set(slot, new LargeDialogOptionButton(slot, text, previewCard));
            } else {
                this.optionList.add(new LargeDialogOptionButton(slot, text, previewCard));
            }
        } else {
            this.optionList.add(new LargeDialogOptionButton(slot, text, previewCard));
        }
    }

    public void updateBodyText(String text) {
        this.updateBodyText(text, DialogWord.AppearEffect.BUMP_IN);
    }

    public void updateBodyText(String text, DialogWord.AppearEffect ae) {
        s = new Scanner(text);
        words.clear();
        textDone = false;
        a_effect = ae;
        curLineWidth = 0.0f;
        curLine = 0;
    }

    private void bodyTextEffectCN() {
        if ((wordTimer -= Gdx.graphics.getDeltaTime()) < 0.0f && !textDone) {
            wordTimer = Settings.FAST_MODE ? 0.005f : 0.02f;
            if (s.hasNext()) {
                DialogWord.WordEffect effect;
                String word = s.next();
                if (word.equals("NL")) {
                    ++curLine;
                    curLineWidth = 0.0f;
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
                    gl.setText(FontHelper.charDescFont, tmp);
                    if (curLineWidth + GenericEventDialog.gl.width > DIALOG_MSG_W) {
                        ++curLine;
                        curLineWidth = GenericEventDialog.gl.width;
                    } else {
                        curLineWidth += GenericEventDialog.gl.width;
                    }
                    words.add(new DialogWord(FontHelper.charDescFont, tmp, a_effect, effect, color, DIALOG_MSG_X + curLineWidth - GenericEventDialog.gl.width, DIALOG_MSG_Y - LINE_SPACING * (float)curLine, curLine));
                    if (show) continue;
                    words.get(words.size() - 1).dialogFadeOut();
                }
            } else {
                textDone = true;
                s.close();
            }
        }
    }

    private void bodyTextEffect() {
        if ((wordTimer -= Gdx.graphics.getDeltaTime()) < 0.0f && !textDone) {
            wordTimer = Settings.FAST_MODE ? 0.005f : 0.02f;
            if (s.hasNext()) {
                DialogWord.WordEffect effect;
                String word = s.next();
                if (word.equals("NL")) {
                    ++curLine;
                    curLineWidth = 0.0f;
                    return;
                }
                DialogWord.WordColor color = DialogWord.identifyWordColor(word);
                if (color != DialogWord.WordColor.DEFAULT) {
                    word = word.substring(2, word.length());
                }
                if ((effect = DialogWord.identifyWordEffect(word)) != DialogWord.WordEffect.NONE) {
                    word = word.substring(1, word.length() - 1);
                }
                gl.setText(FontHelper.charDescFont, word);
                if (curLineWidth + GenericEventDialog.gl.width > DIALOG_MSG_W) {
                    ++curLine;
                    curLineWidth = GenericEventDialog.gl.width + CHAR_SPACING;
                } else {
                    curLineWidth += GenericEventDialog.gl.width + CHAR_SPACING;
                }
                words.add(new DialogWord(FontHelper.charDescFont, word, a_effect, effect, color, DIALOG_MSG_X + curLineWidth - GenericEventDialog.gl.width, DIALOG_MSG_Y - LINE_SPACING * (float)curLine, curLine));
                if (!show) {
                    words.get(words.size() - 1).dialogFadeOut();
                }
            } else {
                textDone = true;
                s.close();
            }
        }
    }

    public void render(SpriteBatch sb) {
        if (show && !AbstractDungeon.player.isDead) {
            sb.setColor(this.panelColor);
            sb.draw(AbstractDungeon.eventBackgroundImg, (float)Settings.WIDTH / 2.0f - 881.5f - 12.0f * Settings.xScale, Settings.EVENT_Y - 403.0f + 64.0f * Settings.scale, 881.5f, 403.0f, 1763.0f, 806.0f, Settings.xScale, Settings.scale, 0.0f, 0, 0, 1763, 806, false, false);
            if (this.img != null) {
                sb.setColor(this.imgColor);
                sb.draw(this.img, 460.0f * Settings.xScale - 300.0f, Settings.EVENT_Y - 300.0f + 16.0f * Settings.scale, 300.0f, 300.0f, 600.0f, 600.0f, Settings.scale, Settings.scale, 0.0f, 0, 0, 600, 600, false, false);
                sb.setColor(this.borderColor);
                sb.draw(ImageMaster.EVENT_IMG_FRAME, 460.0f * Settings.xScale - 305.0f, Settings.EVENT_Y - 305.0f + 16.0f * Settings.scale, 305.0f, 305.0f, 610.0f, 610.0f, Settings.scale, Settings.scale, 0.0f, 0, 0, 610, 610, false, false);
            }
            FontHelper.renderFontCentered(sb, FontHelper.losePowerFont, this.title, TITLE_X, TITLE_Y, this.titleColor, 0.88f);
            for (DialogWord w : words) {
                w.render(sb);
            }
            for (LargeDialogOptionButton b : this.optionList) {
                b.render(sb);
            }
            for (LargeDialogOptionButton b : this.optionList) {
                b.renderCardPreview(sb);
            }
            for (LargeDialogOptionButton b : this.optionList) {
                b.renderRelicPreview(sb);
            }
        }
    }

    public void dispose() {
        if (this.img != null) {
            this.img.dispose();
            this.img = null;
            show = false;
        }
    }

    static {
        gl = new GlyphLayout();
        words = new ArrayList();
        textDone = true;
        wordTimer = 0.0f;
        CHAR_SPACING = 8.0f * Settings.scale;
        LINE_SPACING = Settings.BIG_TEXT_MODE ? 40.0f * Settings.scale : 38.0f * Settings.scale;
        DIALOG_MSG_X_TEXT = 455.0f * Settings.xScale;
        DIALOG_MSG_Y_TEXT = Settings.isMobile ? Settings.EVENT_Y + 330.0f * Settings.scale : Settings.EVENT_Y + 300.0f * Settings.scale;
        DIALOG_MSG_W_TEXT = 1000.0f * Settings.scale;
        DIALOG_MSG_X = DIALOG_MSG_X_TEXT;
        DIALOG_MSG_Y = DIALOG_MSG_Y_TEXT;
        DIALOG_MSG_W = DIALOG_MSG_W_TEXT;
        selectedOption = -1;
        waitForInput = true;
    }
}

