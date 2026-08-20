/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.random;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.RandomXS128;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Random {
    private static final Logger logger = LogManager.getLogger(Random.class.getName());
    public RandomXS128 random;
    public int counter = 0;

    public Random() {
        this(MathUtils.random(9999L), MathUtils.random(99));
    }

    public Random(Long seed) {
        this.random = new RandomXS128(seed);
    }

    public Random(Long seed, int counter) {
        this.random = new RandomXS128(seed);
        for (int i = 0; i < counter; ++i) {
            this.random(999);
        }
    }

    public Random copy() {
        Random copied = new Random();
        copied.random = new RandomXS128(this.random.getState(0), this.random.getState(1));
        copied.counter = this.counter;
        return copied;
    }

    public void setCounter(int targetCounter) {
        if (this.counter < targetCounter) {
            int count = targetCounter - this.counter;
            for (int i = 0; i < count; ++i) {
                this.randomBoolean();
            }
        } else {
            logger.info("ERROR: Counter is already higher than target counter!");
        }
    }

    public int random(int range) {
        ++this.counter;
        return this.random.nextInt(range + 1);
    }

    public int random(int start, int end) {
        ++this.counter;
        return start + this.random.nextInt(end - start + 1);
    }

    public long random(long range) {
        ++this.counter;
        return (long)(this.random.nextDouble() * (double)range);
    }

    public long random(long start, long end) {
        ++this.counter;
        return start + (long)(this.random.nextDouble() * (double)(end - start));
    }

    public long randomLong() {
        ++this.counter;
        return this.random.nextLong();
    }

    public boolean randomBoolean() {
        ++this.counter;
        return this.random.nextBoolean();
    }

    public boolean randomBoolean(float chance) {
        ++this.counter;
        return this.random.nextFloat() < chance;
    }

    public float random() {
        ++this.counter;
        return this.random.nextFloat();
    }

    public float random(float range) {
        ++this.counter;
        return this.random.nextFloat() * range;
    }

    public float random(float start, float end) {
        ++this.counter;
        return start + this.random.nextFloat() * (end - start);
    }
}

