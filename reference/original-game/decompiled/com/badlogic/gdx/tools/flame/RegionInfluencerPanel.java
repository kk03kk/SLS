/*
 * Decompiled with CFR 0.152.
 */
package com.badlogic.gdx.tools.flame;

import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.g3d.particles.influencers.RegionInfluencer;
import com.badlogic.gdx.tools.flame.FlameMain;
import com.badlogic.gdx.tools.flame.InfluencerPanel;
import com.badlogic.gdx.tools.flame.RegionPickerPanel;
import com.badlogic.gdx.utils.Array;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;

public class RegionInfluencerPanel
extends InfluencerPanel<RegionInfluencer>
implements RegionPickerPanel.Listener {
    JDialog regionSelectDialog;
    RegionPickerPanel regionPickerPanel;

    public RegionInfluencerPanel(FlameMain editor, String name, String desc, RegionInfluencer influencer) {
        super(editor, influencer, name, desc);
        this.setValue(influencer);
    }

    @Override
    protected void initializeComponents() {
        super.initializeComponents();
        this.regionSelectDialog = new JDialog(this.editor, "Pick regions", true);
        this.regionPickerPanel = new RegionPickerPanel(this);
        JScrollPane scrollPane = new JScrollPane();
        scrollPane.setViewportView(this.regionPickerPanel);
        this.regionSelectDialog.setContentPane(scrollPane);
        this.regionSelectDialog.setDefaultCloseOperation(1);
        JButton pickButton = new JButton("Pick Regions");
        this.addContent(0, 0, pickButton, false, 17, 0);
        pickButton.addActionListener(new ActionListener(){

            @Override
            public void actionPerformed(ActionEvent arg0) {
                if (RegionInfluencerPanel.this.editor.isUsingDefaultTexture()) {
                    JOptionPane.showMessageDialog(RegionInfluencerPanel.this.editor, "Load a Texture or an Atlas first.");
                    return;
                }
                TextureAtlas atlas = RegionInfluencerPanel.this.editor.getAtlas();
                if (atlas != null) {
                    RegionInfluencerPanel.this.regionPickerPanel.setAtlas(atlas);
                } else {
                    RegionInfluencerPanel.this.regionPickerPanel.setTexture(RegionInfluencerPanel.this.editor.getTexture());
                }
                RegionInfluencerPanel.this.regionPickerPanel.revalidate();
                RegionInfluencerPanel.this.regionPickerPanel.repaint();
                RegionInfluencerPanel.this.regionSelectDialog.validate();
                RegionInfluencerPanel.this.regionSelectDialog.repaint();
                RegionInfluencerPanel.this.regionSelectDialog.pack();
                RegionInfluencerPanel.this.regionSelectDialog.setVisible(true);
            }
        });
    }

    @Override
    public void onRegionsSelected(Array<TextureRegion> regions) {
        this.regionSelectDialog.setVisible(false);
        if (regions.size == 0) {
            return;
        }
        ((RegionInfluencer)this.value).clear();
        ((RegionInfluencer)this.value).add((TextureRegion[])regions.toArray(TextureRegion.class));
        this.editor.setTexture(regions.get(0).getTexture());
        this.editor.restart();
    }
}

