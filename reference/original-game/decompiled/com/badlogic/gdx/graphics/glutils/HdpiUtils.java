/*
 * Decompiled with CFR 0.152.
 */
package com.badlogic.gdx.graphics.glutils;

import com.badlogic.gdx.Gdx;

public class HdpiUtils {
    public static void glScissor(int x, int y, int width, int height) {
        if (Gdx.graphics.getWidth() != Gdx.graphics.getBackBufferWidth() || Gdx.graphics.getHeight() != Gdx.graphics.getBackBufferHeight()) {
            Gdx.gl.glScissor(HdpiUtils.toBackBufferX(x), HdpiUtils.toBackBufferY(y), HdpiUtils.toBackBufferX(width), HdpiUtils.toBackBufferY(height));
        } else {
            Gdx.gl.glScissor(x, y, width, height);
        }
    }

    public static void glViewport(int x, int y, int width, int height) {
        if (Gdx.graphics.getWidth() != Gdx.graphics.getBackBufferWidth() || Gdx.graphics.getHeight() != Gdx.graphics.getBackBufferHeight()) {
            Gdx.gl.glViewport(HdpiUtils.toBackBufferX(x), HdpiUtils.toBackBufferY(y), HdpiUtils.toBackBufferX(width), HdpiUtils.toBackBufferY(height));
        } else {
            Gdx.gl.glViewport(x, y, width, height);
        }
    }

    public static int toLogicalX(int backBufferX) {
        return (int)((float)(backBufferX * Gdx.graphics.getWidth()) / (float)Gdx.graphics.getBackBufferWidth());
    }

    public static int toLogicalY(int backBufferY) {
        return (int)((float)(backBufferY * Gdx.graphics.getHeight()) / (float)Gdx.graphics.getBackBufferHeight());
    }

    public static int toBackBufferX(int logicalX) {
        return (int)((float)(logicalX * Gdx.graphics.getBackBufferWidth()) / (float)Gdx.graphics.getWidth());
    }

    public static int toBackBufferY(int logicalY) {
        return (int)((float)(logicalY * Gdx.graphics.getBackBufferHeight()) / (float)Gdx.graphics.getHeight());
    }
}

