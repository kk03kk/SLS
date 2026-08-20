/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.relics;

import com.badlogic.gdx.math.MathUtils;
import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.actions.common.DamageRandomEnemyAction;
import com.megacrit.cardcrawl.actions.common.RelicAboveCreatureAction;
import com.megacrit.cardcrawl.cards.DamageInfo;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.helpers.input.InputHelper;
import com.megacrit.cardcrawl.relics.AbstractRelic;

public class Tingsha
extends AbstractRelic {
    public static final String ID = "Tingsha";
    private static final int DMG_AMT = 3;

    public Tingsha() {
        super(ID, "tingsha.png", AbstractRelic.RelicTier.RARE, AbstractRelic.LandingSound.CLINK);
    }

    @Override
    public String getUpdatedDescription() {
        return this.DESCRIPTIONS[0] + 3 + this.DESCRIPTIONS[1];
    }

    @Override
    public void onManualDiscard() {
        this.flash();
        CardCrawlGame.sound.play("TINGSHA");
        this.addToBot(new RelicAboveCreatureAction(AbstractDungeon.player, this));
        this.addToBot(new DamageRandomEnemyAction(new DamageInfo(AbstractDungeon.player, 3, DamageInfo.DamageType.THORNS), AbstractGameAction.AttackEffect.FIRE));
    }

    @Override
    public void update() {
        super.update();
        if (this.hb.hovered && InputHelper.justClickedLeft) {
            CardCrawlGame.sound.playA("TINGSHA", MathUtils.random(-0.2f, 0.1f));
            this.flash();
        }
    }

    @Override
    public AbstractRelic makeCopy() {
        return new Tingsha();
    }
}

