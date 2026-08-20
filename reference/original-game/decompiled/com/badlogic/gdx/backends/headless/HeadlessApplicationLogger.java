/*
 * Decompiled with CFR 0.152.
 */
package com.badlogic.gdx.backends.headless;

import com.badlogic.gdx.ApplicationLogger;

public class HeadlessApplicationLogger
implements ApplicationLogger {
    @Override
    public void log(String tag, String message) {
        System.out.println(tag + ": " + message);
    }

    @Override
    public void log(String tag, String message, Throwable exception) {
        System.out.println(tag + ": " + message);
        exception.printStackTrace(System.out);
    }

    @Override
    public void error(String tag, String message) {
        System.err.println(tag + ": " + message);
    }

    @Override
    public void error(String tag, String message, Throwable exception) {
        System.err.println(tag + ": " + message);
        exception.printStackTrace(System.err);
    }

    @Override
    public void debug(String tag, String message) {
        System.out.println(tag + ": " + message);
    }

    @Override
    public void debug(String tag, String message, Throwable exception) {
        System.out.println(tag + ": " + message);
        exception.printStackTrace(System.out);
    }
}

