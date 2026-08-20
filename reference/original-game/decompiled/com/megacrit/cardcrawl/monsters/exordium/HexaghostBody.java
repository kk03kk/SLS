/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.monsters.exordium;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.Disposable;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.helpers.ImageMaster;
import com.megacrit.cardcrawl.helpers.MathHelper;
import com.megacrit.cardcrawl.localization.MonsterStrings;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import com.megacrit.cardcrawl.vfx.BobEffect;

public class HexaghostBody
implements Disposable {
    public static final String ID = "HexaghostBody";
    private static final MonsterStrings monsterStrings = CardCrawlGame.languagePack.getMonsterStrings("HexaghostBody");
    public static final String NAME = HexaghostBody.monsterStrings.NAME;
    public static final String[] MOVES = HexaghostBody.monsterStrings.MOVES;
    public static final String[] DIALOG = HexaghostBody.monsterStrings.DIALOG;
    private float rotationSpeed = 1.0f;
    public float targetRotationSpeed = 30.0f;
    private BobEffect effect = new BobEffect(0.75f);
    private AbstractMonster m;
    private static final String IMG_DIR = "images/monsters/theBottom/boss/ghost/";
    private static final int W = 512;
    private Texture plasma1;
    private Texture plasma2;
    private Texture plasma3;
    private Texture shadow;
    private float plasma1Angle = 0.0f;
    private float plasma2Angle = 0.0f;
    private float plasma3Angle = 0.0f;
    private static final float BODY_OFFSET_Y = 256.0f * Settings.scale;

    public HexaghostBody(AbstractMonster m) {
        this.m = m;
        this.plasma1 = ImageMaster.loadImage("images/monsters/theBottom/boss/ghost/plasma1.png");
        this.plasma2 = ImageMaster.loadImage("images/monsters/theBottom/boss/ghost/plasma2.png");
        this.plasma3 = ImageMaster.loadImage("images/monsters/theBottom/boss/ghost/plasma3.png");
        this.shadow = ImageMaster.loadImage("images/monsters/theBottom/boss/ghost/shadow.png");
    }

    public void update() {
        this.effect.update();
        this.plasma1Angle += this.rotationSpeed * Gdx.graphics.getDeltaTime();
        this.plasma2Angle += this.rotationSpeed / 2.0f * Gdx.graphics.getDeltaTime();
        this.plasma3Angle += this.rotationSpeed / 3.0f * Gdx.graphics.getDeltaTime();
        this.rotationSpeed = MathHelper.fadeLerpSnap(this.rotationSpeed, this.targetRotationSpeed);
        this.effect.speed = this.rotationSpeed * Gdx.graphics.getDeltaTime();
    }

    public void render(SpriteBatch sb) {
        sb.setColor(this.m.tint.color);
        sb.draw(this.plasma3, this.m.drawX - 256.0f + this.m.animX + 12.0f * Settings.scale, this.m.drawY + this.m.animY + this.effect.y * 2.0f - 256.0f + BODY_OFFSET_Y, 256.0f, 256.0f, 512.0f, 512.0f, Settings.scale * 0.95f, Settings.scale * 0.95f, this.plasma3Angle, 0, 0, 512, 512, false, false);
        sb.draw(this.plasma2, this.m.drawX - 256.0f + this.m.animX + 6.0f * Settings.scale, this.m.drawY + this.m.animY + this.effect.y - 256.0f + BODY_OFFSET_Y, 256.0f, 256.0f, 512.0f, 512.0f, Settings.scale, Settings.scale, this.plasma2Angle, 0, 0, 512, 512, false, false);
        sb.draw(this.plasma1, this.m.drawX - 256.0f + this.m.animX, this.m.drawY + this.m.animY + this.effect.y * 0.5f - 256.0f + BODY_OFFSET_Y, 256.0f, 256.0f, 512.0f, 512.0f, Settings.scale, Settings.scale, this.plasma1Angle, 0, 0, 512, 512, false, false);
        sb.draw(this.shadow, this.m.drawX - 256.0f + this.m.animX + 12.0f * Settings.scale, this.m.drawY + this.m.animY + this.effect.y / 4.0f - 15.0f * Settings.scale - 256.0f + BODY_OFFSET_Y, 256.0f, 256.0f, 512.0f, 512.0f, Settings.scale, Settings.scale, 0.0f, 0, 0, 512, 512, false, false);
    }

    @Override
    public void dispose() {
        this.plasma1.dispose();
        this.plasma2.dispose();
        this.plasma3.dispose();
        this.shadow.dispose();
    }
}

