/*
 * Decompiled with CFR 0.152.
 */
package com.badlogic.gdx.math;

import com.badlogic.gdx.math.RandomXS128;
import java.util.Random;

public final class MathUtils {
    public static final float nanoToSec = 1.0E-9f;
    public static final float FLOAT_ROUNDING_ERROR = 1.0E-6f;
    public static final float PI = (float)Math.PI;
    public static final float PI2 = (float)Math.PI * 2;
    public static final float E = (float)Math.E;
    private static final int SIN_BITS = 14;
    private static final int SIN_MASK = 16383;
    private static final int SIN_COUNT = 16384;
    private static final float radFull = (float)Math.PI * 2;
    private static final float degFull = 360.0f;
    private static final float radToIndex = 2607.5945f;
    private static final float degToIndex = 45.511112f;
    public static final float radiansToDegrees = 57.295776f;
    public static final float radDeg = 57.295776f;
    public static final float degreesToRadians = (float)Math.PI / 180;
    public static final float degRad = (float)Math.PI / 180;
    public static Random random = new RandomXS128();
    private static final int BIG_ENOUGH_INT = 16384;
    private static final double BIG_ENOUGH_FLOOR = 16384.0;
    private static final double CEIL = 0.9999999;
    private static final double BIG_ENOUGH_CEIL = 16384.999999999996;
    private static final double BIG_ENOUGH_ROUND = 16384.5;

    public static float sin(float radians) {
        return Sin.table[(int)(radians * 2607.5945f) & 0x3FFF];
    }

    public static float cos(float radians) {
        return Sin.table[(int)((radians + 1.5707964f) * 2607.5945f) & 0x3FFF];
    }

    public static float sinDeg(float degrees) {
        return Sin.table[(int)(degrees * 45.511112f) & 0x3FFF];
    }

    public static float cosDeg(float degrees) {
        return Sin.table[(int)((degrees + 90.0f) * 45.511112f) & 0x3FFF];
    }

    public static float atan2(float y, float x) {
        if (x == 0.0f) {
            if (y > 0.0f) {
                return 1.5707964f;
            }
            if (y == 0.0f) {
                return 0.0f;
            }
            return -1.5707964f;
        }
        float z = y / x;
        if (Math.abs(z) < 1.0f) {
            float atan = z / (1.0f + 0.28f * z * z);
            if (x < 0.0f) {
                return atan + (y < 0.0f ? (float)(-Math.PI) : (float)Math.PI);
            }
            return atan;
        }
        float atan = 1.5707964f - z / (z * z + 0.28f);
        return y < 0.0f ? atan - (float)Math.PI : atan;
    }

    public static int random(int range) {
        return random.nextInt(range + 1);
    }

    public static int random(int start, int end) {
        return start + random.nextInt(end - start + 1);
    }

    public static long random(long range) {
        return (long)(random.nextDouble() * (double)range);
    }

    public static long random(long start, long end) {
        return start + (long)(random.nextDouble() * (double)(end - start));
    }

    public static boolean randomBoolean() {
        return random.nextBoolean();
    }

    public static boolean randomBoolean(float chance) {
        return MathUtils.random() < chance;
    }

    public static float random() {
        return random.nextFloat();
    }

    public static float random(float range) {
        return random.nextFloat() * range;
    }

    public static float random(float start, float end) {
        return start + random.nextFloat() * (end - start);
    }

    public static int randomSign() {
        return 1 | random.nextInt() >> 31;
    }

    public static float randomTriangular() {
        return random.nextFloat() - random.nextFloat();
    }

    public static float randomTriangular(float max) {
        return (random.nextFloat() - random.nextFloat()) * max;
    }

    public static float randomTriangular(float min, float max) {
        return MathUtils.randomTriangular(min, max, (min + max) * 0.5f);
    }

    public static float randomTriangular(float min, float max, float mode) {
        float d;
        float u = random.nextFloat();
        if (u <= (mode - min) / (d = max - min)) {
            return min + (float)Math.sqrt(u * d * (mode - min));
        }
        return max - (float)Math.sqrt((1.0f - u) * d * (max - mode));
    }

    public static int nextPowerOfTwo(int value) {
        if (value == 0) {
            return 1;
        }
        --value;
        value |= value >> 1;
        value |= value >> 2;
        value |= value >> 4;
        value |= value >> 8;
        value |= value >> 16;
        return value + 1;
    }

    public static boolean isPowerOfTwo(int value) {
        return value != 0 && (value & value - 1) == 0;
    }

    public static short clamp(short value, short min, short max) {
        if (value < min) {
            return min;
        }
        if (value > max) {
            return max;
        }
        return value;
    }

    public static int clamp(int value, int min, int max) {
        if (value < min) {
            return min;
        }
        if (value > max) {
            return max;
        }
        return value;
    }

    public static long clamp(long value, long min, long max) {
        if (value < min) {
            return min;
        }
        if (value > max) {
            return max;
        }
        return value;
    }

    public static float clamp(float value, float min, float max) {
        if (value < min) {
            return min;
        }
        if (value > max) {
            return max;
        }
        return value;
    }

    public static double clamp(double value, double min, double max) {
        if (value < min) {
            return min;
        }
        if (value > max) {
            return max;
        }
        return value;
    }

    public static float lerp(float fromValue, float toValue, float progress) {
        return fromValue + (toValue - fromValue) * progress;
    }

    public static float lerpAngle(float fromRadians, float toRadians, float progress) {
        float delta = (toRadians - fromRadians + (float)Math.PI * 2 + (float)Math.PI) % ((float)Math.PI * 2) - (float)Math.PI;
        return (fromRadians + delta * progress + (float)Math.PI * 2) % ((float)Math.PI * 2);
    }

    public static float lerpAngleDeg(float fromDegrees, float toDegrees, float progress) {
        float delta = (toDegrees - fromDegrees + 360.0f + 180.0f) % 360.0f - 180.0f;
        return (fromDegrees + delta * progress + 360.0f) % 360.0f;
    }

    public static int floor(float value) {
        return (int)((double)value + 16384.0) - 16384;
    }

    public static int floorPositive(float value) {
        return (int)value;
    }

    public static int ceil(float value) {
        return 16384 - (int)(16384.0 - (double)value);
    }

    public static int ceilPositive(float value) {
        return (int)((double)value + 0.9999999);
    }

    public static int round(float value) {
        return (int)((double)value + 16384.5) - 16384;
    }

    public static int roundPositive(float value) {
        return (int)(value + 0.5f);
    }

    public static boolean isZero(float value) {
        return Math.abs(value) <= 1.0E-6f;
    }

    public static boolean isZero(float value, float tolerance) {
        return Math.abs(value) <= tolerance;
    }

    public static boolean isEqual(float a, float b) {
        return Math.abs(a - b) <= 1.0E-6f;
    }

    public static boolean isEqual(float a, float b, float tolerance) {
        return Math.abs(a - b) <= tolerance;
    }

    public static float log(float a, float value) {
        return (float)(Math.log(value) / Math.log(a));
    }

    public static float log2(float value) {
        return MathUtils.log(2.0f, value);
    }

    private static class Sin {
        static final float[] table;

        private Sin() {
        }

        static {
            int i;
            table = new float[16384];
            for (i = 0; i < 16384; ++i) {
                Sin.table[i] = (float)Math.sin(((float)i + 0.5f) / 16384.0f * ((float)Math.PI * 2));
            }
            for (i = 0; i < 360; i += 90) {
                Sin.table[(int)((float)i * 45.511112f) & 0x3FFF] = (float)Math.sin((float)i * ((float)Math.PI / 180));
            }
        }
    }
}

