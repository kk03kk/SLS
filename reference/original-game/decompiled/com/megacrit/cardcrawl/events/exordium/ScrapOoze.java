/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.events.exordium;

import com.megacrit.cardcrawl.cards.DamageInfo;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.events.AbstractEvent;
import com.megacrit.cardcrawl.events.AbstractImageEvent;
import com.megacrit.cardcrawl.localization.EventStrings;
import com.megacrit.cardcrawl.relics.AbstractRelic;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ScrapOoze
extends AbstractImageEvent {
    private static final Logger logger = LogManager.getLogger(ScrapOoze.class.getName());
    public static final String ID = "Scrap Ooze";
    private static final EventStrings eventStrings = CardCrawlGame.languagePack.getEventString("Scrap Ooze");
    public static final String NAME = ScrapOoze.eventStrings.NAME;
    public static final String[] DESCRIPTIONS = ScrapOoze.eventStrings.DESCRIPTIONS;
    public static final String[] OPTIONS = ScrapOoze.eventStrings.OPTIONS;
    private int relicObtainChance = 25;
    private int dmg = 3;
    private int totalDamageDealt = 0;
    private int screenNum = 0;
    private static final String DIALOG_1 = DESCRIPTIONS[0];
    private static final String FAIL_MSG = DESCRIPTIONS[1];
    private static final String SUCCESS_MSG = DESCRIPTIONS[2];
    private static final String ESCAPE_MSG = DESCRIPTIONS[3];

    public ScrapOoze() {
        super(NAME, DIALOG_1, "images/events/scrapOoze.jpg");
        if (AbstractDungeon.ascensionLevel >= 15) {
            this.dmg = 5;
        }
        this.imageEventText.setDialogOption(OPTIONS[0] + this.dmg + OPTIONS[1] + this.relicObtainChance + OPTIONS[2]);
        this.imageEventText.setDialogOption(OPTIONS[3]);
    }

    @Override
    public void onEnterRoom() {
        if (Settings.AMBIANCE_ON) {
            CardCrawlGame.sound.play("EVENT_OOZE");
        }
    }

    @Override
    protected void buttonEffect(int buttonPressed) {
        block0 : switch (this.screenNum) {
            case 0: {
                switch (buttonPressed) {
                    case 0: {
                        AbstractDungeon.player.damage(new DamageInfo(null, this.dmg));
                        CardCrawlGame.sound.play("ATTACK_POISON");
                        this.totalDamageDealt += this.dmg;
                        int random = AbstractDungeon.miscRng.random(0, 99);
                        if (random >= 99 - this.relicObtainChance) {
                            this.imageEventText.updateBodyText(SUCCESS_MSG);
                            AbstractRelic r = AbstractDungeon.returnRandomScreenlessRelic(AbstractDungeon.returnRandomRelicTier());
                            AbstractEvent.logMetricObtainRelicAndDamage(ID, "Success", r, this.totalDamageDealt);
                            this.imageEventText.updateDialogOption(0, OPTIONS[3]);
                            this.imageEventText.removeDialogOption(1);
                            this.screenNum = 1;
                            AbstractDungeon.getCurrRoom().spawnRelicAndObtain((float)Settings.WIDTH / 2.0f, (float)Settings.HEIGHT / 2.0f, r);
                            break block0;
                        }
                        this.imageEventText.updateBodyText(FAIL_MSG);
                        this.relicObtainChance += 10;
                        ++this.dmg;
                        this.imageEventText.updateDialogOption(0, OPTIONS[4] + this.dmg + OPTIONS[1] + this.relicObtainChance + OPTIONS[2]);
                        this.imageEventText.updateDialogOption(1, OPTIONS[3]);
                        break block0;
                    }
                    case 1: {
                        AbstractEvent.logMetricTakeDamage(ID, "Fled", this.totalDamageDealt);
                        this.imageEventText.updateBodyText(ESCAPE_MSG);
                        this.imageEventText.updateDialogOption(0, OPTIONS[3]);
                        this.imageEventText.removeDialogOption(1);
                        this.screenNum = 1;
                        break block0;
                    }
                }
                logger.info("ERROR: case " + buttonPressed + " should never be called");
                break;
            }
            case 1: {
                this.openMap();
            }
        }
    }
}

