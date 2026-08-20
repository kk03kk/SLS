/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.vfx.deprecated;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.esotericsoftware.spine.AnimationState;
import com.esotericsoftware.spine.AnimationStateData;
import com.esotericsoftware.spine.Skeleton;
import com.esotericsoftware.spine.SkeletonData;
import com.esotericsoftware.spine.SkeletonJson;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.helpers.input.InputHelper;
import com.megacrit.cardcrawl.vfx.AbstractGameEffect;

public class AnimationTestEffect
extends AbstractGameEffect {
    public float duration = 3.0f;
    public float startingDuration;
    protected Color color;
    protected float scale = 1.0f;
    protected float rotation = 0.0f;
    public boolean renderBehind = false;
    TextureAtlas atlas = new TextureAtlas(Gdx.files.internal("animations/skeleton.atlas"));
    Skeleton skeleton;
    AnimationState state;

    public AnimationTestEffect() {
        SkeletonJson json = new SkeletonJson(this.atlas);
        json.setScale(Settings.scale / 2.0f);
        SkeletonData skeletonData = json.readSkeletonData(Gdx.files.internal("animations/skeleton.json"));
        this.skeleton = new Skeleton(skeletonData);
        this.skeleton.setPosition(1250.0f, 20.0f);
        AnimationStateData stateData = new AnimationStateData(skeletonData);
        this.state = new AnimationState(stateData);
        this.state.setAnimation(0, "animation", true);
    }

    @Override
    public void update() {
        this.skeleton.setPosition(InputHelper.mX, InputHelper.mY);
        this.duration -= Gdx.graphics.getDeltaTime();
        if (this.duration < 0.0f) {
            this.atlas.dispose();
            this.isDone = true;
        }
    }

    @Override
    public void render(SpriteBatch sb) {
    }

    @Override
    public void dispose() {
        this.atlas.dispose();
    }
}

