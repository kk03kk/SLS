/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.events.city;

import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.events.AbstractEvent;
import com.megacrit.cardcrawl.events.RoomEventDialog;
import com.megacrit.cardcrawl.helpers.MonsterHelper;
import com.megacrit.cardcrawl.localization.EventStrings;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import com.megacrit.cardcrawl.relics.Circlet;
import com.megacrit.cardcrawl.relics.RedMask;
import com.megacrit.cardcrawl.vfx.GainPennyEffect;

public class MaskedBandits
extends AbstractEvent {
    public static final String ID = "Masked Bandits";
    private static final EventStrings eventStrings = CardCrawlGame.languagePack.getEventString("Masked Bandits");
    public static final String NAME = MaskedBandits.eventStrings.NAME;
    public static final String[] DESCRIPTIONS = MaskedBandits.eventStrings.DESCRIPTIONS;
    public static final String[] OPTIONS = MaskedBandits.eventStrings.OPTIONS;
    private static final String PAID_MSG_1 = DESCRIPTIONS[0];
    private static final String PAID_MSG_2 = DESCRIPTIONS[1];
    private static final String PAID_MSG_3 = DESCRIPTIONS[2];
    private static final String PAID_MSG_4 = DESCRIPTIONS[3];
    private CurScreen screen = CurScreen.INTRO;

    public MaskedBandits() {
        this.body = DESCRIPTIONS[4];
        this.roomEventText.addDialogOption(OPTIONS[0]);
        this.roomEventText.addDialogOption(OPTIONS[1]);
        this.hasDialog = true;
        this.hasFocus = true;
        AbstractDungeon.getCurrRoom().monsters = MonsterHelper.getEncounter(ID);
    }

    @Override
    public void update() {
        super.update();
        if (!RoomEventDialog.waitForInput) {
            this.buttonEffect(this.roomEventText.getSelectedOption());
        }
    }

    @Override
    protected void buttonEffect(int buttonPressed) {
        switch (this.screen) {
            case INTRO: {
                switch (buttonPressed) {
                    case 0: {
                        this.stealGold();
                        AbstractDungeon.player.loseGold(AbstractDungeon.player.gold);
                        this.roomEventText.updateBodyText(PAID_MSG_1);
                        this.roomEventText.updateDialogOption(0, OPTIONS[2]);
                        this.roomEventText.clearRemainingOptions();
                        this.screen = CurScreen.PAID_1;
                        return;
                    }
                    case 1: {
                        MaskedBandits.logMetric(ID, "Fought Bandits");
                        if (Settings.isDailyRun) {
                            AbstractDungeon.getCurrRoom().addGoldToRewards(AbstractDungeon.miscRng.random(30));
                        } else {
                            AbstractDungeon.getCurrRoom().addGoldToRewards(AbstractDungeon.miscRng.random(25, 35));
                        }
                        if (AbstractDungeon.player.hasRelic("Red Mask")) {
                            AbstractDungeon.getCurrRoom().addRelicToRewards(new Circlet());
                        } else {
                            AbstractDungeon.getCurrRoom().addRelicToRewards(new RedMask());
                        }
                        this.enterCombat();
                        AbstractDungeon.lastCombatMetricKey = ID;
                        return;
                    }
                }
                break;
            }
            case PAID_1: {
                this.roomEventText.updateBodyText(PAID_MSG_2);
                this.screen = CurScreen.PAID_2;
                this.roomEventText.updateDialogOption(0, OPTIONS[2]);
                break;
            }
            case PAID_2: {
                this.roomEventText.updateBodyText(PAID_MSG_3);
                this.screen = CurScreen.PAID_3;
                this.roomEventText.updateDialogOption(0, OPTIONS[3]);
                break;
            }
            case PAID_3: {
                this.roomEventText.updateBodyText(PAID_MSG_4);
                this.roomEventText.updateDialogOption(0, OPTIONS[3]);
                this.screen = CurScreen.END;
                this.openMap();
                break;
            }
            case END: {
                this.openMap();
            }
        }
    }

    private void stealGold() {
        AbstractPlayer target = AbstractDungeon.player;
        if (target.gold == 0) {
            return;
        }
        MaskedBandits.logMetricLoseGold(ID, "Paid Fearfully", target.gold);
        CardCrawlGame.sound.play("GOLD_JINGLE");
        for (int i = 0; i < target.gold; ++i) {
            AbstractMonster source = AbstractDungeon.getCurrRoom().monsters.getRandomMonster();
            AbstractDungeon.effectList.add(new GainPennyEffect(source, target.hb.cX, target.hb.cY, source.hb.cX, source.hb.cY, false));
        }
    }

    private static enum CurScreen {
        INTRO,
        PAID_1,
        PAID_2,
        PAID_3,
        END;

    }
}

