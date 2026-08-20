/*
 * Decompiled with CFR 0.152.
 */
package com.badlogic.gdx.backends.headless.mock.graphics;

import com.badlogic.gdx.Application;
import com.badlogic.gdx.Graphics;
import com.badlogic.gdx.graphics.Cursor;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.GL30;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.glutils.GLVersion;

public class MockGraphics
implements Graphics {
    long frameId = -1L;
    float deltaTime = 0.0f;
    long frameStart = 0L;
    int frames = 0;
    int fps;
    long lastTime = System.nanoTime();
    GLVersion glVersion = new GLVersion(Application.ApplicationType.HeadlessDesktop, "", "", "");

    @Override
    public boolean isGL30Available() {
        return false;
    }

    @Override
    public GL20 getGL20() {
        return null;
    }

    @Override
    public GL30 getGL30() {
        return null;
    }

    @Override
    public int getWidth() {
        return 0;
    }

    @Override
    public int getHeight() {
        return 0;
    }

    @Override
    public int getBackBufferWidth() {
        return 0;
    }

    @Override
    public int getBackBufferHeight() {
        return 0;
    }

    @Override
    public long getFrameId() {
        return this.frameId;
    }

    @Override
    public float getDeltaTime() {
        return this.deltaTime;
    }

    @Override
    public float getRawDeltaTime() {
        return 0.0f;
    }

    @Override
    public int getFramesPerSecond() {
        return 0;
    }

    @Override
    public Graphics.GraphicsType getType() {
        return Graphics.GraphicsType.Mock;
    }

    @Override
    public GLVersion getGLVersion() {
        return this.glVersion;
    }

    @Override
    public float getPpiX() {
        return 0.0f;
    }

    @Override
    public float getPpiY() {
        return 0.0f;
    }

    @Override
    public float getPpcX() {
        return 0.0f;
    }

    @Override
    public float getPpcY() {
        return 0.0f;
    }

    @Override
    public float getDensity() {
        return 0.0f;
    }

    @Override
    public boolean supportsDisplayModeChange() {
        return false;
    }

    @Override
    public Graphics.DisplayMode[] getDisplayModes() {
        return new Graphics.DisplayMode[0];
    }

    @Override
    public Graphics.DisplayMode getDisplayMode() {
        return null;
    }

    @Override
    public boolean setFullscreenMode(Graphics.DisplayMode displayMode) {
        return false;
    }

    @Override
    public boolean setWindowedMode(int width, int height) {
        return false;
    }

    @Override
    public void setTitle(String title) {
    }

    @Override
    public void setVSync(boolean vsync) {
    }

    @Override
    public Graphics.BufferFormat getBufferFormat() {
        return null;
    }

    @Override
    public boolean supportsExtension(String extension) {
        return false;
    }

    @Override
    public void setContinuousRendering(boolean isContinuous) {
    }

    @Override
    public boolean isContinuousRendering() {
        return false;
    }

    @Override
    public void requestRendering() {
    }

    @Override
    public boolean isFullscreen() {
        return false;
    }

    public void updateTime() {
        long time = System.nanoTime();
        this.deltaTime = (float)(time - this.lastTime) / 1.0E9f;
        this.lastTime = time;
        if (time - this.frameStart >= 1000000000L) {
            this.fps = this.frames;
            this.frames = 0;
            this.frameStart = time;
        }
        ++this.frames;
    }

    public void incrementFrameId() {
        ++this.frameId;
    }

    @Override
    public Cursor newCursor(Pixmap pixmap, int xHotspot, int yHotspot) {
        return null;
    }

    @Override
    public void setCursor(Cursor cursor) {
    }

    @Override
    public void setSystemCursor(Cursor.SystemCursor systemCursor) {
    }

    @Override
    public Graphics.Monitor getPrimaryMonitor() {
        return null;
    }

    @Override
    public Graphics.Monitor getMonitor() {
        return null;
    }

    @Override
    public Graphics.Monitor[] getMonitors() {
        return null;
    }

    @Override
    public Graphics.DisplayMode[] getDisplayModes(Graphics.Monitor monitor) {
        return null;
    }

    @Override
    public Graphics.DisplayMode getDisplayMode(Graphics.Monitor monitor) {
        return null;
    }

    @Override
    public void setUndecorated(boolean undecorated) {
    }

    @Override
    public void setResizable(boolean resizable) {
    }
}

