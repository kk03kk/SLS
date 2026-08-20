/*
 * Decompiled with CFR 0.152.
 */
package com.badlogic.gdx.tools.flame;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.FileTextureData;
import com.badlogic.gdx.tools.flame.CustomCardLayout;
import com.badlogic.gdx.tools.flame.TexturePanel;
import com.badlogic.gdx.utils.Array;
import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JButton;
import javax.swing.JPanel;

public class TextureAtlasPanel
extends JPanel {
    JPanel regionsPanel;
    TextureAtlas atlas;

    public TextureAtlasPanel() {
        this.initializeComponents();
    }

    private void initializeComponents() {
        this.setLayout(new GridBagLayout());
        JButton backwardButton = new JButton("<");
        this.add((Component)backwardButton, new GridBagConstraints(0, 0, 1, 1, 1.0, 1.0, 10, 2, new Insets(0, 0, 0, 0), 0, 0));
        this.regionsPanel = new JPanel();
        this.add((Component)this.regionsPanel, new GridBagConstraints(1, 0, 1, 1, 1.0, 1.0, 10, 2, new Insets(0, 0, 0, 0), 0, 0));
        JButton forwardButton = new JButton(">");
        this.add((Component)forwardButton, new GridBagConstraints(2, 0, 1, 1, 1.0, 1.0, 10, 2, new Insets(0, 0, 0, 0), 0, 0));
        this.regionsPanel.setLayout(new CustomCardLayout());
        backwardButton.addActionListener(new ActionListener(){

            @Override
            public void actionPerformed(ActionEvent arg0) {
                if (TextureAtlasPanel.this.atlas == null) {
                    return;
                }
                CustomCardLayout layout = (CustomCardLayout)TextureAtlasPanel.this.regionsPanel.getLayout();
                layout.previous(TextureAtlasPanel.this.regionsPanel);
            }
        });
        forwardButton.addActionListener(new ActionListener(){

            @Override
            public void actionPerformed(ActionEvent arg0) {
                if (TextureAtlasPanel.this.atlas == null) {
                    return;
                }
                CustomCardLayout layout = (CustomCardLayout)TextureAtlasPanel.this.regionsPanel.getLayout();
                layout.next(TextureAtlasPanel.this.regionsPanel);
            }
        });
    }

    public void setAtlas(TextureAtlas atlas) {
        if (atlas == this.atlas) {
            return;
        }
        this.regionsPanel.removeAll();
        Array<TextureAtlas.AtlasRegion> atlasRegions = atlas.getRegions();
        CustomCardLayout layout = (CustomCardLayout)this.regionsPanel.getLayout();
        Array<TextureRegion> regions = new Array<TextureRegion>();
        for (Texture texture : atlas.getTextures()) {
            FileTextureData file = (FileTextureData)texture.getTextureData();
            this.regionsPanel.add(new TexturePanel(texture, this.getRegions(texture, atlasRegions, regions)));
        }
        layout.first(this.regionsPanel);
        this.atlas = atlas;
    }

    protected Array<TextureRegion> getRegions(Texture texture, Array<TextureAtlas.AtlasRegion> atlasRegions, Array<TextureRegion> out) {
        out.clear();
        for (TextureRegion textureRegion : atlasRegions) {
            if (textureRegion.getTexture() != texture) continue;
            out.add(textureRegion);
        }
        return out;
    }

    public Array<TextureRegion> getSelectedRegions() {
        CustomCardLayout layout = (CustomCardLayout)this.regionsPanel.getLayout();
        TexturePanel panel = this.getCurrentRegionPanel();
        return panel.selectedRegions;
    }

    public TexturePanel getCurrentRegionPanel() {
        CustomCardLayout layout = (CustomCardLayout)this.regionsPanel.getLayout();
        return (TexturePanel)layout.getCurrentCard(this.regionsPanel);
    }

    public void clearSelection() {
        for (Component regionPanel : this.regionsPanel.getComponents()) {
            ((TexturePanel)regionPanel).clearSelection();
        }
    }
}

