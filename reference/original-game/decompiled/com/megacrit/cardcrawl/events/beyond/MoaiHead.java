/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.events.beyond;

import com.badlogic.gdx.math.MathUtils;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.events.AbstractImageEvent;
import com.megacrit.cardcrawl.helpers.ScreenShake;
import com.megacrit.cardcrawl.localization.EventStrings;
import com.megacrit.cardcrawl.relics.GoldenIdol;
import com.megacrit.cardcrawl.vfx.RainingGoldEffect;

public class MoaiHead
extends AbstractImageEvent {
    public static final String ID = "The Moai Head";
    private static final EventStrings eventStrings = CardCrawlGame.languagePack.getEventString("The Moai Head");
    public static final String NAME = MoaiHead.eventStrings.NAME;
    public static final String[] DESCRIPTIONS = MoaiHead.eventStrings.DESCRIPTIONS;
    public static final String[] OPTIONS = MoaiHead.eventStrings.OPTIONS;
    private static final float HP_LOSS_PERCENT = 0.125f;
    private static final float A_2_HP_LOSS_PERCENT = 0.18f;
    private int hpAmt = 0;
    private static final int goldAmount = 333;
    private static final String INTRO_BODY = DESCRIPTIONS[0];
    private int screenNum = 0;

    public MoaiHead() {
        super(NAME, INTRO_BODY, "images/events/moaiHead.jpg");
        this.hpAmt = AbstractDungeon.ascensionLevel >= 15 ? MathUtils.round((float)AbstractDungeon.player.maxHealth * 0.18f) : MathUtils.round((float)AbstractDungeon.player.maxHealth * 0.125f);
        this.imageEventText.setDialogOption(OPTIONS[0] + this.hpAmt + OPTIONS[1]);
        if (AbstractDungeon.player.hasRelic("Golden Idol")) {
            this.imageEventText.setDialogOption(OPTIONS[2], !AbstractDungeon.player.hasRelic("Golden Idol"));
        } else {
            this.imageEventText.setDialogOption(OPTIONS[3], !AbstractDungeon.player.hasRelic("Golden Idol"));
        }
        this.imageEventText.setDialogOption(OPTIONS[4]);
    }

    @Override
    protected void buttonEffect(int buttonPressed) {
        block0 : switch (this.screenNum) {
            case 0: {
                switch (buttonPressed) {
                    case 0: {
                        this.imageEventText.updateBodyText(DESCRIPTIONS[1]);
                        CardCrawlGame.screenShake.shake(ScreenShake.ShakeIntensity.HIGH, ScreenShake.ShakeDur.MED, true);
                        CardCrawlGame.sound.play("BLUNT_HEAVY");
                        AbstractDungeon.player.maxHealth -= this.hpAmt;
                        if (AbstractDungeon.player.currentHealth > AbstractDungeon.player.maxHealth) {
                            AbstractDungeon.player.currentHealth = AbstractDungeon.player.maxHealth;
                        }
                        if (AbstractDungeon.player.maxHealth < 1) {
                            AbstractDungeon.player.maxHealth = 1;
                        }
                        AbstractDungeon.player.heal(AbstractDungeon.player.maxHealth);
                        MoaiHead.logMetricHealAndLoseMaxHP(ID, "Heal", AbstractDungeon.player.maxHealth, this.hpAmt);
                        this.screenNum = 1;
                        this.imageEventText.updateDialogOption(0, OPTIONS[4]);
                        this.imageEventText.clearRemainingOptions();
                        break block0;
                    }
                    case 1: {
                        MoaiHead.logMetricGainGoldAndLoseRelic(ID, "Gave Idol", new GoldenIdol(), 333);
                        this.imageEventText.updateBodyText(DESCRIPTIONS[2]);
                        this.screenNum = 1;
                        AbstractDungeon.player.loseRelic("Golden Idol");
                        AbstractDungeon.effectList.add(new RainingGoldEffect(333));
                        AbstractDungeon.player.gainGold(333);
                        this.imageEventText.updateDialogOption(0, OPTIONS[4]);
                        this.imageEventText.clearRemainingOptions();
                        break block0;
                    }
                }
                MoaiHead.logMetricIgnored(ID);
                this.imageEventText.updateBodyText(DESCRIPTIONS[3]);
                this.screenNum = 1;
                this.imageEventText.updateDialogOption(0, OPTIONS[4]);
                this.imageEventText.clearRemainingOptions();
                break;
            }
            default: {
                this.openMap();
            }
        }
    }
}

