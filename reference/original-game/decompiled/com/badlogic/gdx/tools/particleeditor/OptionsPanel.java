/*
 * Decompiled with CFR 0.152.
 */
package com.badlogic.gdx.tools.particleeditor;

import com.badlogic.gdx.graphics.g2d.ParticleEmitter;
import com.badlogic.gdx.tools.particleeditor.EditorPanel;
import com.badlogic.gdx.tools.particleeditor.ParticleEditor;
import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

class OptionsPanel
extends EditorPanel {
    JCheckBox attachedCheckBox;
    JCheckBox continuousCheckbox;
    JCheckBox alignedCheckbox;
    JCheckBox additiveCheckbox;
    JCheckBox behindCheckbox;
    JCheckBox premultipliedAlphaCheckbox;

    public OptionsPanel(final ParticleEditor editor, String name, String description) {
        super(null, name, description);
        this.initializeComponents();
        this.attachedCheckBox.addActionListener(new ActionListener(){

            @Override
            public void actionPerformed(ActionEvent event) {
                editor.getEmitter().setAttached(OptionsPanel.this.attachedCheckBox.isSelected());
            }
        });
        this.continuousCheckbox.addActionListener(new ActionListener(){

            @Override
            public void actionPerformed(ActionEvent event) {
                editor.getEmitter().setContinuous(OptionsPanel.this.continuousCheckbox.isSelected());
            }
        });
        this.alignedCheckbox.addActionListener(new ActionListener(){

            @Override
            public void actionPerformed(ActionEvent event) {
                editor.getEmitter().setAligned(OptionsPanel.this.alignedCheckbox.isSelected());
            }
        });
        this.additiveCheckbox.addActionListener(new ActionListener(){

            @Override
            public void actionPerformed(ActionEvent event) {
                editor.getEmitter().setAdditive(OptionsPanel.this.additiveCheckbox.isSelected());
            }
        });
        this.behindCheckbox.addActionListener(new ActionListener(){

            @Override
            public void actionPerformed(ActionEvent event) {
                editor.getEmitter().setBehind(OptionsPanel.this.behindCheckbox.isSelected());
            }
        });
        this.premultipliedAlphaCheckbox.addActionListener(new ActionListener(){

            @Override
            public void actionPerformed(ActionEvent event) {
                editor.getEmitter().setPremultipliedAlpha(OptionsPanel.this.premultipliedAlphaCheckbox.isSelected());
            }
        });
        ParticleEmitter emitter = editor.getEmitter();
        this.attachedCheckBox.setSelected(emitter.isAttached());
        this.continuousCheckbox.setSelected(emitter.isContinuous());
        this.alignedCheckbox.setSelected(emitter.isAligned());
        this.additiveCheckbox.setSelected(emitter.isAdditive());
        this.behindCheckbox.setSelected(emitter.isBehind());
        this.premultipliedAlphaCheckbox.setSelected(emitter.isPremultipliedAlpha());
    }

    private void initializeComponents() {
        JPanel contentPanel = this.getContentPanel();
        JLabel label = new JLabel("Additive:");
        contentPanel.add((Component)label, new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0, 13, 0, new Insets(6, 0, 0, 0), 0, 0));
        this.additiveCheckbox = new JCheckBox();
        contentPanel.add((Component)this.additiveCheckbox, new GridBagConstraints(1, 1, 1, 1, 0.0, 0.0, 17, 0, new Insets(6, 6, 0, 0), 0, 0));
        label = new JLabel("Attached:");
        contentPanel.add((Component)label, new GridBagConstraints(0, 2, 1, 1, 0.0, 0.0, 13, 0, new Insets(6, 0, 0, 0), 0, 0));
        this.attachedCheckBox = new JCheckBox();
        contentPanel.add((Component)this.attachedCheckBox, new GridBagConstraints(1, 2, 1, 1, 1.0, 0.0, 17, 0, new Insets(6, 6, 0, 0), 0, 0));
        label = new JLabel("Continuous:");
        contentPanel.add((Component)label, new GridBagConstraints(0, 3, 1, 1, 0.0, 0.0, 13, 0, new Insets(6, 0, 0, 0), 0, 0));
        this.continuousCheckbox = new JCheckBox();
        contentPanel.add((Component)this.continuousCheckbox, new GridBagConstraints(1, 3, 1, 1, 0.0, 0.0, 17, 0, new Insets(6, 6, 0, 0), 0, 0));
        label = new JLabel("Aligned:");
        contentPanel.add((Component)label, new GridBagConstraints(0, 4, 1, 1, 0.0, 0.0, 13, 0, new Insets(6, 0, 0, 0), 0, 0));
        this.alignedCheckbox = new JCheckBox();
        contentPanel.add((Component)this.alignedCheckbox, new GridBagConstraints(1, 4, 1, 1, 0.0, 0.0, 17, 0, new Insets(6, 6, 0, 0), 0, 0));
        label = new JLabel("Behind:");
        contentPanel.add((Component)label, new GridBagConstraints(0, 5, 1, 1, 0.0, 0.0, 13, 0, new Insets(6, 0, 0, 0), 0, 0));
        this.behindCheckbox = new JCheckBox();
        contentPanel.add((Component)this.behindCheckbox, new GridBagConstraints(1, 5, 1, 1, 0.0, 0.0, 17, 0, new Insets(6, 6, 0, 0), 0, 0));
        label = new JLabel("Premultiplied Alpha:");
        contentPanel.add((Component)label, new GridBagConstraints(0, 6, 1, 1, 0.0, 0.0, 13, 0, new Insets(6, 0, 0, 0), 0, 0));
        this.premultipliedAlphaCheckbox = new JCheckBox();
        contentPanel.add((Component)this.premultipliedAlphaCheckbox, new GridBagConstraints(1, 6, 1, 1, 0.0, 0.0, 17, 0, new Insets(6, 6, 0, 0), 0, 0));
    }
}

