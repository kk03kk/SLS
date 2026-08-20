/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.events.beyond;

import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.events.AbstractImageEvent;
import com.megacrit.cardcrawl.localization.EventStrings;
import com.megacrit.cardcrawl.relics.RedMask;
import com.megacrit.cardcrawl.vfx.RainingGoldEffect;

public class TombRedMask
extends AbstractImageEvent {
    public static final String ID = "Tomb of Lord Red Mask";
    private static final EventStrings eventStrings = CardCrawlGame.languagePack.getEventString("Tomb of Lord Red Mask");
    public static final String NAME = TombRedMask.eventStrings.NAME;
    public static final String[] DESCRIPTIONS = TombRedMask.eventStrings.DESCRIPTIONS;
    public static final String[] OPTIONS = TombRedMask.eventStrings.OPTIONS;
    private static final int GOLD_AMT = 222;
    private static final String DIALOG_1 = DESCRIPTIONS[0];
    private static final String MASK_RESULT = DESCRIPTIONS[1];
    private static final String RELIC_RESULT = DESCRIPTIONS[2];
    private CurScreen screen = CurScreen.INTRO;

    public TombRedMask() {
        super(NAME, DIALOG_1, "images/events/redMaskTomb.jpg");
        if (AbstractDungeon.player.hasRelic("Red Mask")) {
            this.imageEventText.setDialogOption(OPTIONS[0]);
        } else {
            this.imageEventText.setDialogOption(OPTIONS[1], true);
            this.imageEventText.setDialogOption(OPTIONS[2] + AbstractDungeon.player.gold + OPTIONS[3], new RedMask());
        }
        this.imageEventText.setDialogOption(OPTIONS[4]);
    }

    @Override
    protected void buttonEffect(int buttonPressed) {
        switch (this.screen) {
            case INTRO: {
                if (buttonPressed == 0) {
                    AbstractDungeon.effectList.add(new RainingGoldEffect(222));
                    AbstractDungeon.player.gainGold(222);
                    this.imageEventText.updateBodyText(MASK_RESULT);
                    TombRedMask.logMetricGainGold(ID, "Wore Mask", 222);
                } else if (buttonPressed == 1 && !AbstractDungeon.player.hasRelic("Red Mask")) {
                    RedMask r = new RedMask();
                    TombRedMask.logMetricObtainRelicAtCost(ID, "Paid", r, AbstractDungeon.player.gold);
                    AbstractDungeon.player.loseGold(AbstractDungeon.player.gold);
                    AbstractDungeon.getCurrRoom().spawnRelicAndObtain(Settings.WIDTH / 2, Settings.HEIGHT / 2, r);
                    this.imageEventText.updateBodyText(RELIC_RESULT);
                } else {
                    this.openMap();
                    this.imageEventText.clearAllDialogs();
                    this.imageEventText.setDialogOption(OPTIONS[4]);
                    TombRedMask.logMetricIgnored(ID);
                }
                this.imageEventText.clearAllDialogs();
                this.imageEventText.setDialogOption(OPTIONS[4]);
                this.screen = CurScreen.RESULT;
                break;
            }
            default: {
                this.openMap();
            }
        }
    }

    private static enum CurScreen {
        INTRO,
        RESULT;

    }
}

