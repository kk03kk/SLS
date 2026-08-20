/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.blights;

import com.megacrit.cardcrawl.blights.AbstractBlight;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.localization.BlightStrings;
import com.megacrit.cardcrawl.monsters.AbstractMonster;

public class TimeMaze
extends AbstractBlight {
    public static final String ID = "TimeMaze";
    private static final BlightStrings blightStrings = CardCrawlGame.languagePack.getBlightString("TimeMaze");
    public static final String NAME = TimeMaze.blightStrings.NAME;
    public static final String[] DESC = TimeMaze.blightStrings.DESCRIPTION;
    private static final int CARD_AMT = 15;

    public TimeMaze() {
        super(ID, NAME, DESC[0] + 15 + DESC[1], "maze.png", true);
        this.counter = 15;
    }

    @Override
    public void onPlayCard(AbstractCard card, AbstractMonster m) {
        if (this.counter < 15 && card.type != AbstractCard.CardType.CURSE) {
            ++this.counter;
            if (this.counter >= 15) {
                this.flash();
            }
        }
    }

    @Override
    public boolean canPlay(AbstractCard card) {
        if (this.counter >= 15 && card.type != AbstractCard.CardType.CURSE) {
            card.cantUseMessage = DESC[2] + 15 + DESC[1];
            return false;
        }
        return true;
    }

    @Override
    public void onVictory() {
        this.counter = -1;
    }

    @Override
    public void atBattleStart() {
        this.counter = 0;
    }

    @Override
    public void atTurnStart() {
        this.counter = 0;
    }
}

