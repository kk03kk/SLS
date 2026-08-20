/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.cards;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.cards.Soul;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import java.util.ArrayList;
import java.util.Iterator;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class SoulGroup {
    private static final Logger logger = LogManager.getLogger(SoulGroup.class.getName());
    private ArrayList<Soul> souls = new ArrayList();
    private static final int DEFAULT_SOUL_CACHE = 20;

    public SoulGroup() {
        for (int i = 0; i < 20; ++i) {
            this.souls.add(new Soul());
        }
    }

    public void discard(AbstractCard card, boolean visualOnly) {
        boolean needMoreSouls = true;
        for (Soul s : this.souls) {
            if (!s.isReadyForReuse) continue;
            card.untip();
            card.unhover();
            s.discard(card, visualOnly);
            needMoreSouls = false;
            break;
        }
        if (needMoreSouls) {
            logger.info("Not enough souls, creating...");
            Soul s = new Soul();
            s.discard(card, visualOnly);
            this.souls.add(s);
        }
    }

    public void discard(AbstractCard card) {
        this.discard(card, false);
    }

    public void empower(AbstractCard card) {
        boolean needMoreSouls = true;
        for (Soul s : this.souls) {
            if (!s.isReadyForReuse) continue;
            card.untip();
            card.unhover();
            s.empower(card);
            needMoreSouls = false;
            break;
        }
        if (needMoreSouls) {
            logger.info("Not enough souls, creating...");
            Soul s = new Soul();
            s.empower(card);
            this.souls.add(s);
        }
    }

    public void obtain(AbstractCard card, boolean obtainCard) {
        CardCrawlGame.sound.play("CARD_OBTAIN");
        boolean needMoreSouls = true;
        for (Soul s : this.souls) {
            if (!s.isReadyForReuse) continue;
            if (obtainCard) {
                s.obtain(card);
            }
            needMoreSouls = false;
            break;
        }
        if (needMoreSouls) {
            logger.info("Not enough souls, creating...");
            Soul s = new Soul();
            if (obtainCard) {
                s.obtain(card);
            }
            this.souls.add(s);
        }
    }

    public void shuffle(AbstractCard card, boolean isInvisible) {
        card.untip();
        card.unhover();
        card.darken(true);
        card.shrink(true);
        boolean needMoreSouls = true;
        for (Soul s : this.souls) {
            if (!s.isReadyForReuse) continue;
            s.shuffle(card, isInvisible);
            needMoreSouls = false;
            break;
        }
        if (needMoreSouls) {
            logger.info("Not enough souls, creating...");
            Soul s = new Soul();
            s.shuffle(card, isInvisible);
            this.souls.add(s);
        }
    }

    public void onToBottomOfDeck(AbstractCard card) {
        boolean needMoreSouls = true;
        for (Soul s : this.souls) {
            if (!s.isReadyForReuse) continue;
            card.untip();
            card.unhover();
            s.onToBottomOfDeck(card);
            needMoreSouls = false;
            break;
        }
        if (needMoreSouls) {
            logger.info("Not enough souls, creating...");
            Soul s = new Soul();
            s.onToBottomOfDeck(card);
            this.souls.add(s);
        }
    }

    public void onToDeck(AbstractCard card, boolean randomSpot, boolean visualOnly) {
        boolean needMoreSouls = true;
        for (Soul s : this.souls) {
            if (!s.isReadyForReuse) continue;
            card.untip();
            card.unhover();
            s.onToDeck(card, randomSpot, visualOnly);
            needMoreSouls = false;
            break;
        }
        if (needMoreSouls) {
            logger.info("Not enough souls, creating...");
            Soul s = new Soul();
            s.onToDeck(card, randomSpot, visualOnly);
            this.souls.add(s);
        }
    }

    public void onToDeck(AbstractCard card, boolean randomSpot) {
        this.onToDeck(card, randomSpot, false);
    }

    public void update() {
        for (Soul s : this.souls) {
            if (s.isReadyForReuse) continue;
            s.update();
        }
    }

    public void render(SpriteBatch sb) {
        for (Soul s : this.souls) {
            if (s.isReadyForReuse) continue;
            s.render(sb);
        }
    }

    public void remove(AbstractCard card) {
        Iterator<Soul> s = this.souls.iterator();
        while (s.hasNext()) {
            Soul derp = s.next();
            if (derp.card != card) continue;
            s.remove();
            logger.info(derp + " removed.");
            break;
        }
    }

    public static boolean isActive() {
        for (Soul s : AbstractDungeon.getCurrRoom().souls.souls) {
            if (s.isReadyForReuse) continue;
            return true;
        }
        return false;
    }
}

