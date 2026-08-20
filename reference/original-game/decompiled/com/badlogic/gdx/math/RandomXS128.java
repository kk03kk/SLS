/*
 * Decompiled with CFR 0.152.
 */
package com.badlogic.gdx.math;

import java.util.Random;

public class RandomXS128
extends Random {
    private static final double NORM_DOUBLE = (double)1.110223E-16f;
    private static final double NORM_FLOAT = 5.960464477539063E-8;
    private long seed0;
    private long seed1;

    public RandomXS128() {
        this.setSeed(new Random().nextLong());
    }

    public RandomXS128(long seed) {
        this.setSeed(seed);
    }

    public RandomXS128(long seed0, long seed1) {
        this.setState(seed0, seed1);
    }

    @Override
    public long nextLong() {
        long s0;
        long s1 = this.seed0;
        this.seed0 = s0 = this.seed1;
        s1 ^= s1 << 23;
        this.seed1 = s1 ^ s0 ^ s1 >>> 17 ^ s0 >>> 26;
        return this.seed1 + s0;
    }

    @Override
    protected final int next(int bits) {
        return (int)(this.nextLong() & (1L << bits) - 1L);
    }

    @Override
    public int nextInt() {
        return (int)this.nextLong();
    }

    @Override
    public int nextInt(int n) {
        return (int)this.nextLong(n);
    }

    @Override
    public long nextLong(long n) {
        long value;
        long bits;
        if (n <= 0L) {
            throw new IllegalArgumentException("n must be positive");
        }
        while ((bits = this.nextLong() >>> 1) - (value = bits % n) + (n - 1L) < 0L) {
        }
        return value;
    }

    @Override
    public double nextDouble() {
        return (double)(this.nextLong() >>> 11) * (double)1.110223E-16f;
    }

    @Override
    public float nextFloat() {
        return (float)((double)(this.nextLong() >>> 40) * 5.960464477539063E-8);
    }

    @Override
    public boolean nextBoolean() {
        return (this.nextLong() & 1L) != 0L;
    }

    @Override
    public void nextBytes(byte[] bytes) {
        int n = 0;
        int i = bytes.length;
        while (i != 0) {
            n = i < 8 ? i : 8;
            long bits = this.nextLong();
            while (n-- != 0) {
                bytes[--i] = (byte)bits;
                bits >>= 8;
            }
        }
    }

    @Override
    public void setSeed(long seed) {
        long seed0 = RandomXS128.murmurHash3(seed == 0L ? Long.MIN_VALUE : seed);
        this.setState(seed0, RandomXS128.murmurHash3(seed0));
    }

    public void setState(long seed0, long seed1) {
        this.seed0 = seed0;
        this.seed1 = seed1;
    }

    public long getState(int seed) {
        return seed == 0 ? this.seed0 : this.seed1;
    }

    private static final long murmurHash3(long x) {
        x ^= x >>> 33;
        x *= -49064778989728563L;
        x ^= x >>> 33;
        x *= -4265267296055464877L;
        x ^= x >>> 33;
        return x;
    }
}

