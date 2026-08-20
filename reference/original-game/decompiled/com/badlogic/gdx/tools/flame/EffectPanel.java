/*
 * Decompiled with CFR 0.152.
 */
package com.badlogic.gdx.tools.flame;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.particles.ParticleController;
import com.badlogic.gdx.graphics.g3d.particles.ParticleEffect;
import com.badlogic.gdx.graphics.g3d.particles.emitters.RegularEmitter;
import com.badlogic.gdx.graphics.g3d.particles.influencers.ColorInfluencer;
import com.badlogic.gdx.graphics.g3d.particles.influencers.DynamicsInfluencer;
import com.badlogic.gdx.graphics.g3d.particles.influencers.DynamicsModifier;
import com.badlogic.gdx.graphics.g3d.particles.influencers.ModelInfluencer;
import com.badlogic.gdx.graphics.g3d.particles.influencers.ParticleControllerFinalizerInfluencer;
import com.badlogic.gdx.graphics.g3d.particles.influencers.ParticleControllerInfluencer;
import com.badlogic.gdx.graphics.g3d.particles.influencers.RegionInfluencer;
import com.badlogic.gdx.graphics.g3d.particles.influencers.ScaleInfluencer;
import com.badlogic.gdx.graphics.g3d.particles.influencers.SpawnInfluencer;
import com.badlogic.gdx.graphics.g3d.particles.renderers.BillboardRenderer;
import com.badlogic.gdx.graphics.g3d.particles.renderers.ModelInstanceRenderer;
import com.badlogic.gdx.graphics.g3d.particles.renderers.ParticleControllerControllerRenderer;
import com.badlogic.gdx.graphics.g3d.particles.renderers.PointSpriteRenderer;
import com.badlogic.gdx.graphics.g3d.particles.values.EllipseSpawnShapeValue;
import com.badlogic.gdx.graphics.g3d.particles.values.PointSpawnShapeValue;
import com.badlogic.gdx.graphics.g3d.particles.values.PrimitiveSpawnShapeValue;
import com.badlogic.gdx.tools.flame.FlameMain;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTable;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.DefaultTableModel;

class EffectPanel
extends JPanel {
    FlameMain editor;
    JTable emitterTable;
    DefaultTableModel emitterTableModel;
    int editIndex = -1;
    String lastDir;
    JComboBox controllerTypeCombo;

    public EffectPanel(FlameMain editor) {
        this.editor = editor;
        this.initializeComponents();
    }

    public <T extends ParticleController> T createDefaultEmitter(FlameMain.ControllerType type, boolean select, boolean add) {
        ParticleController controller = null;
        if (type == FlameMain.ControllerType.Billboard) {
            controller = this.createDefaultBillboardController();
        } else if (type == FlameMain.ControllerType.ModelInstance) {
            controller = this.createDefaultModelInstanceController();
        } else if (type == FlameMain.ControllerType.PointSprite) {
            controller = this.createDefaultPointController();
        } else if (type == FlameMain.ControllerType.ParticleController) {
            controller = this.createDefaultParticleController();
        }
        if (add) {
            controller.init();
            this.addEmitter(controller, select);
        }
        return (T)controller;
    }

    private ParticleController createDefaultModelInstanceController() {
        RegularEmitter emitter = new RegularEmitter();
        emitter.getDuration().setLow(3000.0f);
        emitter.getEmission().setHigh(80.0f);
        emitter.getLife().setHigh(500.0f, 1000.0f);
        emitter.getLife().setTimeline(new float[]{0.0f, 0.66f, 1.0f});
        emitter.getLife().setScaling(new float[]{1.0f, 1.0f, 0.3f});
        emitter.setMaxParticleCount(100);
        ColorInfluencer.Random colorInfluencer = new ColorInfluencer.Random();
        EllipseSpawnShapeValue spawnShapeValue = new EllipseSpawnShapeValue();
        spawnShapeValue.setDimensions(1.0f, 1.0f, 1.0f);
        SpawnInfluencer spawnSource = new SpawnInfluencer(spawnShapeValue);
        DynamicsInfluencer velocityInfluencer = new DynamicsInfluencer();
        DynamicsModifier.CentripetalAcceleration velocityValue = new DynamicsModifier.CentripetalAcceleration();
        velocityValue.strengthValue.setHigh(5.0f, 11.0f);
        velocityValue.strengthValue.setActive(true);
        velocityInfluencer.velocities.add(velocityValue);
        return new ParticleController("ModelInstance Controller", emitter, new ModelInstanceRenderer(this.editor.getModelInstanceParticleBatch()), new ModelInfluencer.Single((Model)this.editor.assetManager.get("monkey.g3db")), spawnSource, colorInfluencer, velocityInfluencer);
    }

    private ParticleController createDefaultBillboardController() {
        RegularEmitter emitter = new RegularEmitter();
        emitter.getDuration().setLow(3000.0f);
        emitter.getEmission().setHigh(250.0f);
        emitter.getLife().setHigh(500.0f, 1000.0f);
        emitter.getLife().setTimeline(new float[]{0.0f, 0.66f, 1.0f});
        emitter.getLife().setScaling(new float[]{1.0f, 1.0f, 0.3f});
        emitter.setMaxParticleCount(200);
        PointSpawnShapeValue pointSpawnShapeValue = new PointSpawnShapeValue();
        SpawnInfluencer spawnSource = new SpawnInfluencer(pointSpawnShapeValue);
        ColorInfluencer.Single colorInfluencer = new ColorInfluencer.Single();
        colorInfluencer.colorValue.setColors(new float[]{1.0f, 0.12156863f, 0.047058824f, 0.0f, 0.0f, 0.0f});
        colorInfluencer.colorValue.setTimeline(new float[]{0.0f, 1.0f});
        colorInfluencer.alphaValue.setHigh(1.0f);
        colorInfluencer.alphaValue.setTimeline(new float[]{0.0f, 0.5f, 0.8f, 1.0f});
        colorInfluencer.alphaValue.setScaling(new float[]{0.0f, 0.15f, 0.5f, 0.0f});
        DynamicsInfluencer velocityInfluencer = new DynamicsInfluencer();
        DynamicsModifier.PolarAcceleration velocityValue = new DynamicsModifier.PolarAcceleration();
        velocityValue.phiValue.setHigh(-35.0f, 35.0f);
        velocityValue.phiValue.setActive(true);
        velocityValue.phiValue.setTimeline(new float[]{0.0f, 0.5f, 1.0f});
        velocityValue.phiValue.setScaling(new float[]{1.0f, 0.0f, 0.0f});
        velocityValue.thetaValue.setHigh(0.0f, 360.0f);
        velocityValue.strengthValue.setHigh(5.0f, 10.0f);
        velocityInfluencer.velocities.add(velocityValue);
        return new ParticleController("Billboard Controller", emitter, new BillboardRenderer(this.editor.getBillboardBatch()), new RegionInfluencer.Single(this.editor.getTexture()), spawnSource, colorInfluencer, velocityInfluencer);
    }

    private ParticleController createDefaultPointController() {
        RegularEmitter emitter = new RegularEmitter();
        emitter.getDuration().setLow(3000.0f);
        emitter.getEmission().setHigh(250.0f);
        emitter.getLife().setHigh(500.0f, 1000.0f);
        emitter.getLife().setTimeline(new float[]{0.0f, 0.66f, 1.0f});
        emitter.getLife().setScaling(new float[]{1.0f, 1.0f, 0.3f});
        emitter.setMaxParticleCount(200);
        ScaleInfluencer scaleInfluencer = new ScaleInfluencer();
        scaleInfluencer.value.setHigh(1.0f);
        ColorInfluencer.Single colorInfluencer = new ColorInfluencer.Single();
        colorInfluencer.colorValue.setColors(new float[]{0.12156863f, 0.047058824f, 1.0f, 0.0f, 0.0f, 0.0f});
        colorInfluencer.colorValue.setTimeline(new float[]{0.0f, 1.0f});
        colorInfluencer.alphaValue.setHigh(1.0f);
        colorInfluencer.alphaValue.setTimeline(new float[]{0.0f, 0.5f, 0.8f, 1.0f});
        colorInfluencer.alphaValue.setScaling(new float[]{0.0f, 0.15f, 0.5f, 0.0f});
        PointSpawnShapeValue pointSpawnShapeValue = new PointSpawnShapeValue();
        SpawnInfluencer spawnSource = new SpawnInfluencer(pointSpawnShapeValue);
        DynamicsInfluencer velocityInfluencer = new DynamicsInfluencer();
        DynamicsModifier.PolarAcceleration velocityValue = new DynamicsModifier.PolarAcceleration();
        velocityValue.phiValue.setHigh(-35.0f, 35.0f);
        velocityValue.phiValue.setActive(true);
        velocityValue.phiValue.setTimeline(new float[]{0.0f, 0.5f, 1.0f});
        velocityValue.phiValue.setScaling(new float[]{1.0f, 0.0f, 0.0f});
        velocityValue.thetaValue.setHigh(0.0f, 360.0f);
        velocityValue.strengthValue.setHigh(5.0f, 10.0f);
        return new ParticleController("PointSprite Controller", emitter, new PointSpriteRenderer(this.editor.getPointSpriteBatch()), new RegionInfluencer.Single((Texture)this.editor.assetManager.get("pre_particle.png")), spawnSource, scaleInfluencer, colorInfluencer, velocityInfluencer);
    }

    private ParticleController createDefaultParticleController() {
        RegularEmitter emitter = new RegularEmitter();
        emitter.getDuration().setLow(3000.0f);
        emitter.getEmission().setHigh(90.0f);
        emitter.getLife().setHigh(3000.0f);
        emitter.setMaxParticleCount(100);
        EllipseSpawnShapeValue pointSpawnShapeValue = new EllipseSpawnShapeValue();
        pointSpawnShapeValue.setDimensions(1.0f, 1.0f, 1.0f);
        pointSpawnShapeValue.setSide(PrimitiveSpawnShapeValue.SpawnSide.top);
        SpawnInfluencer spawnSource = new SpawnInfluencer(pointSpawnShapeValue);
        ScaleInfluencer scaleInfluencer = new ScaleInfluencer();
        scaleInfluencer.value.setHigh(1.0f);
        scaleInfluencer.value.setLow(0.0f);
        scaleInfluencer.value.setTimeline(new float[]{0.0f, 1.0f});
        scaleInfluencer.value.setScaling(new float[]{1.0f, 0.0f});
        DynamicsInfluencer velocityInfluencer = new DynamicsInfluencer();
        DynamicsModifier.CentripetalAcceleration velocityValue = new DynamicsModifier.CentripetalAcceleration();
        velocityValue.strengthValue.setHigh(5.0f, 10.0f);
        velocityValue.strengthValue.setActive(true);
        velocityInfluencer.velocities.add(velocityValue);
        return new ParticleController("ParticleController Controller", emitter, new ParticleControllerControllerRenderer(), new ParticleControllerInfluencer.Single(this.editor.assetManager.get("defaultTemplate.pfx", ParticleEffect.class).getControllers().get(0)), spawnSource, scaleInfluencer, velocityInfluencer, new ParticleControllerFinalizerInfluencer());
    }

    public ParticleController createDefaultTemplateController() {
        RegularEmitter emitter = new RegularEmitter();
        emitter.getDuration().setLow(3000.0f);
        emitter.getEmission().setHigh(90.0f);
        emitter.getLife().setHigh(1000.0f);
        emitter.getLife().setTimeline(new float[]{0.0f, 0.66f, 1.0f});
        emitter.getLife().setScaling(new float[]{1.0f, 1.0f, 0.3f});
        emitter.setMaxParticleCount(100);
        PointSpawnShapeValue pointSpawnShapeValue = new PointSpawnShapeValue();
        pointSpawnShapeValue.xOffsetValue.setLow(0.0f, 1.0f);
        pointSpawnShapeValue.xOffsetValue.setActive(true);
        pointSpawnShapeValue.yOffsetValue.setLow(0.0f, 1.0f);
        pointSpawnShapeValue.yOffsetValue.setActive(true);
        pointSpawnShapeValue.zOffsetValue.setLow(0.0f, 1.0f);
        pointSpawnShapeValue.zOffsetValue.setActive(true);
        SpawnInfluencer spawnSource = new SpawnInfluencer(pointSpawnShapeValue);
        ScaleInfluencer scaleInfluencer = new ScaleInfluencer();
        scaleInfluencer.value.setHigh(1.0f);
        ColorInfluencer.Single colorInfluencer = new ColorInfluencer.Single();
        colorInfluencer.colorValue.setColors(new float[]{1.0f, 0.12156863f, 0.047058824f, 0.0f, 0.0f, 0.0f});
        colorInfluencer.colorValue.setTimeline(new float[]{0.0f, 1.0f});
        colorInfluencer.alphaValue.setHigh(1.0f);
        colorInfluencer.alphaValue.setTimeline(new float[]{0.0f, 0.5f, 0.8f, 1.0f});
        colorInfluencer.alphaValue.setScaling(new float[]{0.0f, 0.15f, 0.5f, 0.0f});
        return new ParticleController("Billboard Controller", emitter, new BillboardRenderer(this.editor.getBillboardBatch()), new RegionInfluencer.Single(this.editor.getTexture()), spawnSource, scaleInfluencer, colorInfluencer);
    }

    private void addEmitter(ParticleController emitter, boolean select) {
        this.editor.addEmitter(emitter);
        this.emitterTableModel.addRow(new Object[]{emitter.name, true});
        int row = this.emitterTableModel.getRowCount() - 1;
        this.emitterChecked(row, true);
        if (select) {
            this.emitterTable.getSelectionModel().setSelectionInterval(row, row);
        }
    }

    void emitterSelected() {
        int row = this.emitterTable.getSelectedRow();
        if (row == this.editIndex) {
            return;
        }
        this.editIndex = row;
        this.editor.reloadRows();
    }

    void emitterChecked(int index, boolean checked) {
        this.editor.setEnabled(index, checked);
    }

    void openEffect() {
        File file = this.editor.showFileLoadDialog();
        if (file != null && this.editor.openEffect(file, true) != null) {
            this.emitterTableModel.getDataVector().removeAllElements();
            for (FlameMain.ControllerData data : this.editor.controllersData) {
                this.emitterTableModel.addRow(new Object[]{data.controller.name, true});
            }
            this.editIndex = 0;
            this.emitterTable.getSelectionModel().setSelectionInterval(this.editIndex, this.editIndex);
        }
    }

    protected void importEffect() {
        ParticleEffect effect;
        File file = this.editor.showFileLoadDialog();
        if (file != null && (effect = this.editor.openEffect(file, false)) != null) {
            for (ParticleController controller : effect.getControllers()) {
                this.addEmitter(controller, false);
            }
            this.editIndex = 0;
            this.emitterTable.getSelectionModel().setSelectionInterval(this.editIndex, this.editIndex);
        }
    }

    void saveEffect() {
        File file = this.editor.showFileSaveDialog();
        if (file != null) {
            int index = 0;
            for (FlameMain.ControllerData data : this.editor.controllersData) {
                data.controller.name = (String)this.emitterTableModel.getValueAt(index++, 0);
            }
            this.editor.saveEffect(file);
        }
    }

    void deleteEmitter() {
        int row = this.emitterTable.getSelectedRow();
        if (row == -1) {
            return;
        }
        int newIndex = Math.min(this.editIndex, this.emitterTableModel.getRowCount() - 2);
        this.editor.removeEmitter(row);
        this.emitterTableModel.removeRow(row);
        this.emitterTable.getSelectionModel().setSelectionInterval(newIndex, newIndex);
    }

    protected void cloneEmitter() {
        int row = this.emitterTable.getSelectedRow();
        if (row == -1) {
            return;
        }
        ParticleController controller = this.editor.controllersData.get((int)row).controller.copy();
        controller.init();
        controller.name = controller.name + " Clone";
        this.addEmitter(controller, true);
    }

    void move(int direction) {
    }

    private void initializeComponents() {
        this.setLayout(new GridBagLayout());
        JScrollPane scroll = new JScrollPane();
        this.add((Component)scroll, new GridBagConstraints(0, 0, 1, 1, 1.0, 0.0, 10, 1, new Insets(0, 0, 0, 6), 0, 0));
        this.emitterTable = new JTable(){

            public Class getColumnClass(int column) {
                return column == 1 ? Boolean.class : super.getColumnClass(column);
            }

            @Override
            public Dimension getPreferredScrollableViewportSize() {
                Dimension dim = super.getPreferredScrollableViewportSize();
                dim.height = this.getPreferredSize().height;
                return dim;
            }
        };
        this.emitterTable.getTableHeader().setReorderingAllowed(false);
        this.emitterTable.setSelectionMode(0);
        scroll.setViewportView(this.emitterTable);
        this.emitterTableModel = new DefaultTableModel(new String[0][0], new String[]{"Emitter", ""});
        this.emitterTable.setModel(this.emitterTableModel);
        this.emitterTable.getSelectionModel().addListSelectionListener(new ListSelectionListener(){

            @Override
            public void valueChanged(ListSelectionEvent event) {
                if (event.getValueIsAdjusting()) {
                    return;
                }
                EffectPanel.this.emitterSelected();
            }
        });
        this.emitterTableModel.addTableModelListener(new TableModelListener(){

            @Override
            public void tableChanged(TableModelEvent event) {
                if (event.getColumn() != 1) {
                    return;
                }
                EffectPanel.this.emitterChecked(event.getFirstRow(), (Boolean)EffectPanel.this.emitterTable.getValueAt(event.getFirstRow(), 1));
            }
        });
        JPanel sideButtons = new JPanel(new GridBagLayout());
        this.add((Component)sideButtons, new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0, 18, 1, new Insets(0, 0, 0, 0), 0, 0));
        this.controllerTypeCombo = new JComboBox();
        this.controllerTypeCombo.setModel(new DefaultComboBoxModel<FlameMain.ControllerType>(FlameMain.ControllerType.values()));
        sideButtons.add((Component)this.controllerTypeCombo, new GridBagConstraints(0, -1, 1, 1, 0.0, 0.0, 10, 2, new Insets(0, 0, 6, 0), 0, 0));
        JButton newButton = new JButton("New");
        sideButtons.add((Component)newButton, new GridBagConstraints(0, -1, 1, 1, 0.0, 0.0, 10, 2, new Insets(0, 0, 6, 0), 0, 0));
        newButton.addActionListener(new ActionListener(){

            @Override
            public void actionPerformed(ActionEvent event) {
                FlameMain.ControllerType item = (FlameMain.ControllerType)((Object)EffectPanel.this.controllerTypeCombo.getSelectedItem());
                EffectPanel.this.createDefaultEmitter(item, true, true);
            }
        });
        JButton deleteButton = new JButton("Delete");
        sideButtons.add((Component)deleteButton, new GridBagConstraints(0, -1, 1, 1, 0.0, 0.0, 10, 2, new Insets(0, 0, 6, 0), 0, 0));
        deleteButton.addActionListener(new ActionListener(){

            @Override
            public void actionPerformed(ActionEvent event) {
                EffectPanel.this.deleteEmitter();
            }
        });
        JButton cloneButton = new JButton("Clone");
        sideButtons.add((Component)cloneButton, new GridBagConstraints(0, -1, 1, 1, 0.0, 0.0, 10, 2, new Insets(0, 0, 6, 0), 0, 0));
        cloneButton.addActionListener(new ActionListener(){

            @Override
            public void actionPerformed(ActionEvent event) {
                EffectPanel.this.cloneEmitter();
            }
        });
        sideButtons.add((Component)new JSeparator(0), new GridBagConstraints(0, -1, 1, 1, 0.0, 0.0, 10, 2, new Insets(0, 0, 6, 0), 0, 0));
        JButton saveButton = new JButton("Save");
        sideButtons.add((Component)saveButton, new GridBagConstraints(0, -1, 1, 1, 0.0, 0.0, 10, 2, new Insets(0, 0, 6, 0), 0, 0));
        saveButton.addActionListener(new ActionListener(){

            @Override
            public void actionPerformed(ActionEvent event) {
                EffectPanel.this.saveEffect();
            }
        });
        JButton openButton = new JButton("Open");
        sideButtons.add((Component)openButton, new GridBagConstraints(0, -1, 1, 1, 0.0, 0.0, 10, 2, new Insets(0, 0, 6, 0), 0, 0));
        openButton.addActionListener(new ActionListener(){

            @Override
            public void actionPerformed(ActionEvent event) {
                EffectPanel.this.openEffect();
            }
        });
        JButton importButton = new JButton("Import");
        sideButtons.add((Component)importButton, new GridBagConstraints(0, -1, 1, 1, 0.0, 0.0, 10, 2, new Insets(0, 0, 6, 0), 0, 0));
        importButton.addActionListener(new ActionListener(){

            @Override
            public void actionPerformed(ActionEvent event) {
                EffectPanel.this.importEffect();
            }
        });
    }
}

