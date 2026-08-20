/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.events.exordium;

import com.megacrit.cardcrawl.cards.DamageInfo;
import com.megacrit.cardcrawl.cards.curses.Injury;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.events.AbstractEvent;
import com.megacrit.cardcrawl.events.AbstractImageEvent;
import com.megacrit.cardcrawl.helpers.CardLibrary;
import com.megacrit.cardcrawl.helpers.RelicLibrary;
import com.megacrit.cardcrawl.helpers.ScreenShake;
import com.megacrit.cardcrawl.localization.EventStrings;
import com.megacrit.cardcrawl.relics.AbstractRelic;
import com.megacrit.cardcrawl.relics.GoldenIdol;
import com.megacrit.cardcrawl.vfx.cardManip.ShowCardAndObtainEffect;

public class GoldenIdolEvent
extends AbstractImageEvent {
    public static final String ID = "Golden Idol";
    private static final EventStrings eventStrings = CardCrawlGame.languagePack.getEventString("Golden Idol");
    public static final String NAME = GoldenIdolEvent.eventStrings.NAME;
    public static final String[] DESCRIPTIONS = GoldenIdolEvent.eventStrings.DESCRIPTIONS;
    public static final String[] OPTIONS = GoldenIdolEvent.eventStrings.OPTIONS;
    private static final String DIALOG_START = DESCRIPTIONS[0];
    private static final String DIALOG_BOULDER = DESCRIPTIONS[1];
    private static final String DIALOG_CHOSE_RUN = DESCRIPTIONS[2];
    private static final String DIALOG_CHOSE_FIGHT = DESCRIPTIONS[3];
    private static final String DIALOG_CHOSE_FLAT = DESCRIPTIONS[4];
    private static final String DIALOG_IGNORE = DESCRIPTIONS[5];
    private int screenNum = 0;
    private static final float HP_LOSS_PERCENT = 0.25f;
    private static final float MAX_HP_LOSS_PERCENT = 0.08f;
    private static final float A_2_HP_LOSS_PERCENT = 0.35f;
    private static final float A_2_MAX_HP_LOSS_PERCENT = 0.1f;
    private int damage;
    private int maxHpLoss;
    private AbstractRelic relicMetric = null;

    public GoldenIdolEvent() {
        super(NAME, DIALOG_START, "images/events/goldenIdol.jpg");
        this.imageEventText.setDialogOption(OPTIONS[0], new GoldenIdol());
        this.imageEventText.setDialogOption(OPTIONS[1]);
        if (AbstractDungeon.ascensionLevel >= 15) {
            this.damage = (int)((float)AbstractDungeon.player.maxHealth * 0.35f);
            this.maxHpLoss = (int)((float)AbstractDungeon.player.maxHealth * 0.1f);
        } else {
            this.damage = (int)((float)AbstractDungeon.player.maxHealth * 0.25f);
            this.maxHpLoss = (int)((float)AbstractDungeon.player.maxHealth * 0.08f);
        }
        if (this.maxHpLoss < 1) {
            this.maxHpLoss = 1;
        }
    }

    @Override
    public void onEnterRoom() {
        if (Settings.AMBIANCE_ON) {
            CardCrawlGame.sound.play("EVENT_GOLDEN");
        }
    }

    @Override
    protected void buttonEffect(int buttonPressed) {
        block0 : switch (this.screenNum) {
            case 0: {
                switch (buttonPressed) {
                    case 0: {
                        this.imageEventText.updateBodyText(DIALOG_BOULDER);
                        this.relicMetric = AbstractDungeon.player.hasRelic(ID) ? RelicLibrary.getRelic("Circlet").makeCopy() : RelicLibrary.getRelic(ID).makeCopy();
                        AbstractDungeon.getCurrRoom().spawnRelicAndObtain(Settings.WIDTH / 2, Settings.HEIGHT / 2, this.relicMetric);
                        CardCrawlGame.screenShake.mildRumble(5.0f);
                        CardCrawlGame.sound.play("BLUNT_HEAVY");
                        this.screenNum = 1;
                        this.imageEventText.updateDialogOption(0, OPTIONS[2], CardLibrary.getCopy("Injury"));
                        this.imageEventText.updateDialogOption(1, OPTIONS[3] + this.damage + OPTIONS[4]);
                        this.imageEventText.setDialogOption(OPTIONS[5] + this.maxHpLoss + OPTIONS[6]);
                        break block0;
                    }
                }
                this.imageEventText.updateBodyText(DIALOG_IGNORE);
                this.screenNum = 2;
                this.imageEventText.updateDialogOption(0, OPTIONS[1]);
                this.imageEventText.clearRemainingOptions();
                AbstractEvent.logMetricIgnored(ID);
                break;
            }
            case 1: {
                switch (buttonPressed) {
                    case 0: {
                        CardCrawlGame.screenShake.shake(ScreenShake.ShakeIntensity.MED, ScreenShake.ShakeDur.MED, false);
                        this.imageEventText.updateBodyText(DIALOG_CHOSE_RUN);
                        Injury curse = new Injury();
                        AbstractDungeon.effectList.add(new ShowCardAndObtainEffect(curse, (float)Settings.WIDTH / 2.0f, (float)Settings.HEIGHT / 2.0f));
                        this.screenNum = 2;
                        this.imageEventText.updateDialogOption(0, OPTIONS[1]);
                        AbstractEvent.logMetricObtainCardAndRelic(ID, "Take Wound", curse, this.relicMetric);
                        this.imageEventText.clearRemainingOptions();
                        break block0;
                    }
                    case 1: {
                        this.imageEventText.updateBodyText(DIALOG_CHOSE_FIGHT);
                        CardCrawlGame.screenShake.shake(ScreenShake.ShakeIntensity.MED, ScreenShake.ShakeDur.MED, false);
                        CardCrawlGame.sound.play("BLUNT_FAST");
                        AbstractDungeon.player.damage(new DamageInfo(null, this.damage));
                        this.screenNum = 2;
                        this.imageEventText.updateDialogOption(0, OPTIONS[1]);
                        AbstractEvent.logMetricObtainRelicAndDamage(ID, "Take Damage", this.relicMetric, this.damage);
                        this.imageEventText.clearRemainingOptions();
                        break block0;
                    }
                    case 2: {
                        this.imageEventText.updateBodyText(DIALOG_CHOSE_FLAT);
                        AbstractDungeon.player.decreaseMaxHealth(this.maxHpLoss);
                        CardCrawlGame.screenShake.shake(ScreenShake.ShakeIntensity.MED, ScreenShake.ShakeDur.MED, false);
                        CardCrawlGame.sound.play("BLUNT_FAST");
                        this.screenNum = 2;
                        this.imageEventText.updateDialogOption(0, OPTIONS[1]);
                        AbstractEvent.logMetricObtainRelicAndLoseMaxHP(ID, "Lose Max HP", this.relicMetric, this.maxHpLoss);
                        this.imageEventText.clearRemainingOptions();
                        break block0;
                    }
                }
                this.openMap();
                break;
            }
            case 2: {
                this.openMap();
                break;
            }
            default: {
                this.openMap();
            }
        }
    }
}

