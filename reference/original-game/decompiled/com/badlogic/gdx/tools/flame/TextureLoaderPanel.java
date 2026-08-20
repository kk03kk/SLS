/*
 * Decompiled with CFR 0.152.
 */
package com.badlogic.gdx.tools.flame;

import com.badlogic.gdx.assets.loaders.TextureLoader;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.tools.flame.EditorPanel;
import com.badlogic.gdx.tools.flame.FlameMain;
import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;

public class TextureLoaderPanel
extends EditorPanel {
    public TextureLoaderPanel(FlameMain editor, String name, String description) {
        super(editor, name, description);
        this.setValue(null);
    }

    @Override
    protected void initializeComponents() {
        super.initializeComponents();
        JButton atlasButton = new JButton("Open Atlas");
        JButton textureButton = new JButton("Open Texture");
        JButton defaultTextureButton = new JButton("Default Texture");
        final JCheckBox genMipMaps = new JCheckBox("Generate MipMaps");
        final JComboBox<Texture.TextureFilter> minFilterBox = new JComboBox<Texture.TextureFilter>(new DefaultComboBoxModel<Texture.TextureFilter>(Texture.TextureFilter.values()));
        final JComboBox<Texture.TextureFilter> magFilterBox = new JComboBox<Texture.TextureFilter>(new DefaultComboBoxModel<Texture.TextureFilter>(Texture.TextureFilter.values()));
        minFilterBox.setSelectedItem((Object)this.editor.getTexture().getMinFilter());
        magFilterBox.setSelectedItem((Object)this.editor.getTexture().getMagFilter());
        ActionListener filterListener = new ActionListener(){

            @Override
            public void actionPerformed(ActionEvent event) {
                TextureLoaderPanel.this.editor.getTexture().setFilter((Texture.TextureFilter)((Object)minFilterBox.getSelectedItem()), (Texture.TextureFilter)((Object)magFilterBox.getSelectedItem()));
            }
        };
        minFilterBox.addActionListener(filterListener);
        magFilterBox.addActionListener(filterListener);
        atlasButton.addActionListener(new ActionListener(){

            @Override
            public void actionPerformed(ActionEvent e) {
                TextureAtlas atlas;
                File file = TextureLoaderPanel.this.editor.showFileLoadDialog();
                if (file != null && (atlas = TextureLoaderPanel.this.editor.load(file.getAbsolutePath(), TextureAtlas.class, null, null)) != null) {
                    TextureLoaderPanel.this.editor.setAtlas(atlas);
                }
            }
        });
        textureButton.addActionListener(new ActionListener(){

            @Override
            public void actionPerformed(ActionEvent e) {
                File file = TextureLoaderPanel.this.editor.showFileLoadDialog();
                if (file != null) {
                    TextureLoader.TextureParameter params = new TextureLoader.TextureParameter();
                    params.genMipMaps = genMipMaps.isSelected();
                    params.minFilter = (Texture.TextureFilter)((Object)minFilterBox.getSelectedItem());
                    params.magFilter = (Texture.TextureFilter)((Object)magFilterBox.getSelectedItem());
                    Texture texture = TextureLoaderPanel.this.editor.load(file.getAbsolutePath(), Texture.class, null, params);
                    if (texture != null) {
                        TextureLoaderPanel.this.editor.setTexture(texture);
                    }
                }
            }
        });
        defaultTextureButton.addActionListener(new ActionListener(){

            @Override
            public void actionPerformed(ActionEvent e) {
                TextureLoaderPanel.this.editor.setTexture(TextureLoaderPanel.this.editor.assetManager.get("pre_particle.png", Texture.class));
            }
        });
        this.contentPanel.add((Component)new JLabel("Min. Filter"), new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0, 17, 0, new Insets(6, 0, 0, 0), 0, 0));
        this.contentPanel.add(minFilterBox, new GridBagConstraints(1, 0, 1, 1, 1.0, 0.0, 17, 0, new Insets(6, 0, 0, 0), 0, 0));
        this.contentPanel.add((Component)new JLabel("Mag. Filter"), new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0, 17, 0, new Insets(6, 0, 0, 0), 0, 0));
        this.contentPanel.add(magFilterBox, new GridBagConstraints(1, 1, 1, 1, 1.0, 0.0, 17, 0, new Insets(6, 0, 0, 0), 0, 0));
        this.contentPanel.add((Component)genMipMaps, new GridBagConstraints(0, 2, 1, 1, 0.0, 0.0, 17, 0, new Insets(6, 0, 0, 0), 0, 0));
        this.contentPanel.add((Component)atlasButton, new GridBagConstraints(0, 3, 1, 1, 0.0, 0.0, 17, 0, new Insets(6, 0, 0, 0), 0, 0));
        this.contentPanel.add((Component)textureButton, new GridBagConstraints(1, 3, 1, 1, 1.0, 0.0, 17, 0, new Insets(6, 0, 0, 0), 0, 0));
        this.contentPanel.add((Component)defaultTextureButton, new GridBagConstraints(2, 3, 1, 1, 1.0, 0.0, 17, 0, new Insets(6, 0, 0, 0), 0, 0));
    }
}

