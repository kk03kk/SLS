/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.math.MathUtils;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.helpers.ImageMaster;
import com.megacrit.cardcrawl.vfx.DoorFlashEffect;

public class DoorLock {
    private Color glowColor = Color.WHITE.cpy();
    private Texture lockImg = null;
    private Texture glowImg = null;
    private boolean glowing;
    private boolean unlockAnimation = false;
    private boolean usedFlash = false;
    private float x = 0.0f;
    private float y;
    private float unlockTimer = 2.0f;
    private float startY;
    private float targetY;
    private LockColor c;

    public DoorLock(LockColor c, boolean glowing, boolean eventVersion) {
        this.c = c;
        this.glowing = glowing;
        this.startY = eventVersion ? -48.0f * Settings.scale : 0.0f * Settings.scale;
        this.y = this.startY;
        switch (c) {
            case RED: {
                this.lockImg = ImageMaster.loadImage("images/ui/door/lock_red.png");
                this.glowImg = ImageMaster.loadImage("images/ui/door/glow_red.png");
                glowing = CardCrawlGame.playerPref.getBoolean(AbstractPlayer.PlayerClass.IRONCLAD.name() + "_WIN", false);
                if (eventVersion) {
                    this.targetY = -748.0f * Settings.scale;
                    break;
                }
                this.targetY = -700.0f * Settings.scale;
                break;
            }
            case GREEN: {
                this.lockImg = ImageMaster.loadImage("images/ui/door/lock_green.png");
                this.glowImg = ImageMaster.loadImage("images/ui/door/glow_green.png");
                glowing = CardCrawlGame.playerPref.getBoolean(AbstractPlayer.PlayerClass.THE_SILENT.name() + "_WIN", false);
                if (eventVersion) {
                    this.targetY = 752.0f * Settings.scale;
                    break;
                }
                this.targetY = 800.0f * Settings.scale;
                break;
            }
            case BLUE: {
                this.lockImg = ImageMaster.loadImage("images/ui/door/lock_blue.png");
                this.glowImg = ImageMaster.loadImage("images/ui/door/glow_blue.png");
                glowing = CardCrawlGame.playerPref.getBoolean(AbstractPlayer.PlayerClass.DEFECT.name() + "_WIN", false);
                if (eventVersion) {
                    this.targetY = -748.0f * Settings.scale;
                    break;
                }
                this.targetY = -700.0f * Settings.scale;
                break;
            }
        }
    }

    public void update() {
        this.updateUnlockAnimation();
    }

    private void updateUnlockAnimation() {
        if (this.unlockAnimation) {
            this.unlockTimer -= Gdx.graphics.getDeltaTime();
            if (this.unlockTimer < 0.0f) {
                this.unlockTimer = 0.0f;
                this.unlockAnimation = false;
            }
            switch (this.c) {
                case RED: {
                    this.x = Interpolation.pow5In.apply(-1000.0f * Settings.scale, 0.0f, this.unlockTimer / 2.0f);
                    this.y = Interpolation.pow5In.apply(this.targetY, this.startY, this.unlockTimer / 2.0f);
                    break;
                }
                case GREEN: {
                    this.y = Interpolation.pow5In.apply(800.0f * Settings.scale, this.startY, this.unlockTimer / 2.0f);
                    break;
                }
                case BLUE: {
                    this.x = Interpolation.pow5In.apply(1000.0f * Settings.scale, 0.0f, this.unlockTimer / 2.0f);
                    this.y = Interpolation.pow5In.apply(this.targetY, this.startY, this.unlockTimer / 2.0f);
                    break;
                }
            }
        }
    }

    public void unlock() {
        this.unlockAnimation = true;
        this.unlockTimer = 2.0f;
        this.x = 0.0f;
        this.y = this.startY;
    }

    public void render(SpriteBatch sb) {
        if (this.lockImg == null) {
            return;
        }
        this.renderLock(sb);
        this.renderGlow(sb);
    }

    private void renderLock(SpriteBatch sb) {
        sb.setColor(Color.WHITE);
        sb.draw(this.lockImg, (float)Settings.WIDTH / 2.0f - 960.0f + this.x, (float)Settings.HEIGHT / 2.0f - 600.0f + this.y, 960.0f, 600.0f, 1920.0f, 1200.0f, Settings.scale, Settings.scale, 0.0f, 0, 0, 1920, 1200, false, false);
    }

    private void renderGlow(SpriteBatch sb) {
        if (this.glowing) {
            this.glowColor.a = (MathUtils.cosDeg(System.currentTimeMillis() / 4L % 360L) + 3.0f) / 4.0f;
            sb.setColor(this.glowColor);
            sb.setBlendFunction(770, 1);
            sb.draw(this.glowImg, (float)Settings.WIDTH / 2.0f - 960.0f + this.x, (float)Settings.HEIGHT / 2.0f - 600.0f + this.y, 960.0f, 600.0f, 1920.0f, 1200.0f, Settings.scale, Settings.scale, 0.0f, 0, 0, 1920, 1200, false, false);
            sb.setBlendFunction(770, 771);
        }
    }

    public void reset() {
        this.usedFlash = false;
        this.unlockAnimation = false;
        this.x = 0.0f;
        this.y = this.startY;
        this.unlockTimer = 2.0f;
    }

    public void flash(boolean eventVersion) {
        if (!this.usedFlash) {
            CardCrawlGame.sound.playA("ATTACK_MAGIC_SLOW_2", 1.0f);
            this.usedFlash = true;
            CardCrawlGame.mainMenuScreen.doorUnlockScreen.effects.add(new DoorFlashEffect(this.glowImg, eventVersion));
        }
    }

    public void dispose() {
        this.lockImg.dispose();
        this.glowImg.dispose();
    }

    public static enum LockColor {
        RED,
        GREEN,
        BLUE;

    }
}

