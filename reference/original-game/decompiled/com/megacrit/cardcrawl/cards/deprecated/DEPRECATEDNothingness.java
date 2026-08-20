/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.cards.deprecated;

import com.megacrit.cardcrawl.actions.common.DrawCardAction;
import com.megacrit.cardcrawl.actions.utility.ScryAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.localization.CardStrings;
import com.megacrit.cardcrawl.monsters.AbstractMonster;

public class DEPRECATEDNothingness
extends AbstractCard {
    public static final String ID = "Nothingness";
    private static final CardStrings cardStrings = CardCrawlGame.languagePack.getCardStrings("Nothingness");
    public static final String UPGRADE_DESCRIPTION = DEPRECATEDNothingness.cardStrings.UPGRADE_DESCRIPTION;
    private static final int COST = 1;

    public DEPRECATEDNothingness() {
        super(ID, DEPRECATEDNothingness.cardStrings.NAME, "colorless/skill/purity", 1, DEPRECATEDNothingness.cardStrings.DESCRIPTION, AbstractCard.CardType.SKILL, AbstractCard.CardColor.PURPLE, AbstractCard.CardRarity.UNCOMMON, AbstractCard.CardTarget.SELF);
    }

    public static int countCards() {
        int count = 0;
        for (AbstractCard c : AbstractDungeon.player.hand.group) {
            if (!DEPRECATEDNothingness.isEmpty(c)) continue;
            ++count;
        }
        for (AbstractCard c : AbstractDungeon.player.drawPile.group) {
            if (!DEPRECATEDNothingness.isEmpty(c)) continue;
            ++count;
        }
        for (AbstractCard c : AbstractDungeon.player.discardPile.group) {
            if (!DEPRECATEDNothingness.isEmpty(c)) continue;
            ++count;
        }
        return count;
    }

    public static boolean isEmpty(AbstractCard c) {
        return c.hasTag(AbstractCard.CardTags.EMPTY);
    }

    @Override
    public void use(AbstractPlayer p, AbstractMonster m) {
        if (this.upgraded) {
            this.addToBot(new ScryAction(DEPRECATEDNothingness.countCards()));
        }
        this.addToBot(new DrawCardAction(p, DEPRECATEDNothingness.countCards()));
    }

    @Override
    public AbstractCard makeCopy() {
        return new DEPRECATEDNothingness();
    }

    @Override
    public void upgrade() {
        if (!this.upgraded) {
            this.upgradeName();
            this.rawDescription = UPGRADE_DESCRIPTION;
            this.initializeDescription();
        }
    }
}

