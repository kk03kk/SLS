/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.cards;

import com.megacrit.cardcrawl.core.Settings;

public class DescriptionLine {
    public String text;
    public float width;
    private String[] cachedTokenizedText;
    private String[] cachedTokenizedTextCN;
    private static final float offsetter = 10.0f * Settings.scale;

    public DescriptionLine(String text, float width) {
        this.text = text.trim();
        this.width = width -= offsetter;
    }

    public String[] getCachedTokenizedText() {
        if (this.cachedTokenizedText == null) {
            this.cachedTokenizedText = DescriptionLine.tokenize(this.text);
        }
        return this.cachedTokenizedText;
    }

    public String[] getCachedTokenizedTextCN() {
        if (this.cachedTokenizedTextCN == null) {
            this.cachedTokenizedTextCN = DescriptionLine.tokenizeCN(this.text);
        }
        return this.cachedTokenizedTextCN;
    }

    private static String[] tokenize(String desc) {
        String[] tokenized = desc.split("\\s+");
        int i = 0;
        while (i < tokenized.length) {
            int n = i++;
            tokenized[n] = tokenized[n] + ' ';
        }
        return tokenized;
    }

    private static String[] tokenizeCN(String desc) {
        String[] tokenized = desc.split("\\s+");
        for (int i = 0; i < tokenized.length; ++i) {
            tokenized[i] = tokenized[i].replace("!", "");
        }
        return tokenized;
    }

    public String getText() {
        return this.text;
    }
}

