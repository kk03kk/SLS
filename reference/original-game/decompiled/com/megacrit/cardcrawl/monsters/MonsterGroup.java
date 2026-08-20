/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.monsters;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.dungeons.TheCity;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import com.megacrit.cardcrawl.monsters.MonsterQueueItem;
import com.megacrit.cardcrawl.powers.AbstractPower;
import com.megacrit.cardcrawl.random.Random;
import com.megacrit.cardcrawl.ui.buttons.PeekButton;
import java.util.ArrayList;
import java.util.Collections;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class MonsterGroup {
    private static final Logger logger = LogManager.getLogger(MonsterGroup.class.getName());
    public ArrayList<AbstractMonster> monsters = new ArrayList();
    public AbstractMonster hoveredMonster = null;

    public MonsterGroup(AbstractMonster[] input) {
        Collections.addAll(this.monsters, input);
    }

    public void addMonster(int newIndex, AbstractMonster m) {
        if (newIndex < 0) {
            newIndex = 0;
        }
        this.monsters.add(newIndex, m);
    }

    @Deprecated
    public void addMonster(AbstractMonster m) {
        this.monsters.add(m);
    }

    @Deprecated
    public void addSpawnedMonster(AbstractMonster m) {
        this.monsters.add(0, m);
    }

    public MonsterGroup(AbstractMonster m) {
        this(new AbstractMonster[]{m});
    }

    public void showIntent() {
        for (AbstractMonster m : this.monsters) {
            m.createIntent();
        }
    }

    public void init() {
        for (AbstractMonster m : this.monsters) {
            m.init();
        }
    }

    public void add(AbstractMonster m) {
        this.monsters.add(m);
    }

    public void usePreBattleAction() {
        if (AbstractDungeon.loading_post_combat) {
            return;
        }
        for (AbstractMonster m : this.monsters) {
            m.usePreBattleAction();
            m.useUniversalPreBattleAction();
        }
    }

    public boolean areMonstersDead() {
        for (AbstractMonster m : this.monsters) {
            if (m.isDead || m.escaped) continue;
            return false;
        }
        return true;
    }

    public boolean areMonstersBasicallyDead() {
        for (AbstractMonster m : this.monsters) {
            if (m.isDying || m.isEscaping) continue;
            return false;
        }
        return true;
    }

    public void applyPreTurnLogic() {
        for (AbstractMonster m : this.monsters) {
            if (m.isDying || m.isEscaping) continue;
            if (!m.hasPower("Barricade")) {
                m.loseBlock();
            }
            m.applyStartOfTurnPowers();
        }
    }

    public AbstractMonster getMonster(String id) {
        for (AbstractMonster m : this.monsters) {
            if (!m.id.equals(id)) continue;
            return m;
        }
        logger.info("MONSTER GROUP getMonster(): Could not find monster: " + id);
        return null;
    }

    public void queueMonsters() {
        for (AbstractMonster m : this.monsters) {
            if (m.isDeadOrEscaped() && !m.halfDead) continue;
            AbstractDungeon.actionManager.monsterQueue.add(new MonsterQueueItem(m));
        }
    }

    public boolean haveMonstersEscaped() {
        for (AbstractMonster m : this.monsters) {
            if (m.escaped) continue;
            return false;
        }
        return true;
    }

    public boolean isMonsterEscaping() {
        for (AbstractMonster m : this.monsters) {
            if (m.nextMove != 99) continue;
            return true;
        }
        return false;
    }

    public boolean hasMonsterEscaped() {
        for (AbstractMonster m : this.monsters) {
            if (!m.isEscaping) continue;
            return true;
        }
        return CardCrawlGame.dungeon instanceof TheCity;
    }

    public AbstractMonster getRandomMonster() {
        return this.getRandomMonster(null, false);
    }

    public AbstractMonster getRandomMonster(boolean aliveOnly) {
        return this.getRandomMonster(null, aliveOnly);
    }

    public AbstractMonster getRandomMonster(AbstractMonster exception, boolean aliveOnly, Random rng) {
        if (this.areMonstersBasicallyDead()) {
            return null;
        }
        if (exception == null) {
            if (aliveOnly) {
                ArrayList<AbstractMonster> tmp = new ArrayList<AbstractMonster>();
                for (AbstractMonster m : this.monsters) {
                    if (m.halfDead || m.isDying || m.isEscaping) continue;
                    tmp.add(m);
                }
                if (tmp.size() <= 0) {
                    return null;
                }
                return (AbstractMonster)tmp.get(rng.random(0, tmp.size() - 1));
            }
            return this.monsters.get(rng.random(0, this.monsters.size() - 1));
        }
        if (this.monsters.size() == 1) {
            return this.monsters.get(0);
        }
        if (aliveOnly) {
            ArrayList<AbstractMonster> tmp = new ArrayList<AbstractMonster>();
            for (AbstractMonster m : this.monsters) {
                if (m.halfDead || m.isDying || m.isEscaping || exception.equals(m)) continue;
                tmp.add(m);
            }
            if (tmp.size() == 0) {
                return null;
            }
            return (AbstractMonster)tmp.get(rng.random(0, tmp.size() - 1));
        }
        ArrayList<AbstractMonster> tmp = new ArrayList<AbstractMonster>();
        for (AbstractMonster m : this.monsters) {
            if (exception.equals(m)) continue;
            tmp.add(m);
        }
        return (AbstractMonster)tmp.get(rng.random(0, tmp.size() - 1));
    }

    public AbstractMonster getRandomMonster(AbstractMonster exception, boolean aliveOnly) {
        if (this.areMonstersBasicallyDead()) {
            return null;
        }
        if (exception == null) {
            if (aliveOnly) {
                ArrayList<AbstractMonster> tmp = new ArrayList<AbstractMonster>();
                for (AbstractMonster m : this.monsters) {
                    if (m.halfDead || m.isDying || m.isEscaping) continue;
                    tmp.add(m);
                }
                if (tmp.size() <= 0) {
                    return null;
                }
                return (AbstractMonster)tmp.get(MathUtils.random(0, tmp.size() - 1));
            }
            return this.monsters.get(MathUtils.random(0, this.monsters.size() - 1));
        }
        if (this.monsters.size() == 1) {
            return this.monsters.get(0);
        }
        if (aliveOnly) {
            ArrayList<AbstractMonster> tmp = new ArrayList<AbstractMonster>();
            for (AbstractMonster m : this.monsters) {
                if (m.halfDead || m.isDying || m.isEscaping || exception.equals(m)) continue;
                tmp.add(m);
            }
            if (tmp.size() == 0) {
                return null;
            }
            return (AbstractMonster)tmp.get(MathUtils.random(0, tmp.size() - 1));
        }
        ArrayList<AbstractMonster> tmp = new ArrayList<AbstractMonster>();
        for (AbstractMonster m : this.monsters) {
            if (exception.equals(m)) continue;
            tmp.add(m);
        }
        return (AbstractMonster)tmp.get(MathUtils.random(0, tmp.size() - 1));
    }

    public void update() {
        for (AbstractMonster m : this.monsters) {
            m.update();
        }
        if (AbstractDungeon.screen != AbstractDungeon.CurrentScreen.DEATH) {
            this.hoveredMonster = null;
            for (AbstractMonster m : this.monsters) {
                if (m.isDying || m.isEscaping) continue;
                m.hb.update();
                m.intentHb.update();
                m.healthHb.update();
                if (!m.hb.hovered && !m.intentHb.hovered && !m.healthHb.hovered || AbstractDungeon.player.isDraggingCard) continue;
                this.hoveredMonster = m;
                break;
            }
            if (this.hoveredMonster == null) {
                AbstractDungeon.player.hoverEnemyWaitTimer = -1.0f;
            }
        } else {
            this.hoveredMonster = null;
        }
    }

    public void updateAnimations() {
        for (AbstractMonster m : this.monsters) {
            m.updatePowers();
        }
    }

    public boolean shouldFlipVfx() {
        return AbstractDungeon.lastCombatMetricKey.equals("Shield and Spear") && this.monsters.get((int)1).isDying;
    }

    public void escape() {
        for (AbstractMonster m : this.monsters) {
            m.escape();
        }
    }

    public void unhover() {
        for (AbstractMonster m : this.monsters) {
            m.unhover();
        }
    }

    public void render(SpriteBatch sb) {
        if (!(this.hoveredMonster == null || this.hoveredMonster.isDead || this.hoveredMonster.escaped || !(AbstractDungeon.player.hoverEnemyWaitTimer < 0.0f) || AbstractDungeon.isScreenUp && !PeekButton.isPeeking)) {
            this.hoveredMonster.renderTip(sb);
        }
        for (AbstractMonster m : this.monsters) {
            m.render(sb);
        }
    }

    public void applyEndOfTurnPowers() {
        for (AbstractMonster m : this.monsters) {
            if (m.isDying || m.isEscaping) continue;
            m.applyEndOfTurnTriggers();
        }
        for (AbstractPower p : AbstractDungeon.player.powers) {
            p.atEndOfRound();
        }
        for (AbstractMonster m : this.monsters) {
            if (m.isDying || m.isEscaping) continue;
            for (AbstractPower p : m.powers) {
                p.atEndOfRound();
            }
        }
    }

    public void renderReticle(SpriteBatch sb) {
        for (AbstractMonster m : this.monsters) {
            if (m.isDying || m.isEscaping) continue;
            m.renderReticle(sb);
        }
    }

    public ArrayList<String> getMonsterNames() {
        ArrayList<String> arr = new ArrayList<String>();
        for (AbstractMonster m : this.monsters) {
            arr.add(m.id);
        }
        return arr;
    }
}

