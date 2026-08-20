/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.relics;

import com.badlogic.gdx.math.MathUtils;
import com.megacrit.cardcrawl.actions.common.ApplyPowerAction;
import com.megacrit.cardcrawl.actions.common.RelicAboveCreatureAction;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.helpers.input.InputHelper;
import com.megacrit.cardcrawl.powers.watcher.MantraPower;
import com.megacrit.cardcrawl.relics.AbstractRelic;

public class Damaru
extends AbstractRelic {
    public static final String ID = "Damaru";

    public Damaru() {
        super(ID, "damaru.png", AbstractRelic.RelicTier.COMMON, AbstractRelic.LandingSound.SOLID);
    }

    @Override
    public String getUpdatedDescription() {
        return this.DESCRIPTIONS[0] + 1 + this.DESCRIPTIONS[1];
    }

    @Override
    public void atTurnStart() {
        this.flash();
        this.addToBot(new RelicAboveCreatureAction(AbstractDungeon.player, this));
        this.addToBot(new ApplyPowerAction(AbstractDungeon.player, null, new MantraPower(AbstractDungeon.player, 1), 1));
    }

    @Override
    public void update() {
        super.update();
        if (this.hb.hovered && InputHelper.justClickedLeft) {
            CardCrawlGame.sound.playA("DAMARU", MathUtils.random(-0.2f, 0.1f));
            this.flash();
        }
    }

    @Override
    public AbstractRelic makeCopy() {
        return new Damaru();
    }
}

