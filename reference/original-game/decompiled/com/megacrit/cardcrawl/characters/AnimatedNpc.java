/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.characters;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.esotericsoftware.spine.AnimationState;
import com.esotericsoftware.spine.AnimationStateData;
import com.esotericsoftware.spine.Skeleton;
import com.esotericsoftware.spine.SkeletonData;
import com.esotericsoftware.spine.SkeletonJson;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.helpers.HeartAnimListener;

public class AnimatedNpc {
    private TextureAtlas atlas = null;
    public Skeleton skeleton;
    private AnimationState state;
    private AnimationStateData stateData;

    public AnimatedNpc(float x, float y, String atlasUrl, String skeletonUrl, String trackName) {
        this.loadAnimation(atlasUrl, skeletonUrl, 1.0f);
        this.skeleton.setPosition(x, y);
        this.state.setAnimation(0, trackName, true);
        this.state.setTimeScale(1.0f);
    }

    private void loadAnimation(String atlasUrl, String skeletonUrl, float scale) {
        this.atlas = new TextureAtlas(Gdx.files.internal(atlasUrl));
        SkeletonJson json = new SkeletonJson(this.atlas);
        json.setScale(Settings.renderScale / scale);
        SkeletonData skeletonData = json.readSkeletonData(Gdx.files.internal(skeletonUrl));
        this.skeleton = new Skeleton(skeletonData);
        this.skeleton.setColor(Color.WHITE);
        this.stateData = new AnimationStateData(skeletonData);
        this.state = new AnimationState(this.stateData);
    }

    public void render(SpriteBatch sb) {
        this.state.update(Gdx.graphics.getDeltaTime());
        this.state.apply(this.skeleton);
        this.skeleton.updateWorldTransform();
        this.skeleton.setFlip(false, false);
        this.skeleton.setColor(Color.WHITE);
        sb.end();
        CardCrawlGame.psb.begin();
        AbstractCreature.sr.draw(CardCrawlGame.psb, this.skeleton);
        CardCrawlGame.psb.end();
        sb.begin();
        sb.setBlendFunction(770, 771);
    }

    public void render(SpriteBatch sb, Color color) {
        this.state.update(Gdx.graphics.getDeltaTime());
        this.state.apply(this.skeleton);
        this.skeleton.updateWorldTransform();
        this.skeleton.setFlip(false, false);
        this.skeleton.setColor(color);
        sb.end();
        CardCrawlGame.psb.begin();
        AbstractCreature.sr.draw(CardCrawlGame.psb, this.skeleton);
        CardCrawlGame.psb.end();
        sb.begin();
        sb.setBlendFunction(770, 771);
    }

    public void dispose() {
        this.atlas.dispose();
    }

    public void setTimeScale(float setScale) {
        this.state.setTimeScale(setScale);
    }

    public void addListener(HeartAnimListener listener) {
        this.state.addListener(listener);
    }
}

