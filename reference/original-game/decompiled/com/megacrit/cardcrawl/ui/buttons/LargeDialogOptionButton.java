/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.ui.buttons;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Interpolation;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.events.AbstractEvent;
import com.megacrit.cardcrawl.events.RoomEventDialog;
import com.megacrit.cardcrawl.helpers.FontHelper;
import com.megacrit.cardcrawl.helpers.Hitbox;
import com.megacrit.cardcrawl.helpers.ImageMaster;
import com.megacrit.cardcrawl.helpers.TipHelper;
import com.megacrit.cardcrawl.helpers.controller.CInputActionSet;
import com.megacrit.cardcrawl.helpers.input.InputHelper;
import com.megacrit.cardcrawl.relics.AbstractRelic;

public class LargeDialogOptionButton {
    private static final float OPTION_SPACING_Y = -82.0f * Settings.scale;
    private static final float TEXT_ADJUST_X = -400.0f * Settings.xScale;
    private static final float TEXT_ADJUST_Y = 10.0f * Settings.scale;
    public String msg;
    private Color textColor = new Color(0.0f, 0.0f, 0.0f, 0.0f);
    private Color boxColor = new Color(0.0f, 0.0f, 0.0f, 0.0f);
    private float x;
    private float y = -9999.0f * Settings.scale;
    public Hitbox hb;
    private static final float ANIM_TIME = 0.5f;
    private float animTimer = 0.5f;
    private float alpha = 0.0f;
    private static final Color TEXT_ACTIVE_COLOR = Color.WHITE.cpy();
    private static final Color TEXT_INACTIVE_COLOR = new Color(0.8f, 0.8f, 0.8f, 1.0f);
    private static final Color TEXT_DISABLED_COLOR = Color.FIREBRICK.cpy();
    private Color boxInactiveColor = new Color(0.2f, 0.25f, 0.25f, 0.0f);
    public boolean pressed = false;
    public boolean isDisabled;
    public int slot = 0;
    private AbstractCard cardToPreview;
    private AbstractRelic relicToPreview;
    private static final int W = 890;
    private static final int H = 77;

    public LargeDialogOptionButton(int slot, String msg, boolean isDisabled, AbstractCard previewCard, AbstractRelic previewRelic) {
        switch (AbstractEvent.type) {
            case TEXT: {
                this.x = 895.0f * Settings.xScale;
                break;
            }
            case IMAGE: {
                this.x = 1260.0f * Settings.xScale;
                break;
            }
            case ROOM: {
                this.x = 620.0f * Settings.xScale;
            }
        }
        this.slot = slot;
        this.isDisabled = isDisabled;
        this.cardToPreview = previewCard;
        this.relicToPreview = previewRelic;
        this.msg = isDisabled ? this.stripColor(msg) : msg;
        this.hb = new Hitbox(892.0f * Settings.xScale, 80.0f * Settings.yScale);
    }

    public LargeDialogOptionButton(int slot, String msg, AbstractCard previewCard, AbstractRelic previewRelic) {
        this(slot, msg, false, previewCard, previewRelic);
    }

    public LargeDialogOptionButton(int slot, String msg, boolean isDisabled, AbstractCard previewCard) {
        this(slot, msg, isDisabled, previewCard, null);
    }

    public LargeDialogOptionButton(int slot, String msg, boolean isDisabled, AbstractRelic previewRelic) {
        this(slot, msg, isDisabled, null, previewRelic);
    }

    public LargeDialogOptionButton(int slot, String msg) {
        this(slot, msg, false, null, null);
    }

    public LargeDialogOptionButton(int slot, String msg, boolean isDisabled) {
        this(slot, msg, isDisabled, null, null);
    }

    public LargeDialogOptionButton(int slot, String msg, AbstractCard previewCard) {
        this(slot, msg, false, previewCard);
    }

    public LargeDialogOptionButton(int slot, String msg, AbstractRelic previewRelic) {
        this(slot, msg, false, previewRelic);
    }

    private String stripColor(String input) {
        input = input.replace("#r", "");
        input = input.replace("#g", "");
        input = input.replace("#b", "");
        input = input.replace("#y", "");
        return input;
    }

    public void calculateY(int size) {
        if (AbstractEvent.type != AbstractEvent.EventType.ROOM) {
            this.y = Settings.OPTION_Y - 424.0f * Settings.scale;
            this.y += (float)this.slot * OPTION_SPACING_Y;
            this.y -= (float)size * OPTION_SPACING_Y;
        } else {
            this.y = Settings.OPTION_Y - 500.0f * Settings.scale;
            this.y += (float)this.slot * OPTION_SPACING_Y;
            this.y -= (float)RoomEventDialog.optionList.size() * OPTION_SPACING_Y;
        }
        this.hb.move(this.x, this.y);
    }

    public void update(int size) {
        this.calculateY(size);
        this.hoverAndClickLogic();
        this.updateAnimation();
    }

    private void updateAnimation() {
        if (this.animTimer != 0.0f) {
            this.animTimer -= Gdx.graphics.getDeltaTime();
            if (this.animTimer < 0.0f) {
                this.animTimer = 0.0f;
            }
            this.alpha = Interpolation.exp5In.apply(0.0f, 1.0f, 1.0f - this.animTimer / 0.5f);
        }
        this.textColor.a = this.alpha;
        this.boxColor.a = this.alpha;
    }

    private void hoverAndClickLogic() {
        this.hb.update();
        if (this.hb.hovered && InputHelper.justClickedLeft && !this.isDisabled && this.animTimer < 0.1f) {
            InputHelper.justClickedLeft = false;
            this.hb.clickStarted = true;
        }
        if (this.hb.hovered && CInputActionSet.select.isJustPressed() && !this.isDisabled) {
            this.hb.clicked = true;
        }
        if (this.hb.clicked) {
            this.hb.clicked = false;
            this.pressed = true;
        }
        if (!this.isDisabled) {
            if (this.hb.hovered) {
                this.textColor = TEXT_ACTIVE_COLOR;
                this.boxColor = Color.WHITE.cpy();
            } else {
                this.textColor = TEXT_INACTIVE_COLOR;
                this.boxColor = new Color(0.4f, 0.4f, 0.4f, 1.0f);
            }
        } else {
            this.textColor = TEXT_DISABLED_COLOR;
            this.boxColor = this.boxInactiveColor;
        }
        this.textColor = this.hb.hovered ? (!this.isDisabled ? TEXT_ACTIVE_COLOR : Color.GRAY.cpy()) : (!this.isDisabled ? TEXT_ACTIVE_COLOR : Color.GRAY.cpy());
    }

    public void render(SpriteBatch sb) {
        float scale = Settings.scale;
        float xScale = Settings.xScale;
        if (this.hb.clickStarted) {
            scale *= 0.99f;
            xScale *= 0.99f;
        }
        if (this.isDisabled) {
            sb.setColor(Color.WHITE);
            sb.draw(ImageMaster.EVENT_BUTTON_DISABLED, this.x - 445.0f, this.y - 38.5f, 445.0f, 38.5f, 890.0f, 77.0f, xScale, scale, 0.0f, 0, 0, 890, 77, false, false);
        } else {
            sb.setColor(this.boxColor);
            sb.draw(ImageMaster.EVENT_BUTTON_ENABLED, this.x - 445.0f, this.y - 38.5f, 445.0f, 38.5f, 890.0f, 77.0f, xScale, scale, 0.0f, 0, 0, 890, 77, false, false);
            sb.setBlendFunction(770, 1);
            sb.setColor(new Color(1.0f, 1.0f, 1.0f, 0.15f));
            sb.draw(ImageMaster.EVENT_BUTTON_ENABLED, this.x - 445.0f, this.y - 38.5f, 445.0f, 38.5f, 890.0f, 77.0f, xScale, scale, 0.0f, 0, 0, 890, 77, false, false);
            sb.setBlendFunction(770, 771);
        }
        if (FontHelper.getSmartWidth(FontHelper.largeDialogOptionFont, this.msg, Settings.WIDTH, 0.0f) > 800.0f * Settings.xScale) {
            FontHelper.renderSmartText(sb, FontHelper.smallDialogOptionFont, this.msg, this.x + TEXT_ADJUST_X, this.y + TEXT_ADJUST_Y, Settings.WIDTH, 0.0f, this.textColor);
        } else {
            FontHelper.renderSmartText(sb, FontHelper.largeDialogOptionFont, this.msg, this.x + TEXT_ADJUST_X, this.y + TEXT_ADJUST_Y, Settings.WIDTH, 0.0f, this.textColor);
        }
        this.hb.render(sb);
    }

    public void renderCardPreview(SpriteBatch sb) {
        if (this.cardToPreview != null && this.hb.hovered) {
            this.cardToPreview.current_x = this.x + this.hb.width / 1.75f;
            if (this.y < this.cardToPreview.hb.height / 2.0f + 5.0f) {
                this.y = this.cardToPreview.hb.height / 2.0f + 5.0f;
            }
            this.cardToPreview.current_y = this.y;
            this.cardToPreview.render(sb);
        }
    }

    public void renderRelicPreview(SpriteBatch sb) {
        if (!Settings.isControllerMode && this.relicToPreview != null && this.hb.hovered) {
            TipHelper.queuePowerTips(470.0f * Settings.scale, (float)InputHelper.mY + TipHelper.calculateToAvoidOffscreen(this.relicToPreview.tips, InputHelper.mY), this.relicToPreview.tips);
        }
    }
}

