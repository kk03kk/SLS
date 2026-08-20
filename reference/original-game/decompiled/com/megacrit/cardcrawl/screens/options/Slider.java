/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.screens.options;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.helpers.FontHelper;
import com.megacrit.cardcrawl.helpers.Hitbox;
import com.megacrit.cardcrawl.helpers.ImageMaster;
import com.megacrit.cardcrawl.helpers.MathHelper;
import com.megacrit.cardcrawl.helpers.controller.CInputActionSet;
import com.megacrit.cardcrawl.helpers.input.InputHelper;
import java.text.DecimalFormat;

public class Slider {
    private static final int BG_W = 250;
    private static final int BG_H = 24;
    private static final int S_W = 44;
    private static final float SLIDE_W = 230.0f * Settings.xScale;
    private static final float BG_X = 1350.0f * Settings.xScale;
    private static final float L_X = 1235.0f * Settings.xScale;
    private float x;
    private float y;
    private float volume;
    public Hitbox hb;
    public Hitbox bgHb;
    private boolean sliderGrabbed = false;
    private SliderType type;
    private static DecimalFormat df = new DecimalFormat("#");

    public Slider(float y, float volume, SliderType type) {
        this.type = type;
        this.y = y;
        this.volume = volume;
        this.hb = new Hitbox(42.0f * Settings.scale, 38.0f * Settings.scale);
        this.bgHb = new Hitbox(300.0f * Settings.scale, 38.0f * Settings.scale);
        this.bgHb.move(BG_X, y);
        this.x = L_X + SLIDE_W * volume;
    }

    public void update() {
        this.hb.update();
        this.bgHb.update();
        this.hb.move(this.x, this.y);
        if (this.sliderGrabbed) {
            if (InputHelper.isMouseDown) {
                this.x = MathHelper.fadeLerpSnap(this.x, InputHelper.mX);
                if (this.x < L_X) {
                    this.x = L_X;
                } else if (this.x > L_X + SLIDE_W) {
                    this.x = L_X + SLIDE_W;
                }
                this.volume = (this.x - L_X) / SLIDE_W;
                this.modifyVolume();
            } else {
                if (this.type == SliderType.SFX) {
                    int roll = MathUtils.random(2);
                    if (roll == 0) {
                        CardCrawlGame.sound.play("ATTACK_DAGGER_1");
                    } else if (roll == 1) {
                        CardCrawlGame.sound.play("ATTACK_DAGGER_2");
                    } else if (roll == 2) {
                        CardCrawlGame.sound.play("ATTACK_DAGGER_3");
                    }
                }
                this.sliderGrabbed = false;
                Settings.soundPref.flush();
            }
        } else if (InputHelper.justClickedLeft) {
            if (this.hb.hovered) {
                this.sliderGrabbed = true;
            } else if (this.bgHb.hovered) {
                this.sliderGrabbed = true;
            }
        }
        if (Settings.isControllerMode && this.bgHb.hovered) {
            if (CInputActionSet.inspectLeft.isJustPressed()) {
                this.x -= 5.0f * Settings.scale;
                if (this.x < L_X) {
                    this.x = L_X;
                }
                this.volume = (this.x - L_X) / SLIDE_W;
                this.modifyVolume();
            } else if (CInputActionSet.inspectRight.isJustPressed()) {
                this.x += 5.0f * Settings.scale;
                if (this.x > L_X + SLIDE_W) {
                    this.x = L_X + SLIDE_W;
                }
                this.volume = (this.x - L_X) / SLIDE_W;
                this.modifyVolume();
            }
        }
    }

    private void modifyVolume() {
        switch (this.type) {
            case MASTER: {
                Settings.MASTER_VOLUME = this.volume;
                Settings.soundPref.putFloat("Master Volume", this.volume);
                CardCrawlGame.music.updateVolume();
                if (CardCrawlGame.mode == CardCrawlGame.GameMode.CHAR_SELECT) {
                    CardCrawlGame.mainMenuScreen.updateAmbienceVolume();
                    break;
                }
                if (AbstractDungeon.scene == null) break;
                AbstractDungeon.scene.updateAmbienceVolume();
                break;
            }
            case BGM: {
                Settings.MUSIC_VOLUME = this.volume;
                CardCrawlGame.music.updateVolume();
                Settings.soundPref.putFloat("Music Volume", this.volume);
                break;
            }
            case SFX: {
                Settings.SOUND_VOLUME = this.volume;
                if (CardCrawlGame.mode == CardCrawlGame.GameMode.CHAR_SELECT) {
                    CardCrawlGame.mainMenuScreen.updateAmbienceVolume();
                } else if (AbstractDungeon.scene != null) {
                    AbstractDungeon.scene.updateAmbienceVolume();
                }
                Settings.soundPref.putFloat("Sound Volume", this.volume);
                break;
            }
        }
    }

    public void render(SpriteBatch sb) {
        sb.setColor(Color.WHITE);
        if (Settings.isControllerMode && this.bgHb.hovered) {
            sb.draw(ImageMaster.CONTROLLER_RS, this.bgHb.cX + 195.0f * Settings.scale, this.bgHb.cY - 46.0f * Settings.scale, 32.0f, 32.0f, 64.0f, 64.0f, Settings.scale, Settings.scale, 0.0f, 0, 0, 64, 64, false, false);
        }
        sb.draw(ImageMaster.OPTION_SLIDER_BG, BG_X - 125.0f, this.y - 12.0f, 125.0f, 12.0f, 250.0f, 24.0f, Settings.scale, Settings.scale, 0.0f, 0, 0, 250, 24, false, false);
        if (this.sliderGrabbed) {
            FontHelper.renderFontCentered(sb, FontHelper.tipBodyFont, df.format(this.volume * 100.0f) + '%', BG_X + 170.0f * Settings.scale, this.y, Settings.GREEN_TEXT_COLOR);
        } else {
            FontHelper.renderFontCentered(sb, FontHelper.tipBodyFont, df.format(this.volume * 100.0f) + '%', BG_X + 170.0f * Settings.scale, this.y, Settings.BLUE_TEXT_COLOR);
        }
        sb.draw(ImageMaster.OPTION_SLIDER, this.x - 22.0f, this.y - 22.0f, 22.0f, 22.0f, 44.0f, 44.0f, Settings.scale, Settings.scale, 0.0f, 0, 0, 44, 44, false, false);
        this.hb.render(sb);
        this.bgHb.render(sb);
    }

    public static enum SliderType {
        MASTER,
        BGM,
        SFX;

    }
}

