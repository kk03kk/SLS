/*
 * Decompiled with CFR 0.152.
 */
package com.badlogic.gdx.graphics.glutils;

import com.badlogic.gdx.Application;
import com.badlogic.gdx.Gdx;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GLVersion {
    private int majorVersion;
    private int minorVersion;
    private int releaseVersion;
    private final String vendorString;
    private final String rendererString;
    private final Type type;
    private final String TAG = "GLVersion";

    public GLVersion(Application.ApplicationType appType, String versionString, String vendorString, String rendererString) {
        this.type = appType == Application.ApplicationType.Android ? Type.GLES : (appType == Application.ApplicationType.iOS ? Type.GLES : (appType == Application.ApplicationType.Desktop ? Type.OpenGL : (appType == Application.ApplicationType.Applet ? Type.OpenGL : (appType == Application.ApplicationType.WebGL ? Type.WebGL : Type.NONE))));
        if (this.type == Type.GLES) {
            this.extractVersion("OpenGL ES (\\d(\\.\\d){0,2})", versionString);
        } else if (this.type == Type.WebGL) {
            this.extractVersion("WebGL (\\d(\\.\\d){0,2})", versionString);
        } else if (this.type == Type.OpenGL) {
            this.extractVersion("(\\d(\\.\\d){0,2})", versionString);
        } else {
            this.majorVersion = -1;
            this.minorVersion = -1;
            this.releaseVersion = -1;
            vendorString = "";
            rendererString = "";
        }
        this.vendorString = vendorString;
        this.rendererString = rendererString;
    }

    private void extractVersion(String patternString, String versionString) {
        Pattern pattern = Pattern.compile(patternString);
        Matcher matcher = pattern.matcher(versionString);
        boolean found = matcher.find();
        if (found) {
            String result = matcher.group(1);
            String[] resultSplit = result.split("\\.");
            this.majorVersion = this.parseInt(resultSplit[0], 2);
            this.minorVersion = resultSplit.length < 2 ? 0 : this.parseInt(resultSplit[1], 0);
            this.releaseVersion = resultSplit.length < 3 ? 0 : this.parseInt(resultSplit[2], 0);
        } else {
            Gdx.app.log("GLVersion", "Invalid version string: " + versionString);
            this.majorVersion = 2;
            this.minorVersion = 0;
            this.releaseVersion = 0;
        }
    }

    private int parseInt(String v, int defaultValue) {
        try {
            return Integer.parseInt(v);
        }
        catch (NumberFormatException nfe) {
            Gdx.app.error("LibGDX GL", "Error parsing number: " + v + ", assuming: " + defaultValue);
            return defaultValue;
        }
    }

    public Type getType() {
        return this.type;
    }

    public int getMajorVersion() {
        return this.majorVersion;
    }

    public int getMinorVersion() {
        return this.minorVersion;
    }

    public int getReleaseVersion() {
        return this.releaseVersion;
    }

    public String getVendorString() {
        return this.vendorString;
    }

    public String getRendererString() {
        return this.rendererString;
    }

    public boolean isVersionEqualToOrHigher(int testMajorVersion, int testMinorVersion) {
        return this.majorVersion > testMajorVersion || this.majorVersion == testMajorVersion && this.minorVersion >= testMinorVersion;
    }

    public String getDebugVersionString() {
        return "Type: " + (Object)((Object)this.type) + "\nVersion: " + this.majorVersion + ":" + this.minorVersion + ":" + this.releaseVersion + "\nVendor: " + this.vendorString + "\nRenderer: " + this.rendererString;
    }

    public static enum Type {
        OpenGL,
        GLES,
        WebGL,
        NONE;

    }
}

