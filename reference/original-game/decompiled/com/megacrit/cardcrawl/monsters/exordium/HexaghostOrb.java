/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.monsters.exordium;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.MathUtils;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.helpers.MathHelper;
import com.megacrit.cardcrawl.localization.MonsterStrings;
import com.megacrit.cardcrawl.vfx.BobEffect;
import com.megacrit.cardcrawl.vfx.GhostlyFireEffect;
import com.megacrit.cardcrawl.vfx.GhostlyWeakFireEffect;
import com.megacrit.cardcrawl.vfx.combat.GhostIgniteEffect;

public class HexaghostOrb {
    public static final String ID = "HexaghostOrb";
    private static final MonsterStrings monsterStrings = CardCrawlGame.languagePack.getMonsterStrings("HexaghostOrb");
    public static final String NAME = HexaghostOrb.monsterStrings.NAME;
    public static final String[] MOVES = HexaghostOrb.monsterStrings.MOVES;
    public static final String[] DIALOG = HexaghostOrb.monsterStrings.DIALOG;
    private BobEffect effect = new BobEffect(2.0f);
    private float activateTimer;
    public boolean activated = false;
    public boolean hidden = false;
    public boolean playedSfx = false;
    private Color color;
    private float x;
    private float y;
    private float particleTimer = 0.0f;
    private static final float PARTICLE_INTERVAL = 0.06f;

    public HexaghostOrb(float x, float y, int index) {
        this.x = x * Settings.scale + MathUtils.random(-10.0f, 10.0f) * Settings.scale;
        this.y = y * Settings.scale + MathUtils.random(-10.0f, 10.0f) * Settings.scale;
        this.activateTimer = (float)index * 0.3f;
        this.color = Color.CHARTREUSE.cpy();
        this.color.a = 0.0f;
        this.hidden = true;
    }

    public void activate(float oX, float oY) {
        this.playedSfx = false;
        this.activated = true;
        this.hidden = false;
    }

    public void deactivate() {
        this.activated = false;
    }

    public void hide() {
        this.hidden = true;
    }

    public void update(float oX, float oY) {
        if (!this.hidden) {
            if (this.activated) {
                this.activateTimer -= Gdx.graphics.getDeltaTime();
                if (this.activateTimer < 0.0f) {
                    if (!this.playedSfx) {
                        this.playedSfx = true;
                        AbstractDungeon.effectsQueue.add(new GhostIgniteEffect(this.x + oX, this.y + oY));
                        if (MathUtils.randomBoolean()) {
                            CardCrawlGame.sound.play("GHOST_ORB_IGNITE_1", 0.3f);
                        } else {
                            CardCrawlGame.sound.play("GHOST_ORB_IGNITE_2", 0.3f);
                        }
                    }
                    this.color.a = MathHelper.fadeLerpSnap(this.color.a, 1.0f);
                    this.effect.update();
                    this.effect.update();
                    this.particleTimer -= Gdx.graphics.getDeltaTime();
                    if (this.particleTimer < 0.0f) {
                        AbstractDungeon.effectList.add(new GhostlyFireEffect(this.x + oX + this.effect.y * 2.0f, this.y + oY + this.effect.y * 2.0f));
                        this.particleTimer = 0.06f;
                    }
                }
            } else {
                this.effect.update();
                this.particleTimer -= Gdx.graphics.getDeltaTime();
                if (this.particleTimer < 0.0f) {
                    AbstractDungeon.effectList.add(new GhostlyWeakFireEffect(this.x + oX + this.effect.y * 2.0f, this.y + oY + this.effect.y * 2.0f));
                    this.particleTimer = 0.06f;
                }
            }
        } else {
            this.color.a = MathHelper.fadeLerpSnap(this.color.a, 0.0f);
        }
    }
}

