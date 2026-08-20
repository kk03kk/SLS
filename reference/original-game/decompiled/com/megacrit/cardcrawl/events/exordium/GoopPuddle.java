/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.events.exordium;

import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.cards.DamageInfo;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.events.AbstractEvent;
import com.megacrit.cardcrawl.events.AbstractImageEvent;
import com.megacrit.cardcrawl.localization.EventStrings;
import com.megacrit.cardcrawl.vfx.RainingGoldEffect;
import com.megacrit.cardcrawl.vfx.combat.FlashAtkImgEffect;

public class GoopPuddle
extends AbstractImageEvent {
    public static final String ID = "World of Goop";
    private static final EventStrings eventStrings = CardCrawlGame.languagePack.getEventString("World of Goop");
    public static final String NAME = GoopPuddle.eventStrings.NAME;
    public static final String[] DESCRIPTIONS = GoopPuddle.eventStrings.DESCRIPTIONS;
    public static final String[] OPTIONS = GoopPuddle.eventStrings.OPTIONS;
    private static final String DIALOG_1 = DESCRIPTIONS[0];
    private static final String GOLD_DIALOG = DESCRIPTIONS[1];
    private static final String LEAVE_DIALOG = DESCRIPTIONS[2];
    private CurScreen screen = CurScreen.INTRO;
    private int damage = 11;
    private int gold = 75;
    private int goldLoss = AbstractDungeon.ascensionLevel >= 15 ? AbstractDungeon.miscRng.random(35, 75) : AbstractDungeon.miscRng.random(20, 50);

    public GoopPuddle() {
        super(NAME, DIALOG_1, "images/events/goopPuddle.jpg");
        if (this.goldLoss > AbstractDungeon.player.gold) {
            this.goldLoss = AbstractDungeon.player.gold;
        }
        this.imageEventText.setDialogOption(OPTIONS[0] + this.gold + OPTIONS[1] + this.damage + OPTIONS[2]);
        this.imageEventText.setDialogOption(OPTIONS[3] + this.goldLoss + OPTIONS[4]);
    }

    @Override
    public void onEnterRoom() {
        if (Settings.AMBIANCE_ON) {
            CardCrawlGame.sound.play("EVENT_SPIRITS");
        }
    }

    @Override
    protected void buttonEffect(int buttonPressed) {
        switch (this.screen) {
            case INTRO: {
                switch (buttonPressed) {
                    case 0: {
                        this.imageEventText.updateBodyText(GOLD_DIALOG);
                        this.imageEventText.clearAllDialogs();
                        AbstractDungeon.player.damage(new DamageInfo(AbstractDungeon.player, this.damage));
                        AbstractDungeon.effectList.add(new FlashAtkImgEffect(AbstractDungeon.player.hb.cX, AbstractDungeon.player.hb.cY, AbstractGameAction.AttackEffect.FIRE));
                        AbstractDungeon.effectList.add(new RainingGoldEffect(this.gold));
                        AbstractDungeon.player.gainGold(this.gold);
                        this.imageEventText.setDialogOption(OPTIONS[5]);
                        this.screen = CurScreen.RESULT;
                        AbstractEvent.logMetricGainGoldAndDamage(ID, "Gather Gold", this.gold, this.damage);
                        break;
                    }
                    case 1: {
                        this.imageEventText.updateBodyText(LEAVE_DIALOG);
                        AbstractDungeon.player.loseGold(this.goldLoss);
                        this.imageEventText.clearAllDialogs();
                        this.imageEventText.setDialogOption(OPTIONS[5]);
                        this.screen = CurScreen.RESULT;
                        GoopPuddle.logMetricLoseGold(ID, "Left Gold", this.goldLoss);
                    }
                }
                break;
            }
            default: {
                this.openMap();
            }
        }
    }

    private static enum CurScreen {
        INTRO,
        RESULT;

    }
}

