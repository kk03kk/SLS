/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.potions;

import com.megacrit.cardcrawl.actions.watcher.ChooseOneAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.cards.optionCards.ChooseCalm;
import com.megacrit.cardcrawl.cards.optionCards.ChooseWrath;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.helpers.GameDictionary;
import com.megacrit.cardcrawl.helpers.PowerTip;
import com.megacrit.cardcrawl.helpers.TipHelper;
import com.megacrit.cardcrawl.helpers.input.InputHelper;
import com.megacrit.cardcrawl.localization.PotionStrings;
import com.megacrit.cardcrawl.potions.AbstractPotion;
import java.util.ArrayList;

public class StancePotion
extends AbstractPotion {
    public static final String POTION_ID = "StancePotion";
    public static final PotionStrings potionStrings = CardCrawlGame.languagePack.getPotionString("StancePotion");

    public StancePotion() {
        super(StancePotion.potionStrings.NAME, POTION_ID, AbstractPotion.PotionRarity.UNCOMMON, AbstractPotion.PotionSize.SPHERE, AbstractPotion.PotionColor.WEAK);
        this.labOutlineColor = Settings.PURPLE_RELIC_COLOR;
        this.description = StancePotion.potionStrings.DESCRIPTIONS[0];
        this.isThrown = false;
        this.tips.clear();
        this.tips.add(new PowerTip(this.name, this.description));
        this.tips.add(new PowerTip(TipHelper.capitalize(GameDictionary.CALM.NAMES[0]), GameDictionary.keywords.get(GameDictionary.CALM.NAMES[0])));
        this.tips.add(new PowerTip(TipHelper.capitalize(GameDictionary.WRATH.NAMES[0]), GameDictionary.keywords.get(GameDictionary.WRATH.NAMES[0])));
    }

    @Override
    public void use(AbstractCreature target) {
        InputHelper.moveCursorToNeutralPosition();
        ArrayList<AbstractCard> stanceChoices = new ArrayList<AbstractCard>();
        stanceChoices.add(new ChooseWrath());
        stanceChoices.add(new ChooseCalm());
        this.addToBot(new ChooseOneAction(stanceChoices));
    }

    @Override
    public int getPotency(int ascensionLevel) {
        return 0;
    }

    @Override
    public AbstractPotion makeCopy() {
        return new StancePotion();
    }
}

