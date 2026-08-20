/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.badlogic.gdx.jnigen.AntScriptGenerator
 *  com.badlogic.gdx.jnigen.BuildConfig
 *  com.badlogic.gdx.jnigen.BuildTarget
 *  com.badlogic.gdx.jnigen.BuildTarget$TargetOs
 *  com.badlogic.gdx.jnigen.NativeCodeGenerator
 */
package com.badlogic.gdx.controllers.desktop;

import com.badlogic.gdx.jnigen.AntScriptGenerator;
import com.badlogic.gdx.jnigen.BuildConfig;
import com.badlogic.gdx.jnigen.BuildTarget;
import com.badlogic.gdx.jnigen.NativeCodeGenerator;

public class DesktopControllersBuild {
    public static void main(String[] args) throws Exception {
        new NativeCodeGenerator().generate("src/", "bin/", "jni/");
        BuildConfig buildConfig = new BuildConfig("gdx-controllers-desktop");
        String[] windowsSrc = new String[]{"*.cpp", "ois-v1-4svn/src/*.cpp", "ois-v1-4svn/src/win32/*.cpp"};
        String[] linuxSrc = new String[]{"*.cpp", "ois-v1-4svn/src/*.cpp", "ois-v1-4svn/src/linux/*.cpp"};
        String[] mac64Src = new String[]{"*.cpp", "ois-v1-4svn/src/*.cpp", "ois-v1-4svn/src/mac/*.mm", "ois-v1-4svn/src/mac/MacHIDManager.cpp", "ois-v1-4svn/src/mac/MacJoyStick.cpp"};
        String[] includes = new String[]{"ois-v1-4svn/includes", "dinput/"};
        BuildTarget win32home = BuildTarget.newDefaultTarget((BuildTarget.TargetOs)BuildTarget.TargetOs.Windows, (boolean)false);
        win32home.buildFileName = "build-windows32home.xml";
        win32home.excludeFromMasterBuildFile = true;
        win32home.is64Bit = false;
        win32home.compilerPrefix = "";
        win32home.cppIncludes = windowsSrc;
        win32home.headerDirs = includes;
        win32home.cIncludes = new String[0];
        win32home.libraries = "-ldinput8 -ldxguid";
        BuildTarget win32 = BuildTarget.newDefaultTarget((BuildTarget.TargetOs)BuildTarget.TargetOs.Windows, (boolean)false);
        win32.cppIncludes = windowsSrc;
        win32.headerDirs = includes;
        win32.libraries = "-ldinput8 -ldxguid";
        BuildTarget win64 = BuildTarget.newDefaultTarget((BuildTarget.TargetOs)BuildTarget.TargetOs.Windows, (boolean)true);
        win64.cppIncludes = windowsSrc;
        win64.headerDirs = includes;
        win64.libraries = "-ldinput8 -ldxguid";
        BuildTarget lin32 = BuildTarget.newDefaultTarget((BuildTarget.TargetOs)BuildTarget.TargetOs.Linux, (boolean)false);
        lin32.cppIncludes = linuxSrc;
        lin32.headerDirs = includes;
        lin32.libraries = "-lX11";
        BuildTarget lin64 = BuildTarget.newDefaultTarget((BuildTarget.TargetOs)BuildTarget.TargetOs.Linux, (boolean)true);
        lin64.cppIncludes = linuxSrc;
        lin64.headerDirs = includes;
        lin64.libraries = "-lX11";
        BuildTarget mac = BuildTarget.newDefaultTarget((BuildTarget.TargetOs)BuildTarget.TargetOs.MacOsX, (boolean)false);
        mac.cppIncludes = mac64Src;
        mac.headerDirs = includes;
        mac.cppFlags = mac.cppFlags + " -x objective-c++";
        mac.libraries = "-framework CoreServices -framework Carbon -framework IOKit -framework Cocoa";
        BuildTarget mac64 = BuildTarget.newDefaultTarget((BuildTarget.TargetOs)BuildTarget.TargetOs.MacOsX, (boolean)true);
        mac64.cppIncludes = mac64Src;
        mac64.headerDirs = includes;
        mac64.cppFlags = mac64.cppFlags + " -x objective-c++";
        mac64.libraries = "-framework CoreServices -framework Carbon -framework IOKit -framework Cocoa";
        new AntScriptGenerator().generate(buildConfig, new BuildTarget[]{win32home, win32, win64, lin32, lin64, mac, mac64});
    }
}

