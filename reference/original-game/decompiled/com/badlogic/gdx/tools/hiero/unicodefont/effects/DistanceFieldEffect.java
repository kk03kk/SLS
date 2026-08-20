/*
 * Decompiled with CFR 0.152.
 */
package com.badlogic.gdx.tools.hiero.unicodefont.effects;

import com.badlogic.gdx.tools.distancefield.DistanceFieldGenerator;
import com.badlogic.gdx.tools.hiero.unicodefont.Glyph;
import com.badlogic.gdx.tools.hiero.unicodefont.UnicodeFont;
import com.badlogic.gdx.tools.hiero.unicodefont.effects.ConfigurableEffect;
import com.badlogic.gdx.tools.hiero.unicodefont.effects.EffectUtil;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

public class DistanceFieldEffect
implements ConfigurableEffect {
    private Color color = Color.WHITE;
    private int scale = 1;
    private float spread = 1.0f;

    private void drawGlyph(BufferedImage image, Glyph glyph) {
        Graphics2D inputG = (Graphics2D)image.getGraphics();
        inputG.setTransform(AffineTransform.getScaleInstance(this.scale, this.scale));
        inputG.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
        inputG.setColor(Color.WHITE);
        inputG.fill(glyph.getShape());
    }

    @Override
    public void draw(BufferedImage image, Graphics2D g, UnicodeFont unicodeFont, Glyph glyph) {
        BufferedImage input = new BufferedImage(this.scale * glyph.getWidth(), this.scale * glyph.getHeight(), 12);
        this.drawGlyph(input, glyph);
        DistanceFieldGenerator generator = new DistanceFieldGenerator();
        generator.setColor(this.color);
        generator.setDownscale(this.scale);
        generator.setSpread((float)this.scale * this.spread);
        BufferedImage distanceField = generator.generateDistanceField(input);
        g.drawImage(distanceField, new AffineTransform(), null);
    }

    public String toString() {
        return "Distance field";
    }

    @Override
    public List getValues() {
        ArrayList<ConfigurableEffect.Value> values = new ArrayList<ConfigurableEffect.Value>();
        values.add(EffectUtil.colorValue("Color", this.color));
        values.add(EffectUtil.intValue("Scale", this.scale, "The distance field is computed from an image larger than the output glyph by this factor. Set this to a higher value for more accuracy, but slower font generation."));
        values.add(EffectUtil.floatValue("Spread", this.spread, 1.0f, Float.MAX_VALUE, "The maximum distance from edges where the effect of the distance field is seen. Set this to about half the width of lines in your output font."));
        return values;
    }

    @Override
    public void setValues(List values) {
        for (ConfigurableEffect.Value value : values) {
            if ("Color".equals(value.getName())) {
                this.color = (Color)value.getObject();
                continue;
            }
            if ("Scale".equals(value.getName())) {
                this.scale = Math.max(1, (Integer)value.getObject());
                continue;
            }
            if (!"Spread".equals(value.getName())) continue;
            this.spread = Math.max(0.0f, ((Float)value.getObject()).floatValue());
        }
    }
}

