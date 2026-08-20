/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.vfx;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.ui.DialogWord;
import com.megacrit.cardcrawl.vfx.AbstractGameEffect;
import com.megacrit.cardcrawl.vfx.CloudBubble;
import com.megacrit.cardcrawl.vfx.SpeechTextEffect;
import java.util.ArrayList;

public class ThoughtBubble
extends AbstractGameEffect {
    private static final float DEFAULT_DURATION = 2.0f;
    private static final float OFFSET_X = 190.0f * Settings.scale;
    private static final float OFFSET_Y = 124.0f * Settings.scale;
    private static final float CLOUD_W = 100.0f * Settings.scale;
    private static final float CLOUD_H = 50.0f * Settings.scale;
    private ArrayList<CloudBubble> bubbles = new ArrayList();

    public ThoughtBubble(float x, float y, String msg, boolean isPlayer) {
        this(x, y, 2.0f, msg, isPlayer);
    }

    public ThoughtBubble(float x, float y, float duration, String msg, boolean isPlayer) {
        if (msg == null) {
            this.isDone = true;
            return;
        }
        for (AbstractGameEffect e : AbstractDungeon.effectList) {
            if (!(e instanceof ThoughtBubble) || e.equals(this)) continue;
            ((ThoughtBubble)e).killClouds();
        }
        this.duration = duration;
        x = isPlayer ? (x += OFFSET_X) : (x -= OFFSET_X);
        AbstractDungeon.effectsQueue.add(new SpeechTextEffect(x, y += OFFSET_Y, duration, msg, DialogWord.AppearEffect.BUMP_IN));
        this.bubbles.add(new CloudBubble(x + CLOUD_W * MathUtils.random(0.7f, 1.1f), y + CLOUD_H * MathUtils.random(0.1f, 0.3f), MathUtils.random(1.0f, 1.2f)));
        this.bubbles.add(new CloudBubble(x - CLOUD_W * MathUtils.random(0.7f, 1.1f), y + CLOUD_H * MathUtils.random(0.1f, 0.3f), MathUtils.random(1.0f, 1.2f)));
        this.bubbles.add(new CloudBubble(x + CLOUD_W * MathUtils.random(0.7f, 1.1f), y + CLOUD_H * MathUtils.random(-0.1f, -0.3f), MathUtils.random(0.9f, 1.1f)));
        this.bubbles.add(new CloudBubble(x - CLOUD_W * MathUtils.random(0.7f, 1.1f), y + CLOUD_H * MathUtils.random(-0.1f, -0.3f), MathUtils.random(0.9f, 1.1f)));
        this.bubbles.add(new CloudBubble(x + CLOUD_W * MathUtils.random(-0.2f, 0.2f), y + CLOUD_H * MathUtils.random(0.65f, 0.72f), MathUtils.random(0.9f, 1.1f)));
        this.bubbles.add(new CloudBubble(x + CLOUD_W * MathUtils.random(-0.2f, 0.2f), y - CLOUD_H * MathUtils.random(0.65f, 0.72f), MathUtils.random(1.0f, 1.2f)));
        this.bubbles.add(new CloudBubble(x + CLOUD_W * MathUtils.random(0.3f, 0.8f), y + CLOUD_H * MathUtils.random(0.3f, 0.7f), MathUtils.random(0.9f, 1.1f)));
        this.bubbles.add(new CloudBubble(x - CLOUD_W * MathUtils.random(0.3f, 0.8f), y + CLOUD_H * MathUtils.random(0.3f, 0.7f), MathUtils.random(0.9f, 1.1f)));
        this.bubbles.add(new CloudBubble(x + CLOUD_W * MathUtils.random(0.3f, 0.8f), y - CLOUD_H * MathUtils.random(0.3f, 0.7f), MathUtils.random(0.9f, 1.1f)));
        this.bubbles.add(new CloudBubble(x - CLOUD_W * MathUtils.random(0.3f, 0.8f), y - CLOUD_H * MathUtils.random(0.3f, 0.7f), MathUtils.random(0.9f, 1.1f)));
        float off_x = isPlayer ? OFFSET_X : -OFFSET_X;
        this.bubbles.add(new CloudBubble(x - off_x * MathUtils.random(-0.05f, -0.15f), y - OFFSET_Y * MathUtils.random(0.67f, 0.72f), MathUtils.random(0.4f, 0.45f)));
        this.bubbles.add(new CloudBubble(x - off_x * MathUtils.random(0.07f, 0.15f), y - OFFSET_Y * MathUtils.random(0.65f, 0.7f), MathUtils.random(0.4f, 0.45f)));
        this.bubbles.add(new CloudBubble(x - off_x * MathUtils.random(0.1f, 0.2f), y - OFFSET_Y * MathUtils.random(0.9f, 1.02f), MathUtils.random(0.35f, 0.4f)));
        this.bubbles.add(new CloudBubble(x - off_x * MathUtils.random(0.3f, 0.35f), y - OFFSET_Y * MathUtils.random(1.05f, 1.1f), MathUtils.random(0.18f, 0.23f)));
        this.bubbles.add(new CloudBubble(x - off_x * MathUtils.random(0.35f, 0.45f), y - OFFSET_Y * MathUtils.random(1.1f, 1.2f), MathUtils.random(0.1f, 0.13f)));
        this.bubbles.add(new CloudBubble(x - off_x * MathUtils.random(0.45f, 0.5f), y - OFFSET_Y * MathUtils.random(1.1f, 1.16f), MathUtils.random(0.08f, 0.09f)));
        this.bubbles.add(new CloudBubble(x - off_x * MathUtils.random(0.5f, 0.6f), y - OFFSET_Y * MathUtils.random(1.1f, 1.16f), MathUtils.random(0.08f, 0.09f)));
        this.bubbles.add(new CloudBubble(x - off_x * MathUtils.random(0.6f, 0.65f), y - OFFSET_Y * MathUtils.random(1.05f, 1.12f), MathUtils.random(0.08f, 0.09f)));
    }

    @Override
    public void update() {
        this.duration -= Gdx.graphics.getDeltaTime();
        if (this.duration < 0.0f) {
            this.isDone = true;
        } else if (this.duration < 0.3f) {
            this.killClouds();
        }
        for (CloudBubble b : this.bubbles) {
            b.update();
        }
    }

    private void killClouds() {
        for (CloudBubble b : this.bubbles) {
            b.kill();
        }
    }

    @Override
    public void render(SpriteBatch sb) {
        for (CloudBubble b : this.bubbles) {
            b.render(sb);
        }
    }

    @Override
    public void dispose() {
    }
}

