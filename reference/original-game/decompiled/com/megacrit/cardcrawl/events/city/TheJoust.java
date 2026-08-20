/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.events.city;

import com.badlogic.gdx.Gdx;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.events.AbstractImageEvent;
import com.megacrit.cardcrawl.helpers.ScreenShake;
import com.megacrit.cardcrawl.localization.EventStrings;

public class TheJoust
extends AbstractImageEvent {
    public static final String ID = "The Joust";
    private static final EventStrings eventStrings = CardCrawlGame.languagePack.getEventString("The Joust");
    public static final String NAME = TheJoust.eventStrings.NAME;
    public static final String[] DESCRIPTIONS = TheJoust.eventStrings.DESCRIPTIONS;
    public static final String[] OPTIONS = TheJoust.eventStrings.OPTIONS;
    private static final String HALT_MSG = DESCRIPTIONS[0];
    private static final String EXPL_MSG = DESCRIPTIONS[1];
    private static final String BET_AGAINST = DESCRIPTIONS[2];
    private static final String BET_FOR = DESCRIPTIONS[3];
    private static final String COMBAT_MSG = DESCRIPTIONS[4];
    private static final String NOODLES_WIN = DESCRIPTIONS[5];
    private static final String NOODLES_LOSE = DESCRIPTIONS[6];
    private static final String BET_WON_MSG = DESCRIPTIONS[7];
    private static final String BET_LOSE_MSG = DESCRIPTIONS[8];
    private boolean betFor;
    private boolean ownerWins;
    private static final int WIN_OWNER = 250;
    private static final int WIN_MURDERER = 100;
    private static final int BET_AMT = 50;
    private CUR_SCREEN screen = CUR_SCREEN.HALT;
    private float joustTimer = 0.0f;
    private int clangCount = 0;

    public TheJoust() {
        super(NAME, HALT_MSG, "images/events/joust.jpg");
        this.imageEventText.setDialogOption(OPTIONS[0]);
    }

    @Override
    public void update() {
        super.update();
        if (this.joustTimer != 0.0f) {
            this.joustTimer -= Gdx.graphics.getDeltaTime();
            if (this.joustTimer < 0.0f) {
                ++this.clangCount;
                if (this.clangCount == 1) {
                    CardCrawlGame.screenShake.shake(ScreenShake.ShakeIntensity.HIGH, ScreenShake.ShakeDur.MED, false);
                    CardCrawlGame.sound.play("BLUNT_HEAVY");
                    this.joustTimer = 1.0f;
                } else if (this.clangCount == 2) {
                    CardCrawlGame.screenShake.shake(ScreenShake.ShakeIntensity.MED, ScreenShake.ShakeDur.SHORT, false);
                    CardCrawlGame.sound.play("BLUNT_FAST");
                    this.joustTimer = 0.25f;
                } else if (this.clangCount == 3) {
                    CardCrawlGame.screenShake.shake(ScreenShake.ShakeIntensity.MED, ScreenShake.ShakeDur.LONG, false);
                    CardCrawlGame.sound.play("BLUNT_HEAVY");
                    this.joustTimer = 0.0f;
                }
            }
        }
    }

    @Override
    protected void buttonEffect(int buttonPressed) {
        switch (this.screen) {
            case HALT: {
                this.imageEventText.updateBodyText(EXPL_MSG);
                this.imageEventText.updateDialogOption(0, OPTIONS[1] + 50 + OPTIONS[2] + 100 + OPTIONS[3]);
                this.imageEventText.setDialogOption(OPTIONS[4] + 50 + OPTIONS[5] + 250 + OPTIONS[3]);
                this.screen = CUR_SCREEN.EXPLANATION;
                break;
            }
            case EXPLANATION: {
                if (buttonPressed == 0) {
                    this.betFor = false;
                    this.imageEventText.updateBodyText(BET_AGAINST);
                } else {
                    this.betFor = true;
                    this.imageEventText.updateBodyText(BET_FOR);
                }
                AbstractDungeon.player.loseGold(50);
                this.imageEventText.updateDialogOption(0, OPTIONS[6]);
                this.imageEventText.clearRemainingOptions();
                this.screen = CUR_SCREEN.PRE_JOUST;
                break;
            }
            case PRE_JOUST: {
                this.imageEventText.updateBodyText(COMBAT_MSG);
                this.imageEventText.updateDialogOption(0, OPTIONS[6]);
                this.ownerWins = AbstractDungeon.miscRng.randomBoolean(0.3f);
                this.screen = CUR_SCREEN.JOUST;
                this.joustTimer = 0.01f;
                break;
            }
            case JOUST: {
                String tmp;
                this.imageEventText.updateDialogOption(0, OPTIONS[7]);
                if (this.ownerWins) {
                    tmp = NOODLES_WIN;
                    if (this.betFor) {
                        tmp = tmp + BET_WON_MSG;
                        AbstractDungeon.player.gainGold(250);
                        CardCrawlGame.sound.play("GOLD_GAIN");
                        TheJoust.logMetricGainAndLoseGold(ID, "Bet on Owner", 250, 50);
                    } else {
                        tmp = tmp + BET_LOSE_MSG;
                        TheJoust.logMetricLoseGold(ID, "Bet on Owner", 50);
                    }
                } else {
                    tmp = NOODLES_LOSE;
                    if (this.betFor) {
                        tmp = tmp + BET_LOSE_MSG;
                        TheJoust.logMetricLoseGold(ID, "Bet on Murderer", 50);
                    } else {
                        tmp = tmp + BET_WON_MSG;
                        AbstractDungeon.player.gainGold(100);
                        CardCrawlGame.sound.play("GOLD_GAIN");
                        TheJoust.logMetricGainAndLoseGold(ID, "Bet on Murderer", 100, 50);
                    }
                }
                this.imageEventText.updateBodyText(tmp);
                this.screen = CUR_SCREEN.COMPLETE;
                break;
            }
            case COMPLETE: {
                this.openMap();
            }
        }
    }

    private static enum CUR_SCREEN {
        HALT,
        EXPLANATION,
        PRE_JOUST,
        JOUST,
        COMPLETE;

    }
}

