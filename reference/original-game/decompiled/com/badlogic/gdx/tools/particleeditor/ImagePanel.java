/*
 * Decompiled with CFR 0.152.
 */
package com.badlogic.gdx.tools.particleeditor;

import com.badlogic.gdx.graphics.g2d.ParticleEmitter;
import com.badlogic.gdx.tools.particleeditor.EditorPanel;
import com.badlogic.gdx.tools.particleeditor.ParticleEditor;
import java.awt.Component;
import java.awt.FileDialog;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;

class ImagePanel
extends EditorPanel {
    JLabel imageLabel;
    JLabel widthLabel;
    JLabel heightLabel;
    String lastDir;

    public ImagePanel(final ParticleEditor editor, String name, String description) {
        super(null, name, description);
        JPanel contentPanel = this.getContentPanel();
        JButton openButton = new JButton("Open");
        contentPanel.add((Component)openButton, new GridBagConstraints(2, 1, 1, 1, 0.0, 0.0, 18, 0, new Insets(0, 0, 0, 6), 0, 0));
        openButton.addActionListener(new ActionListener(){

            @Override
            public void actionPerformed(ActionEvent event) {
                FileDialog dialog = new FileDialog((Frame)editor, "Open Image", 0);
                if (ImagePanel.this.lastDir != null) {
                    dialog.setDirectory(ImagePanel.this.lastDir);
                }
                dialog.setVisible(true);
                String file = dialog.getFile();
                String dir = dialog.getDirectory();
                if (dir == null || file == null || file.trim().length() == 0) {
                    return;
                }
                ImagePanel.this.lastDir = dir;
                try {
                    ImageIcon icon = new ImageIcon(new File(dir, file).toURI().toURL());
                    ParticleEmitter emitter = editor.getEmitter();
                    editor.setIcon(emitter, icon);
                    ImagePanel.this.updateIconInfo(icon);
                    emitter.setImagePath(new File(dir, file).getAbsolutePath());
                    emitter.setSprite(null);
                }
                catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        });
        JButton defaultButton = new JButton("Default");
        contentPanel.add((Component)defaultButton, new GridBagConstraints(2, 2, 1, 1, 0.0, 0.0, 18, 0, new Insets(0, 0, 0, 6), 0, 0));
        defaultButton.addActionListener(new ActionListener(){

            @Override
            public void actionPerformed(ActionEvent e) {
                ParticleEmitter emitter = editor.getEmitter();
                emitter.setImagePath("particle.png");
                emitter.setSprite(null);
                editor.setIcon(emitter, null);
                ImagePanel.this.updateIconInfo(null);
            }
        });
        JButton defaultPremultButton = new JButton("Default (Premultiplied Alpha)");
        contentPanel.add((Component)defaultPremultButton, new GridBagConstraints(3, 2, 1, 1, 0.0, 0.0, 18, 0, new Insets(0, 0, 0, 6), 0, 0));
        defaultPremultButton.addActionListener(new ActionListener(){

            @Override
            public void actionPerformed(ActionEvent e) {
                ParticleEmitter emitter = editor.getEmitter();
                emitter.setImagePath("pre_particle.png");
                emitter.setSprite(null);
                editor.setIcon(emitter, null);
                ImagePanel.this.updateIconInfo(null);
            }
        });
        this.widthLabel = new JLabel();
        contentPanel.add((Component)this.widthLabel, new GridBagConstraints(2, 3, 1, 1, 0.0, 0.0, 18, 0, new Insets(0, 0, 6, 6), 0, 0));
        this.heightLabel = new JLabel();
        contentPanel.add((Component)this.heightLabel, new GridBagConstraints(2, 4, 1, 1, 0.0, 1.0, 18, 0, new Insets(0, 0, 0, 6), 0, 0));
        this.imageLabel = new JLabel();
        contentPanel.add((Component)this.imageLabel, new GridBagConstraints(3, 1, 1, 3, 1.0, 0.0, 18, 0, new Insets(0, 6, 0, 0), 0, 0));
        this.updateIconInfo(editor.getIcon(editor.getEmitter()));
    }

    public void updateIconInfo(ImageIcon icon) {
        if (icon != null) {
            this.imageLabel.setIcon(icon);
            this.widthLabel.setText("Width: " + icon.getIconWidth());
            this.heightLabel.setText("Height: " + icon.getIconHeight());
        } else {
            this.imageLabel.setIcon(null);
            this.widthLabel.setText("");
            this.heightLabel.setText("");
        }
        this.revalidate();
    }
}

