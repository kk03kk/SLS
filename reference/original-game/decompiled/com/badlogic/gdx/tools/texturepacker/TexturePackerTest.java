/*
 * Decompiled with CFR 0.152.
 */
package com.badlogic.gdx.tools.texturepacker;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.lwjgl.LwjglApplication;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.tools.texturepacker.MaxRectsPacker;
import com.badlogic.gdx.tools.texturepacker.TexturePacker;
import com.badlogic.gdx.utils.Array;
import java.util.Random;

public class TexturePackerTest
extends ApplicationAdapter {
    ShapeRenderer renderer;
    Array<TexturePacker.Page> pages;

    @Override
    public void create() {
        this.renderer = new ShapeRenderer();
    }

    @Override
    public void render() {
        Gdx.gl.glClear(16384);
        TexturePacker.Settings settings = new TexturePacker.Settings();
        settings.fast = false;
        settings.pot = false;
        settings.maxWidth = 1024;
        settings.maxHeight = 1024;
        settings.rotation = false;
        settings.paddingX = 0;
        if (this.pages == null) {
            TexturePacker.Rect rect;
            int i;
            Random random = new Random(1243L);
            Array<TexturePacker.Rect> inputRects = new Array<TexturePacker.Rect>();
            for (i = 0; i < 240; ++i) {
                rect = new TexturePacker.Rect();
                rect.name = "rect" + i;
                rect.height = 16 + random.nextInt(120);
                rect.width = 16 + random.nextInt(240);
                inputRects.add(rect);
            }
            for (i = 0; i < 10; ++i) {
                rect = new TexturePacker.Rect();
                rect.name = "rect" + (40 + i);
                rect.height = 400 + random.nextInt(340);
                rect.width = 1 + random.nextInt(10);
                inputRects.add(rect);
            }
            long s = System.nanoTime();
            this.pages = new MaxRectsPacker(settings).pack(inputRects);
            long e = System.nanoTime();
            System.out.println("fast: " + settings.fast);
            System.out.println((float)(e - s) / 1000000.0f + " ms");
            System.out.println();
        }
        int x = 20;
        int y = 20;
        for (TexturePacker.Page page : this.pages) {
            TexturePacker.Rect rect;
            int i;
            this.renderer.setColor(Color.GRAY);
            this.renderer.begin(ShapeRenderer.ShapeType.Filled);
            for (i = 0; i < page.outputRects.size; ++i) {
                rect = page.outputRects.get(i);
                this.renderer.rect(x + rect.x + settings.paddingX, y + rect.y + settings.paddingY, rect.width - settings.paddingX, rect.height - settings.paddingY);
            }
            this.renderer.end();
            this.renderer.setColor(Color.RED);
            this.renderer.begin(ShapeRenderer.ShapeType.Line);
            for (i = 0; i < page.outputRects.size; ++i) {
                rect = page.outputRects.get(i);
                this.renderer.rect(x + rect.x + settings.paddingX, y + rect.y + settings.paddingY, rect.width - settings.paddingX, rect.height - settings.paddingY);
            }
            this.renderer.setColor(Color.GREEN);
            this.renderer.rect(x, y, page.width + settings.paddingX * 2, page.height + settings.paddingY * 2);
            this.renderer.end();
            x += page.width + 20;
        }
    }

    @Override
    public void resize(int width, int height) {
        this.renderer.setProjectionMatrix(new Matrix4().setToOrtho2D(0.0f, 0.0f, Gdx.graphics.getWidth(), Gdx.graphics.getHeight()));
    }

    public static void main(String[] args) throws Exception {
        new LwjglApplication(new TexturePackerTest(), "", 640, 480);
    }
}

