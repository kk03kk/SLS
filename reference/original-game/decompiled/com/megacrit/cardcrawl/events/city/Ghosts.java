/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.events.city;

import com.badlogic.gdx.math.MathUtils;
import com.megacrit.cardcrawl.cards.colorless.Apparition;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.events.AbstractImageEvent;
import com.megacrit.cardcrawl.localization.EventStrings;
import com.megacrit.cardcrawl.vfx.cardManip.ShowCardAndObtainEffect;
import java.util.ArrayList;

public class Ghosts
extends AbstractImageEvent {
    public static final String ID = "Ghosts";
    private static final EventStrings eventStrings = CardCrawlGame.languagePack.getEventString("Ghosts");
    public static final String NAME = Ghosts.eventStrings.NAME;
    public static final String[] DESCRIPTIONS = Ghosts.eventStrings.DESCRIPTIONS;
    public static final String[] OPTIONS = Ghosts.eventStrings.OPTIONS;
    private static final String INTRO_BODY_M = DESCRIPTIONS[0];
    private static final String ACCEPT_BODY = DESCRIPTIONS[2];
    private static final String EXIT_BODY = DESCRIPTIONS[3];
    private static final float HP_DRAIN = 0.5f;
    private int screenNum = 0;
    private int hpLoss = 0;

    public Ghosts() {
        super(NAME, "test", "images/events/ghost.jpg");
        this.body = INTRO_BODY_M;
        this.hpLoss = MathUtils.ceil((float)AbstractDungeon.player.maxHealth * 0.5f);
        if (this.hpLoss >= AbstractDungeon.player.maxHealth) {
            this.hpLoss = AbstractDungeon.player.maxHealth - 1;
        }
        if (AbstractDungeon.ascensionLevel >= 15) {
            this.imageEventText.setDialogOption(OPTIONS[3] + this.hpLoss + OPTIONS[1], new Apparition());
        } else {
            this.imageEventText.setDialogOption(OPTIONS[0] + this.hpLoss + OPTIONS[1], new Apparition());
        }
        this.imageEventText.setDialogOption(OPTIONS[2]);
    }

    @Override
    public void onEnterRoom() {
        if (Settings.AMBIANCE_ON) {
            CardCrawlGame.sound.play("EVENT_GHOSTS");
        }
    }

    @Override
    protected void buttonEffect(int buttonPressed) {
        block0 : switch (this.screenNum) {
            case 0: {
                switch (buttonPressed) {
                    case 0: {
                        this.imageEventText.updateBodyText(ACCEPT_BODY);
                        AbstractDungeon.player.decreaseMaxHealth(this.hpLoss);
                        this.becomeGhost();
                        this.screenNum = 1;
                        this.imageEventText.updateDialogOption(0, OPTIONS[5]);
                        this.imageEventText.clearRemainingOptions();
                        break block0;
                    }
                }
                Ghosts.logMetricIgnored(ID);
                this.imageEventText.updateBodyText(EXIT_BODY);
                this.screenNum = 2;
                this.imageEventText.updateDialogOption(0, OPTIONS[5]);
                this.imageEventText.clearRemainingOptions();
                break;
            }
            case 1: {
                this.openMap();
                break;
            }
            default: {
                this.openMap();
            }
        }
    }

    private void becomeGhost() {
        ArrayList<String> cards = new ArrayList<String>();
        int amount = 5;
        if (AbstractDungeon.ascensionLevel >= 15) {
            amount -= 2;
        }
        for (int i = 0; i < amount; ++i) {
            Apparition c = new Apparition();
            cards.add(c.cardID);
            AbstractDungeon.effectList.add(new ShowCardAndObtainEffect(c, (float)Settings.WIDTH / 2.0f, (float)Settings.HEIGHT / 2.0f));
        }
        Ghosts.logMetricObtainCardsLoseMapHP(ID, "Became a Ghost", cards, this.hpLoss);
    }
}

