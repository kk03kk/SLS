/*
 * Decompiled with CFR 0.152.
 */
package com.badlogic.gdx.tools.flame;

import com.badlogic.gdx.graphics.g3d.particles.influencers.DynamicsModifier;
import com.badlogic.gdx.tools.flame.EditorPanel;
import com.badlogic.gdx.tools.flame.FlameMain;
import com.badlogic.gdx.tools.flame.ScaledNumericPanel;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

public class StrengthVelocityPanel
extends EditorPanel<DynamicsModifier.Strength> {
    JCheckBox isGlobalCheckBox;
    ScaledNumericPanel magnitudePanel;

    public StrengthVelocityPanel(FlameMain editor, DynamicsModifier.Strength value, String charTitle, String name, String description) {
        super(editor, name, description);
        this.initializeComponents(charTitle);
        this.setValue(value);
    }

    @Override
    public void setValue(DynamicsModifier.Strength value) {
        super.setValue(value);
        if (value == null) {
            return;
        }
        StrengthVelocityPanel.setValue(this.isGlobalCheckBox, ((DynamicsModifier.Strength)this.value).isGlobal);
        this.magnitudePanel.setValue(value.strengthValue);
    }

    private void initializeComponents(String charTitle) {
        JPanel contentPanel = this.getContentPanel();
        JPanel panel = new JPanel();
        panel.add((Component)new JLabel("Global"), new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0, 17, 0, new Insets(0, 0, 0, 0), 0, 0));
        this.isGlobalCheckBox = new JCheckBox();
        panel.add((Component)this.isGlobalCheckBox, new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0, 17, 0, new Insets(0, 0, 0, 0), 0, 0));
        contentPanel.add((Component)panel, new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0, 17, 0, new Insets(0, 0, 0, 0), 0, 0));
        this.magnitudePanel = new ScaledNumericPanel(this.editor, null, charTitle, "Strength", "In world units per second.", true);
        contentPanel.add((Component)this.magnitudePanel, new GridBagConstraints(0, 2, 1, 1, 0.0, 0.0, 17, 0, new Insets(0, 0, 0, 6), 0, 0));
        JPanel spacer = new JPanel();
        spacer.setPreferredSize(new Dimension());
        contentPanel.add((Component)spacer, new GridBagConstraints(6, 0, 1, 1, 1.0, 0.0, 17, 0, new Insets(0, 0, 0, 0), 0, 0));
        this.magnitudePanel.setIsAlwayShown(true);
        this.isGlobalCheckBox.addActionListener(new ActionListener(){

            @Override
            public void actionPerformed(ActionEvent e) {
                ((DynamicsModifier.Strength)StrengthVelocityPanel.this.value).isGlobal = StrengthVelocityPanel.this.isGlobalCheckBox.isSelected();
            }
        });
    }
}

