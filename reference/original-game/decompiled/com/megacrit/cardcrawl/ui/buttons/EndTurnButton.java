/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.ui.buttons;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.megacrit.cardcrawl.actions.utility.NewQueueCardAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.helpers.FontHelper;
import com.megacrit.cardcrawl.helpers.Hitbox;
import com.megacrit.cardcrawl.helpers.ImageMaster;
import com.megacrit.cardcrawl.helpers.ShaderHelper;
import com.megacrit.cardcrawl.helpers.TipHelper;
import com.megacrit.cardcrawl.helpers.controller.CInputActionSet;
import com.megacrit.cardcrawl.helpers.input.InputActionSet;
import com.megacrit.cardcrawl.helpers.input.InputHelper;
import com.megacrit.cardcrawl.localization.TutorialStrings;
import com.megacrit.cardcrawl.localization.UIStrings;
import com.megacrit.cardcrawl.vfx.AbstractGameEffect;
import com.megacrit.cardcrawl.vfx.EndTurnGlowEffect;
import com.megacrit.cardcrawl.vfx.EndTurnLongPressBarFlashEffect;
import java.util.ArrayList;
import java.util.Iterator;

public class EndTurnButton {
    private static final TutorialStrings tutorialStrings = CardCrawlGame.languagePack.getTutorialString("End Turn Tip");
    public static final String[] MSG = EndTurnButton.tutorialStrings.TEXT;
    public static final String[] LABEL = EndTurnButton.tutorialStrings.LABEL;
    private static final UIStrings uiStrings = CardCrawlGame.languagePack.getUIString("End Turn Button");
    public static final String[] TEXT = EndTurnButton.uiStrings.TEXT;
    private String label = TEXT[0];
    public static final String END_TURN_MSG = TEXT[0];
    public static final String ENEMY_TURN_MSG = TEXT[1];
    private static final Color DISABLED_COLOR = new Color(0.7f, 0.7f, 0.7f, 1.0f);
    private static final float SHOW_X = 1640.0f * Settings.xScale;
    private static final float SHOW_Y = 210.0f * Settings.yScale;
    private static final float HIDE_X = SHOW_X + 500.0f * Settings.xScale;
    private float current_x = HIDE_X;
    private float current_y = SHOW_Y;
    private float target_x = this.current_x;
    private boolean isHidden = true;
    public boolean enabled = false;
    private boolean isDisabled = false;
    private Color textColor;
    private ArrayList<EndTurnGlowEffect> glowList = new ArrayList();
    private static final float GLOW_INTERVAL = 1.2f;
    private float glowTimer = 0.0f;
    public boolean isGlowing = false;
    public boolean isWarning = false;
    private Hitbox hb = new Hitbox(0.0f, 0.0f, 230.0f * Settings.scale, 110.0f * Settings.scale);
    private float holdProgress = 0.0f;
    private static final float HOLD_DUR = 0.4f;
    private Color holdBarColor = new Color(1.0f, 1.0f, 1.0f, 0.0f);

    public void update() {
        this.glow();
        this.updateHoldProgress();
        if (this.current_x != this.target_x) {
            this.current_x = MathUtils.lerp(this.current_x, this.target_x, Gdx.graphics.getDeltaTime() * 9.0f);
            if (Math.abs(this.current_x - this.target_x) < Settings.UI_SNAP_THRESHOLD) {
                this.current_x = this.target_x;
            }
        }
        this.hb.move(this.current_x, this.current_y);
        if (this.enabled) {
            this.isDisabled = false;
            if (AbstractDungeon.isScreenUp || AbstractDungeon.player.isDraggingCard || AbstractDungeon.player.inSingleTargetMode) {
                this.isDisabled = true;
            }
            if (AbstractDungeon.player.hoveredCard == null) {
                this.hb.update();
            }
            if (!Settings.USE_LONG_PRESS && InputHelper.justClickedLeft && this.hb.hovered && !this.isDisabled && !AbstractDungeon.isScreenUp) {
                this.hb.clickStarted = true;
                CardCrawlGame.sound.play("UI_CLICK_1");
            }
            if (this.hb.hovered && !this.isDisabled && !AbstractDungeon.isScreenUp) {
                this.isWarning = this.showWarning();
                if (this.hb.justHovered && AbstractDungeon.player.hoveredCard == null) {
                    CardCrawlGame.sound.play("UI_HOVER");
                    for (AbstractCard c : AbstractDungeon.player.hand.group) {
                        if (!c.isGlowing) continue;
                        c.superFlash(c.glowColor);
                    }
                }
            }
        }
        if (this.holdProgress == 0.4f && !this.isDisabled && !AbstractDungeon.isScreenUp) {
            this.disable(true);
            this.holdProgress = 0.0f;
            AbstractDungeon.effectsQueue.add(new EndTurnLongPressBarFlashEffect());
        }
        if ((!Settings.USE_LONG_PRESS || !Settings.isControllerMode && !InputActionSet.endTurn.isPressed()) && (this.hb.clicked || (InputActionSet.endTurn.isJustPressed() || CInputActionSet.proceed.isJustPressed()) && !this.isDisabled && this.enabled)) {
            this.hb.clicked = false;
            if (!this.isDisabled && !AbstractDungeon.isScreenUp) {
                this.disable(true);
            }
        }
    }

    private void updateHoldProgress() {
        if (!Settings.USE_LONG_PRESS || !Settings.isControllerMode && !InputActionSet.endTurn.isPressed() && !InputHelper.isMouseDown) {
            this.holdProgress -= Gdx.graphics.getDeltaTime();
            if (this.holdProgress < 0.0f) {
                this.holdProgress = 0.0f;
            }
            return;
        }
        if ((this.hb.hovered && InputHelper.isMouseDown || CInputActionSet.proceed.isPressed() || InputActionSet.endTurn.isPressed()) && !this.isDisabled && this.enabled) {
            this.holdProgress += Gdx.graphics.getDeltaTime();
            if (this.holdProgress > 0.4f) {
                this.holdProgress = 0.4f;
            }
        } else {
            this.holdProgress -= Gdx.graphics.getDeltaTime();
            if (this.holdProgress < 0.0f) {
                this.holdProgress = 0.0f;
            }
        }
    }

    private boolean showWarning() {
        for (AbstractCard card : AbstractDungeon.player.hand.group) {
            if (!card.isGlowing) continue;
            return true;
        }
        return false;
    }

    public void enable() {
        this.enabled = true;
        this.updateText(END_TURN_MSG);
    }

    public void disable(boolean isEnemyTurn) {
        InputHelper.moveCursorToNeutralPosition();
        AbstractDungeon.actionManager.addToBottom(new NewQueueCardAction());
        this.enabled = false;
        this.hb.hovered = false;
        this.isGlowing = false;
        if (isEnemyTurn) {
            this.updateText(ENEMY_TURN_MSG);
            CardCrawlGame.sound.play("END_TURN");
            AbstractDungeon.player.endTurnQueued = true;
            AbstractDungeon.player.releaseCard();
        } else {
            this.updateText(END_TURN_MSG);
        }
    }

    public void disable() {
        this.enabled = false;
        this.hb.hovered = false;
        this.isGlowing = false;
    }

    public void updateText(String msg) {
        this.label = msg;
    }

    private void glow() {
        if (this.isGlowing && !this.isHidden) {
            if (this.glowTimer < 0.0f) {
                this.glowList.add(new EndTurnGlowEffect());
                this.glowTimer = 1.2f;
            } else {
                this.glowTimer -= Gdx.graphics.getDeltaTime();
            }
        }
        Iterator<EndTurnGlowEffect> i = this.glowList.iterator();
        while (i.hasNext()) {
            AbstractGameEffect e = i.next();
            e.update();
            if (!e.isDone) continue;
            i.remove();
        }
    }

    public void hide() {
        if (!this.isHidden) {
            this.target_x = HIDE_X;
            this.isHidden = true;
        }
    }

    public void show() {
        if (this.isHidden) {
            this.target_x = SHOW_X;
            this.isHidden = false;
            if (this.isGlowing) {
                this.glowTimer = -1.0f;
            }
        }
    }

    public void render(SpriteBatch sb) {
        if (!Settings.hideEndTurn) {
            float tmpY = this.current_y;
            this.renderHoldEndTurn(sb);
            if (this.isDisabled || !this.enabled) {
                this.textColor = this.label.equals(ENEMY_TURN_MSG) ? Settings.CREAM_COLOR : Color.LIGHT_GRAY;
            } else {
                this.textColor = this.hb.hovered ? (this.isWarning ? Settings.RED_TEXT_COLOR : Color.CYAN) : (this.isGlowing ? Settings.GOLD_COLOR : Settings.CREAM_COLOR);
                if (this.hb.hovered && !AbstractDungeon.isScreenUp && !Settings.isTouchScreen) {
                    TipHelper.renderGenericTip(this.current_x - 90.0f * Settings.scale, this.current_y + 300.0f * Settings.scale, LABEL[0] + " (" + InputActionSet.endTurn.getKeyString() + ")", MSG[0] + AbstractDungeon.player.gameHandSize + MSG[1]);
                }
            }
            if (this.hb.clickStarted && !AbstractDungeon.isScreenUp) {
                tmpY -= 2.0f * Settings.scale;
            } else if (this.hb.hovered && !AbstractDungeon.isScreenUp) {
                tmpY += 2.0f * Settings.scale;
            }
            if (this.enabled) {
                if (this.isDisabled || this.hb.clickStarted && this.hb.hovered) {
                    sb.setColor(DISABLED_COLOR);
                } else {
                    sb.setColor(Color.WHITE);
                }
            } else {
                ShaderHelper.setShader(sb, ShaderHelper.Shader.GRAYSCALE);
            }
            Texture buttonImg = this.isGlowing && !this.hb.clickStarted ? ImageMaster.END_TURN_BUTTON_GLOW : ImageMaster.END_TURN_BUTTON;
            if (this.hb.hovered && !this.isDisabled && !AbstractDungeon.isScreenUp) {
                sb.draw(ImageMaster.END_TURN_HOVER, this.current_x - 128.0f, tmpY - 128.0f, 128.0f, 128.0f, 256.0f, 256.0f, Settings.scale, Settings.scale, 0.0f, 0, 0, 256, 256, false, false);
            }
            sb.draw(buttonImg, this.current_x - 128.0f, tmpY - 128.0f, 128.0f, 128.0f, 256.0f, 256.0f, Settings.scale, Settings.scale, 0.0f, 0, 0, 256, 256, false, false);
            if (!this.enabled) {
                ShaderHelper.setShader(sb, ShaderHelper.Shader.DEFAULT);
            }
            this.renderGlowEffect(sb, this.current_x, this.current_y);
            if ((this.hb.hovered || this.holdProgress > 0.0f) && !this.isDisabled && !AbstractDungeon.isScreenUp) {
                sb.setBlendFunction(770, 1);
                sb.setColor(Settings.HALF_TRANSPARENT_WHITE_COLOR);
                sb.draw(buttonImg, this.current_x - 128.0f, tmpY - 128.0f, 128.0f, 128.0f, 256.0f, 256.0f, Settings.scale, Settings.scale, 0.0f, 0, 0, 256, 256, false, false);
                sb.setBlendFunction(770, 771);
            }
            if (Settings.isControllerMode && this.enabled) {
                sb.setColor(Color.WHITE);
                sb.draw(CInputActionSet.proceed.getKeyImg(), this.current_x - 32.0f - 42.0f * Settings.scale - FontHelper.getSmartWidth(FontHelper.panelEndTurnFont, this.label, 99999.0f, 0.0f) / 2.0f, tmpY - 32.0f, 32.0f, 32.0f, 64.0f, 64.0f, Settings.scale, Settings.scale, 0.0f, 0, 0, 64, 64, false, false);
            }
            FontHelper.renderFontCentered(sb, FontHelper.panelEndTurnFont, this.label, this.current_x - 0.0f * Settings.scale, tmpY - 3.0f * Settings.scale, this.textColor);
            if (!this.isHidden) {
                this.hb.render(sb);
            }
        }
    }

    private void renderHoldEndTurn(SpriteBatch sb) {
        if (!Settings.USE_LONG_PRESS) {
            return;
        }
        this.holdBarColor.r = 0.0f;
        this.holdBarColor.g = 0.0f;
        this.holdBarColor.b = 0.0f;
        this.holdBarColor.a = this.holdProgress * 1.5f;
        sb.setColor(this.holdBarColor);
        sb.draw(ImageMaster.WHITE_SQUARE_IMG, this.current_x - 107.0f * Settings.scale, this.current_y + 53.0f * Settings.scale - 7.0f * Settings.scale, 525.0f * Settings.scale * this.holdProgress + 14.0f * Settings.scale, 20.0f * Settings.scale);
        this.holdBarColor.r = this.holdProgress * 2.5f;
        this.holdBarColor.g = 0.6f + this.holdProgress;
        this.holdBarColor.b = 0.6f;
        this.holdBarColor.a = 1.0f;
        sb.setColor(this.holdBarColor);
        sb.draw(ImageMaster.WHITE_SQUARE_IMG, this.current_x - 100.0f * Settings.scale, this.current_y + 53.0f * Settings.scale, 525.0f * Settings.scale * this.holdProgress, 6.0f * Settings.scale);
    }

    private void renderGlowEffect(SpriteBatch sb, float x, float y) {
        for (EndTurnGlowEffect e : this.glowList) {
            e.render(sb, x, y);
        }
    }
}

