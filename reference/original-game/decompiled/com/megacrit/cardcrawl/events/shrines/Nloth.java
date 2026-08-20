/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.events.shrines;

import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.events.AbstractEvent;
import com.megacrit.cardcrawl.events.AbstractImageEvent;
import com.megacrit.cardcrawl.localization.EventStrings;
import com.megacrit.cardcrawl.relics.AbstractRelic;
import com.megacrit.cardcrawl.relics.Circlet;
import com.megacrit.cardcrawl.relics.NlothsGift;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;

public class Nloth
extends AbstractImageEvent {
    public static final String ID = "N'loth";
    private static final EventStrings eventStrings = CardCrawlGame.languagePack.getEventString("N'loth");
    public static final String NAME = Nloth.eventStrings.NAME;
    public static final String[] DESCRIPTIONS = Nloth.eventStrings.DESCRIPTIONS;
    public static final String[] OPTIONS = Nloth.eventStrings.OPTIONS;
    private static final String DIALOG_1 = DESCRIPTIONS[0];
    private static final String DIALOG_2 = DESCRIPTIONS[1];
    private static final String DIALOG_3 = DESCRIPTIONS[2];
    private int screenNum = 0;
    private AbstractRelic choice1;
    private AbstractRelic choice2;
    private AbstractRelic gift;

    public Nloth() {
        super(NAME, DIALOG_1, "images/events/nloth.jpg");
        ArrayList<AbstractRelic> relics = new ArrayList<AbstractRelic>();
        relics.addAll(AbstractDungeon.player.relics);
        Collections.shuffle(relics, new Random(AbstractDungeon.miscRng.randomLong()));
        this.choice1 = (AbstractRelic)relics.get(0);
        this.choice2 = (AbstractRelic)relics.get(1);
        this.gift = new NlothsGift();
        this.imageEventText.setDialogOption(OPTIONS[0] + this.choice1.name + OPTIONS[1], new NlothsGift());
        this.imageEventText.setDialogOption(OPTIONS[0] + this.choice2.name + OPTIONS[1], new NlothsGift());
        this.imageEventText.setDialogOption(OPTIONS[2]);
    }

    @Override
    public void onEnterRoom() {
        if (Settings.AMBIANCE_ON) {
            CardCrawlGame.sound.play("EVENT_SERPENT");
        }
    }

    @Override
    protected void buttonEffect(int buttonPressed) {
        block0 : switch (this.screenNum) {
            case 0: {
                switch (buttonPressed) {
                    case 0: {
                        this.imageEventText.updateBodyText(DIALOG_2);
                        if (AbstractDungeon.player.hasRelic("Nloth's Gift")) {
                            this.gift = new Circlet();
                            AbstractEvent.logMetricRelicSwap(ID, "Traded Relic", this.gift, this.choice1);
                            AbstractDungeon.getCurrRoom().spawnRelicAndObtain(Settings.WIDTH / 2, Settings.HEIGHT / 2, this.gift);
                        } else {
                            AbstractEvent.logMetricRelicSwap(ID, "Traded Relic", this.gift, this.choice1);
                            AbstractDungeon.player.loseRelic(this.choice1.relicId);
                            AbstractDungeon.getCurrRoom().spawnRelicAndObtain(Settings.WIDTH / 2, Settings.HEIGHT / 2, this.gift);
                        }
                        this.screenNum = 1;
                        this.imageEventText.updateDialogOption(0, OPTIONS[2]);
                        this.imageEventText.clearRemainingOptions();
                        break block0;
                    }
                    case 1: {
                        this.imageEventText.updateBodyText(DIALOG_2);
                        if (AbstractDungeon.player.hasRelic("Nloth's Gift")) {
                            this.gift = new Circlet();
                            AbstractEvent.logMetricRelicSwap(ID, "Traded Relic", this.gift, this.choice2);
                            AbstractDungeon.getCurrRoom().spawnRelicAndObtain(Settings.WIDTH / 2, Settings.HEIGHT / 2, this.gift);
                        } else {
                            AbstractEvent.logMetricRelicSwap(ID, "Traded Relic", this.gift, this.choice2);
                            AbstractDungeon.player.loseRelic(this.choice2.relicId);
                            AbstractDungeon.getCurrRoom().spawnRelicAndObtain(Settings.WIDTH / 2, Settings.HEIGHT / 2, this.gift);
                        }
                        this.screenNum = 1;
                        this.imageEventText.updateDialogOption(0, OPTIONS[2]);
                        this.imageEventText.clearRemainingOptions();
                        break block0;
                    }
                    case 2: {
                        AbstractEvent.logMetricIgnored(ID);
                        this.imageEventText.updateBodyText(DIALOG_3);
                        this.screenNum = 1;
                        this.imageEventText.updateDialogOption(0, OPTIONS[2]);
                        this.imageEventText.clearRemainingOptions();
                        break block0;
                    }
                }
                this.imageEventText.updateBodyText(DIALOG_3);
                this.screenNum = 1;
                this.imageEventText.updateDialogOption(0, OPTIONS[2]);
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
}

