/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.vfx;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.megacrit.cardcrawl.vfx.AbstractGameEffect;
import com.megacrit.cardcrawl.vfx.FastDarkSmoke;
import java.util.ArrayList;

public class DarkSmokePuffEffect
extends AbstractGameEffect {
    private static final float DEFAULT_DURATION = 0.8f;
    private ArrayList<FastDarkSmoke> smoke = new ArrayList();

    public DarkSmokePuffEffect(float x, float y) {
        this.duration = 0.8f;
        for (int i = 0; i < 20; ++i) {
            this.smoke.add(new FastDarkSmoke(x, y));
        }
    }

    @Override
    public void update() {
        this.duration -= Gdx.graphics.getDeltaTime();
        if (this.duration < 0.0f) {
            this.isDone = true;
        } else if (this.duration < 0.7f) {
            this.killSmoke();
        }
        for (FastDarkSmoke b : this.smoke) {
            b.update();
        }
    }

    private void killSmoke() {
        for (FastDarkSmoke s : this.smoke) {
            s.kill();
        }
    }

    @Override
    public void render(SpriteBatch sb) {
        for (FastDarkSmoke b : this.smoke) {
            b.render(sb);
        }
    }

    @Override
    public void dispose() {
    }
}

