/*
 * Decompiled with CFR 0.152.
 */
package com.badlogic.gdx.graphics;

import com.badlogic.gdx.utils.NumberUtils;

public class Color {
    public static final Color CLEAR = new Color(0.0f, 0.0f, 0.0f, 0.0f);
    public static final Color BLACK = new Color(0.0f, 0.0f, 0.0f, 1.0f);
    public static final Color WHITE = new Color(-1);
    public static final Color LIGHT_GRAY = new Color(-1077952513);
    public static final Color GRAY = new Color(0x7F7F7FFF);
    public static final Color DARK_GRAY = new Color(0x3F3F3FFF);
    public static final Color BLUE = new Color(0.0f, 0.0f, 1.0f, 1.0f);
    public static final Color NAVY = new Color(0.0f, 0.0f, 0.5f, 1.0f);
    public static final Color ROYAL = new Color(1097458175);
    public static final Color SLATE = new Color(1887473919);
    public static final Color SKY = new Color(-2016482305);
    public static final Color CYAN = new Color(0.0f, 1.0f, 1.0f, 1.0f);
    public static final Color TEAL = new Color(0.0f, 0.5f, 0.5f, 1.0f);
    public static final Color GREEN = new Color(0xFF00FF);
    public static final Color CHARTREUSE = new Color(0x7FFF00FF);
    public static final Color LIME = new Color(852308735);
    public static final Color FOREST = new Color(579543807);
    public static final Color OLIVE = new Color(1804477439);
    public static final Color YELLOW = new Color(-65281);
    public static final Color GOLD = new Color(-2686721);
    public static final Color GOLDENROD = new Color(-626712321);
    public static final Color ORANGE = new Color(-5963521);
    public static final Color BROWN = new Color(-1958407169);
    public static final Color TAN = new Color(-759919361);
    public static final Color FIREBRICK = new Color(-1306385665);
    public static final Color RED = new Color(-16776961);
    public static final Color SCARLET = new Color(-13361921);
    public static final Color CORAL = new Color(-8433409);
    public static final Color SALMON = new Color(-92245249);
    public static final Color PINK = new Color(-9849601);
    public static final Color MAGENTA = new Color(1.0f, 0.0f, 1.0f, 1.0f);
    public static final Color PURPLE = new Color(-1608453889);
    public static final Color VIOLET = new Color(-293409025);
    public static final Color MAROON = new Color(-1339006721);
    public float r;
    public float g;
    public float b;
    public float a;

    public Color() {
    }

    public Color(int rgba8888) {
        Color.rgba8888ToColor(this, rgba8888);
    }

    public Color(float r, float g, float b, float a) {
        this.r = r;
        this.g = g;
        this.b = b;
        this.a = a;
        this.clamp();
    }

    public Color(Color color) {
        this.set(color);
    }

    public Color set(Color color) {
        this.r = color.r;
        this.g = color.g;
        this.b = color.b;
        this.a = color.a;
        return this;
    }

    public Color mul(Color color) {
        this.r *= color.r;
        this.g *= color.g;
        this.b *= color.b;
        this.a *= color.a;
        return this.clamp();
    }

    public Color mul(float value) {
        this.r *= value;
        this.g *= value;
        this.b *= value;
        this.a *= value;
        return this.clamp();
    }

    public Color add(Color color) {
        this.r += color.r;
        this.g += color.g;
        this.b += color.b;
        this.a += color.a;
        return this.clamp();
    }

    public Color sub(Color color) {
        this.r -= color.r;
        this.g -= color.g;
        this.b -= color.b;
        this.a -= color.a;
        return this.clamp();
    }

    public Color clamp() {
        if (this.r < 0.0f) {
            this.r = 0.0f;
        } else if (this.r > 1.0f) {
            this.r = 1.0f;
        }
        if (this.g < 0.0f) {
            this.g = 0.0f;
        } else if (this.g > 1.0f) {
            this.g = 1.0f;
        }
        if (this.b < 0.0f) {
            this.b = 0.0f;
        } else if (this.b > 1.0f) {
            this.b = 1.0f;
        }
        if (this.a < 0.0f) {
            this.a = 0.0f;
        } else if (this.a > 1.0f) {
            this.a = 1.0f;
        }
        return this;
    }

    public Color set(float r, float g, float b, float a) {
        this.r = r;
        this.g = g;
        this.b = b;
        this.a = a;
        return this.clamp();
    }

    public Color set(int rgba) {
        Color.rgba8888ToColor(this, rgba);
        return this;
    }

    public Color add(float r, float g, float b, float a) {
        this.r += r;
        this.g += g;
        this.b += b;
        this.a += a;
        return this.clamp();
    }

    public Color sub(float r, float g, float b, float a) {
        this.r -= r;
        this.g -= g;
        this.b -= b;
        this.a -= a;
        return this.clamp();
    }

    public Color mul(float r, float g, float b, float a) {
        this.r *= r;
        this.g *= g;
        this.b *= b;
        this.a *= a;
        return this.clamp();
    }

    public Color lerp(Color target, float t) {
        this.r += t * (target.r - this.r);
        this.g += t * (target.g - this.g);
        this.b += t * (target.b - this.b);
        this.a += t * (target.a - this.a);
        return this.clamp();
    }

    public Color lerp(float r, float g, float b, float a, float t) {
        this.r += t * (r - this.r);
        this.g += t * (g - this.g);
        this.b += t * (b - this.b);
        this.a += t * (a - this.a);
        return this.clamp();
    }

    public Color premultiplyAlpha() {
        this.r *= this.a;
        this.g *= this.a;
        this.b *= this.a;
        return this;
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || this.getClass() != o.getClass()) {
            return false;
        }
        Color color = (Color)o;
        return this.toIntBits() == color.toIntBits();
    }

    public int hashCode() {
        int result = this.r != 0.0f ? NumberUtils.floatToIntBits(this.r) : 0;
        result = 31 * result + (this.g != 0.0f ? NumberUtils.floatToIntBits(this.g) : 0);
        result = 31 * result + (this.b != 0.0f ? NumberUtils.floatToIntBits(this.b) : 0);
        result = 31 * result + (this.a != 0.0f ? NumberUtils.floatToIntBits(this.a) : 0);
        return result;
    }

    public float toFloatBits() {
        int color = (int)(255.0f * this.a) << 24 | (int)(255.0f * this.b) << 16 | (int)(255.0f * this.g) << 8 | (int)(255.0f * this.r);
        return NumberUtils.intToFloatColor(color);
    }

    public int toIntBits() {
        int color = (int)(255.0f * this.a) << 24 | (int)(255.0f * this.b) << 16 | (int)(255.0f * this.g) << 8 | (int)(255.0f * this.r);
        return color;
    }

    public String toString() {
        String value = Integer.toHexString((int)(255.0f * this.r) << 24 | (int)(255.0f * this.g) << 16 | (int)(255.0f * this.b) << 8 | (int)(255.0f * this.a));
        while (value.length() < 8) {
            value = "0" + value;
        }
        return value;
    }

    public static Color valueOf(String hex) {
        hex = hex.charAt(0) == '#' ? hex.substring(1) : hex;
        int r = Integer.valueOf(hex.substring(0, 2), 16);
        int g = Integer.valueOf(hex.substring(2, 4), 16);
        int b = Integer.valueOf(hex.substring(4, 6), 16);
        int a = hex.length() != 8 ? 255 : Integer.valueOf(hex.substring(6, 8), 16);
        return new Color((float)r / 255.0f, (float)g / 255.0f, (float)b / 255.0f, (float)a / 255.0f);
    }

    public static float toFloatBits(int r, int g, int b, int a) {
        int color = a << 24 | b << 16 | g << 8 | r;
        float floatColor = NumberUtils.intToFloatColor(color);
        return floatColor;
    }

    public static float toFloatBits(float r, float g, float b, float a) {
        int color = (int)(255.0f * a) << 24 | (int)(255.0f * b) << 16 | (int)(255.0f * g) << 8 | (int)(255.0f * r);
        return NumberUtils.intToFloatColor(color);
    }

    public static int toIntBits(int r, int g, int b, int a) {
        return a << 24 | b << 16 | g << 8 | r;
    }

    public static int alpha(float alpha) {
        return (int)(alpha * 255.0f);
    }

    public static int luminanceAlpha(float luminance, float alpha) {
        return (int)(luminance * 255.0f) << 8 | (int)(alpha * 255.0f);
    }

    public static int rgb565(float r, float g, float b) {
        return (int)(r * 31.0f) << 11 | (int)(g * 63.0f) << 5 | (int)(b * 31.0f);
    }

    public static int rgba4444(float r, float g, float b, float a) {
        return (int)(r * 15.0f) << 12 | (int)(g * 15.0f) << 8 | (int)(b * 15.0f) << 4 | (int)(a * 15.0f);
    }

    public static int rgb888(float r, float g, float b) {
        return (int)(r * 255.0f) << 16 | (int)(g * 255.0f) << 8 | (int)(b * 255.0f);
    }

    public static int rgba8888(float r, float g, float b, float a) {
        return (int)(r * 255.0f) << 24 | (int)(g * 255.0f) << 16 | (int)(b * 255.0f) << 8 | (int)(a * 255.0f);
    }

    public static int argb8888(float a, float r, float g, float b) {
        return (int)(a * 255.0f) << 24 | (int)(r * 255.0f) << 16 | (int)(g * 255.0f) << 8 | (int)(b * 255.0f);
    }

    public static int rgb565(Color color) {
        return (int)(color.r * 31.0f) << 11 | (int)(color.g * 63.0f) << 5 | (int)(color.b * 31.0f);
    }

    public static int rgba4444(Color color) {
        return (int)(color.r * 15.0f) << 12 | (int)(color.g * 15.0f) << 8 | (int)(color.b * 15.0f) << 4 | (int)(color.a * 15.0f);
    }

    public static int rgb888(Color color) {
        return (int)(color.r * 255.0f) << 16 | (int)(color.g * 255.0f) << 8 | (int)(color.b * 255.0f);
    }

    public static int rgba8888(Color color) {
        return (int)(color.r * 255.0f) << 24 | (int)(color.g * 255.0f) << 16 | (int)(color.b * 255.0f) << 8 | (int)(color.a * 255.0f);
    }

    public static int argb8888(Color color) {
        return (int)(color.a * 255.0f) << 24 | (int)(color.r * 255.0f) << 16 | (int)(color.g * 255.0f) << 8 | (int)(color.b * 255.0f);
    }

    public static void rgb565ToColor(Color color, int value) {
        color.r = (float)((value & 0xF800) >>> 11) / 31.0f;
        color.g = (float)((value & 0x7E0) >>> 5) / 63.0f;
        color.b = (float)((value & 0x1F) >>> 0) / 31.0f;
    }

    public static void rgba4444ToColor(Color color, int value) {
        color.r = (float)((value & 0xF000) >>> 12) / 15.0f;
        color.g = (float)((value & 0xF00) >>> 8) / 15.0f;
        color.b = (float)((value & 0xF0) >>> 4) / 15.0f;
        color.a = (float)(value & 0xF) / 15.0f;
    }

    public static void rgb888ToColor(Color color, int value) {
        color.r = (float)((value & 0xFF0000) >>> 16) / 255.0f;
        color.g = (float)((value & 0xFF00) >>> 8) / 255.0f;
        color.b = (float)(value & 0xFF) / 255.0f;
    }

    public static void rgba8888ToColor(Color color, int value) {
        color.r = (float)((value & 0xFF000000) >>> 24) / 255.0f;
        color.g = (float)((value & 0xFF0000) >>> 16) / 255.0f;
        color.b = (float)((value & 0xFF00) >>> 8) / 255.0f;
        color.a = (float)(value & 0xFF) / 255.0f;
    }

    public static void argb8888ToColor(Color color, int value) {
        color.a = (float)((value & 0xFF000000) >>> 24) / 255.0f;
        color.r = (float)((value & 0xFF0000) >>> 16) / 255.0f;
        color.g = (float)((value & 0xFF00) >>> 8) / 255.0f;
        color.b = (float)(value & 0xFF) / 255.0f;
    }

    public static void abgr8888ToColor(Color color, float value) {
        int c = NumberUtils.floatToIntColor(value);
        color.a = (float)((c & 0xFF000000) >>> 24) / 255.0f;
        color.b = (float)((c & 0xFF0000) >>> 16) / 255.0f;
        color.g = (float)((c & 0xFF00) >>> 8) / 255.0f;
        color.r = (float)(c & 0xFF) / 255.0f;
    }

    public Color cpy() {
        return new Color(this);
    }
}

