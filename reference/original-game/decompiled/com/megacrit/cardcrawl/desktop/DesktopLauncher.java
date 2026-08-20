/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.desktop;

import com.badlogic.gdx.ApplicationListener;
import com.badlogic.gdx.Files;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.lwjgl.LwjglApplication;
import com.badlogic.gdx.backends.lwjgl.LwjglApplicationConfiguration;
import com.badlogic.gdx.utils.GdxNativesLoader;
import com.badlogic.gdx.utils.SharedLibraryLoader;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.core.DisplayConfig;
import com.megacrit.cardcrawl.core.ExceptionHandler;
import com.megacrit.cardcrawl.core.SystemStats;
import com.megacrit.cardcrawl.desktop.STSDisplayModeCallback;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileAttribute;
import java.util.Locale;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class DesktopLauncher {
    private static final Logger logger;

    static void OverrideLibraryLoadingFix() {
        if (!System.getProperty("os.name").toLowerCase().contains("windows")) {
            return;
        }
        Path normalPath = Paths.get(System.getProperty("java.io.tmpdir"), "libgdx", System.getProperty("user.name"));
        if (DesktopLauncher.Is7bitAscii(normalPath.toAbsolutePath().toString())) {
            return;
        }
        System.out.println("Detected invalid path: " + normalPath);
        GdxNativesLoader.disableNativesLoading = true;
        Path lib = Paths.get(System.getProperty("user.dir"), "lib").toAbsolutePath();
        System.out.println("Loading libs extracted to custom path: " + lib);
        System.setProperty("org.lwjgl.librarypath", lib.toString());
        System.setProperty("com.codedisaster.steamworks.SharedLibraryExtractPath", lib.toString());
        System.setProperty("com.codedisaster.steamworks.SDKLibraryPath", lib.toString());
        try {
            Files.createDirectories(lib, new FileAttribute[0]);
            SharedLibraryLoader loader = new SharedLibraryLoader();
            loader.extractFileTo(SharedLibraryLoader.is64Bit ? "gdx64.dll" : "gdx.dll", new File(lib.toString()));
            loader.extractFileTo(SharedLibraryLoader.is64Bit ? "lwjgl64.dll" : "lwjgl.dll", new File(lib.toString()));
            if (!LwjglApplicationConfiguration.disableAudio) {
                loader.extractFileTo(SharedLibraryLoader.is64Bit ? "OpenAL64.dll" : "OpenAL32.dll", new File(lib.toString()));
            }
        }
        catch (IOException e) {
            logger.info("Exception occurred while initializing application!");
            ExceptionHandler.handleException(e, logger);
            Gdx.app.exit();
        }
        System.load(Paths.get(lib.toString(), "gdx64.dll").toAbsolutePath().toString());
    }

    public static boolean Is7bitAscii(String str) {
        for (char c : str.toCharArray()) {
            if (c <= '\u007f') continue;
            return false;
        }
        return true;
    }

    public static void main(String[] arg) {
        logger.info("time: " + System.currentTimeMillis());
        logger.info("version: " + CardCrawlGame.TRUE_VERSION_NUM);
        logger.info("libgdx:  1.9.5");
        logger.info("default_locale: " + Locale.getDefault());
        logger.info("default_charset: " + Charset.defaultCharset());
        logger.info("default_encoding: " + System.getProperty("file.encoding"));
        logger.info("java_version: " + System.getProperty("java.version"));
        logger.info("os_arch: " + System.getProperty("os.arch"));
        logger.info("os_name: " + System.getProperty("os.name"));
        logger.info("os_version: " + System.getProperty("os.version"));
        SystemStats.logMemoryStats();
        SystemStats.logDiskStats();
        try {
            LwjglApplicationConfiguration config = new LwjglApplicationConfiguration();
            config.setDisplayModeCallback = new STSDisplayModeCallback();
            config.addIcon("images/ui/icon.png", Files.FileType.Internal);
            config.resizable = false;
            config.title = "Slay the Spire";
            DesktopLauncher.loadSettings(config);
            logger.info("Launching application...");
            new LwjglApplication((ApplicationListener)new CardCrawlGame(config.preferencesDirectory), config);
        }
        catch (Exception e) {
            logger.info("Exception occurred while initializing application!");
            ExceptionHandler.handleException(e, logger);
            Gdx.app.exit();
        }
    }

    private static void loadSettings(LwjglApplicationConfiguration config) {
        DisplayConfig displayConf = DisplayConfig.readConfig();
        if (displayConf.getWidth() < 800 || displayConf.getHeight() < 450) {
            logger.info("[ERROR] Display Config set lower than minimum allowed, resetting config.");
            config.width = 1280;
            config.height = 720;
            DisplayConfig.writeDisplayConfigFile(1280, 720, displayConf.getMaxFPS(), displayConf.getIsFullscreen(), displayConf.getWFS(), displayConf.getIsVsync());
        } else {
            config.height = displayConf.getHeight();
            config.width = displayConf.getWidth();
        }
        config.backgroundFPS = config.foregroundFPS = displayConf.getMaxFPS();
        if (displayConf.getIsFullscreen()) {
            logger.info("[FULLSCREEN_MODE]");
            System.setProperty("org.lwjgl.opengl.Display.enableOSXFullscreenModeAPI", "true");
            System.setProperty("org.lwjgl.opengl.Window.undecorated", "false");
            config.fullscreen = true;
            config.height = displayConf.getHeight();
            config.width = displayConf.getWidth();
            logger.info("Running the game in: " + config.width + " x " + config.height);
        } else {
            config.fullscreen = false;
            if (displayConf.getWFS() && config.width == LwjglApplicationConfiguration.getDesktopDisplayMode().width && config.height == LwjglApplicationConfiguration.getDesktopDisplayMode().height) {
                logger.info("[BORDERLESS_FULLSCREEN_MODE]");
                System.setProperty("org.lwjgl.opengl.Window.undecorated", "true");
                config.width = LwjglApplicationConfiguration.getDesktopDisplayMode().width;
                config.height = LwjglApplicationConfiguration.getDesktopDisplayMode().height;
                logger.info("Running the game in: " + config.width + " x " + config.height);
            } else {
                logger.info("[WINDOWED_MODE]");
            }
        }
        if (config.fullscreen && (displayConf.getWidth() > LwjglApplicationConfiguration.getDesktopDisplayMode().width || displayConf.getHeight() > LwjglApplicationConfiguration.getDesktopDisplayMode().height)) {
            logger.info("[ERROR] Monitor resolution is lower than config, resetting config.");
            config.width = LwjglApplicationConfiguration.getDesktopDisplayMode().width;
            config.height = LwjglApplicationConfiguration.getDesktopDisplayMode().height;
            DisplayConfig.writeDisplayConfigFile(config.width, config.height, displayConf.getMaxFPS(), false, false, displayConf.getIsVsync());
        }
        config.vSyncEnabled = displayConf.getIsVsync();
        logger.info("Settings successfully loaded");
    }

    static {
        System.setProperty("log4j.configurationFile", "log4j2.xml");
        DesktopLauncher.OverrideLibraryLoadingFix();
        logger = LogManager.getLogger(DesktopLauncher.class.getName());
    }
}

