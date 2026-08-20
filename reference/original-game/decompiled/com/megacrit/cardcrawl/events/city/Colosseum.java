/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.events.city;

import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.events.AbstractEvent;
import com.megacrit.cardcrawl.events.AbstractImageEvent;
import com.megacrit.cardcrawl.helpers.MonsterHelper;
import com.megacrit.cardcrawl.localization.EventStrings;
import com.megacrit.cardcrawl.relics.AbstractRelic;

public class Colosseum
extends AbstractImageEvent {
    public static final String ID = "Colosseum";
    private static final EventStrings eventStrings = CardCrawlGame.languagePack.getEventString("Colosseum");
    public static final String NAME = Colosseum.eventStrings.NAME;
    public static final String[] DESCRIPTIONS = Colosseum.eventStrings.DESCRIPTIONS;
    public static final String[] OPTIONS = Colosseum.eventStrings.OPTIONS;
    private CurScreen screen = CurScreen.INTRO;

    public Colosseum() {
        super(NAME, DESCRIPTIONS[0], "images/events/colosseum.jpg");
        this.imageEventText.setDialogOption(OPTIONS[0]);
    }

    @Override
    protected void buttonEffect(int buttonPressed) {
        block0 : switch (this.screen) {
            case INTRO: {
                switch (buttonPressed) {
                    case 0: {
                        this.imageEventText.updateBodyText(DESCRIPTIONS[1] + DESCRIPTIONS[2] + 4200 + DESCRIPTIONS[3]);
                        this.imageEventText.updateDialogOption(0, OPTIONS[1]);
                        this.screen = CurScreen.FIGHT;
                    }
                }
                break;
            }
            case FIGHT: {
                switch (buttonPressed) {
                    case 0: {
                        this.screen = CurScreen.POST_COMBAT;
                        this.logMetric("Fight");
                        AbstractDungeon.getCurrRoom().monsters = MonsterHelper.getEncounter("Colosseum Slavers");
                        AbstractDungeon.getCurrRoom().rewards.clear();
                        AbstractDungeon.getCurrRoom().rewardAllowed = false;
                        this.enterCombatFromImage();
                        AbstractDungeon.lastCombatMetricKey = "Colosseum Slavers";
                        break;
                    }
                }
                this.imageEventText.clearRemainingOptions();
                break;
            }
            case POST_COMBAT: {
                AbstractDungeon.getCurrRoom().rewardAllowed = true;
                switch (buttonPressed) {
                    case 1: {
                        this.screen = CurScreen.LEAVE;
                        this.logMetric("Fought Nobs");
                        AbstractDungeon.getCurrRoom().monsters = MonsterHelper.getEncounter("Colosseum Nobs");
                        AbstractDungeon.getCurrRoom().rewards.clear();
                        AbstractDungeon.getCurrRoom().addRelicToRewards(AbstractRelic.RelicTier.RARE);
                        AbstractDungeon.getCurrRoom().addRelicToRewards(AbstractRelic.RelicTier.UNCOMMON);
                        AbstractDungeon.getCurrRoom().addGoldToRewards(100);
                        AbstractDungeon.getCurrRoom().eliteTrigger = true;
                        this.enterCombatFromImage();
                        AbstractDungeon.lastCombatMetricKey = "Colosseum Nobs";
                        break block0;
                    }
                }
                this.logMetric("Fled From Nobs");
                this.openMap();
                break;
            }
            case LEAVE: {
                this.openMap();
                break;
            }
            default: {
                this.openMap();
            }
        }
    }

    public void logMetric(String actionTaken) {
        AbstractEvent.logMetric(ID, actionTaken);
    }

    @Override
    public void reopen() {
        if (this.screen != CurScreen.LEAVE) {
            AbstractDungeon.resetPlayer();
            AbstractDungeon.player.drawX = (float)Settings.WIDTH * 0.25f;
            AbstractDungeon.player.preBattlePrep();
            this.enterImageFromCombat();
            this.imageEventText.updateBodyText(DESCRIPTIONS[4]);
            this.imageEventText.updateDialogOption(0, OPTIONS[2]);
            this.imageEventText.setDialogOption(OPTIONS[3]);
        }
    }

    private static enum CurScreen {
        INTRO,
        FIGHT,
        LEAVE,
        POST_COMBAT;

    }
}

