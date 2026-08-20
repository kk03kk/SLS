/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.cards.curses;

import com.megacrit.cardcrawl.actions.common.MakeTempCardInHandAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.localization.CardStrings;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import com.megacrit.cardcrawl.vfx.NecronomicurseEffect;

public class Necronomicurse
extends AbstractCard {
    public static final String ID = "Necronomicurse";
    private static final CardStrings cardStrings = CardCrawlGame.languagePack.getCardStrings("Necronomicurse");

    public Necronomicurse() {
        super(ID, Necronomicurse.cardStrings.NAME, "curse/necronomicurse", -2, Necronomicurse.cardStrings.DESCRIPTION, AbstractCard.CardType.CURSE, AbstractCard.CardColor.CURSE, AbstractCard.CardRarity.SPECIAL, AbstractCard.CardTarget.NONE);
    }

    @Override
    public void use(AbstractPlayer p, AbstractMonster m) {
    }

    @Override
    public void onRemoveFromMasterDeck() {
        if (AbstractDungeon.player.hasRelic("Necronomicon")) {
            AbstractDungeon.player.getRelic("Necronomicon").flash();
        }
        AbstractDungeon.effectsQueue.add(new NecronomicurseEffect(new Necronomicurse(), (float)Settings.WIDTH / 2.0f, (float)Settings.HEIGHT / 2.0f));
    }

    @Override
    public void triggerOnExhaust() {
        if (AbstractDungeon.player.hasRelic("Necronomicon")) {
            AbstractDungeon.player.getRelic("Necronomicon").flash();
        }
        this.addToBot(new MakeTempCardInHandAction(this.makeCopy()));
    }

    @Override
    public void upgrade() {
    }

    @Override
    public AbstractCard makeCopy() {
        return new Necronomicurse();
    }
}

