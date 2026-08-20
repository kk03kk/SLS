/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.orbs;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.actions.animations.VFXAction;
import com.megacrit.cardcrawl.actions.common.DamageAction;
import com.megacrit.cardcrawl.actions.common.DamageAllEnemiesAction;
import com.megacrit.cardcrawl.actions.defect.LightningOrbEvokeAction;
import com.megacrit.cardcrawl.actions.defect.LightningOrbPassiveAction;
import com.megacrit.cardcrawl.actions.utility.SFXAction;
import com.megacrit.cardcrawl.cards.DamageInfo;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.helpers.ImageMaster;
import com.megacrit.cardcrawl.localization.OrbStrings;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import com.megacrit.cardcrawl.orbs.AbstractOrb;
import com.megacrit.cardcrawl.vfx.combat.LightningEffect;
import com.megacrit.cardcrawl.vfx.combat.LightningOrbActivateEffect;
import com.megacrit.cardcrawl.vfx.combat.LightningOrbPassiveEffect;
import com.megacrit.cardcrawl.vfx.combat.OrbFlareEffect;

public class Lightning
extends AbstractOrb {
    public static final String ORB_ID = "Lightning";
    private static final OrbStrings orbString = CardCrawlGame.languagePack.getOrbString("Lightning");
    private float vfxTimer = 1.0f;
    private static final float PI_DIV_16 = 0.19634955f;
    private static final float ORB_WAVY_DIST = 0.05f;
    private static final float PI_4 = (float)Math.PI * 4;
    private static final float ORB_BORDER_SCALE = 1.2f;

    public Lightning() {
        this.ID = ORB_ID;
        this.img = ImageMaster.ORB_LIGHTNING;
        this.name = Lightning.orbString.NAME;
        this.evokeAmount = this.baseEvokeAmount = 8;
        this.passiveAmount = this.basePassiveAmount = 3;
        this.updateDescription();
        this.angle = MathUtils.random(360.0f);
        this.channelAnimTimer = 0.5f;
    }

    @Override
    public void updateDescription() {
        this.applyFocus();
        this.description = Lightning.orbString.DESCRIPTION[0] + this.passiveAmount + Lightning.orbString.DESCRIPTION[1] + this.evokeAmount + Lightning.orbString.DESCRIPTION[2];
    }

    @Override
    public void onEvoke() {
        if (AbstractDungeon.player.hasPower("Electro")) {
            AbstractDungeon.actionManager.addToTop(new LightningOrbEvokeAction(new DamageInfo(AbstractDungeon.player, this.evokeAmount, DamageInfo.DamageType.THORNS), true));
        } else {
            AbstractDungeon.actionManager.addToTop(new LightningOrbEvokeAction(new DamageInfo(AbstractDungeon.player, this.evokeAmount, DamageInfo.DamageType.THORNS), false));
        }
    }

    @Override
    public void onEndOfTurn() {
        if (AbstractDungeon.player.hasPower("Electro")) {
            float speedTime = 0.2f / (float)AbstractDungeon.player.orbs.size();
            if (Settings.FAST_MODE) {
                speedTime = 0.0f;
            }
            AbstractDungeon.actionManager.addToBottom(new VFXAction(new OrbFlareEffect(this, OrbFlareEffect.OrbFlareColor.LIGHTNING), speedTime));
            AbstractDungeon.actionManager.addToBottom(new LightningOrbEvokeAction(new DamageInfo(AbstractDungeon.player, this.passiveAmount, DamageInfo.DamageType.THORNS), true));
        } else {
            AbstractDungeon.actionManager.addToBottom(new LightningOrbPassiveAction(new DamageInfo(AbstractDungeon.player, this.passiveAmount, DamageInfo.DamageType.THORNS), this, false));
        }
    }

    private void triggerPassiveEffect(DamageInfo info, boolean hitAll) {
        if (!hitAll) {
            AbstractMonster m = AbstractDungeon.getRandomMonster();
            if (m != null) {
                float speedTime = 0.2f / (float)AbstractDungeon.player.orbs.size();
                if (Settings.FAST_MODE) {
                    speedTime = 0.0f;
                }
                AbstractDungeon.actionManager.addToBottom(new DamageAction(m, info, AbstractGameAction.AttackEffect.NONE, true));
                AbstractDungeon.actionManager.addToBottom(new VFXAction(new OrbFlareEffect(this, OrbFlareEffect.OrbFlareColor.LIGHTNING), speedTime));
                AbstractDungeon.actionManager.addToBottom(new VFXAction(new LightningEffect(m.drawX, m.drawY), speedTime));
                AbstractDungeon.actionManager.addToBottom(new SFXAction("ORB_LIGHTNING_EVOKE"));
            }
        } else {
            float speedTime = 0.2f / (float)AbstractDungeon.player.orbs.size();
            if (Settings.FAST_MODE) {
                speedTime = 0.0f;
            }
            AbstractDungeon.actionManager.addToBottom(new VFXAction(new OrbFlareEffect(this, OrbFlareEffect.OrbFlareColor.LIGHTNING), speedTime));
            AbstractDungeon.actionManager.addToBottom(new DamageAllEnemiesAction((AbstractCreature)AbstractDungeon.player, DamageInfo.createDamageMatrix(info.base, true), DamageInfo.DamageType.THORNS, AbstractGameAction.AttackEffect.NONE));
            for (AbstractMonster m3 : AbstractDungeon.getMonsters().monsters) {
                if (m3.isDeadOrEscaped() || m3.halfDead) continue;
                AbstractDungeon.actionManager.addToBottom(new VFXAction(new LightningEffect(m3.drawX, m3.drawY), speedTime));
            }
            AbstractDungeon.actionManager.addToBottom(new SFXAction("ORB_LIGHTNING_EVOKE"));
        }
    }

    @Override
    public void triggerEvokeAnimation() {
        CardCrawlGame.sound.play("ORB_LIGHTNING_EVOKE", 0.1f);
        AbstractDungeon.effectsQueue.add(new LightningOrbActivateEffect(this.cX, this.cY));
    }

    @Override
    public void updateAnimation() {
        super.updateAnimation();
        this.angle += Gdx.graphics.getDeltaTime() * 180.0f;
        this.vfxTimer -= Gdx.graphics.getDeltaTime();
        if (this.vfxTimer < 0.0f) {
            AbstractDungeon.effectList.add(new LightningOrbPassiveEffect(this.cX, this.cY));
            if (MathUtils.randomBoolean()) {
                AbstractDungeon.effectList.add(new LightningOrbPassiveEffect(this.cX, this.cY));
            }
            this.vfxTimer = MathUtils.random(0.15f, 0.8f);
        }
    }

    @Override
    public void render(SpriteBatch sb) {
        this.shineColor.a = this.c.a / 2.0f;
        sb.setColor(this.shineColor);
        sb.setBlendFunction(770, 1);
        sb.draw(this.img, this.cX - 48.0f, this.cY - 48.0f + this.bobEffect.y, 48.0f, 48.0f, 96.0f, 96.0f, this.scale + MathUtils.sin(this.angle / ((float)Math.PI * 4)) * 0.05f + 0.19634955f, this.scale * 1.2f, this.angle, 0, 0, 96, 96, false, false);
        sb.draw(this.img, this.cX - 48.0f, this.cY - 48.0f + this.bobEffect.y, 48.0f, 48.0f, 96.0f, 96.0f, this.scale * 1.2f, this.scale + MathUtils.sin(this.angle / ((float)Math.PI * 4)) * 0.05f + 0.19634955f, -this.angle, 0, 0, 96, 96, false, false);
        sb.setBlendFunction(770, 771);
        sb.setColor(this.c);
        sb.draw(this.img, this.cX - 48.0f, this.cY - 48.0f + this.bobEffect.y, 48.0f, 48.0f, 96.0f, 96.0f, this.scale, this.scale, this.angle / 12.0f, 0, 0, 96, 96, false, false);
        this.renderText(sb);
        this.hb.render(sb);
    }

    @Override
    public void playChannelSFX() {
        CardCrawlGame.sound.play("ORB_LIGHTNING_CHANNEL", 0.1f);
    }

    @Override
    public AbstractOrb makeCopy() {
        return new Lightning();
    }
}

