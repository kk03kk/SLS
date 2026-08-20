/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.events.shrines;

import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.events.AbstractImageEvent;
import com.megacrit.cardcrawl.helpers.FontHelper;
import com.megacrit.cardcrawl.helpers.ScreenShake;
import com.megacrit.cardcrawl.localization.EventStrings;
import com.megacrit.cardcrawl.potions.AbstractPotion;
import com.megacrit.cardcrawl.relics.AbstractRelic;
import com.megacrit.cardcrawl.vfx.cardManip.PurgeCardEffect;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;

public class WeMeetAgain
extends AbstractImageEvent {
    public static final String ID = "WeMeetAgain";
    private static final EventStrings eventStrings = CardCrawlGame.languagePack.getEventString("WeMeetAgain");
    public static final String NAME = WeMeetAgain.eventStrings.NAME;
    public static final String[] DESCRIPTIONS = WeMeetAgain.eventStrings.DESCRIPTIONS;
    public static final String[] OPTIONS = WeMeetAgain.eventStrings.OPTIONS;
    private static final int MAX_GOLD = 150;
    private AbstractPotion potionOption;
    private AbstractCard cardOption;
    private int goldAmount;
    private AbstractRelic givenRelic;
    private static final String DIALOG_1 = DESCRIPTIONS[0];
    private CUR_SCREEN screen = CUR_SCREEN.INTRO;

    public WeMeetAgain() {
        super(NAME, DIALOG_1, "images/events/weMeetAgain.jpg");
        this.potionOption = AbstractDungeon.player.getRandomPotion();
        if (this.potionOption != null) {
            this.imageEventText.setDialogOption(OPTIONS[0] + FontHelper.colorString(this.potionOption.name, "r") + OPTIONS[6]);
        } else {
            this.imageEventText.setDialogOption(OPTIONS[1], true);
        }
        this.goldAmount = this.getGoldAmount();
        if (this.goldAmount != 0) {
            this.imageEventText.setDialogOption(OPTIONS[2] + this.goldAmount + OPTIONS[9] + OPTIONS[6]);
        } else {
            this.imageEventText.setDialogOption(OPTIONS[3], true);
        }
        this.cardOption = this.getRandomNonBasicCard();
        if (this.cardOption != null) {
            this.imageEventText.setDialogOption(OPTIONS[4] + this.cardOption.name + OPTIONS[6], this.cardOption.makeStatEquivalentCopy());
        } else {
            this.imageEventText.setDialogOption(OPTIONS[5], true);
        }
        this.imageEventText.setDialogOption(OPTIONS[7]);
    }

    private AbstractCard getRandomNonBasicCard() {
        ArrayList<AbstractCard> list = new ArrayList<AbstractCard>();
        for (AbstractCard c : AbstractDungeon.player.masterDeck.group) {
            if (c.rarity == AbstractCard.CardRarity.BASIC || c.type == AbstractCard.CardType.CURSE) continue;
            list.add(c);
        }
        if (list.isEmpty()) {
            return null;
        }
        Collections.shuffle(list, new Random(AbstractDungeon.miscRng.randomLong()));
        return (AbstractCard)list.get(0);
    }

    private int getGoldAmount() {
        if (AbstractDungeon.player.gold < 50) {
            return 0;
        }
        if (AbstractDungeon.player.gold > 150) {
            return AbstractDungeon.miscRng.random(50, 150);
        }
        return AbstractDungeon.miscRng.random(50, AbstractDungeon.player.gold);
    }

    @Override
    protected void buttonEffect(int buttonPressed) {
        switch (this.screen) {
            case INTRO: {
                this.screen = CUR_SCREEN.COMPLETE;
                switch (buttonPressed) {
                    case 0: {
                        this.imageEventText.updateBodyText(DESCRIPTIONS[1] + DESCRIPTIONS[5]);
                        AbstractDungeon.player.removePotion(this.potionOption);
                        this.relicReward();
                        WeMeetAgain.logMetricObtainRelic(ID, "Gave Potion", this.givenRelic);
                        break;
                    }
                    case 1: {
                        this.imageEventText.updateBodyText(DESCRIPTIONS[2] + DESCRIPTIONS[5]);
                        AbstractDungeon.player.loseGold(this.goldAmount);
                        this.relicReward();
                        WeMeetAgain.logMetricObtainRelicAtCost(ID, "Paid Gold", this.givenRelic, this.goldAmount);
                        break;
                    }
                    case 2: {
                        this.imageEventText.updateBodyText(DESCRIPTIONS[3] + DESCRIPTIONS[5]);
                        AbstractDungeon.topLevelEffects.add(new PurgeCardEffect(this.cardOption, (float)Settings.WIDTH / 2.0f, (float)Settings.HEIGHT / 2.0f));
                        AbstractDungeon.player.masterDeck.removeCard(this.cardOption);
                        this.relicReward();
                        WeMeetAgain.logMetricRemoveCardAndObtainRelic(ID, "Gave Card", this.cardOption, this.givenRelic);
                        break;
                    }
                    case 3: {
                        this.imageEventText.updateBodyText(DESCRIPTIONS[4]);
                        CardCrawlGame.screenShake.shake(ScreenShake.ShakeIntensity.HIGH, ScreenShake.ShakeDur.SHORT, false);
                        CardCrawlGame.sound.play("BLUNT_HEAVY");
                        WeMeetAgain.logMetricIgnored(ID);
                        break;
                    }
                }
                this.imageEventText.updateDialogOption(0, OPTIONS[8]);
                this.imageEventText.clearRemainingOptions();
                break;
            }
            case COMPLETE: {
                this.openMap();
            }
        }
    }

    private void relicReward() {
        this.givenRelic = AbstractDungeon.returnRandomScreenlessRelic(AbstractDungeon.returnRandomRelicTier());
        AbstractDungeon.getCurrRoom().spawnRelicAndObtain((float)Settings.WIDTH * 0.28f, (float)Settings.HEIGHT / 2.0f, this.givenRelic);
    }

    private static enum CUR_SCREEN {
        INTRO,
        COMPLETE;

    }
}

