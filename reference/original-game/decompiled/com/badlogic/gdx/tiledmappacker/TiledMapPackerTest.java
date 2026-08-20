/*
 * Decompiled with CFR 0.152.
 */
package com.badlogic.gdx.tiledmappacker;

import com.badlogic.gdx.tiledmappacker.TiledMapPacker;
import java.io.File;

public class TiledMapPackerTest {
    public static void main(String[] args) throws Exception {
        String path = "../../tests/gdx-tests-android/assets/data/maps/";
        String input = path + "tiled-atlas-src";
        String output = path + "tiled-atlas-processed/deleteMe";
        String verboseOpt = "-v";
        String unused = "--strip-unused";
        String combine = "--combine-tilesets";
        String badOpt = "bad";
        File outputDir = new File(output);
        if (outputDir.exists()) {
            System.out.println("Please run TiledMapPackerTestRender or delete \"deleteMe\" folder located in");
            System.out.println("gdx-tests-android: assets/data/maps/tiled-atlas-processed/deleteMe");
            return;
        }
        TestType testType = TestType.DefaultUsage;
        String[] noArgs = new String[]{};
        String[] defaultUsage = new String[]{input, output};
        String[] verbose = new String[]{input, output, verboseOpt};
        String[] stripUnused = new String[]{input, output, unused};
        String[] combineTilesets = new String[]{input, output, combine};
        String[] unusedAndCombine = new String[]{input, output, unused, combine};
        String[] badOption = new String[]{input, output, unused, verboseOpt, combine, badOpt};
        switch (testType) {
            case NoArgs: {
                TiledMapPacker.main(noArgs);
                break;
            }
            case DefaultUsage: {
                TiledMapPacker.main(defaultUsage);
                break;
            }
            case Verbose: {
                TiledMapPacker.main(verbose);
                break;
            }
            case StripUnused: {
                TiledMapPacker.main(stripUnused);
                break;
            }
            case CombineTilesets: {
                TiledMapPacker.main(combineTilesets);
                break;
            }
            case UnusedAndCombine: {
                TiledMapPacker.main(unusedAndCombine);
                break;
            }
            case BadOption: {
                TiledMapPacker.main(badOption);
                break;
            }
        }
    }

    public static enum TestType {
        NoArgs,
        DefaultUsage,
        Verbose,
        StripUnused,
        CombineTilesets,
        UnusedAndCombine,
        BadOption;

    }
}

