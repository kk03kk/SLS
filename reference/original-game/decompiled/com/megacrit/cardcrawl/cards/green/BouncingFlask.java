/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.cards.green;

import com.megacrit.cardcrawl.actions.animations.VFXAction;
import com.megacrit.cardcrawl.actions.unique.BouncingFlaskAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.localization.CardStrings;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import com.megacrit.cardcrawl.vfx.combat.PotionBounceEffect;

public class BouncingFlask
extends AbstractCard {
    public static final String ID = "Bouncing Flask";
    private static final CardStrings cardStrings = CardCrawlGame.languagePack.getCardStrings("Bouncing Flask");

    public BouncingFlask() {
        super(ID, BouncingFlask.cardStrings.NAME, "green/skill/bouncing_flask", 2, BouncingFlask.cardStrings.DESCRIPTION, AbstractCard.CardType.SKILL, AbstractCard.CardColor.GREEN, AbstractCard.CardRarity.UNCOMMON, AbstractCard.CardTarget.ALL_ENEMY);
        this.magicNumber = this.baseMagicNumber = 3;
    }

    @Override
    public void use(AbstractPlayer p, AbstractMonster m) {
        AbstractMonster randomMonster = AbstractDungeon.getMonsters().getRandomMonster(null, true, AbstractDungeon.cardRandomRng);
        if (randomMonster != null) {
            this.addToBot(new VFXAction(new PotionBounceEffect(p.hb.cX, p.hb.cY, randomMonster.hb.cX, this.hb.cY), 0.4f));
        }
        this.addToBot(new BouncingFlaskAction(randomMonster, 3, this.magicNumber));
    }

    @Override
    public void upgrade() {
        if (!this.upgraded) {
            this.upgradeName();
            this.upgradeMagicNumber(1);
        }
    }

    @Override
    public AbstractCard makeCopy() {
        return new BouncingFlask();
    }
}

