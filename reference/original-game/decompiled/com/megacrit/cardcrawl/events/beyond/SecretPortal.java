/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.events.beyond;

import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.events.AbstractImageEvent;
import com.megacrit.cardcrawl.localization.EventStrings;
import com.megacrit.cardcrawl.map.MapRoomNode;
import com.megacrit.cardcrawl.rooms.AbstractRoom;
import com.megacrit.cardcrawl.rooms.MonsterRoomBoss;
import com.megacrit.cardcrawl.vfx.FadeWipeParticle;

public class SecretPortal
extends AbstractImageEvent {
    public static final String ID = "SecretPortal";
    private static final EventStrings eventStrings = CardCrawlGame.languagePack.getEventString("SecretPortal");
    public static final String NAME = SecretPortal.eventStrings.NAME;
    public static final String[] DESCRIPTIONS = SecretPortal.eventStrings.DESCRIPTIONS;
    public static final String[] OPTIONS = SecretPortal.eventStrings.OPTIONS;
    public static final String EVENT_CHOICE_TOOK_PORTAL = "Took Portal";
    private static final String DIALOG_1 = DESCRIPTIONS[0];
    private static final String DIALOG_2 = DESCRIPTIONS[1];
    private static final String DIALOG_3 = DESCRIPTIONS[2];
    private CurScreen screen = CurScreen.INTRO;

    public SecretPortal() {
        super(NAME, DIALOG_1, "images/events/secretPortal.jpg");
        this.imageEventText.setDialogOption(OPTIONS[0]);
        this.imageEventText.setDialogOption(OPTIONS[1]);
    }

    @Override
    public void onEnterRoom() {
        if (Settings.AMBIANCE_ON) {
            CardCrawlGame.sound.play("EVENT_PORTAL");
        }
    }

    @Override
    protected void buttonEffect(int buttonPressed) {
        switch (this.screen) {
            case INTRO: {
                switch (buttonPressed) {
                    case 0: {
                        this.imageEventText.updateBodyText(DIALOG_2);
                        this.screen = CurScreen.ACCEPT;
                        SecretPortal.logMetric(ID, EVENT_CHOICE_TOOK_PORTAL);
                        this.imageEventText.updateDialogOption(0, OPTIONS[1]);
                        CardCrawlGame.screenShake.mildRumble(5.0f);
                        CardCrawlGame.sound.play("ATTACK_MAGIC_SLOW_2");
                        break;
                    }
                    case 1: {
                        this.imageEventText.updateBodyText(DIALOG_3);
                        this.screen = CurScreen.LEAVE;
                        SecretPortal.logMetricIgnored(ID);
                        this.imageEventText.updateDialogOption(0, OPTIONS[1]);
                    }
                }
                this.imageEventText.clearRemainingOptions();
                break;
            }
            case ACCEPT: {
                AbstractDungeon.getCurrRoom().phase = AbstractRoom.RoomPhase.COMPLETE;
                MapRoomNode node = new MapRoomNode(-1, 15);
                node.room = new MonsterRoomBoss();
                AbstractDungeon.nextRoom = node;
                CardCrawlGame.music.fadeOutTempBGM();
                AbstractDungeon.pathX.add(1);
                AbstractDungeon.pathY.add(15);
                AbstractDungeon.topLevelEffects.add(new FadeWipeParticle());
                AbstractDungeon.nextRoomTransitionStart();
                break;
            }
            default: {
                this.openMap();
            }
        }
    }

    private static enum CurScreen {
        INTRO,
        ACCEPT,
        LEAVE;

    }
}

