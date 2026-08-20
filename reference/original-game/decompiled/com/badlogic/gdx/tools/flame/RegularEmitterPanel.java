/*
 * Decompiled with CFR 0.152.
 */
package com.badlogic.gdx.tools.flame;

import com.badlogic.gdx.graphics.g3d.particles.emitters.RegularEmitter;
import com.badlogic.gdx.tools.flame.CountPanel;
import com.badlogic.gdx.tools.flame.EditorPanel;
import com.badlogic.gdx.tools.flame.FlameMain;
import com.badlogic.gdx.tools.flame.RangedNumericPanel;
import com.badlogic.gdx.tools.flame.ScaledNumericPanel;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JCheckBox;

public class RegularEmitterPanel
extends EditorPanel<RegularEmitter> {
    CountPanel countPanel;
    RangedNumericPanel delayPanel;
    RangedNumericPanel durationPanel;
    ScaledNumericPanel emissionPanel;
    ScaledNumericPanel lifePanel;
    ScaledNumericPanel lifeOffsetPanel;
    JCheckBox continuousCheckbox;

    public RegularEmitterPanel(FlameMain particleEditor3D, RegularEmitter emitter) {
        super(particleEditor3D, "Regular Emitter", "This is a generic emitter used to generate particles regularly.");
        this.initializeComponents(emitter);
        this.setValue(null);
    }

    private void initializeComponents(RegularEmitter emitter) {
        this.continuousCheckbox = new JCheckBox("Continuous");
        this.continuousCheckbox.setSelected(emitter.isContinuous());
        this.continuousCheckbox.addActionListener(new ActionListener(){

            @Override
            public void actionPerformed(ActionEvent event) {
                RegularEmitter emitter = (RegularEmitter)RegularEmitterPanel.this.editor.getEmitter().emitter;
                emitter.setContinuous(RegularEmitterPanel.this.continuousCheckbox.isSelected());
            }
        });
        this.continuousCheckbox.setHorizontalTextPosition(2);
        int i = 0;
        this.addContent(i++, 0, this.continuousCheckbox, 17, 0);
        int n = i++;
        this.countPanel = new CountPanel(this.editor, "Count", "Min number of particles at all times, max number of particles allowed.", emitter.minParticleCount, emitter.maxParticleCount);
        this.addContent(n, 0, this.countPanel);
        int n2 = i++;
        this.delayPanel = new RangedNumericPanel(this.editor, emitter.getDelay(), "Delay", "Time from beginning of effect to emission start, in milliseconds.", false);
        this.addContent(n2, 0, this.delayPanel);
        int n3 = i++;
        this.durationPanel = new RangedNumericPanel(this.editor, emitter.getDuration(), "Duration", "Time particles will be emitted, in milliseconds.");
        this.addContent(n3, 0, this.durationPanel);
        int n4 = i++;
        this.emissionPanel = new ScaledNumericPanel(this.editor, emitter.getEmission(), "Duration", "Emission", "Number of particles emitted per second.");
        this.addContent(n4, 0, this.emissionPanel);
        int n5 = i++;
        this.lifePanel = new ScaledNumericPanel(this.editor, emitter.getLife(), "Duration", "Life", "Time particles will live, in milliseconds.");
        this.addContent(n5, 0, this.lifePanel);
        int n6 = i++;
        this.lifeOffsetPanel = new ScaledNumericPanel(this.editor, emitter.getLifeOffset(), "Duration", "Life Offset", "Particle starting life consumed, in milliseconds.", false);
        this.addContent(n6, 0, this.lifeOffsetPanel);
    }
}

