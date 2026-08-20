/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.vfx;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.helpers.ImageMaster;
import com.megacrit.cardcrawl.screens.DungeonMapScreen;

public class MapDot {
    private float x;
    private float y;
    private float rotation;
    private static final int RAW_W = 16;
    private static final float DIST_JITTER = 4.0f * Settings.scale;
    private static final float OFFSET_Y = 172.0f * Settings.scale;

    public MapDot(float x, float y, float rotation, boolean jitter) {
        if (jitter) {
            this.x = x + MathUtils.random(-DIST_JITTER, DIST_JITTER);
            this.y = y + MathUtils.random(-DIST_JITTER, DIST_JITTER);
            this.rotation = rotation + MathUtils.random(-20.0f, 20.0f);
        } else {
            this.x = x;
            this.y = y;
            this.rotation = rotation;
        }
    }

    public void render(SpriteBatch sb) {
        sb.draw(ImageMaster.MAP_DOT_1, this.x - 8.0f, this.y - 8.0f + DungeonMapScreen.offsetY + OFFSET_Y, 8.0f, 8.0f, 16.0f, 16.0f, Settings.scale, Settings.scale, this.rotation, 0, 0, 16, 16, false, false);
    }
}

