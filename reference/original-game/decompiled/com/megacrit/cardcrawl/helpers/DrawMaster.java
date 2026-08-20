/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.helpers;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.megacrit.cardcrawl.helpers.AbstractDrawable;
import com.megacrit.cardcrawl.helpers.Label;
import com.megacrit.cardcrawl.helpers.Sprite;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class DrawMaster {
    public static List<AbstractDrawable> drawList = new ArrayList<AbstractDrawable>();

    public static void draw(SpriteBatch sb) {
        Collections.sort(drawList);
        sb.setColor(Color.WHITE);
        for (AbstractDrawable o : drawList) {
            o.render(sb);
        }
        drawList.clear();
    }

    public static void queue(Texture img, float x, float y, int z, Color color) {
        drawList.add(new Sprite(img, x, y, z, color));
    }

    public static void queue(Texture img, float x, float y, int z, float scale, Color color) {
        drawList.add(new Sprite(img, x, y, z, scale, color));
    }

    public static void queue(Texture img, float x, float y, int z, float scale, float rotation, Color color) {
        drawList.add(new Sprite(img, x, y, z, scale, rotation, color));
    }

    public static void queue(BitmapFont font, String label, float x, float y, int z, float scale, Color color) {
        drawList.add(new Label(font, label, x, y, z, scale, color));
    }
}

