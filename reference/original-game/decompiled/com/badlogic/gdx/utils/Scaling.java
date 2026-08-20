/*
 * Decompiled with CFR 0.152.
 */
package com.badlogic.gdx.utils;

import com.badlogic.gdx.math.Vector2;

public enum Scaling {
    fit,
    fill,
    fillX,
    fillY,
    stretch,
    stretchX,
    stretchY,
    none;

    private static final Vector2 temp;

    public Vector2 apply(float sourceWidth, float sourceHeight, float targetWidth, float targetHeight) {
        switch (this) {
            case fit: {
                float targetRatio = targetHeight / targetWidth;
                float sourceRatio = sourceHeight / sourceWidth;
                float scale = targetRatio > sourceRatio ? targetWidth / sourceWidth : targetHeight / sourceHeight;
                Scaling.temp.x = sourceWidth * scale;
                Scaling.temp.y = sourceHeight * scale;
                break;
            }
            case fill: {
                float targetRatio = targetHeight / targetWidth;
                float sourceRatio = sourceHeight / sourceWidth;
                float scale = targetRatio < sourceRatio ? targetWidth / sourceWidth : targetHeight / sourceHeight;
                Scaling.temp.x = sourceWidth * scale;
                Scaling.temp.y = sourceHeight * scale;
                break;
            }
            case fillX: {
                float scale = targetWidth / sourceWidth;
                Scaling.temp.x = sourceWidth * scale;
                Scaling.temp.y = sourceHeight * scale;
                break;
            }
            case fillY: {
                float scale = targetHeight / sourceHeight;
                Scaling.temp.x = sourceWidth * scale;
                Scaling.temp.y = sourceHeight * scale;
                break;
            }
            case stretch: {
                Scaling.temp.x = targetWidth;
                Scaling.temp.y = targetHeight;
                break;
            }
            case stretchX: {
                Scaling.temp.x = targetWidth;
                Scaling.temp.y = sourceHeight;
                break;
            }
            case stretchY: {
                Scaling.temp.x = sourceWidth;
                Scaling.temp.y = targetHeight;
                break;
            }
            case none: {
                Scaling.temp.x = sourceWidth;
                Scaling.temp.y = sourceHeight;
            }
        }
        return temp;
    }

    static {
        temp = new Vector2();
    }
}

