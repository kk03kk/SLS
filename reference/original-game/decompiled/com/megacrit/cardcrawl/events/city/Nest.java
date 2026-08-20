/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.events.city;

import com.megacrit.cardcrawl.cards.DamageInfo;
import com.megacrit.cardcrawl.cards.colorless.RitualDagger;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.events.AbstractImageEvent;
import com.megacrit.cardcrawl.localization.EventStrings;
import com.megacrit.cardcrawl.unlock.UnlockTracker;
import com.megacrit.cardcrawl.vfx.RainingGoldEffect;
import com.megacrit.cardcrawl.vfx.cardManip.ShowCardAndObtainEffect;

public class Nest
extends AbstractImageEvent {
    public static final String ID = "Nest";
    private static final EventStrings eventStrings = CardCrawlGame.languagePack.getEventString("Nest");
    public static final String NAME = Nest.eventStrings.NAME;
    public static final String[] DESCRIPTIONS = Nest.eventStrings.DESCRIPTIONS;
    public static final String[] OPTIONS = Nest.eventStrings.OPTIONS;
    private static final String INTRO_BODY_M = DESCRIPTIONS[0];
    private static final String INTRO_BODY_M_2 = DESCRIPTIONS[1];
    private static final String ACCEPT_BODY = DESCRIPTIONS[2];
    private static final String EXIT_BODY = DESCRIPTIONS[3];
    private static final int HP_LOSS = 6;
    private int goldGain;
    private int screenNum = 0;

    public Nest() {
        super(NAME, INTRO_BODY_M, "images/events/theNest.jpg");
        this.imageEventText.setDialogOption(OPTIONS[5]);
        this.goldGain = AbstractDungeon.ascensionLevel >= 15 ? 50 : 99;
    }

    @Override
    protected void buttonEffect(int buttonPressed) {
        block0 : switch (this.screenNum) {
            case 0: {
                this.imageEventText.updateBodyText(INTRO_BODY_M_2);
                this.imageEventText.setDialogOption(OPTIONS[0] + 6 + OPTIONS[1], new RitualDagger());
                UnlockTracker.markCardAsSeen("RitualDagger");
                this.imageEventText.updateDialogOption(0, OPTIONS[2] + this.goldGain + OPTIONS[3]);
                this.screenNum = 1;
                break;
            }
            case 1: {
                switch (buttonPressed) {
                    case 0: {
                        Nest.logMetricGainGold(ID, "Stole From Cult", this.goldGain);
                        this.imageEventText.updateBodyText(EXIT_BODY);
                        this.screenNum = 2;
                        AbstractDungeon.effectList.add(new RainingGoldEffect(this.goldGain));
                        AbstractDungeon.player.gainGold(this.goldGain);
                        this.imageEventText.updateDialogOption(0, OPTIONS[4]);
                        this.imageEventText.clearRemainingOptions();
                        break block0;
                    }
                    case 1: {
                        RitualDagger c = new RitualDagger();
                        Nest.logMetricObtainCardAndDamage(ID, "Joined the Cult", c, 6);
                        this.imageEventText.updateBodyText(ACCEPT_BODY);
                        AbstractDungeon.player.damage(new DamageInfo(null, 6));
                        AbstractDungeon.effectList.add(new ShowCardAndObtainEffect(c, (float)Settings.WIDTH * 0.3f, (float)Settings.HEIGHT / 2.0f));
                        this.screenNum = 2;
                        this.imageEventText.updateDialogOption(0, OPTIONS[4]);
                        this.imageEventText.clearRemainingOptions();
                        break block0;
                    }
                }
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

