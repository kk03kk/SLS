/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.potions;

import com.megacrit.cardcrawl.actions.watcher.ChangeStanceAction;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.helpers.GameDictionary;
import com.megacrit.cardcrawl.helpers.PowerTip;
import com.megacrit.cardcrawl.helpers.TipHelper;
import com.megacrit.cardcrawl.localization.PotionStrings;
import com.megacrit.cardcrawl.potions.AbstractPotion;
import com.megacrit.cardcrawl.rooms.AbstractRoom;

public class Ambrosia
extends AbstractPotion {
    public static final String POTION_ID = "Ambrosia";
    private static final PotionStrings potionStrings = CardCrawlGame.languagePack.getPotionString("Ambrosia");

    public Ambrosia() {
        super(Ambrosia.potionStrings.NAME, POTION_ID, AbstractPotion.PotionRarity.RARE, AbstractPotion.PotionSize.EYE, AbstractPotion.PotionColor.WEAK);
        this.labOutlineColor = Settings.PURPLE_RELIC_COLOR;
        this.isThrown = false;
    }

    @Override
    public void initializeData() {
        this.potency = this.getPotency();
        this.description = Ambrosia.potionStrings.DESCRIPTIONS[0];
        this.tips.clear();
        this.tips.add(new PowerTip(this.name, this.description));
        this.tips.add(new PowerTip(TipHelper.capitalize(GameDictionary.STANCE.NAMES[0]), GameDictionary.keywords.get(GameDictionary.STANCE.NAMES[0])));
    }

    @Override
    public void use(AbstractCreature target) {
        if (AbstractDungeon.getCurrRoom().phase == AbstractRoom.RoomPhase.COMBAT) {
            this.addToBot(new ChangeStanceAction("Divinity"));
        }
    }

    @Override
    public int getPotency(int ascensionLevel) {
        return 2;
    }

    @Override
    public AbstractPotion makeCopy() {
        return new Ambrosia();
    }
}

