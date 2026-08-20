/*
 * Decompiled with CFR 0.152.
 */
package com.badlogic.gdx.tools.flame;

import com.badlogic.gdx.graphics.g3d.particles.values.ParticleValue;
import com.badlogic.gdx.tools.flame.EditorPanel;
import com.badlogic.gdx.tools.flame.FlameMain;

public class ParticleValuePanel<T extends ParticleValue>
extends EditorPanel<T> {
    public ParticleValuePanel(FlameMain editor, String name, String description) {
        this(editor, name, description, true);
    }

    public ParticleValuePanel(FlameMain editor, String name, String description, boolean isAlwaysActive) {
        this(editor, name, description, isAlwaysActive, false);
    }

    public ParticleValuePanel(FlameMain editor, String name, String description, boolean isAlwaysActive, boolean isRemovable) {
        super(editor, name, description, isAlwaysActive, isRemovable);
    }

    @Override
    public void setHasAdvanced(boolean hasAdvanced) {
        super.setHasAdvanced(hasAdvanced);
        this.advancedButton.setVisible(hasAdvanced && (((ParticleValue)this.value).isActive() || this.isAlwaysActive));
    }

    @Override
    public void setValue(T value) {
        super.setValue(value);
        if (value != null) {
            this.activeButton.setSelected(((ParticleValue)value).isActive());
        }
    }

    @Override
    protected void activate() {
        super.activate();
        if (this.value != null) {
            ((ParticleValue)this.value).setActive(this.activeButton.isSelected());
        }
    }
}

