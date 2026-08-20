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
package com.badlogic.gdx.graphics.g2d.freetype;

import com.badlogic.gdx.jnigen.AntScriptGenerator;
import com.badlogic.gdx.jnigen.BuildConfig;
import com.badlogic.gdx.jnigen.BuildTarget;
import com.badlogic.gdx.jnigen.NativeCodeGenerator;

public class FreetypeBuild {
    public static void main(String[] args) throws Exception {
        String[] headers = new String[]{"freetype-2.6.2/include"};
        String[] sources = new String[]{"freetype-2.6.2/src/base/ftsystem.c", "freetype-2.6.2/src/base/ftinit.c", "freetype-2.6.2/src/base/ftdebug.c", "freetype-2.6.2/src/base/ftbase.c", "freetype-2.6.2/src/base/ftbbox.c", "freetype-2.6.2/src/base/ftglyph.c", "freetype-2.6.2/src/base/ftbdf.c", "freetype-2.6.2/src/base/ftbitmap.c", "freetype-2.6.2/src/base/ftcid.c", "freetype-2.6.2/src/base/ftfstype.c", "freetype-2.6.2/src/base/ftgasp.c", "freetype-2.6.2/src/base/ftgxval.c", "freetype-2.6.2/src/base/ftlcdfil.c", "freetype-2.6.2/src/base/ftmm.c", "freetype-2.6.2/src/base/ftotval.c", "freetype-2.6.2/src/base/ftpatent.c", "freetype-2.6.2/src/base/ftpfr.c", "freetype-2.6.2/src/base/ftstroke.c", "freetype-2.6.2/src/base/ftsynth.c", "freetype-2.6.2/src/base/fttype1.c", "freetype-2.6.2/src/base/ftwinfnt.c", "freetype-2.6.2/src/base/ftxf86.c", "freetype-2.6.2/src/bdf/bdf.c", "freetype-2.6.2/src/cff/cff.c", "freetype-2.6.2/src/cid/type1cid.c", "freetype-2.6.2/src/pcf/pcf.c", "freetype-2.6.2/src/pfr/pfr.c", "freetype-2.6.2/src/sfnt/sfnt.c", "freetype-2.6.2/src/truetype/truetype.c", "freetype-2.6.2/src/type1/type1.c", "freetype-2.6.2/src/type42/type42.c", "freetype-2.6.2/src/winfonts/winfnt.c", "freetype-2.6.2/src/raster/raster.c", "freetype-2.6.2/src/smooth/smooth.c", "freetype-2.6.2/src/autofit/autofit.c", "freetype-2.6.2/src/cache/ftcache.c", "freetype-2.6.2/src/gzip/ftgzip.c", "freetype-2.6.2/src/lzw/ftlzw.c", "freetype-2.6.2/src/bzip2/ftbzip2.c", "freetype-2.6.2/src/gxvalid/gxvalid.c", "freetype-2.6.2/src/otvalid/otvalid.c", "freetype-2.6.2/src/psaux/psaux.c", "freetype-2.6.2/src/pshinter/pshinter.c", "freetype-2.6.2/src/psnames/psnames.c"};
        BuildTarget win32home = BuildTarget.newDefaultTarget((BuildTarget.TargetOs)BuildTarget.TargetOs.Windows, (boolean)false);
        win32home.compilerPrefix = "";
        win32home.buildFileName = "build-windows32home.xml";
        win32home.excludeFromMasterBuildFile = true;
        win32home.headerDirs = headers;
        win32home.cIncludes = sources;
        win32home.cFlags = win32home.cFlags + " -std=c99 -DFT2_BUILD_LIBRARY";
        win32home.cppFlags = win32home.cppFlags + " -std=c99 -DFT2_BUILD_LIBRARY";
        BuildTarget win32 = BuildTarget.newDefaultTarget((BuildTarget.TargetOs)BuildTarget.TargetOs.Windows, (boolean)false);
        win32.headerDirs = headers;
        win32.cIncludes = sources;
        win32.cFlags = win32.cFlags + " -std=c99 -DFT2_BUILD_LIBRARY";
        win32.cppFlags = win32.cppFlags + " -std=c99 -DFT2_BUILD_LIBRARY";
        BuildTarget win64 = BuildTarget.newDefaultTarget((BuildTarget.TargetOs)BuildTarget.TargetOs.Windows, (boolean)true);
        win64.headerDirs = headers;
        win64.cIncludes = sources;
        win64.cFlags = win64.cFlags + " -std=c99 -DFT2_BUILD_LIBRARY";
        win64.cppFlags = win64.cppFlags + " -std=c99 -DFT2_BUILD_LIBRARY";
        BuildTarget lin32 = BuildTarget.newDefaultTarget((BuildTarget.TargetOs)BuildTarget.TargetOs.Linux, (boolean)false);
        lin32.headerDirs = headers;
        lin32.cIncludes = sources;
        lin32.cFlags = lin32.cFlags + " -std=c99 -DFT2_BUILD_LIBRARY";
        lin32.cppFlags = lin32.cppFlags + " -std=c99 -DFT2_BUILD_LIBRARY";
        BuildTarget lin64 = BuildTarget.newDefaultTarget((BuildTarget.TargetOs)BuildTarget.TargetOs.Linux, (boolean)true);
        lin64.headerDirs = headers;
        lin64.cIncludes = sources;
        lin64.cFlags = lin64.cFlags + " -std=c99 -DFT2_BUILD_LIBRARY";
        lin64.cppFlags = lin64.cppFlags + " -std=c99 -DFT2_BUILD_LIBRARY";
        BuildTarget mac = BuildTarget.newDefaultTarget((BuildTarget.TargetOs)BuildTarget.TargetOs.MacOsX, (boolean)false);
        mac.headerDirs = headers;
        mac.cIncludes = sources;
        mac.cFlags = mac.cFlags + " -DFT2_BUILD_LIBRARY";
        mac.cppFlags = mac.cppFlags + " -DFT2_BUILD_LIBRARY";
        mac.linkerFlags = mac.linkerFlags + " -framework CoreServices -framework Carbon";
        BuildTarget mac64 = BuildTarget.newDefaultTarget((BuildTarget.TargetOs)BuildTarget.TargetOs.MacOsX, (boolean)true);
        mac64.headerDirs = headers;
        mac64.cIncludes = sources;
        mac64.cFlags = mac64.cFlags + " -DFT2_BUILD_LIBRARY";
        mac64.cppFlags = mac64.cppFlags + " -DFT2_BUILD_LIBRARY";
        mac64.linkerFlags = mac64.linkerFlags + " -framework CoreServices -framework Carbon";
        BuildTarget android = BuildTarget.newDefaultTarget((BuildTarget.TargetOs)BuildTarget.TargetOs.Android, (boolean)false);
        android.headerDirs = headers;
        android.cIncludes = sources;
        android.cFlags = android.cFlags + " -std=c99 -DFT2_BUILD_LIBRARY";
        android.cppFlags = android.cppFlags + " -std=c99 -DFT2_BUILD_LIBRARY";
        BuildTarget ios = BuildTarget.newDefaultTarget((BuildTarget.TargetOs)BuildTarget.TargetOs.IOS, (boolean)false);
        ios.headerDirs = headers;
        ios.cIncludes = sources;
        ios.cFlags = ios.cFlags + " -DFT2_BUILD_LIBRARY";
        ios.cppFlags = ios.cppFlags + " -DFT2_BUILD_LIBRARY";
        new NativeCodeGenerator().generate();
        new AntScriptGenerator().generate(new BuildConfig("gdx-freetype"), new BuildTarget[]{win32home, win32, win64, lin32, lin64, mac, mac64, android, ios});
    }
}

