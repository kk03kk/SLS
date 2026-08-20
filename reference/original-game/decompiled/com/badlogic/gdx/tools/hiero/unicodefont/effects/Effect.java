/*
 * Decompiled with CFR 0.152.
 */
package com.badlogic.gdx.tools.hiero.unicodefont.effects;

import com.badlogic.gdx.tools.hiero.unicodefont.Glyph;
import com.badlogic.gdx.tools.hiero.unicodefont.UnicodeFont;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

public interface Effect {
    public void draw(BufferedImage var1, Graphics2D var2, UnicodeFont var3, Glyph var4);
}

