/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.orbs;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.megacrit.cardcrawl.actions.animations.VFXAction;
import com.megacrit.cardcrawl.actions.common.GainBlockAction;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.helpers.ImageMaster;
import com.megacrit.cardcrawl.localization.OrbStrings;
import com.megacrit.cardcrawl.orbs.AbstractOrb;
import com.megacrit.cardcrawl.vfx.combat.FrostOrbActivateEffect;
import com.megacrit.cardcrawl.vfx.combat.FrostOrbPassiveEffect;
import com.megacrit.cardcrawl.vfx.combat.OrbFlareEffect;

public class Frost
extends AbstractOrb {
    public static final String ORB_ID = "Frost";
    private static final OrbStrings orbString = CardCrawlGame.languagePack.getOrbString("Frost");
    private boolean hFlip1 = MathUtils.randomBoolean();
    private boolean hFlip2 = MathUtils.randomBoolean();
    private float vfxTimer = 1.0f;
    private float vfxIntervalMin = 0.15f;
    private float vfxIntervalMax = 0.8f;

    public Frost() {
        this.ID = ORB_ID;
        this.name = Frost.orbString.NAME;
        this.evokeAmount = this.baseEvokeAmount = 5;
        this.passiveAmount = this.basePassiveAmount = 2;
        this.updateDescription();
        this.channelAnimTimer = 0.5f;
    }

    @Override
    public void updateDescription() {
        this.applyFocus();
        this.description = Frost.orbString.DESCRIPTION[0] + this.passiveAmount + Frost.orbString.DESCRIPTION[1] + this.evokeAmount + Frost.orbString.DESCRIPTION[2];
    }

    @Override
    public void onEvoke() {
        AbstractDungeon.actionManager.addToTop(new GainBlockAction((AbstractCreature)AbstractDungeon.player, AbstractDungeon.player, this.evokeAmount));
    }

    @Override
    public void updateAnimation() {
        super.updateAnimation();
        this.angle += Gdx.graphics.getDeltaTime() * 180.0f;
        this.vfxTimer -= Gdx.graphics.getDeltaTime();
        if (this.vfxTimer < 0.0f) {
            AbstractDungeon.effectList.add(new FrostOrbPassiveEffect(this.cX, this.cY));
            if (MathUtils.randomBoolean()) {
                AbstractDungeon.effectList.add(new FrostOrbPassiveEffect(this.cX, this.cY));
            }
            this.vfxTimer = MathUtils.random(this.vfxIntervalMin, this.vfxIntervalMax);
        }
    }

    @Override
    public void onEndOfTurn() {
        float speedTime = 0.6f / (float)AbstractDungeon.player.orbs.size();
        if (Settings.FAST_MODE) {
            speedTime = 0.0f;
        }
        AbstractDungeon.actionManager.addToBottom(new VFXAction(new OrbFlareEffect(this, OrbFlareEffect.OrbFlareColor.FROST), speedTime));
        AbstractDungeon.actionManager.addToBottom(new GainBlockAction(AbstractDungeon.player, AbstractDungeon.player, this.passiveAmount, true));
    }

    @Override
    public void triggerEvokeAnimation() {
        CardCrawlGame.sound.play("ORB_FROST_EVOKE", 0.1f);
        AbstractDungeon.effectsQueue.add(new FrostOrbActivateEffect(this.cX, this.cY));
    }

    @Override
    public void render(SpriteBatch sb) {
        sb.setColor(this.c);
        sb.draw(ImageMaster.FROST_ORB_RIGHT, this.cX - 48.0f + this.bobEffect.y / 4.0f, this.cY - 48.0f + this.bobEffect.y / 4.0f, 48.0f, 48.0f, 96.0f, 96.0f, this.scale, this.scale, 0.0f, 0, 0, 96, 96, this.hFlip1, false);
        sb.draw(ImageMaster.FROST_ORB_LEFT, this.cX - 48.0f + this.bobEffect.y / 4.0f, this.cY - 48.0f - this.bobEffect.y / 4.0f, 48.0f, 48.0f, 96.0f, 96.0f, this.scale, this.scale, 0.0f, 0, 0, 96, 96, this.hFlip1, false);
        sb.draw(ImageMaster.FROST_ORB_MIDDLE, this.cX - 48.0f - this.bobEffect.y / 4.0f, this.cY - 48.0f + this.bobEffect.y / 2.0f, 48.0f, 48.0f, 96.0f, 96.0f, this.scale, this.scale, 0.0f, 0, 0, 96, 96, this.hFlip2, false);
        this.renderText(sb);
        this.hb.render(sb);
    }

    @Override
    public void playChannelSFX() {
        CardCrawlGame.sound.play("ORB_FROST_CHANNEL", 0.1f);
    }

    @Override
    public AbstractOrb makeCopy() {
        return new Frost();
    }
}

