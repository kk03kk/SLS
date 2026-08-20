/*
 * Decompiled with CFR 0.152.
 */
package com.badlogic.gdx.tools.flame;

import com.badlogic.gdx.graphics.g3d.particles.ParticleController;
import com.badlogic.gdx.tools.flame.EditorPanel;
import com.badlogic.gdx.tools.flame.FlameMain;
import com.badlogic.gdx.tools.flame.Slider;
import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.Insets;
import javax.swing.JLabel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

class CountPanel
extends EditorPanel {
    Slider maxSlider;
    Slider minSlider;

    public CountPanel(FlameMain editor, String name, String description, int min, int max) {
        super(editor, name, description);
        this.initializeComponents(min, max);
        this.setValue(null);
    }

    private void initializeComponents(int min, int max) {
        this.minSlider = new Slider(0.0f, 0.0f, 999999.0f, 1.0f);
        this.minSlider.setValue(min);
        this.minSlider.addChangeListener(new ChangeListener(){

            @Override
            public void stateChanged(ChangeEvent event) {
                ParticleController controller = CountPanel.this.editor.getEmitter();
                controller.emitter.minParticleCount = (int)CountPanel.this.minSlider.getValue();
                CountPanel.this.editor.restart();
            }
        });
        this.maxSlider = new Slider(0.0f, 0.0f, 999999.0f, 1.0f);
        this.maxSlider.setValue(max);
        this.maxSlider.addChangeListener(new ChangeListener(){

            @Override
            public void stateChanged(ChangeEvent event) {
                ParticleController controller = CountPanel.this.editor.getEmitter();
                controller.emitter.maxParticleCount = (int)CountPanel.this.maxSlider.getValue();
                CountPanel.this.editor.restart();
            }
        });
        int i = 0;
        this.contentPanel.add((Component)new JLabel("Min"), new GridBagConstraints(0, i, 1, 1, 0.0, 0.0, 17, 0, new Insets(6, 0, 0, 0), 0, 0));
        this.contentPanel.add((Component)this.minSlider, new GridBagConstraints(1, i++, 1, 1, 1.0, 0.0, 17, 0, new Insets(6, 0, 0, 0), 0, 0));
        this.contentPanel.add((Component)new JLabel("Max"), new GridBagConstraints(0, i, 1, 1, 0.0, 0.0, 17, 0, new Insets(6, 0, 0, 0), 0, 0));
        this.contentPanel.add((Component)this.maxSlider, new GridBagConstraints(1, i++, 1, 1, 1.0, 0.0, 17, 0, new Insets(6, 0, 0, 0), 0, 0));
    }
}

