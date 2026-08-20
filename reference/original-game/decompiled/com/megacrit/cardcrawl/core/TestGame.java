/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.core;

import com.badlogic.gdx.ApplicationListener;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.viewport.FitViewport;

public class TestGame
implements ApplicationListener {
    private OrthographicCamera camera = new OrthographicCamera();
    public static FitViewport viewport;
    private SpriteBatch sb;
    private Texture texture;

    @Override
    public void create() {
        viewport = new FitViewport(1600.0f, 900.0f, this.camera);
        viewport.apply();
        this.sb = new SpriteBatch();
        this.texture = new Texture("images/whiteScreen.png");
    }

    @Override
    public void render() {
        this.sb.begin();
        this.sb.setColor(Color.RED);
        this.sb.draw(this.texture, 0.0f, 0.0f, 1600.0f, 900.0f);
        this.sb.end();
    }

    @Override
    public void resize(int width, int height) {
        viewport.update(width, height);
        this.camera.update();
    }

    @Override
    public void pause() {
    }

    @Override
    public void resume() {
    }

    @Override
    public void dispose() {
    }
}

