/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.events.beyond;

import com.badlogic.gdx.math.MathUtils;
import com.megacrit.cardcrawl.cards.DamageInfo;
import com.megacrit.cardcrawl.cards.colorless.Madness;
import com.megacrit.cardcrawl.cards.curses.Writhe;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.events.AbstractImageEvent;
import com.megacrit.cardcrawl.helpers.CardLibrary;
import com.megacrit.cardcrawl.helpers.ScreenShake;
import com.megacrit.cardcrawl.localization.EventStrings;
import com.megacrit.cardcrawl.vfx.cardManip.ShowCardAndObtainEffect;
import java.util.ArrayList;

public class WindingHalls
extends AbstractImageEvent {
    public static final String ID = "Winding Halls";
    private static final EventStrings eventStrings = CardCrawlGame.languagePack.getEventString("Winding Halls");
    public static final String NAME = WindingHalls.eventStrings.NAME;
    public static final String[] DESCRIPTIONS = WindingHalls.eventStrings.DESCRIPTIONS;
    public static final String[] OPTIONS = WindingHalls.eventStrings.OPTIONS;
    private static final float HP_LOSS_PERCENT = 0.125f;
    private static final float HP_MAX_LOSS_PERCENT = 0.05f;
    private static final float A_2_HP_LOSS_PERCENT = 0.18f;
    private static final float HEAL_AMT = 0.25f;
    private static final float A_2_HEAL_AMT = 0.2f;
    private int hpAmt;
    private int healAmt;
    private int maxHPAmt;
    private static final String INTRO_BODY1 = DESCRIPTIONS[0];
    private static final String INTRO_BODY2 = DESCRIPTIONS[1];
    private static final String CHOICE_1_TEXT = DESCRIPTIONS[2];
    private static final String CHOICE_2_TEXT = DESCRIPTIONS[3];
    private int screenNum = 0;

    public WindingHalls() {
        super(NAME, INTRO_BODY1, "images/events/winding.jpg");
        if (AbstractDungeon.ascensionLevel >= 15) {
            this.hpAmt = MathUtils.round((float)AbstractDungeon.player.maxHealth * 0.18f);
            this.healAmt = MathUtils.round((float)AbstractDungeon.player.maxHealth * 0.2f);
        } else {
            this.hpAmt = MathUtils.round((float)AbstractDungeon.player.maxHealth * 0.125f);
            this.healAmt = MathUtils.round((float)AbstractDungeon.player.maxHealth * 0.25f);
        }
        this.maxHPAmt = MathUtils.round((float)AbstractDungeon.player.maxHealth * 0.05f);
        this.imageEventText.setDialogOption(OPTIONS[0]);
    }

    @Override
    public void onEnterRoom() {
        if (Settings.AMBIANCE_ON) {
            CardCrawlGame.sound.play("EVENT_WINDING");
        }
    }

    @Override
    protected void buttonEffect(int buttonPressed) {
        block0 : switch (this.screenNum) {
            case 0: {
                this.imageEventText.updateBodyText(INTRO_BODY2);
                this.screenNum = 1;
                this.imageEventText.updateDialogOption(0, OPTIONS[1] + this.hpAmt + OPTIONS[2], CardLibrary.getCopy("Madness"));
                this.imageEventText.setDialogOption(OPTIONS[3] + this.healAmt + OPTIONS[5], CardLibrary.getCopy("Writhe"));
                this.imageEventText.setDialogOption(OPTIONS[6] + this.maxHPAmt + OPTIONS[7]);
                break;
            }
            case 1: {
                switch (buttonPressed) {
                    case 0: {
                        ArrayList<String> cards = new ArrayList<String>();
                        cards.add("Madness");
                        cards.add("Madness");
                        WindingHalls.logMetric(ID, "Embrace Madness", cards, null, null, null, null, null, null, this.hpAmt, 0, 0, 0, 0, 0);
                        this.imageEventText.updateBodyText(CHOICE_1_TEXT);
                        AbstractDungeon.player.damage(new DamageInfo(null, this.hpAmt));
                        CardCrawlGame.sound.play("ATTACK_MAGIC_SLOW_1");
                        AbstractDungeon.effectList.add(new ShowCardAndObtainEffect(new Madness(), (float)Settings.WIDTH / 2.0f - 350.0f * Settings.xScale, (float)Settings.HEIGHT / 2.0f));
                        AbstractDungeon.effectList.add(new ShowCardAndObtainEffect(new Madness(), (float)Settings.WIDTH / 2.0f + 350.0f * Settings.xScale, (float)Settings.HEIGHT / 2.0f));
                        this.screenNum = 2;
                        this.imageEventText.updateDialogOption(0, OPTIONS[4]);
                        this.imageEventText.clearRemainingOptions();
                        break block0;
                    }
                    case 1: {
                        this.imageEventText.updateBodyText(CHOICE_2_TEXT);
                        AbstractDungeon.player.heal(this.healAmt);
                        Writhe c = new Writhe();
                        AbstractDungeon.effectList.add(new ShowCardAndObtainEffect(c, (float)Settings.WIDTH / 2.0f + 10.0f * Settings.xScale, (float)Settings.HEIGHT / 2.0f));
                        WindingHalls.logMetricObtainCardAndHeal(ID, "Writhe", c, this.healAmt);
                        this.screenNum = 2;
                        this.imageEventText.updateDialogOption(0, OPTIONS[4]);
                        this.imageEventText.clearRemainingOptions();
                        break block0;
                    }
                    case 2: {
                        this.screenNum = 2;
                        this.imageEventText.updateBodyText(DESCRIPTIONS[4]);
                        WindingHalls.logMetricMaxHPLoss(ID, "Max HP", this.maxHPAmt);
                        AbstractDungeon.player.decreaseMaxHealth(this.maxHPAmt);
                        CardCrawlGame.screenShake.shake(ScreenShake.ShakeIntensity.LOW, ScreenShake.ShakeDur.SHORT, true);
                        this.imageEventText.updateDialogOption(0, OPTIONS[4]);
                        this.imageEventText.clearRemainingOptions();
                        break block0;
                    }
                }
                break;
            }
            default: {
                this.openMap();
            }
        }
    }
}

