/*
 * Decompiled with CFR 0.152.
 */
package com.badlogic.gdx;

import com.badlogic.gdx.ApplicationListener;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;

public abstract class Game
implements ApplicationListener {
    protected Screen screen;

    @Override
    public void dispose() {
        if (this.screen != null) {
            this.screen.hide();
        }
    }

    @Override
    public void pause() {
        if (this.screen != null) {
            this.screen.pause();
        }
    }

    @Override
    public void resume() {
        if (this.screen != null) {
            this.screen.resume();
        }
    }

    @Override
    public void render() {
        if (this.screen != null) {
            this.screen.render(Gdx.graphics.getDeltaTime());
        }
    }

    @Override
    public void resize(int width, int height) {
        if (this.screen != null) {
            this.screen.resize(width, height);
        }
    }

    public void setScreen(Screen screen) {
        if (this.screen != null) {
            this.screen.hide();
        }
        this.screen = screen;
        if (this.screen != null) {
            this.screen.show();
            this.screen.resize(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        }
    }

    public Screen getScreen() {
        return this.screen;
    }
}

