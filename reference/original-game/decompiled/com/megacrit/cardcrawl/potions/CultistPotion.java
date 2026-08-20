/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.potions;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.MathUtils;
import com.megacrit.cardcrawl.actions.animations.TalkAction;
import com.megacrit.cardcrawl.actions.common.ApplyPowerAction;
import com.megacrit.cardcrawl.actions.utility.SFXAction;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.helpers.GameDictionary;
import com.megacrit.cardcrawl.helpers.PowerTip;
import com.megacrit.cardcrawl.helpers.TipHelper;
import com.megacrit.cardcrawl.localization.PotionStrings;
import com.megacrit.cardcrawl.monsters.city.Byrd;
import com.megacrit.cardcrawl.potions.AbstractPotion;
import com.megacrit.cardcrawl.powers.RitualPower;
import com.megacrit.cardcrawl.rooms.AbstractRoom;

public class CultistPotion
extends AbstractPotion {
    public static final String POTION_ID = "CultistPotion";
    private static final PotionStrings potionStrings = CardCrawlGame.languagePack.getPotionString("CultistPotion");

    public CultistPotion() {
        super(CultistPotion.potionStrings.NAME, POTION_ID, AbstractPotion.PotionRarity.RARE, AbstractPotion.PotionSize.MOON, AbstractPotion.PotionEffect.NONE, new Color(676576511), new Color(472670463), null);
        this.isThrown = false;
    }

    @Override
    public void initializeData() {
        this.potency = this.getPotency();
        this.description = CultistPotion.potionStrings.DESCRIPTIONS[0] + this.potency + CultistPotion.potionStrings.DESCRIPTIONS[1];
        this.tips.clear();
        this.tips.add(new PowerTip(this.name, this.description));
        this.tips.add(new PowerTip(TipHelper.capitalize(GameDictionary.RITUAL.NAMES[0]), GameDictionary.keywords.get(GameDictionary.RITUAL.NAMES[0])));
        this.tips.add(new PowerTip(TipHelper.capitalize(GameDictionary.STRENGTH.NAMES[0]), GameDictionary.keywords.get(GameDictionary.STRENGTH.NAMES[0])));
    }

    @Override
    public void use(AbstractCreature target) {
        target = AbstractDungeon.player;
        if (AbstractDungeon.getCurrRoom().phase == AbstractRoom.RoomPhase.COMBAT) {
            this.playSfx();
            AbstractDungeon.actionManager.addToBottom(new TalkAction(true, Byrd.DIALOG[0], 1.2f, 1.2f));
            this.addToBot(new ApplyPowerAction(target, AbstractDungeon.player, new RitualPower(target, this.potency, true), this.potency));
        }
    }

    private void playSfx() {
        int roll = MathUtils.random(2);
        if (roll == 0) {
            AbstractDungeon.actionManager.addToBottom(new SFXAction("VO_CULTIST_1A"));
        } else if (roll == 1) {
            AbstractDungeon.actionManager.addToBottom(new SFXAction("VO_CULTIST_1B"));
        } else {
            AbstractDungeon.actionManager.addToBottom(new SFXAction("VO_CULTIST_1C"));
        }
    }

    @Override
    public int getPotency(int ascensionLevel) {
        return 1;
    }

    @Override
    public AbstractPotion makeCopy() {
        return new CultistPotion();
    }
}

