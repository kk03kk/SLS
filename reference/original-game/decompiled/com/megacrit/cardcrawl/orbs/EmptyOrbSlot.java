/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.orbs;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.helpers.ImageMaster;
import com.megacrit.cardcrawl.localization.OrbStrings;
import com.megacrit.cardcrawl.orbs.AbstractOrb;

public class EmptyOrbSlot
extends AbstractOrb {
    public static final String ORB_ID = "Empty";
    private static final OrbStrings orbString = CardCrawlGame.languagePack.getOrbString("Empty");
    public static final String[] DESC = EmptyOrbSlot.orbString.DESCRIPTION;

    public EmptyOrbSlot(float x, float y) {
        this.angle = MathUtils.random(360.0f);
        this.ID = ORB_ID;
        this.name = EmptyOrbSlot.orbString.NAME;
        this.evokeAmount = 0;
        this.cX = x;
        this.cY = y;
        this.updateDescription();
        this.channelAnimTimer = 0.5f;
    }

    public EmptyOrbSlot() {
        this.angle = MathUtils.random(360.0f);
        this.name = EmptyOrbSlot.orbString.NAME;
        this.evokeAmount = 0;
        this.cX = AbstractDungeon.player.drawX + AbstractDungeon.player.hb_x;
        this.cY = AbstractDungeon.player.drawY + AbstractDungeon.player.hb_y + AbstractDungeon.player.hb_h / 2.0f;
        this.updateDescription();
    }

    @Override
    public void updateDescription() {
        this.description = DESC[0];
    }

    @Override
    public void onEvoke() {
    }

    @Override
    public void updateAnimation() {
        super.updateAnimation();
        this.angle += Gdx.graphics.getDeltaTime() * 10.0f;
    }

    @Override
    public void render(SpriteBatch sb) {
        sb.setColor(this.c);
        sb.draw(ImageMaster.ORB_SLOT_2, this.cX - 48.0f - this.bobEffect.y / 8.0f, this.cY - 48.0f + this.bobEffect.y / 8.0f, 48.0f, 48.0f, 96.0f, 96.0f, this.scale, this.scale, 0.0f, 0, 0, 96, 96, false, false);
        sb.draw(ImageMaster.ORB_SLOT_1, this.cX - 48.0f + this.bobEffect.y / 8.0f, this.cY - 48.0f - this.bobEffect.y / 8.0f, 48.0f, 48.0f, 96.0f, 96.0f, this.scale, this.scale, this.angle, 0, 0, 96, 96, false, false);
        this.renderText(sb);
        this.hb.render(sb);
    }

    @Override
    public AbstractOrb makeCopy() {
        return new EmptyOrbSlot();
    }

    @Override
    public void playChannelSFX() {
    }
}

