/*
 * Decompiled with CFR 0.152.
 */
package com.esotericsoftware.spine.utils;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.esotericsoftware.spine.AnimationState;
import com.esotericsoftware.spine.Skeleton;
import com.esotericsoftware.spine.SkeletonRenderer;

public class SkeletonActor
extends Actor {
    private SkeletonRenderer renderer;
    private Skeleton skeleton;
    AnimationState state;

    public SkeletonActor() {
    }

    public SkeletonActor(SkeletonRenderer renderer, Skeleton skeleton, AnimationState state) {
        this.renderer = renderer;
        this.skeleton = skeleton;
        this.state = state;
    }

    @Override
    public void act(float delta) {
        this.state.update(delta);
        this.state.apply(this.skeleton);
        this.skeleton.updateWorldTransform();
        super.act(delta);
    }

    @Override
    public void draw(Batch batch, float parentAlpha) {
        Color color = this.skeleton.getColor();
        float oldAlpha = color.a;
        this.skeleton.getColor().a *= parentAlpha;
        this.skeleton.setPosition(this.getX(), this.getY());
        this.renderer.draw(batch, this.skeleton);
        color.a = oldAlpha;
    }

    public SkeletonRenderer getRenderer() {
        return this.renderer;
    }

    public void setRenderer(SkeletonRenderer renderer) {
        this.renderer = renderer;
    }

    public Skeleton getSkeleton() {
        return this.skeleton;
    }

    public void setSkeleton(Skeleton skeleton) {
        this.skeleton = skeleton;
    }

    public AnimationState getAnimationState() {
        return this.state;
    }

    public void setAnimationState(AnimationState state) {
        this.state = state;
    }
}

