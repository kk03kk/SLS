/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.events.exordium;

import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.cards.CardGroup;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.events.AbstractEvent;
import com.megacrit.cardcrawl.events.AbstractImageEvent;
import com.megacrit.cardcrawl.localization.EventStrings;
import com.megacrit.cardcrawl.vfx.cardManip.PurgeCardEffect;

public class Cleric
extends AbstractImageEvent {
    public static final String ID = "The Cleric";
    private static final EventStrings eventStrings = CardCrawlGame.languagePack.getEventString("The Cleric");
    public static final String NAME = Cleric.eventStrings.NAME;
    public static final String[] DESCRIPTIONS = Cleric.eventStrings.DESCRIPTIONS;
    public static final String[] OPTIONS = Cleric.eventStrings.OPTIONS;
    public static final int HEAL_COST = 35;
    private static final int PURIFY_COST = 50;
    private static final int A_2_PURIFY_COST = 75;
    private int purifyCost = AbstractDungeon.ascensionLevel >= 15 ? 75 : 50;
    private static final float HEAL_AMT = 0.25f;
    private static final String DIALOG_1 = DESCRIPTIONS[0];
    private static final String DIALOG_2 = DESCRIPTIONS[1];
    private static final String DIALOG_3 = DESCRIPTIONS[2];
    private static final String DIALOG_4 = DESCRIPTIONS[3];
    private int healAmt;

    public Cleric() {
        super(NAME, DIALOG_1, "images/events/cleric.jpg");
        int gold = AbstractDungeon.player.gold;
        if (gold >= 35) {
            this.healAmt = (int)((float)AbstractDungeon.player.maxHealth * 0.25f);
            this.imageEventText.setDialogOption(OPTIONS[0] + this.healAmt + OPTIONS[8], gold < 35);
        } else {
            this.imageEventText.setDialogOption(OPTIONS[1] + 35 + OPTIONS[2], gold < 35);
        }
        if (gold >= 50) {
            this.imageEventText.setDialogOption(OPTIONS[3] + this.purifyCost + OPTIONS[4], gold < this.purifyCost);
        } else {
            this.imageEventText.setDialogOption(OPTIONS[5], gold < this.purifyCost);
        }
        this.imageEventText.setDialogOption(OPTIONS[6]);
    }

    @Override
    public void update() {
        super.update();
        if (!AbstractDungeon.gridSelectScreen.selectedCards.isEmpty()) {
            AbstractCard c = AbstractDungeon.gridSelectScreen.selectedCards.get(0);
            AbstractDungeon.topLevelEffects.add(new PurgeCardEffect(c, Settings.WIDTH / 2, Settings.HEIGHT / 2));
            AbstractEvent.logMetricCardRemovalAtCost(ID, "Card Removal", c, this.purifyCost);
            AbstractDungeon.player.masterDeck.removeCard(c);
            AbstractDungeon.gridSelectScreen.selectedCards.remove(c);
        }
    }

    @Override
    protected void buttonEffect(int buttonPressed) {
        block0 : switch (this.screenNum) {
            case 0: {
                switch (buttonPressed) {
                    case 0: {
                        AbstractDungeon.player.loseGold(35);
                        AbstractDungeon.player.heal(this.healAmt);
                        this.showProceedScreen(DIALOG_2);
                        AbstractEvent.logMetricHealAtCost(ID, "Healed", 35, this.healAmt);
                        break block0;
                    }
                    case 1: {
                        if (CardGroup.getGroupWithoutBottledCards(AbstractDungeon.player.masterDeck.getPurgeableCards()).size() > 0) {
                            AbstractDungeon.player.loseGold(this.purifyCost);
                            AbstractDungeon.gridSelectScreen.open(CardGroup.getGroupWithoutBottledCards(AbstractDungeon.player.masterDeck.getPurgeableCards()), 1, OPTIONS[7], false, false, false, true);
                        }
                        this.showProceedScreen(DIALOG_3);
                        break block0;
                    }
                }
                this.showProceedScreen(DIALOG_4);
                AbstractEvent.logMetric(ID, "Leave");
                break;
            }
            default: {
                this.openMap();
            }
        }
    }
}

