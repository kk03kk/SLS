/*
 * Decompiled with CFR 0.152.
 */
package com.badlogic.gdx.tools.particleeditor;

import com.badlogic.gdx.graphics.g2d.ParticleEmitter;
import com.badlogic.gdx.tools.particleeditor.ParticleEditor;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JToggleButton;

class EditorPanel
extends JPanel {
    private final String name;
    private final String description;
    private final ParticleEmitter.ParticleValue value;
    private JPanel titlePanel;
    JToggleButton activeButton;
    private JPanel contentPanel;
    JToggleButton advancedButton;
    JPanel advancedPanel;
    private boolean hasAdvanced;
    JLabel descriptionLabel;

    public EditorPanel(ParticleEmitter.ParticleValue value, String name, String description) {
        this.name = name;
        this.value = value;
        this.description = description;
        this.initializeComponents();
        this.titlePanel.addMouseListener(new MouseAdapter(){

            @Override
            public void mouseClicked(MouseEvent event) {
                if (!EditorPanel.this.activeButton.isVisible()) {
                    return;
                }
                EditorPanel.this.activeButton.setSelected(!EditorPanel.this.activeButton.isSelected());
                EditorPanel.this.updateActive();
            }
        });
        this.activeButton.addActionListener(new ActionListener(){

            @Override
            public void actionPerformed(ActionEvent event) {
                EditorPanel.this.updateActive();
            }
        });
        this.advancedButton.addActionListener(new ActionListener(){

            @Override
            public void actionPerformed(ActionEvent event) {
                EditorPanel.this.advancedPanel.setVisible(EditorPanel.this.advancedButton.isSelected());
            }
        });
        if (value != null) {
            this.activeButton.setSelected(value.isActive());
            this.updateActive();
        }
        boolean alwaysActive = value == null ? true : value.isAlwaysActive();
        this.activeButton.setVisible(!alwaysActive);
        if (alwaysActive) {
            this.contentPanel.setVisible(true);
        }
        if (alwaysActive) {
            this.titlePanel.setCursor(null);
        }
    }

    void updateActive() {
        this.contentPanel.setVisible(this.activeButton.isSelected());
        this.advancedPanel.setVisible(this.activeButton.isSelected() && this.advancedButton.isSelected());
        this.advancedButton.setVisible(this.activeButton.isSelected() && this.hasAdvanced);
        this.descriptionLabel.setText(this.activeButton.isSelected() ? this.description : "");
        if (this.value != null) {
            this.value.setActive(this.activeButton.isSelected());
        }
    }

    public void update(ParticleEditor editor) {
    }

    public void setHasAdvanced(boolean hasAdvanced) {
        this.hasAdvanced = hasAdvanced;
        this.advancedButton.setVisible(hasAdvanced && (this.value.isActive() || this.value.isAlwaysActive()));
    }

    public JPanel getContentPanel() {
        return this.contentPanel;
    }

    public JPanel getAdvancedPanel() {
        return this.advancedPanel;
    }

    @Override
    public String getName() {
        return this.name;
    }

    public void setEmbedded() {
        GridBagLayout layout = (GridBagLayout)this.getLayout();
        GridBagConstraints constraints = layout.getConstraints(this.contentPanel);
        constraints.insets = new Insets(0, 0, 0, 0);
        layout.setConstraints(this.contentPanel, constraints);
        this.titlePanel.setVisible(false);
    }

    private void initializeComponents() {
        this.setLayout(new GridBagLayout());
        this.titlePanel = new JPanel(new GridBagLayout());
        this.add((Component)this.titlePanel, new GridBagConstraints(1, 0, 1, 1, 1.0, 0.0, 17, 2, new Insets(3, 0, 3, 0), 0, 0));
        this.titlePanel.setCursor(Cursor.getPredefinedCursor(12));
        JLabel label = new JLabel(this.name);
        this.titlePanel.add((Component)label, new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0, 17, 0, new Insets(3, 6, 3, 6), 0, 0));
        label.setFont(label.getFont().deriveFont(1));
        this.descriptionLabel = new JLabel(this.description);
        this.titlePanel.add((Component)this.descriptionLabel, new GridBagConstraints(1, 0, 1, 1, 1.0, 0.0, 17, 0, new Insets(3, 6, 3, 6), 0, 0));
        this.advancedButton = new JToggleButton("Advanced");
        this.titlePanel.add((Component)this.advancedButton, new GridBagConstraints(2, 0, 1, 1, 0.0, 0.0, 10, 0, new Insets(0, 0, 0, 6), 0, 0));
        this.advancedButton.setVisible(false);
        this.activeButton = new JToggleButton("Active");
        this.titlePanel.add((Component)this.activeButton, new GridBagConstraints(3, 0, 1, 1, 0.0, 0.0, 10, 0, new Insets(0, 0, 0, 6), 0, 0));
        this.contentPanel = new JPanel(new GridBagLayout());
        this.add((Component)this.contentPanel, new GridBagConstraints(1, 1, 1, 1, 0.0, 0.0, 10, 1, new Insets(0, 6, 6, 6), 0, 0));
        this.contentPanel.setVisible(false);
        this.advancedPanel = new JPanel(new GridBagLayout());
        this.add((Component)this.advancedPanel, new GridBagConstraints(1, 2, 1, 1, 0.0, 0.0, 10, 1, new Insets(0, 6, 6, 6), 0, 0));
        this.advancedPanel.setVisible(false);
    }
}

