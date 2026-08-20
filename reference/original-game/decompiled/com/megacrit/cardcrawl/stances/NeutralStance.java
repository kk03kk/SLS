/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.stances;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.localization.StanceStrings;
import com.megacrit.cardcrawl.stances.AbstractStance;

public class NeutralStance
extends AbstractStance {
    public static final String STANCE_ID = "Neutral";
    private static final StanceStrings stanceString = CardCrawlGame.languagePack.getStanceString("Neutral");

    public NeutralStance() {
        this.ID = STANCE_ID;
        this.img = null;
        this.name = null;
        this.updateDescription();
    }

    @Override
    public void updateDescription() {
        this.description = NeutralStance.stanceString.DESCRIPTION[0];
    }

    @Override
    public void onEnterStance() {
    }

    @Override
    public void render(SpriteBatch sb) {
    }
}

