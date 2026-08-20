/*
 * Decompiled with CFR 0.152.
 */
package com.badlogic.gdx.tools.particleeditor;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.ParticleEffect;
import com.badlogic.gdx.graphics.g2d.ParticleEmitter;
import com.badlogic.gdx.tools.particleeditor.ParticleEditor;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.StreamUtils;
import java.awt.Component;
import java.awt.FileDialog;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileWriter;
import java.net.URI;
import javax.swing.JButton;
import javax.swing.JOptionPane;
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
    ParticleEditor editor;
    JTable emitterTable;
    DefaultTableModel emitterTableModel;
    int editIndex;
    String lastDir;

    public EffectPanel(ParticleEditor editor) {
        this.editor = editor;
        this.initializeComponents();
    }

    public ParticleEmitter newEmitter(String name, boolean select) {
        ParticleEmitter emitter = new ParticleEmitter();
        emitter.getDuration().setLow(1000.0f);
        emitter.getEmission().setHigh(50.0f);
        emitter.getLife().setHigh(500.0f);
        emitter.getScale().setHigh(32.0f, 32.0f);
        emitter.getTint().setColors(new float[]{1.0f, 0.12156863f, 0.047058824f});
        emitter.getTransparency().setHigh(1.0f);
        emitter.setMaxParticleCount(25);
        emitter.setImagePath("particle.png");
        this.addEmitter(name, select, emitter);
        return emitter;
    }

    public ParticleEmitter newExampleEmitter(String name, boolean select) {
        ParticleEmitter emitter = new ParticleEmitter();
        emitter.getDuration().setLow(3000.0f);
        emitter.getEmission().setHigh(250.0f);
        emitter.getLife().setHigh(500.0f, 1000.0f);
        emitter.getLife().setTimeline(new float[]{0.0f, 0.66f, 1.0f});
        emitter.getLife().setScaling(new float[]{1.0f, 1.0f, 0.3f});
        emitter.getScale().setHigh(32.0f, 32.0f);
        emitter.getRotation().setLow(1.0f, 360.0f);
        emitter.getRotation().setHigh(180.0f, 180.0f);
        emitter.getRotation().setTimeline(new float[]{0.0f, 1.0f});
        emitter.getRotation().setScaling(new float[]{0.0f, 1.0f});
        emitter.getRotation().setRelative(true);
        emitter.getAngle().setHigh(45.0f, 135.0f);
        emitter.getAngle().setLow(90.0f);
        emitter.getAngle().setTimeline(new float[]{0.0f, 0.5f, 1.0f});
        emitter.getAngle().setScaling(new float[]{1.0f, 0.0f, 0.0f});
        emitter.getAngle().setActive(true);
        emitter.getVelocity().setHigh(30.0f, 300.0f);
        emitter.getVelocity().setActive(true);
        emitter.getTint().setColors(new float[]{1.0f, 0.12156863f, 0.047058824f});
        emitter.getTransparency().setHigh(1.0f, 1.0f);
        emitter.getTransparency().setTimeline(new float[]{0.0f, 0.2f, 0.8f, 1.0f});
        emitter.getTransparency().setScaling(new float[]{0.0f, 1.0f, 0.75f, 0.0f});
        emitter.setMaxParticleCount(200);
        emitter.setImagePath("particle.png");
        this.addEmitter(name, select, emitter);
        return emitter;
    }

    private void addEmitter(String name, boolean select, ParticleEmitter emitter) {
        Array<ParticleEmitter> emitters = this.editor.effect.getEmitters();
        if (emitters.size == 0) {
            emitter.setPosition(this.editor.worldCamera.viewportWidth / 2.0f, this.editor.worldCamera.viewportHeight / 2.0f);
        } else {
            ParticleEmitter p = emitters.get(0);
            emitter.setPosition(p.getX(), p.getY());
        }
        emitters.add(emitter);
        this.emitterTableModel.addRow(new Object[]{name, true});
        if (select) {
            this.editor.reloadRows();
            int row = this.emitterTableModel.getRowCount() - 1;
            this.emitterTable.getSelectionModel().setSelectionInterval(row, row);
        }
    }

    void emitterSelected() {
        int row = this.emitterTable.getSelectedRow();
        if (row == -1) {
            row = this.editIndex;
            this.emitterTable.getSelectionModel().setSelectionInterval(row, row);
        }
        if (row == this.editIndex) {
            return;
        }
        this.editIndex = row;
        this.editor.reloadRows();
    }

    void openEffect(boolean mergeIntoCurrent) {
        FileDialog dialog = new FileDialog((Frame)this.editor, "Open Effect", 0);
        if (this.lastDir != null) {
            dialog.setDirectory(this.lastDir);
        }
        dialog.setVisible(true);
        String file = dialog.getFile();
        String dir = dialog.getDirectory();
        if (dir == null || file == null || file.trim().length() == 0) {
            return;
        }
        this.lastDir = dir;
        ParticleEffect effect = new ParticleEffect();
        try {
            File effectFile = new File(dir, file);
            effect.loadEmitters(Gdx.files.absolute(effectFile.getAbsolutePath()));
            if (mergeIntoCurrent) {
                for (ParticleEmitter emitter : effect.getEmitters()) {
                    this.addEmitter(emitter.getName(), false, emitter);
                }
            } else {
                this.editor.effect = effect;
                this.editor.effectFile = effectFile;
            }
            this.emitterTableModel.getDataVector().removeAllElements();
            this.editor.particleData.clear();
        }
        catch (Exception ex) {
            System.out.println("Error loading effect: " + new File(dir, file).getAbsolutePath());
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this.editor, "Error opening effect.");
            return;
        }
        for (ParticleEmitter emitter : this.editor.effect.getEmitters()) {
            emitter.setPosition(this.editor.worldCamera.viewportWidth / 2.0f, this.editor.worldCamera.viewportHeight / 2.0f);
            this.emitterTableModel.addRow(new Object[]{emitter.getName(), true});
        }
        this.editIndex = 0;
        this.emitterTable.getSelectionModel().setSelectionInterval(this.editIndex, this.editIndex);
        this.editor.reloadRows();
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    void saveEffect() {
        FileDialog dialog = new FileDialog((Frame)this.editor, "Save Effect", 1);
        if (this.lastDir != null) {
            dialog.setDirectory(this.lastDir);
        }
        dialog.setVisible(true);
        String file = dialog.getFile();
        String dir = dialog.getDirectory();
        if (dir == null || file == null || file.trim().length() == 0) {
            return;
        }
        this.lastDir = dir;
        int index = 0;
        File effectFile = new File(dir, file);
        URI effectDirUri = effectFile.getParentFile().toURI();
        for (ParticleEmitter emitter : this.editor.effect.getEmitters()) {
            emitter.setName((String)this.emitterTableModel.getValueAt(index++, 0));
            String imagePath = emitter.getImagePath();
            if (!imagePath.contains("/") && !imagePath.contains("\\") || imagePath.contains("..")) continue;
            URI imageUri = new File(emitter.getImagePath()).toURI();
            emitter.setImagePath(effectDirUri.relativize(imageUri).getPath());
        }
        File outputFile = new File(dir, file);
        FileWriter fileWriter = null;
        try {
            fileWriter = new FileWriter(outputFile);
            this.editor.effect.save(fileWriter);
        }
        catch (Exception ex) {
            try {
                System.out.println("Error saving effect: " + outputFile.getAbsolutePath());
                ex.printStackTrace();
                JOptionPane.showMessageDialog(this.editor, "Error saving effect.");
            }
            catch (Throwable throwable) {
                StreamUtils.closeQuietly(fileWriter);
                throw throwable;
            }
            StreamUtils.closeQuietly(fileWriter);
        }
        StreamUtils.closeQuietly(fileWriter);
    }

    void duplicateEmitter() {
        int row = this.emitterTable.getSelectedRow();
        if (row == -1) {
            return;
        }
        String name = (String)this.emitterTableModel.getValueAt(row, 0);
        this.addEmitter(name, true, new ParticleEmitter(this.editor.effect.getEmitters().get(row)));
    }

    void deleteEmitter() {
        if (this.editor.effect.getEmitters().size == 1) {
            return;
        }
        int row = this.emitterTable.getSelectedRow();
        if (row == -1) {
            return;
        }
        if (row <= this.editIndex) {
            int oldEditIndex = this.editIndex;
            this.editIndex = Math.max(0, this.editIndex - 1);
            if (oldEditIndex == row) {
                this.editor.reloadRows();
            }
        }
        this.editor.effect.getEmitters().removeIndex(row);
        this.emitterTableModel.removeRow(row);
        this.emitterTable.getSelectionModel().setSelectionInterval(this.editIndex, this.editIndex);
    }

    void move(int direction) {
        if (direction < 0 && this.editIndex == 0) {
            return;
        }
        Array<ParticleEmitter> emitters = this.editor.effect.getEmitters();
        if (direction > 0 && this.editIndex == emitters.size - 1) {
            return;
        }
        int insertIndex = this.editIndex + direction;
        Object name = this.emitterTableModel.getValueAt(this.editIndex, 0);
        this.emitterTableModel.removeRow(this.editIndex);
        ParticleEmitter emitter = emitters.removeIndex(this.editIndex);
        this.emitterTableModel.insertRow(insertIndex, new Object[]{name});
        emitters.insert(insertIndex, emitter);
        this.editIndex = insertIndex;
        this.emitterTable.getSelectionModel().setSelectionInterval(this.editIndex, this.editIndex);
    }

    void emitterChecked(int index, boolean checked) {
        this.editor.setEnabled(this.editor.effect.getEmitters().get(index), checked);
        this.editor.effect.start();
    }

    private void initializeComponents() {
        this.setLayout(new GridBagLayout());
        JPanel sideButtons = new JPanel(new GridBagLayout());
        this.add((Component)sideButtons, new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0, 10, 1, new Insets(0, 0, 0, 0), 0, 0));
        JButton newButton = new JButton("New");
        sideButtons.add((Component)newButton, new GridBagConstraints(0, -1, 1, 1, 0.0, 0.0, 10, 2, new Insets(0, 0, 6, 0), 0, 0));
        newButton.addActionListener(new ActionListener(){

            @Override
            public void actionPerformed(ActionEvent event) {
                EffectPanel.this.newEmitter("Untitled", true);
            }
        });
        newButton = new JButton("Duplicate");
        sideButtons.add((Component)newButton, new GridBagConstraints(0, -1, 1, 1, 0.0, 0.0, 10, 2, new Insets(0, 0, 6, 0), 0, 0));
        newButton.addActionListener(new ActionListener(){

            @Override
            public void actionPerformed(ActionEvent event) {
                EffectPanel.this.duplicateEmitter();
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
                EffectPanel.this.openEffect(false);
            }
        });
        JButton mergeButton = new JButton("Merge");
        sideButtons.add((Component)mergeButton, new GridBagConstraints(0, -1, 1, 1, 0.0, 0.0, 10, 2, new Insets(0, 0, 6, 0), 0, 0));
        mergeButton.addActionListener(new ActionListener(){

            @Override
            public void actionPerformed(ActionEvent event) {
                EffectPanel.this.openEffect(true);
            }
        });
        JButton upButton = new JButton("Up");
        sideButtons.add((Component)upButton, new GridBagConstraints(0, -1, 1, 1, 0.0, 1.0, 15, 2, new Insets(0, 0, 6, 0), 0, 0));
        upButton.addActionListener(new ActionListener(){

            @Override
            public void actionPerformed(ActionEvent event) {
                EffectPanel.this.move(-1);
            }
        });
        JButton downButton = new JButton("Down");
        sideButtons.add((Component)downButton, new GridBagConstraints(0, -1, 1, 1, 0.0, 0.0, 10, 2, new Insets(0, 0, 0, 0), 0, 0));
        downButton.addActionListener(new ActionListener(){

            @Override
            public void actionPerformed(ActionEvent event) {
                EffectPanel.this.move(1);
            }
        });
        JScrollPane scroll = new JScrollPane();
        this.add((Component)scroll, new GridBagConstraints(0, 0, 1, 1, 1.0, 1.0, 10, 1, new Insets(0, 0, 0, 6), 0, 0));
        this.emitterTable = new JTable(){

            public Class getColumnClass(int column) {
                return column == 1 ? Boolean.class : super.getColumnClass(column);
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
    }
}

