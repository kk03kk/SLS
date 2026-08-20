/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.potions;

import com.badlogic.gdx.graphics.Color;
import com.megacrit.cardcrawl.actions.common.ApplyPowerAction;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.helpers.GameDictionary;
import com.megacrit.cardcrawl.helpers.PowerTip;
import com.megacrit.cardcrawl.helpers.TipHelper;
import com.megacrit.cardcrawl.localization.PotionStrings;
import com.megacrit.cardcrawl.potions.AbstractPotion;
import com.megacrit.cardcrawl.powers.ThornsPower;
import com.megacrit.cardcrawl.rooms.AbstractRoom;

public class LiquidBronze
extends AbstractPotion {
    public static final String POTION_ID = "LiquidBronze";
    private static final PotionStrings potionStrings = CardCrawlGame.languagePack.getPotionString("LiquidBronze");

    public LiquidBronze() {
        super(LiquidBronze.potionStrings.NAME, POTION_ID, AbstractPotion.PotionRarity.UNCOMMON, AbstractPotion.PotionSize.SPIKY, AbstractPotion.PotionEffect.NONE, new Color(-491249153), new Color(415023359), null);
        this.isThrown = false;
    }

    @Override
    public void initializeData() {
        this.potency = this.getPotency();
        this.description = LiquidBronze.potionStrings.DESCRIPTIONS[0] + this.potency + LiquidBronze.potionStrings.DESCRIPTIONS[1];
        this.tips.clear();
        this.tips.add(new PowerTip(this.name, this.description));
        this.tips.add(new PowerTip(TipHelper.capitalize(GameDictionary.THORNS.NAMES[0]), GameDictionary.keywords.get(GameDictionary.THORNS.NAMES[0])));
    }

    @Override
    public void use(AbstractCreature target) {
        target = AbstractDungeon.player;
        if (AbstractDungeon.getCurrRoom().phase == AbstractRoom.RoomPhase.COMBAT) {
            this.addToBot(new ApplyPowerAction(target, AbstractDungeon.player, new ThornsPower(target, this.potency), this.potency));
        }
    }

    @Override
    public int getPotency(int ascensionLevel) {
        return 3;
    }

    @Override
    public AbstractPotion makeCopy() {
        return new LiquidBronze();
    }
}

