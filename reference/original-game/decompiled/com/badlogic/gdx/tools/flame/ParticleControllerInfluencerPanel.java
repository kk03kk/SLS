/*
 * Decompiled with CFR 0.152.
 */
package com.badlogic.gdx.tools.flame;

import com.badlogic.gdx.graphics.g3d.particles.ParticleController;
import com.badlogic.gdx.graphics.g3d.particles.ParticleEffect;
import com.badlogic.gdx.graphics.g3d.particles.influencers.ParticleControllerInfluencer;
import com.badlogic.gdx.tools.flame.EventManager;
import com.badlogic.gdx.tools.flame.FlameMain;
import com.badlogic.gdx.tools.flame.InfluencerPanel;
import com.badlogic.gdx.tools.flame.LoaderButton;
import com.badlogic.gdx.tools.flame.TemplatePickerPanel;
import com.badlogic.gdx.utils.Array;
import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.Insets;

public class ParticleControllerInfluencerPanel
extends InfluencerPanel<ParticleControllerInfluencer>
implements TemplatePickerPanel.Listener<ParticleController>,
LoaderButton.Listener<ParticleEffect>,
EventManager.Listener {
    TemplatePickerPanel<ParticleController> controllerPicker;

    public ParticleControllerInfluencerPanel(FlameMain editor, ParticleControllerInfluencer influencer, boolean single, String name, String desc) {
        super(editor, influencer, name, desc, true, false);
        this.controllerPicker.setMultipleSelectionAllowed(!single);
        EventManager.get().attach(0, this);
    }

    @Override
    public void setValue(ParticleControllerInfluencer value) {
        super.setValue(value);
        if (value == null) {
            return;
        }
        this.controllerPicker.setValue(value.templates);
    }

    @Override
    protected void initializeComponents() {
        super.initializeComponents();
        this.controllerPicker = new TemplatePickerPanel<ParticleController>(this.editor, null, (TemplatePickerPanel.Listener)this, ParticleController.class){

            @Override
            protected String getTemplateName(ParticleController template, int index) {
                return template.name;
            }
        };
        this.reloadControllers();
        this.controllerPicker.setIsAlwayShown(true);
        this.contentPanel.add((Component)new LoaderButton.ParticleEffectLoaderButton(this.editor, this), new GridBagConstraints(0, 0, 1, 1, 1.0, 1.0, 10, 1, new Insets(0, 0, 0, 6), 0, 0));
        this.contentPanel.add(this.controllerPicker, new GridBagConstraints(0, 1, 1, 1, 1.0, 1.0, 10, 1, new Insets(0, 0, 0, 6), 0, 0));
    }

    @Override
    public void onTemplateChecked(ParticleController model, boolean isChecked) {
        this.editor.restart();
    }

    @Override
    public void onResourceLoaded(ParticleEffect resource) {
        this.reloadControllers();
    }

    private void reloadControllers() {
        Array effects = new Array();
        Array<ParticleController> controllers = new Array<ParticleController>();
        this.editor.assetManager.getAll(ParticleEffect.class, effects);
        for (ParticleEffect effect : effects) {
            controllers.addAll(effect.getControllers());
        }
        this.controllerPicker.setLoadedTemplates(controllers);
    }

    @Override
    public void handle(int aEventType, Object aEventData) {
        Object[] data;
        if (aEventType == 0 && (data = (Object[])aEventData)[0] instanceof ParticleEffect) {
            ParticleEffect oldEffect = (ParticleEffect)data[0];
            int currentCount = ((ParticleControllerInfluencer)this.value).templates.size;
            ((ParticleControllerInfluencer)this.value).templates.removeAll(oldEffect.getControllers(), true);
            if (((ParticleControllerInfluencer)this.value).templates.size != currentCount) {
                int diff = currentCount - ((ParticleControllerInfluencer)this.value).templates.size;
                if (diff > 0) {
                    ParticleEffect newEffect = (ParticleEffect)data[1];
                    Array<ParticleController> newControllers = newEffect.getControllers();
                    if (newControllers.size > 0) {
                        int c = Math.min(diff, newControllers.size);
                        for (int i = 0; i < c; ++i) {
                            ((ParticleControllerInfluencer)this.value).templates.add(newControllers.get(i));
                        }
                    }
                } else {
                    ((ParticleControllerInfluencer)this.value).templates.addAll(((ParticleEffect)this.editor.assetManager.get("pre_particle.png")).getControllers());
                }
                this.controllerPicker.reloadTemplates();
                this.controllerPicker.setValue(((ParticleControllerInfluencer)this.value).templates);
                this.editor.restart();
            }
        }
    }
}

