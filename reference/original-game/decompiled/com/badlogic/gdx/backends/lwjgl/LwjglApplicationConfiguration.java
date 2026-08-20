/*
 * Decompiled with CFR 0.152.
 */
package com.badlogic.gdx.backends.lwjgl;

import com.badlogic.gdx.Files;
import com.badlogic.gdx.Graphics;
import com.badlogic.gdx.backends.lwjgl.LwjglGraphics;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.utils.Array;
import java.awt.DisplayMode;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.util.ArrayList;

public class LwjglApplicationConfiguration {
    public static boolean disableAudio;
    public boolean useGL30 = false;
    public int gles30ContextMajorVersion = 3;
    public int gles30ContextMinorVersion = 2;
    public int r = 8;
    public int g = 8;
    public int b = 8;
    public int a = 8;
    public int depth = 16;
    public int stencil = 0;
    public int samples = 0;
    public int width = 640;
    public int height = 480;
    public int x = -1;
    public int y = -1;
    public boolean fullscreen = false;
    public int overrideDensity = -1;
    public boolean vSyncEnabled = true;
    public String title;
    public boolean forceExit = true;
    public boolean resizable = true;
    public int audioDeviceSimultaneousSources = 16;
    public int audioDeviceBufferSize = 512;
    public int audioDeviceBufferCount = 9;
    public Color initialBackgroundColor = Color.BLACK;
    public int foregroundFPS = 60;
    public int backgroundFPS = 60;
    public boolean allowSoftwareMode = false;
    public String preferencesDirectory = ".prefs/";
    public Files.FileType preferencesFileType = Files.FileType.External;
    public LwjglGraphics.SetDisplayModeCallback setDisplayModeCallback;
    public boolean useHDPI = false;
    Array<String> iconPaths = new Array();
    Array<Files.FileType> iconFileTypes = new Array();

    public void addIcon(String path, Files.FileType fileType) {
        this.iconPaths.add(path);
        this.iconFileTypes.add(fileType);
    }

    public void setFromDisplayMode(Graphics.DisplayMode mode) {
        this.width = mode.width;
        this.height = mode.height;
        if (mode.bitsPerPixel == 16) {
            this.r = 5;
            this.g = 6;
            this.b = 5;
            this.a = 0;
        }
        if (mode.bitsPerPixel == 24) {
            this.r = 8;
            this.g = 8;
            this.b = 8;
            this.a = 0;
        }
        if (mode.bitsPerPixel == 32) {
            this.r = 8;
            this.g = 8;
            this.b = 8;
            this.a = 8;
        }
        this.fullscreen = true;
    }

    public static Graphics.DisplayMode getDesktopDisplayMode() {
        GraphicsEnvironment genv = GraphicsEnvironment.getLocalGraphicsEnvironment();
        GraphicsDevice device = genv.getDefaultScreenDevice();
        DisplayMode mode = device.getDisplayMode();
        return new LwjglApplicationConfigurationDisplayMode(mode.getWidth(), mode.getHeight(), mode.getRefreshRate(), mode.getBitDepth());
    }

    public static Graphics.DisplayMode[] getDisplayModes() {
        GraphicsEnvironment genv = GraphicsEnvironment.getLocalGraphicsEnvironment();
        GraphicsDevice device = genv.getDefaultScreenDevice();
        DisplayMode desktopMode = device.getDisplayMode();
        DisplayMode[] displayModes = device.getDisplayModes();
        ArrayList<LwjglApplicationConfigurationDisplayMode> modes = new ArrayList<LwjglApplicationConfigurationDisplayMode>();
        boolean idx = false;
        for (DisplayMode mode : displayModes) {
            boolean duplicate = false;
            for (int i = 0; i < modes.size(); ++i) {
                if (((Graphics.DisplayMode)modes.get((int)i)).width != mode.getWidth() || ((Graphics.DisplayMode)modes.get((int)i)).height != mode.getHeight() || ((Graphics.DisplayMode)modes.get((int)i)).bitsPerPixel != mode.getBitDepth()) continue;
                duplicate = true;
                break;
            }
            if (duplicate || mode.getBitDepth() != desktopMode.getBitDepth()) continue;
            modes.add(new LwjglApplicationConfigurationDisplayMode(mode.getWidth(), mode.getHeight(), mode.getRefreshRate(), mode.getBitDepth()));
        }
        return modes.toArray(new Graphics.DisplayMode[modes.size()]);
    }

    protected static class LwjglApplicationConfigurationDisplayMode
    extends Graphics.DisplayMode {
        protected LwjglApplicationConfigurationDisplayMode(int width, int height, int refreshRate, int bitsPerPixel) {
            super(width, height, refreshRate, bitsPerPixel);
        }
    }
}

