/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.desktop;

import com.badlogic.gdx.tools.texturepacker.TexturePacker;

public class MyTexturePacker {
    public static void main(String[] arg) {
        MyTexturePacker.packTextures();
    }

    private static void packTextures() {
        TexturePacker.Settings settings = new TexturePacker.Settings();
        settings.maxWidth = 2048;
        settings.maxHeight = 2048;
        System.out.println("Done!");
    }
}

