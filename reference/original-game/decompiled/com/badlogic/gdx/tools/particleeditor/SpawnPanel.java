/*
 * Decompiled with CFR 0.152.
 */
package com.badlogic.gdx.tools.particleeditor;

import com.badlogic.gdx.graphics.g2d.ParticleEmitter;
import com.badlogic.gdx.tools.particleeditor.EditorPanel;
import com.badlogic.gdx.tools.particleeditor.ParticleEditor;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

class SpawnPanel
extends EditorPanel {
    JComboBox shapeCombo;
    JCheckBox edgesCheckbox;
    JLabel edgesLabel;
    JComboBox sideCombo;
    JLabel sideLabel;

    public SpawnPanel(final ParticleEditor editor, final ParticleEmitter.SpawnShapeValue spawnShapeValue, String name, String description) {
        super(null, name, description);
        this.initializeComponents();
        this.edgesCheckbox.setSelected(spawnShapeValue.isEdges());
        this.sideCombo.setSelectedItem((Object)spawnShapeValue.getShape());
        this.shapeCombo.addActionListener(new ActionListener(){

            @Override
            public void actionPerformed(ActionEvent event) {
                ParticleEmitter.SpawnShape shape = (ParticleEmitter.SpawnShape)((Object)SpawnPanel.this.shapeCombo.getSelectedItem());
                spawnShapeValue.setShape(shape);
                switch (shape) {
                    case line: 
                    case square: {
                        SpawnPanel.this.setEdgesVisible(false);
                        editor.setVisible("Spawn Width", true);
                        editor.setVisible("Spawn Height", true);
                        break;
                    }
                    case ellipse: {
                        SpawnPanel.this.setEdgesVisible(true);
                        editor.setVisible("Spawn Width", true);
                        editor.setVisible("Spawn Height", true);
                        break;
                    }
                    case point: {
                        SpawnPanel.this.setEdgesVisible(false);
                        editor.setVisible("Spawn Width", false);
                        editor.setVisible("Spawn Height", false);
                    }
                }
            }
        });
        this.edgesCheckbox.addActionListener(new ActionListener(){

            @Override
            public void actionPerformed(ActionEvent event) {
                spawnShapeValue.setEdges(SpawnPanel.this.edgesCheckbox.isSelected());
                SpawnPanel.this.setEdgesVisible(true);
            }
        });
        this.sideCombo.addActionListener(new ActionListener(){

            @Override
            public void actionPerformed(ActionEvent event) {
                ParticleEmitter.SpawnEllipseSide side = (ParticleEmitter.SpawnEllipseSide)((Object)SpawnPanel.this.sideCombo.getSelectedItem());
                spawnShapeValue.setSide(side);
            }
        });
        this.shapeCombo.setSelectedItem((Object)spawnShapeValue.getShape());
    }

    @Override
    public void update(ParticleEditor editor) {
        this.shapeCombo.setSelectedItem((Object)editor.getEmitter().getSpawnShape().getShape());
    }

    void setEdgesVisible(boolean visible) {
        this.edgesCheckbox.setVisible(visible);
        this.edgesLabel.setVisible(visible);
        visible = visible && this.edgesCheckbox.isSelected();
        this.sideCombo.setVisible(visible);
        this.sideLabel.setVisible(visible);
    }

    private void initializeComponents() {
        JPanel contentPanel = this.getContentPanel();
        JLabel label = new JLabel("Shape:");
        contentPanel.add((Component)label, new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0, 13, 0, new Insets(0, 0, 0, 6), 0, 0));
        this.shapeCombo = new JComboBox();
        this.shapeCombo.setModel(new DefaultComboBoxModel<ParticleEmitter.SpawnShape>(ParticleEmitter.SpawnShape.values()));
        contentPanel.add((Component)this.shapeCombo, new GridBagConstraints(1, 1, 1, 1, 0.0, 0.0, 17, 0, new Insets(0, 0, 0, 0), 0, 0));
        this.edgesLabel = new JLabel("Edges:");
        contentPanel.add((Component)this.edgesLabel, new GridBagConstraints(2, 1, 1, 1, 0.0, 0.0, 13, 0, new Insets(0, 12, 0, 6), 0, 0));
        this.edgesCheckbox = new JCheckBox();
        contentPanel.add((Component)this.edgesCheckbox, new GridBagConstraints(3, 1, 1, 1, 0.0, 0.0, 17, 0, new Insets(0, 0, 0, 0), 0, 0));
        this.sideLabel = new JLabel("Side:");
        contentPanel.add((Component)this.sideLabel, new GridBagConstraints(4, 1, 1, 1, 0.0, 0.0, 13, 0, new Insets(0, 12, 0, 6), 0, 0));
        this.sideCombo = new JComboBox();
        this.sideCombo.setModel(new DefaultComboBoxModel<ParticleEmitter.SpawnEllipseSide>(ParticleEmitter.SpawnEllipseSide.values()));
        contentPanel.add((Component)this.sideCombo, new GridBagConstraints(5, 1, 1, 1, 0.0, 0.0, 17, 0, new Insets(0, 0, 0, 0), 0, 0));
        JPanel spacer = new JPanel();
        spacer.setPreferredSize(new Dimension());
        contentPanel.add((Component)spacer, new GridBagConstraints(6, 0, 1, 1, 1.0, 0.0, 17, 0, new Insets(0, 0, 0, 0), 0, 0));
    }
}

