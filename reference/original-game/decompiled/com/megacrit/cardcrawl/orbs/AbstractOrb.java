/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.orbs;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.math.MathUtils;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.helpers.FontHelper;
import com.megacrit.cardcrawl.helpers.Hitbox;
import com.megacrit.cardcrawl.helpers.MathHelper;
import com.megacrit.cardcrawl.helpers.PowerTip;
import com.megacrit.cardcrawl.helpers.TipHelper;
import com.megacrit.cardcrawl.orbs.Dark;
import com.megacrit.cardcrawl.orbs.EmptyOrbSlot;
import com.megacrit.cardcrawl.orbs.Frost;
import com.megacrit.cardcrawl.orbs.Lightning;
import com.megacrit.cardcrawl.orbs.Plasma;
import com.megacrit.cardcrawl.powers.AbstractPower;
import com.megacrit.cardcrawl.vfx.BobEffect;
import java.util.ArrayList;

public abstract class AbstractOrb {
    public String name;
    public String description;
    public String ID;
    protected ArrayList<PowerTip> tips = new ArrayList();
    public int evokeAmount = 0;
    public int passiveAmount = 0;
    protected int baseEvokeAmount = 0;
    protected int basePassiveAmount = 0;
    public float cX = 0.0f;
    public float cY = 0.0f;
    public float tX;
    public float tY;
    protected Color c = Settings.CREAM_COLOR.cpy();
    protected Color shineColor = new Color(1.0f, 1.0f, 1.0f, 0.0f);
    protected static final int W = 96;
    public Hitbox hb = new Hitbox(96.0f * Settings.scale, 96.0f * Settings.scale);
    protected Texture img = null;
    protected BobEffect bobEffect = new BobEffect(3.0f * Settings.scale, 3.0f);
    protected static final float NUM_X_OFFSET = 20.0f * Settings.scale;
    protected static final float NUM_Y_OFFSET = -12.0f * Settings.scale;
    protected float angle;
    protected float scale;
    protected float fontScale = 0.7f;
    protected boolean showEvokeValue = false;
    protected static final float CHANNEL_IN_TIME = 0.5f;
    protected float channelAnimTimer = 0.5f;

    public abstract void updateDescription();

    public abstract void onEvoke();

    public static AbstractOrb getRandomOrb(boolean useCardRng) {
        ArrayList<AbstractOrb> orbs = new ArrayList<AbstractOrb>();
        orbs.add(new Dark());
        orbs.add(new Frost());
        orbs.add(new Lightning());
        orbs.add(new Plasma());
        if (useCardRng) {
            return (AbstractOrb)orbs.get(AbstractDungeon.cardRandomRng.random(orbs.size() - 1));
        }
        return (AbstractOrb)orbs.get(MathUtils.random(orbs.size() - 1));
    }

    public void onStartOfTurn() {
    }

    public void onEndOfTurn() {
    }

    public void applyFocus() {
        AbstractPower power = AbstractDungeon.player.getPower("Focus");
        if (power != null && !this.ID.equals("Plasma")) {
            this.passiveAmount = Math.max(0, this.basePassiveAmount + power.amount);
            this.evokeAmount = Math.max(0, this.baseEvokeAmount + power.amount);
        } else {
            this.passiveAmount = this.basePassiveAmount;
            this.evokeAmount = this.baseEvokeAmount;
        }
    }

    public static int applyLockOn(AbstractCreature target, int dmg) {
        int retVal = dmg;
        if (target.hasPower("Lockon")) {
            retVal = (int)((float)retVal * 1.5f);
        }
        return retVal;
    }

    public abstract AbstractOrb makeCopy();

    public void update() {
        this.hb.update();
        if (this.hb.hovered) {
            TipHelper.renderGenericTip(this.tX + 96.0f * Settings.scale, this.tY + 64.0f * Settings.scale, this.name, this.description);
        }
        this.fontScale = MathHelper.scaleLerpSnap(this.fontScale, 0.7f);
    }

    public void updateAnimation() {
        this.bobEffect.update();
        this.cX = MathHelper.orbLerpSnap(this.cX, AbstractDungeon.player.animX + this.tX);
        this.cY = MathHelper.orbLerpSnap(this.cY, AbstractDungeon.player.animY + this.tY);
        if (this.channelAnimTimer != 0.0f) {
            this.channelAnimTimer -= Gdx.graphics.getDeltaTime();
            if (this.channelAnimTimer < 0.0f) {
                this.channelAnimTimer = 0.0f;
            }
        }
        this.c.a = Interpolation.pow2In.apply(1.0f, 0.01f, this.channelAnimTimer / 0.5f);
        this.scale = Interpolation.swingIn.apply(Settings.scale, 0.01f, this.channelAnimTimer / 0.5f);
    }

    public void setSlot(int slotNum, int maxOrbs) {
        float dist = 160.0f * Settings.scale + (float)maxOrbs * 10.0f * Settings.scale;
        float angle = 100.0f + (float)maxOrbs * 12.0f;
        float offsetAngle = angle / 2.0f;
        angle *= (float)slotNum / ((float)maxOrbs - 1.0f);
        this.tX = dist * MathUtils.cosDeg(angle += 90.0f - offsetAngle) + AbstractDungeon.player.drawX;
        this.tY = dist * MathUtils.sinDeg(angle) + AbstractDungeon.player.drawY + AbstractDungeon.player.hb_h / 2.0f;
        if (maxOrbs == 1) {
            this.tX = AbstractDungeon.player.drawX;
            this.tY = 160.0f * Settings.scale + AbstractDungeon.player.drawY + AbstractDungeon.player.hb_h / 2.0f;
        }
        this.hb.move(this.tX, this.tY);
    }

    public abstract void render(SpriteBatch var1);

    protected void renderText(SpriteBatch sb) {
        if (!(this instanceof EmptyOrbSlot)) {
            if (this.showEvokeValue) {
                FontHelper.renderFontCentered(sb, FontHelper.cardEnergyFont_L, Integer.toString(this.evokeAmount), this.cX + NUM_X_OFFSET, this.cY + this.bobEffect.y / 2.0f + NUM_Y_OFFSET, new Color(0.2f, 1.0f, 1.0f, this.c.a), this.fontScale);
            } else {
                FontHelper.renderFontCentered(sb, FontHelper.cardEnergyFont_L, Integer.toString(this.passiveAmount), this.cX + NUM_X_OFFSET, this.cY + this.bobEffect.y / 2.0f + NUM_Y_OFFSET, this.c, this.fontScale);
            }
        }
    }

    public void triggerEvokeAnimation() {
    }

    public void showEvokeValue() {
        this.showEvokeValue = true;
        this.fontScale = 1.5f;
    }

    public void hideEvokeValues() {
        this.showEvokeValue = false;
    }

    public abstract void playChannelSFX();
}

