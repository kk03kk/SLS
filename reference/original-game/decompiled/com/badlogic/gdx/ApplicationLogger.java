/*
 * Decompiled with CFR 0.152.
 */
package com.badlogic.gdx;

public interface ApplicationLogger {
    public void log(String var1, String var2);

    public void log(String var1, String var2, Throwable var3);

    public void error(String var1, String var2);

    public void error(String var1, String var2, Throwable var3);

    public void debug(String var1, String var2);

    public void debug(String var1, String var2, Throwable var3);
}

