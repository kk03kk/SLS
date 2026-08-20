/*
 * Decompiled with CFR 0.152.
 */
package com.badlogic.gdx.tools.flame;

import com.badlogic.gdx.graphics.g3d.particles.influencers.Influencer;
import com.badlogic.gdx.tools.flame.EditorPanel;
import com.badlogic.gdx.tools.flame.FlameMain;

public abstract class InfluencerPanel<T extends Influencer>
extends EditorPanel<T> {
    public InfluencerPanel(FlameMain editor, T influencer, String name, String description) {
        super(editor, name, description, true, true);
        this.setValue(influencer);
    }

    public InfluencerPanel(FlameMain editor, T influencer, String name, String description, boolean isAlwaysActive, boolean isRemovable) {
        super(editor, name, description, isAlwaysActive, isRemovable);
        this.setValue(influencer);
    }

    @Override
    protected void removePanel() {
        super.removePanel();
        this.editor.getEmitter().influencers.removeValue((Influencer)this.value, true);
        this.editor.getEmitter().init();
        this.editor.getEmitter().start();
        this.editor.reloadRows();
    }
}

