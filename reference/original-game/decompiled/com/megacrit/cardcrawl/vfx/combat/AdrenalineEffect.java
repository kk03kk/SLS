/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.vfx.combat;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.helpers.ImageMaster;
import com.megacrit.cardcrawl.vfx.AbstractGameEffect;
import com.megacrit.cardcrawl.vfx.BorderFlashEffect;
import java.util.ArrayList;

public class AdrenalineEffect
extends AbstractGameEffect {
    private Vector2 position;
    private Vector2 velocity;
    private TextureAtlas.AtlasRegion img = null;
    private ArrayList<Vector2> prevPositions = new ArrayList();
    private static boolean flipper = true;

    public AdrenalineEffect() {
        this.img = ImageMaster.GLOW_SPARK_2;
        this.duration = 1.5f;
        this.position = flipper ? new Vector2(-100.0f * Settings.scale - (float)this.img.packedWidth / 2.0f, (float)Settings.HEIGHT / 2.0f - (float)this.img.packedHeight / 2.0f) : new Vector2(-50.0f * Settings.scale - (float)this.img.packedWidth / 2.0f, (float)Settings.HEIGHT / 2.0f - (float)this.img.packedHeight / 2.0f);
        flipper = !flipper;
        this.velocity = new Vector2(3000.0f * Settings.scale, 0.0f);
        this.color = new Color(1.0f, 1.0f, 0.2f, 1.0f);
        this.scale = 3.0f * Settings.scale;
    }

    @Override
    public void update() {
        if (this.duration == 1.5f) {
            CardCrawlGame.sound.playA("ATTACK_WHIFF_1", -0.6f);
            CardCrawlGame.sound.playA("ORB_LIGHTNING_CHANNEL", 0.6f);
            AbstractDungeon.effectsQueue.add(new BorderFlashEffect(Color.BLUE.cpy(), true));
        }
        if (this.position.x > (float)Settings.WIDTH * 0.55f && this.position.y > (float)Settings.HEIGHT / 2.0f - (float)this.img.packedHeight / 2.0f) {
            this.velocity.y = 0.0f;
            this.position.y = (float)Settings.HEIGHT / 2.0f - (float)this.img.packedHeight / 2.0f;
            this.velocity.x = 3000.0f * Settings.scale;
        } else if (this.position.x > (float)Settings.WIDTH * 0.5f) {
            this.velocity.y = 6000.0f * Settings.scale;
        } else if (this.position.x > (float)Settings.WIDTH * 0.4f) {
            this.velocity.y = -6000.0f * Settings.scale;
        } else if (this.position.x > (float)Settings.WIDTH * 0.35f) {
            this.velocity.y = 6000.0f * Settings.scale;
            this.velocity.x = 2000.0f * Settings.scale;
        }
        this.prevPositions.add(this.position.cpy());
        this.position.mulAdd(this.velocity, Gdx.graphics.getDeltaTime());
        if (this.prevPositions.size() > 30) {
            this.prevPositions.remove(0);
        }
        this.duration -= Gdx.graphics.getDeltaTime();
        if (this.duration < 0.0f) {
            this.isDone = true;
        }
    }

    @Override
    public void render(SpriteBatch sb) {
        sb.setBlendFunction(770, 1);
        for (int i = 0; i < this.prevPositions.size(); ++i) {
            sb.setColor(new Color(1.0f, 0.9f, 0.3f, 1.0f));
            sb.draw(this.img, this.prevPositions.get((int)i).x, this.prevPositions.get((int)i).y, (float)this.img.packedWidth / 2.0f, (float)this.img.packedHeight / 2.0f, this.img.packedWidth, this.img.packedHeight, this.scale / 8.0f * ((float)i * 0.05f + 1.0f) * MathUtils.random(1.5f, 3.0f), this.scale / 8.0f * ((float)i * 0.05f + 1.0f) * MathUtils.random(0.5f, 2.0f), 0.0f);
        }
        sb.setColor(Color.RED);
        sb.draw(this.img, this.position.x, this.position.y, (float)this.img.packedWidth / 2.0f, (float)this.img.packedHeight / 2.0f, this.img.packedWidth, this.img.packedHeight, this.scale * 2.5f, this.scale * 2.5f, 0.0f);
        sb.setBlendFunction(770, 771);
        sb.setColor(Color.YELLOW);
        sb.draw(this.img, this.position.x, this.position.y, (float)this.img.packedWidth / 2.0f, (float)this.img.packedHeight / 2.0f, this.img.packedWidth, this.img.packedHeight, this.scale, this.scale, 0.0f);
    }

    @Override
    public void dispose() {
    }
}

