/*
 * Decompiled with CFR 0.152.
 */
package com.badlogic.gdx.backends.lwjgl;

import com.badlogic.gdx.Application;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Graphics;
import com.badlogic.gdx.backends.lwjgl.LwjglApplicationConfiguration;
import com.badlogic.gdx.backends.lwjgl.LwjglCursor;
import com.badlogic.gdx.backends.lwjgl.LwjglGL20;
import com.badlogic.gdx.backends.lwjgl.LwjglGL30;
import com.badlogic.gdx.graphics.Cursor;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.GL30;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.glutils.GLVersion;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.GdxRuntimeException;
import com.badlogic.gdx.utils.SharedLibraryLoader;
import java.awt.Canvas;
import java.awt.Toolkit;
import java.nio.ByteBuffer;
import org.lwjgl.LWJGLException;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.ContextAttribs;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.DisplayMode;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.PixelFormat;

public class LwjglGraphics
implements Graphics {
    static Array<String> extensions;
    static GLVersion glVersion;
    GL20 gl20;
    GL30 gl30;
    long frameId = -1L;
    float deltaTime = 0.0f;
    long frameStart = 0L;
    int frames = 0;
    int fps;
    long lastTime = System.nanoTime();
    Canvas canvas;
    boolean vsync = false;
    boolean resize = false;
    LwjglApplicationConfiguration config;
    Graphics.BufferFormat bufferFormat = new Graphics.BufferFormat(8, 8, 8, 8, 16, 8, 0, false);
    volatile boolean isContinuous = true;
    volatile boolean requestRendering = false;
    boolean softwareMode;
    boolean usingGL30;

    LwjglGraphics(LwjglApplicationConfiguration config) {
        this.config = config;
    }

    LwjglGraphics(Canvas canvas) {
        this.config = new LwjglApplicationConfiguration();
        this.config.width = canvas.getWidth();
        this.config.height = canvas.getHeight();
        this.canvas = canvas;
    }

    LwjglGraphics(Canvas canvas, LwjglApplicationConfiguration config) {
        this.config = config;
        this.canvas = canvas;
    }

    @Override
    public GL20 getGL20() {
        return this.gl20;
    }

    @Override
    public int getHeight() {
        if (this.canvas != null) {
            return Math.max(1, this.canvas.getHeight());
        }
        return (int)((float)Display.getHeight() * Display.getPixelScaleFactor());
    }

    @Override
    public int getWidth() {
        if (this.canvas != null) {
            return Math.max(1, this.canvas.getWidth());
        }
        return (int)((float)Display.getWidth() * Display.getPixelScaleFactor());
    }

    @Override
    public int getBackBufferWidth() {
        return this.getWidth();
    }

    @Override
    public int getBackBufferHeight() {
        return this.getHeight();
    }

    public boolean isGL20Available() {
        return this.gl20 != null;
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
        return this.deltaTime;
    }

    @Override
    public Graphics.GraphicsType getType() {
        return Graphics.GraphicsType.LWJGL;
    }

    @Override
    public GLVersion getGLVersion() {
        return glVersion;
    }

    @Override
    public int getFramesPerSecond() {
        return this.fps;
    }

    void updateTime() {
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

    void setupDisplay() throws LWJGLException {
        if (this.config.useHDPI) {
            System.setProperty("org.lwjgl.opengl.Display.enableHighDPI", "true");
        }
        if (this.canvas != null) {
            Display.setParent(this.canvas);
        } else {
            boolean displayCreated = false;
            if (!this.config.fullscreen) {
                displayCreated = this.setWindowedMode(this.config.width, this.config.height);
            } else {
                Graphics.DisplayMode bestMode = null;
                for (Graphics.DisplayMode mode : this.getDisplayModes()) {
                    if (mode.width != this.config.width || mode.height != this.config.height || bestMode != null && bestMode.refreshRate >= this.getDisplayMode().refreshRate) continue;
                    bestMode = mode;
                }
                if (bestMode == null) {
                    bestMode = this.getDisplayMode();
                }
                displayCreated = this.setFullscreenMode(bestMode);
            }
            if (!displayCreated) {
                if (this.config.setDisplayModeCallback != null) {
                    this.config = this.config.setDisplayModeCallback.onFailure(this.config);
                    if (this.config != null) {
                        displayCreated = this.setWindowedMode(this.config.width, this.config.height);
                    }
                }
                if (!displayCreated) {
                    throw new GdxRuntimeException("Couldn't set display mode " + this.config.width + "x" + this.config.height + ", fullscreen: " + this.config.fullscreen);
                }
            }
            if (this.config.iconPaths.size > 0) {
                ByteBuffer[] icons = new ByteBuffer[this.config.iconPaths.size];
                int n = this.config.iconPaths.size;
                for (int i = 0; i < n; ++i) {
                    Pixmap pixmap = new Pixmap(Gdx.files.getFileHandle(this.config.iconPaths.get(i), this.config.iconFileTypes.get(i)));
                    if (pixmap.getFormat() != Pixmap.Format.RGBA8888) {
                        Pixmap rgba = new Pixmap(pixmap.getWidth(), pixmap.getHeight(), Pixmap.Format.RGBA8888);
                        rgba.drawPixmap(pixmap, 0, 0);
                        pixmap.dispose();
                        pixmap = rgba;
                    }
                    icons[i] = ByteBuffer.allocateDirect(pixmap.getPixels().limit());
                    icons[i].put(pixmap.getPixels()).flip();
                    pixmap.dispose();
                }
                Display.setIcon(icons);
            }
        }
        Display.setTitle(this.config.title);
        Display.setResizable(this.config.resizable);
        Display.setInitialBackground(this.config.initialBackgroundColor.r, this.config.initialBackgroundColor.g, this.config.initialBackgroundColor.b);
        Display.setLocation(this.config.x, this.config.y);
        this.createDisplayPixelFormat(this.config.useGL30, this.config.gles30ContextMajorVersion, this.config.gles30ContextMinorVersion);
        this.initiateGL();
    }

    void initiateGL() {
        LwjglGraphics.extractVersion();
        LwjglGraphics.extractExtensions();
        this.initiateGLInstances();
    }

    private static void extractVersion() {
        String versionString = GL11.glGetString(7938);
        String vendorString = GL11.glGetString(7936);
        String rendererString = GL11.glGetString(7937);
        glVersion = new GLVersion(Application.ApplicationType.Desktop, versionString, vendorString, rendererString);
    }

    private static void extractExtensions() {
        extensions = new Array();
        if (glVersion.isVersionEqualToOrHigher(3, 2)) {
            int numExtensions = GL11.glGetInteger(33309);
            for (int i = 0; i < numExtensions; ++i) {
                extensions.add(org.lwjgl.opengl.GL30.glGetStringi(7939, i));
            }
        } else {
            extensions.addAll((String[])GL11.glGetString(7939).split(" "));
        }
    }

    private static boolean fullCompatibleWithGLES3() {
        return glVersion.isVersionEqualToOrHigher(4, 3);
    }

    private static boolean fullCompatibleWithGLES2() {
        return glVersion.isVersionEqualToOrHigher(4, 1) || extensions.contains("GL_ARB_ES2_compatibility", false);
    }

    private static boolean supportsFBO() {
        return glVersion.isVersionEqualToOrHigher(3, 0) || extensions.contains("GL_EXT_framebuffer_object", false) || extensions.contains("GL_ARB_framebuffer_object", false);
    }

    private void createDisplayPixelFormat(boolean useGL30, int gles30ContextMajor, int gles30ContextMinor) {
        block20: {
            try {
                if (useGL30) {
                    ContextAttribs context = new ContextAttribs(gles30ContextMajor, gles30ContextMinor).withForwardCompatible(false).withProfileCore(true);
                    try {
                        Display.create(new PixelFormat(this.config.r + this.config.g + this.config.b, this.config.a, this.config.depth, this.config.stencil, this.config.samples), context);
                    }
                    catch (Exception e) {
                        System.out.println("LwjglGraphics: OpenGL " + gles30ContextMajor + "." + gles30ContextMinor + "+ core profile (GLES 3.0) not supported.");
                        this.createDisplayPixelFormat(false, gles30ContextMajor, gles30ContextMinor);
                        return;
                    }
                    System.out.println("LwjglGraphics: created OpenGL " + gles30ContextMajor + "." + gles30ContextMinor + "+ core profile (GLES 3.0) context. This is experimental!");
                    this.usingGL30 = true;
                } else {
                    Display.create(new PixelFormat(this.config.r + this.config.g + this.config.b, this.config.a, this.config.depth, this.config.stencil, this.config.samples));
                    this.usingGL30 = false;
                }
                this.bufferFormat = new Graphics.BufferFormat(this.config.r, this.config.g, this.config.b, this.config.a, this.config.depth, this.config.stencil, this.config.samples, false);
            }
            catch (Exception ex) {
                Display.destroy();
                try {
                    Thread.sleep(200L);
                }
                catch (InterruptedException e) {
                    // empty catch block
                }
                try {
                    Display.create(new PixelFormat(0, 16, 8));
                    if (this.getDisplayMode().bitsPerPixel == 16) {
                        this.bufferFormat = new Graphics.BufferFormat(5, 6, 5, 0, 16, 8, 0, false);
                    }
                    if (this.getDisplayMode().bitsPerPixel == 24) {
                        this.bufferFormat = new Graphics.BufferFormat(8, 8, 8, 0, 16, 8, 0, false);
                    }
                    if (this.getDisplayMode().bitsPerPixel == 32) {
                        this.bufferFormat = new Graphics.BufferFormat(8, 8, 8, 8, 16, 8, 0, false);
                    }
                }
                catch (Exception ex2) {
                    Display.destroy();
                    try {
                        Thread.sleep(200L);
                    }
                    catch (InterruptedException interruptedException) {
                        // empty catch block
                    }
                    try {
                        Display.create(new PixelFormat());
                    }
                    catch (Exception ex3) {
                        if (!this.softwareMode && this.config.allowSoftwareMode) {
                            this.softwareMode = true;
                            System.setProperty("org.lwjgl.opengl.Display.allowSoftwareOpenGL", "true");
                            this.createDisplayPixelFormat(useGL30, gles30ContextMajor, gles30ContextMinor);
                            return;
                        }
                        throw new GdxRuntimeException("OpenGL is not supported by the video driver.", ex3);
                    }
                    if (this.getDisplayMode().bitsPerPixel == 16) {
                        this.bufferFormat = new Graphics.BufferFormat(5, 6, 5, 0, 8, 0, 0, false);
                    }
                    if (this.getDisplayMode().bitsPerPixel == 24) {
                        this.bufferFormat = new Graphics.BufferFormat(8, 8, 8, 0, 8, 0, 0, false);
                    }
                    if (this.getDisplayMode().bitsPerPixel != 32) break block20;
                    this.bufferFormat = new Graphics.BufferFormat(8, 8, 8, 8, 8, 0, 0, false);
                }
            }
        }
    }

    public void initiateGLInstances() {
        if (this.usingGL30) {
            this.gl30 = new LwjglGL30();
            this.gl20 = this.gl30;
        } else {
            this.gl20 = new LwjglGL20();
        }
        if (!glVersion.isVersionEqualToOrHigher(2, 0)) {
            throw new GdxRuntimeException("OpenGL 2.0 or higher with the FBO extension is required. OpenGL version: " + GL11.glGetString(7938) + "\n" + glVersion.getDebugVersionString());
        }
        if (!LwjglGraphics.supportsFBO()) {
            throw new GdxRuntimeException("OpenGL 2.0 or higher with the FBO extension is required. OpenGL version: " + GL11.glGetString(7938) + ", FBO extension: false\n" + glVersion.getDebugVersionString());
        }
        Gdx.gl = this.gl20;
        Gdx.gl20 = this.gl20;
        Gdx.gl30 = this.gl30;
    }

    @Override
    public float getPpiX() {
        return Toolkit.getDefaultToolkit().getScreenResolution();
    }

    @Override
    public float getPpiY() {
        return Toolkit.getDefaultToolkit().getScreenResolution();
    }

    @Override
    public float getPpcX() {
        return (float)Toolkit.getDefaultToolkit().getScreenResolution() / 2.54f;
    }

    @Override
    public float getPpcY() {
        return (float)Toolkit.getDefaultToolkit().getScreenResolution() / 2.54f;
    }

    @Override
    public float getDensity() {
        if (this.config.overrideDensity != -1) {
            return (float)this.config.overrideDensity / 160.0f;
        }
        return (float)Toolkit.getDefaultToolkit().getScreenResolution() / 160.0f;
    }

    @Override
    public boolean supportsDisplayModeChange() {
        return true;
    }

    @Override
    public Graphics.Monitor getPrimaryMonitor() {
        return new LwjglMonitor(0, 0, "Primary Monitor");
    }

    @Override
    public Graphics.Monitor getMonitor() {
        return this.getPrimaryMonitor();
    }

    @Override
    public Graphics.Monitor[] getMonitors() {
        return new Graphics.Monitor[]{this.getPrimaryMonitor()};
    }

    @Override
    public Graphics.DisplayMode[] getDisplayModes(Graphics.Monitor monitor) {
        return this.getDisplayModes();
    }

    @Override
    public Graphics.DisplayMode getDisplayMode(Graphics.Monitor monitor) {
        return this.getDisplayMode();
    }

    @Override
    public boolean setFullscreenMode(Graphics.DisplayMode displayMode) {
        DisplayMode mode = ((LwjglDisplayMode)displayMode).mode;
        try {
            if (!mode.isFullscreenCapable()) {
                Display.setDisplayMode(mode);
            } else {
                Display.setDisplayModeAndFullscreen(mode);
            }
            float scaleFactor = Display.getPixelScaleFactor();
            this.config.width = (int)((float)mode.getWidth() * scaleFactor);
            this.config.height = (int)((float)mode.getHeight() * scaleFactor);
            if (Gdx.gl != null) {
                Gdx.gl.glViewport(0, 0, this.config.width, this.config.height);
            }
            this.resize = true;
            return true;
        }
        catch (LWJGLException e) {
            return false;
        }
    }

    @Override
    public boolean setWindowedMode(int width, int height) {
        if (this.getWidth() == width && this.getHeight() == height && !Display.isFullscreen()) {
            return true;
        }
        try {
            DisplayMode targetDisplayMode = null;
            boolean fullscreen = false;
            if (fullscreen) {
                DisplayMode[] modes = Display.getAvailableDisplayModes();
                int freq = 0;
                for (int i = 0; i < modes.length; ++i) {
                    DisplayMode current = modes[i];
                    if (current.getWidth() != width || current.getHeight() != height) continue;
                    if (!(targetDisplayMode != null && current.getFrequency() < freq || targetDisplayMode != null && current.getBitsPerPixel() <= targetDisplayMode.getBitsPerPixel())) {
                        targetDisplayMode = current;
                        freq = targetDisplayMode.getFrequency();
                    }
                    if (current.getBitsPerPixel() != Display.getDesktopDisplayMode().getBitsPerPixel() || current.getFrequency() != Display.getDesktopDisplayMode().getFrequency()) continue;
                    targetDisplayMode = current;
                    break;
                }
            } else {
                targetDisplayMode = new DisplayMode(width, height);
            }
            if (targetDisplayMode == null) {
                return false;
            }
            boolean resizable = !fullscreen && this.config.resizable;
            Display.setDisplayMode(targetDisplayMode);
            Display.setFullscreen(fullscreen);
            if (resizable == Display.isResizable()) {
                Display.setResizable(!resizable);
            }
            Display.setResizable(resizable);
            float scaleFactor = Display.getPixelScaleFactor();
            this.config.width = (int)((float)targetDisplayMode.getWidth() * scaleFactor);
            this.config.height = (int)((float)targetDisplayMode.getHeight() * scaleFactor);
            if (Gdx.gl != null) {
                Gdx.gl.glViewport(0, 0, this.config.width, this.config.height);
            }
            this.resize = true;
            return true;
        }
        catch (LWJGLException e) {
            return false;
        }
    }

    @Override
    public Graphics.DisplayMode[] getDisplayModes() {
        try {
            DisplayMode[] availableDisplayModes = Display.getAvailableDisplayModes();
            Graphics.DisplayMode[] modes = new Graphics.DisplayMode[availableDisplayModes.length];
            int idx = 0;
            for (DisplayMode mode : availableDisplayModes) {
                if (!mode.isFullscreenCapable()) continue;
                modes[idx++] = new LwjglDisplayMode(mode.getWidth(), mode.getHeight(), mode.getFrequency(), mode.getBitsPerPixel(), mode);
            }
            return modes;
        }
        catch (LWJGLException e) {
            throw new GdxRuntimeException("Couldn't fetch available display modes", e);
        }
    }

    @Override
    public Graphics.DisplayMode getDisplayMode() {
        DisplayMode mode = Display.getDesktopDisplayMode();
        return new LwjglDisplayMode(mode.getWidth(), mode.getHeight(), mode.getFrequency(), mode.getBitsPerPixel(), mode);
    }

    @Override
    public void setTitle(String title) {
        Display.setTitle(title);
    }

    @Override
    public void setUndecorated(boolean undecorated) {
        System.setProperty("org.lwjgl.opengl.Window.undecorated", undecorated ? "true" : "false");
    }

    @Override
    public void setResizable(boolean resizable) {
        this.config.resizable = resizable;
        Display.setResizable(resizable);
    }

    @Override
    public Graphics.BufferFormat getBufferFormat() {
        return this.bufferFormat;
    }

    @Override
    public void setVSync(boolean vsync) {
        this.vsync = vsync;
        Display.setVSyncEnabled(vsync);
    }

    @Override
    public boolean supportsExtension(String extension) {
        return extensions.contains(extension, false);
    }

    @Override
    public void setContinuousRendering(boolean isContinuous) {
        this.isContinuous = isContinuous;
    }

    @Override
    public boolean isContinuousRendering() {
        return this.isContinuous;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    @Override
    public void requestRendering() {
        LwjglGraphics lwjglGraphics = this;
        synchronized (lwjglGraphics) {
            this.requestRendering = true;
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public boolean shouldRender() {
        LwjglGraphics lwjglGraphics = this;
        synchronized (lwjglGraphics) {
            boolean rq = this.requestRendering;
            this.requestRendering = false;
            return rq || this.isContinuous || Display.isDirty();
        }
    }

    @Override
    public boolean isFullscreen() {
        return Display.isFullscreen();
    }

    public boolean isSoftwareMode() {
        return this.softwareMode;
    }

    @Override
    public boolean isGL30Available() {
        return this.gl30 != null;
    }

    @Override
    public GL30 getGL30() {
        return this.gl30;
    }

    @Override
    public Cursor newCursor(Pixmap pixmap, int xHotspot, int yHotspot) {
        return new LwjglCursor(pixmap, xHotspot, yHotspot);
    }

    @Override
    public void setCursor(Cursor cursor) {
        if (this.canvas != null && SharedLibraryLoader.isMac) {
            return;
        }
        try {
            Mouse.setNativeCursor(((LwjglCursor)cursor).lwjglCursor);
        }
        catch (LWJGLException e) {
            throw new GdxRuntimeException("Could not set cursor image.", e);
        }
    }

    @Override
    public void setSystemCursor(Cursor.SystemCursor systemCursor) {
        if (this.canvas != null && SharedLibraryLoader.isMac) {
            return;
        }
        try {
            Mouse.setNativeCursor(null);
        }
        catch (LWJGLException e) {
            throw new GdxRuntimeException("Couldn't set system cursor");
        }
    }

    private class LwjglMonitor
    extends Graphics.Monitor {
        protected LwjglMonitor(int virtualX, int virtualY, String name) {
            super(virtualX, virtualY, name);
        }
    }

    private class LwjglDisplayMode
    extends Graphics.DisplayMode {
        DisplayMode mode;

        public LwjglDisplayMode(int width, int height, int refreshRate, int bitsPerPixel, DisplayMode mode) {
            super(width, height, refreshRate, bitsPerPixel);
            this.mode = mode;
        }
    }

    public static interface SetDisplayModeCallback {
        public LwjglApplicationConfiguration onFailure(LwjglApplicationConfiguration var1);
    }
}

