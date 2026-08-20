/*
 * Decompiled with CFR 0.152.
 */
package com.badlogic.gdx.tools.particleeditor;

import com.badlogic.gdx.tools.particleeditor.EditorPanel;
import com.badlogic.gdx.tools.particleeditor.ParticleEditor;
import com.badlogic.gdx.tools.particleeditor.Slider;
import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.Insets;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

class CountPanel
extends EditorPanel {
    Slider maxSlider;
    Slider minSlider;

    public CountPanel(final ParticleEditor editor, String name, String description) {
        super(null, name, description);
        this.initializeComponents();
        this.maxSlider.setValue(editor.getEmitter().getMaxParticleCount());
        this.maxSlider.addChangeListener(new ChangeListener(){

            @Override
            public void stateChanged(ChangeEvent event) {
                editor.getEmitter().setMaxParticleCount((int)CountPanel.this.maxSlider.getValue());
            }
        });
        this.minSlider.setValue(editor.getEmitter().getMinParticleCount());
        this.minSlider.addChangeListener(new ChangeListener(){

            @Override
            public void stateChanged(ChangeEvent event) {
                editor.getEmitter().setMinParticleCount((int)CountPanel.this.minSlider.getValue());
            }
        });
    }

    private void initializeComponents() {
        JPanel contentPanel = this.getContentPanel();
        JLabel label = new JLabel("Min:");
        contentPanel.add((Component)label, new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0, 13, 0, new Insets(0, 0, 0, 6), 0, 0));
        this.minSlider = new Slider(0.0f, 0.0f, 99999.0f, 1.0f, 0.0f, 500.0f);
        contentPanel.add((Component)this.minSlider, new GridBagConstraints(1, 1, 1, 1, 0.0, 0.0, 17, 0, new Insets(0, 0, 0, 0), 0, 0));
        label = new JLabel("Max:");
        contentPanel.add((Component)label, new GridBagConstraints(2, 1, 1, 1, 0.0, 0.0, 13, 0, new Insets(0, 12, 0, 6), 0, 0));
        this.maxSlider = new Slider(0.0f, 0.0f, 99999.0f, 1.0f, 0.0f, 500.0f);
        contentPanel.add((Component)this.maxSlider, new GridBagConstraints(3, 1, 1, 1, 1.0, 0.0, 17, 0, new Insets(0, 0, 0, 0), 0, 0));
    }
}

