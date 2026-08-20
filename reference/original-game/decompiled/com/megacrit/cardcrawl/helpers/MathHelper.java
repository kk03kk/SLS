/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.helpers;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.math.MathUtils;
import com.megacrit.cardcrawl.core.Settings;

public class MathHelper {
    public static float cardLerpSnap(float startX, float targetX) {
        if (startX != targetX && Math.abs((startX = MathUtils.lerp(startX, targetX, Gdx.graphics.getDeltaTime() * 6.0f)) - targetX) < Settings.CARD_SNAP_THRESHOLD) {
            startX = targetX;
        }
        return startX;
    }

    public static float cardScaleLerpSnap(float startX, float targetX) {
        if (startX != targetX && Math.abs((startX = MathUtils.lerp(startX, targetX, Gdx.graphics.getDeltaTime() * 7.5f)) - targetX) < 0.003f) {
            startX = targetX;
        }
        return startX;
    }

    public static float uiLerpSnap(float startX, float targetX) {
        if (startX != targetX && Math.abs((startX = MathUtils.lerp(startX, targetX, Gdx.graphics.getDeltaTime() * 9.0f)) - targetX) < Settings.UI_SNAP_THRESHOLD) {
            startX = targetX;
        }
        return startX;
    }

    public static float orbLerpSnap(float startX, float targetX) {
        if (startX != targetX && Math.abs((startX = MathUtils.lerp(startX, targetX, Gdx.graphics.getDeltaTime() * 6.0f)) - targetX) < Settings.UI_SNAP_THRESHOLD) {
            startX = targetX;
        }
        return startX;
    }

    public static float mouseLerpSnap(float startX, float targetX) {
        if (startX != targetX && Math.abs((startX = MathUtils.lerp(startX, targetX, Gdx.graphics.getDeltaTime() * 20.0f)) - targetX) < Settings.UI_SNAP_THRESHOLD) {
            startX = targetX;
        }
        return startX;
    }

    public static float scaleLerpSnap(float startX, float targetX) {
        if (startX != targetX && Math.abs((startX = MathUtils.lerp(startX, targetX, Gdx.graphics.getDeltaTime() * 8.0f)) - targetX) < 0.003f) {
            startX = targetX;
        }
        return startX;
    }

    public static float fadeLerpSnap(float startX, float targetX) {
        if (startX != targetX && Math.abs((startX = MathUtils.lerp(startX, targetX, Gdx.graphics.getDeltaTime() * 12.0f)) - targetX) < 0.01f) {
            startX = targetX;
        }
        return startX;
    }

    public static float popLerpSnap(float startX, float targetX) {
        if (startX != targetX && Math.abs((startX = MathUtils.lerp(startX, targetX, Gdx.graphics.getDeltaTime() * 8.0f)) - targetX) < 0.003f) {
            startX = targetX;
        }
        return startX;
    }

    public static float angleLerpSnap(float startX, float targetX) {
        if (startX != targetX && Math.abs((startX = MathUtils.lerp(startX, targetX, Gdx.graphics.getDeltaTime() * 12.0f)) - targetX) < 0.003f) {
            startX = targetX;
        }
        return startX;
    }

    public static float slowColorLerpSnap(float startX, float targetX) {
        if (startX != targetX && Math.abs((startX = MathUtils.lerp(startX, targetX, Gdx.graphics.getDeltaTime() * 3.0f)) - targetX) < 0.01f) {
            startX = targetX;
        }
        return startX;
    }

    public static float scrollSnapLerpSpeed(float startX, float targetX) {
        if (startX != targetX && Math.abs((startX = MathUtils.lerp(startX, targetX, Gdx.graphics.getDeltaTime() * 10.0f)) - targetX) < Settings.UI_SNAP_THRESHOLD) {
            startX = targetX;
        }
        return startX;
    }

    public static float valueFromPercentBetween(float min, float max, float percent) {
        float diff = max - min;
        return min + diff * percent;
    }

    public static float percentFromValueBetween(float min, float max, float value) {
        float diff = max - min;
        float offset = value - min;
        return offset / diff;
    }
}

