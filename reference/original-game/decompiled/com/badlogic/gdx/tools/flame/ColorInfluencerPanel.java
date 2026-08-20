/*
 * Decompiled with CFR 0.152.
 */
package com.badlogic.gdx.tools.flame;

import com.badlogic.gdx.graphics.g3d.particles.influencers.ColorInfluencer;
import com.badlogic.gdx.tools.flame.FlameMain;
import com.badlogic.gdx.tools.flame.GradientPanel;
import com.badlogic.gdx.tools.flame.InfluencerPanel;
import com.badlogic.gdx.tools.flame.PercentagePanel;

public class ColorInfluencerPanel
extends InfluencerPanel<ColorInfluencer.Single> {
    GradientPanel tintPanel;
    PercentagePanel alphaPanel;

    public ColorInfluencerPanel(FlameMain particleEditor3D, ColorInfluencer.Single influencer) {
        super(particleEditor3D, influencer, "Color Influencer", "Sets the particle color.");
        this.initializeComponents(influencer);
        this.setValue(influencer);
    }

    private void initializeComponents(ColorInfluencer.Single emitter) {
        int i = 0;
        int n = i++;
        this.tintPanel = new GradientPanel(this.editor, emitter.colorValue, "Tint", "", false);
        this.addContent(n, 0, this.tintPanel);
        int n2 = i++;
        this.alphaPanel = new PercentagePanel(this.editor, emitter.alphaValue, "Life", "Transparency", "");
        this.addContent(n2, 0, this.alphaPanel);
        this.tintPanel.showContent(true);
        this.alphaPanel.showContent(true);
    }
}

