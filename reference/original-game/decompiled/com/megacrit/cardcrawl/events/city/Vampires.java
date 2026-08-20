/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.events.city;

import com.badlogic.gdx.math.MathUtils;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.cards.colorless.Bite;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.events.AbstractImageEvent;
import com.megacrit.cardcrawl.localization.EventStrings;
import com.megacrit.cardcrawl.relics.BloodVial;
import com.megacrit.cardcrawl.vfx.cardManip.ShowCardAndObtainEffect;
import java.util.ArrayList;
import java.util.List;

public class Vampires
extends AbstractImageEvent {
    public static final String ID = "Vampires";
    private static final EventStrings eventStrings = CardCrawlGame.languagePack.getEventString("Vampires");
    public static final String NAME = Vampires.eventStrings.NAME;
    public static final String[] DESCRIPTIONS = Vampires.eventStrings.DESCRIPTIONS;
    public static final String[] OPTIONS = Vampires.eventStrings.OPTIONS;
    private static final String ACCEPT_BODY = DESCRIPTIONS[2];
    private static final String EXIT_BODY = DESCRIPTIONS[3];
    private static final String GIVE_VIAL = DESCRIPTIONS[4];
    private static final float HP_DRAIN = 0.3f;
    private int maxHpLoss;
    private int screenNum = 0;
    private boolean hasVial;
    private List<String> bites;

    public Vampires() {
        super(NAME, "test", "images/events/vampires.jpg");
        this.body = AbstractDungeon.player.getVampireText();
        this.maxHpLoss = MathUtils.ceil((float)AbstractDungeon.player.maxHealth * 0.3f);
        if (this.maxHpLoss >= AbstractDungeon.player.maxHealth) {
            this.maxHpLoss = AbstractDungeon.player.maxHealth - 1;
        }
        this.bites = new ArrayList<String>();
        this.hasVial = AbstractDungeon.player.hasRelic("Blood Vial");
        this.imageEventText.setDialogOption(OPTIONS[0] + this.maxHpLoss + OPTIONS[1], new Bite());
        if (this.hasVial) {
            String vialName = new BloodVial().name;
            this.imageEventText.setDialogOption(OPTIONS[3] + vialName + OPTIONS[4], new Bite());
        }
        this.imageEventText.setDialogOption(OPTIONS[2]);
    }

    @Override
    protected void buttonEffect(int buttonPressed) {
        block0 : switch (this.screenNum) {
            case 0: {
                switch (buttonPressed) {
                    case 0: {
                        CardCrawlGame.sound.play("EVENT_VAMP_BITE");
                        this.imageEventText.updateBodyText(ACCEPT_BODY);
                        AbstractDungeon.player.decreaseMaxHealth(this.maxHpLoss);
                        this.replaceAttacks();
                        Vampires.logMetricObtainCardsLoseMapHP(ID, "Became a vampire", this.bites, this.maxHpLoss);
                        this.screenNum = 1;
                        this.imageEventText.updateDialogOption(0, OPTIONS[5]);
                        this.imageEventText.clearRemainingOptions();
                        break block0;
                    }
                    case 1: {
                        if (!this.hasVial) break;
                        CardCrawlGame.sound.play("EVENT_VAMP_BITE");
                        this.imageEventText.updateBodyText(GIVE_VIAL);
                        AbstractDungeon.player.loseRelic("Blood Vial");
                        this.replaceAttacks();
                        Vampires.logMetricObtainCardsLoseRelic(ID, "Became a vampire (Vial)", this.bites, new BloodVial());
                        this.screenNum = 1;
                        this.imageEventText.updateDialogOption(0, OPTIONS[5]);
                        this.imageEventText.clearRemainingOptions();
                        break block0;
                    }
                }
                Vampires.logMetricIgnored(ID);
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

    private void replaceAttacks() {
        int i;
        ArrayList<AbstractCard> masterDeck = AbstractDungeon.player.masterDeck.group;
        for (i = masterDeck.size() - 1; i >= 0; --i) {
            AbstractCard card = masterDeck.get(i);
            if (!card.tags.contains((Object)AbstractCard.CardTags.STARTER_STRIKE)) continue;
            AbstractDungeon.player.masterDeck.removeCard(card);
        }
        for (i = 0; i < 5; ++i) {
            Bite c = new Bite();
            AbstractDungeon.effectList.add(new ShowCardAndObtainEffect(c, (float)Settings.WIDTH / 2.0f, (float)Settings.HEIGHT / 2.0f));
            this.bites.add(c.cardID);
        }
    }
}

