/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.core;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.helpers.ImageMaster;
import com.megacrit.cardcrawl.helpers.MathHelper;
import com.megacrit.cardcrawl.helpers.input.InputHelper;

public class GameCursor {
    private Texture img;
    private Texture mImg;
    public static final int W = 64;
    public static boolean hidden = false;
    private float rotation;
    private static final float OFFSET_X = 24.0f * Settings.scale;
    private static final float OFFSET_Y = -24.0f * Settings.scale;
    private static final float SHADOW_OFFSET_X = -10.0f * Settings.scale;
    private static final float SHADOW_OFFSET_Y = 8.0f * Settings.scale;
    private static final Color SHADOW_COLOR = new Color(0.0f, 0.0f, 0.0f, 0.15f);
    private static final float TILT_ANGLE = 6.0f;
    private CursorType type = CursorType.NORMAL;
    public Color color = new Color(1.0f, 1.0f, 1.0f, 0.0f);

    public GameCursor() {
        this.img = ImageMaster.loadImage("images/ui/cursors/gold2.png");
        this.mImg = ImageMaster.loadImage("images/ui/cursors/magGlass2.png");
    }

    public void update() {
        this.rotation = InputHelper.isMouseDown ? 6.0f : 0.0f;
    }

    public void changeType(CursorType type) {
        this.type = type;
    }

    public void render(SpriteBatch sb) {
        if (hidden || Settings.isControllerMode) {
            return;
        }
        if (!Settings.isTouchScreen || Settings.isDev) {
            switch (this.type) {
                case NORMAL: {
                    sb.setColor(SHADOW_COLOR);
                    sb.draw(this.img, (float)InputHelper.mX - 32.0f - SHADOW_OFFSET_X + OFFSET_X, (float)InputHelper.mY - 32.0f - SHADOW_OFFSET_Y + OFFSET_Y, 32.0f, 32.0f, 64.0f, 64.0f, Settings.scale, Settings.scale, this.rotation, 0, 0, 64, 64, false, false);
                    sb.setColor(Color.WHITE);
                    sb.draw(this.img, (float)InputHelper.mX - 32.0f + OFFSET_X, (float)InputHelper.mY - 32.0f + OFFSET_Y, 32.0f, 32.0f, 64.0f, 64.0f, Settings.scale, Settings.scale, this.rotation, 0, 0, 64, 64, false, false);
                    break;
                }
                case INSPECT: {
                    sb.setColor(SHADOW_COLOR);
                    sb.draw(this.mImg, (float)InputHelper.mX - 32.0f - SHADOW_OFFSET_X + OFFSET_X, (float)InputHelper.mY - 32.0f - SHADOW_OFFSET_Y + OFFSET_Y, 32.0f, 32.0f, 64.0f, 64.0f, Settings.scale, Settings.scale, this.rotation, 0, 0, 64, 64, false, false);
                    sb.setColor(Color.WHITE);
                    sb.draw(this.mImg, (float)InputHelper.mX - 32.0f + OFFSET_X, (float)InputHelper.mY - 32.0f + OFFSET_Y, 32.0f, 32.0f, 64.0f, 64.0f, Settings.scale, Settings.scale, this.rotation, 0, 0, 64, 64, false, false);
                    break;
                }
            }
            this.changeType(CursorType.NORMAL);
        } else {
            this.color.a = MathHelper.slowColorLerpSnap(this.color.a, 0.0f);
            sb.setColor(this.color);
            sb.draw(ImageMaster.WOBBLY_ORB_VFX, (float)InputHelper.mX - 16.0f + OFFSET_X - 24.0f * Settings.scale, (float)InputHelper.mY - 16.0f + OFFSET_Y + 24.0f * Settings.scale, 16.0f, 16.0f, 32.0f, 32.0f, Settings.scale, Settings.scale, this.rotation, 0, 0, 32, 32, false, false);
        }
    }

    public static enum CursorType {
        NORMAL,
        INSPECT;

    }
}

