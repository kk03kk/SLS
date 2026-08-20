/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.cards.red;

import com.badlogic.gdx.graphics.Color;
import com.megacrit.cardcrawl.actions.animations.VFXAction;
import com.megacrit.cardcrawl.actions.unique.FeedAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.cards.DamageInfo;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.localization.CardStrings;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import com.megacrit.cardcrawl.vfx.combat.BiteEffect;

public class Feed
extends AbstractCard {
    public static final String ID = "Feed";
    private static final CardStrings cardStrings = CardCrawlGame.languagePack.getCardStrings("Feed");

    public Feed() {
        super(ID, Feed.cardStrings.NAME, "red/attack/feed", 1, Feed.cardStrings.DESCRIPTION, AbstractCard.CardType.ATTACK, AbstractCard.CardColor.RED, AbstractCard.CardRarity.RARE, AbstractCard.CardTarget.ENEMY);
        this.baseDamage = 10;
        this.exhaust = true;
        this.magicNumber = this.baseMagicNumber = 3;
        this.tags.add(AbstractCard.CardTags.HEALING);
    }

    @Override
    public void use(AbstractPlayer p, AbstractMonster m) {
        if (m != null) {
            this.addToBot(new VFXAction(new BiteEffect(m.hb.cX, m.hb.cY - 40.0f * Settings.scale, Color.SCARLET.cpy()), 0.3f));
        }
        this.addToBot(new FeedAction(m, new DamageInfo(p, this.damage, this.damageTypeForTurn), this.magicNumber));
    }

    @Override
    public void upgrade() {
        if (!this.upgraded) {
            this.upgradeName();
            this.upgradeDamage(2);
            this.upgradeMagicNumber(1);
        }
    }

    @Override
    public AbstractCard makeCopy() {
        return new Feed();
    }
}

