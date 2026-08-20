/*
 * Decompiled with CFR 0.152.
 */
package com.badlogic.gdx.tools.flame;

import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.particles.influencers.ModelInfluencer;
import com.badlogic.gdx.tools.flame.EventManager;
import com.badlogic.gdx.tools.flame.FlameMain;
import com.badlogic.gdx.tools.flame.InfluencerPanel;
import com.badlogic.gdx.tools.flame.LoaderButton;
import com.badlogic.gdx.tools.flame.TemplatePickerPanel;
import java.awt.GridBagConstraints;
import java.awt.Insets;

public class ModelInfluencerPanel
extends InfluencerPanel<ModelInfluencer>
implements TemplatePickerPanel.Listener<Model>,
EventManager.Listener {
    TemplatePickerPanel<Model> pickerPanel;

    public ModelInfluencerPanel(FlameMain editor, ModelInfluencer influencer, boolean single, String name, String desc) {
        super(editor, influencer, name, desc, true, false);
        this.pickerPanel.setMultipleSelectionAllowed(!single);
        EventManager.get().attach(0, this);
    }

    @Override
    public void setValue(ModelInfluencer value) {
        super.setValue(value);
        if (value == null) {
            return;
        }
        this.pickerPanel.setValue(value.models);
    }

    @Override
    protected void initializeComponents() {
        super.initializeComponents();
        this.pickerPanel = new TemplatePickerPanel<Model>(this.editor, null, this, Model.class, new LoaderButton.ModelLoaderButton(this.editor));
        this.pickerPanel.setIsAlwayShown(true);
        this.contentPanel.add(this.pickerPanel, new GridBagConstraints(0, 0, 1, 1, 1.0, 1.0, 10, 1, new Insets(0, 0, 0, 6), 0, 0));
    }

    @Override
    public void onTemplateChecked(Model model, boolean isChecked) {
        this.editor.restart();
    }

    @Override
    public void handle(int aEventType, Object aEventData) {
        Object[] data;
        if (aEventType == 0 && (data = (Object[])aEventData)[0] instanceof Model && ((ModelInfluencer)this.value).models.removeValue((Model)data[0], true)) {
            ((ModelInfluencer)this.value).models.add((Model)data[1]);
            this.pickerPanel.reloadTemplates();
            this.pickerPanel.setValue(((ModelInfluencer)this.value).models);
            this.editor.restart();
        }
    }
}

