/*
 * Decompiled with CFR 0.152.
 */
package com.badlogic.gdx.tools.flame;

import com.badlogic.gdx.graphics.g3d.particles.influencers.ScaleInfluencer;
import com.badlogic.gdx.tools.flame.FlameMain;
import com.badlogic.gdx.tools.flame.InfluencerPanel;
import com.badlogic.gdx.tools.flame.ScaledNumericPanel;

public class ScaleInfluencerPanel
extends InfluencerPanel<ScaleInfluencer> {
    ScaledNumericPanel scalePanel;

    public ScaleInfluencerPanel(FlameMain editor, ScaleInfluencer influencer) {
        super(editor, influencer, "Scale Influencer", "Particle scale, in world units.");
        this.setValue(influencer);
    }

    @Override
    public void setValue(ScaleInfluencer value) {
        super.setValue(value);
        if (value == null) {
            return;
        }
        this.scalePanel.setValue(value.value);
    }

    @Override
    protected void initializeComponents() {
        super.initializeComponents();
        this.scalePanel = new ScaledNumericPanel(this.editor, null, "Life", "", "");
        this.addContent(0, 0, this.scalePanel);
        this.scalePanel.setIsAlwayShown(true);
    }
}

