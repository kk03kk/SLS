/*
 * Decompiled with CFR 0.152.
 */
package com.badlogic.gdx.tools.flame;

import com.badlogic.gdx.graphics.g3d.particles.ParticleController;
import com.badlogic.gdx.graphics.g3d.particles.influencers.DynamicsInfluencer;
import com.badlogic.gdx.graphics.g3d.particles.influencers.DynamicsModifier;
import com.badlogic.gdx.tools.flame.AngularVelocityPanel;
import com.badlogic.gdx.tools.flame.EditorPanel;
import com.badlogic.gdx.tools.flame.FlameMain;
import com.badlogic.gdx.tools.flame.InfluencerPanel;
import com.badlogic.gdx.tools.flame.ParticleValuePanel;
import com.badlogic.gdx.tools.flame.StrengthVelocityPanel;
import com.badlogic.gdx.utils.Array;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.DefaultTableModel;

public class DynamicsInfluencerPanel
extends InfluencerPanel<DynamicsInfluencer> {
    private static final String VEL_TYPE_ROTATIONAL_2D = "Angular Velocity 2D";
    private static final String VEL_TYPE_ROTATIONAL_3D = "Angular Velocity 3D";
    private static final String VEL_TYPE_CENTRIPETAL = "Centripetal";
    private static final String VEL_TYPE_TANGENTIAL = "Tangential";
    private static final String VEL_TYPE_POLAR = "Polar";
    private static final String VEL_TYPE_BROWNIAN = "Brownian";
    private static final String VEL_TYPE_FACE = "Face";
    JComboBox velocityBox;
    JTable velocityTable;
    DefaultTableModel velocityTableModel;
    JPanel selectedVelocityPanel;
    AngularVelocityPanel angularVelocityPanel;
    StrengthVelocityPanel strengthVelocityPanel;
    ParticleValuePanel emptyPanel;
    Array<VelocityWrapper> velocities = new Array();

    public DynamicsInfluencerPanel(FlameMain editor, DynamicsInfluencer influencer) {
        super(editor, influencer, "Dynamics Influencer", "Defines how the particles dynamics (acceleration, angular velocity).");
        this.setValue(this.value);
        this.set(influencer);
    }

    private void set(DynamicsInfluencer influencer) {
        int i;
        for (i = this.velocityTableModel.getRowCount() - 1; i >= 0; --i) {
            this.velocityTableModel.removeRow(i);
        }
        this.velocities.clear();
        int c = influencer.velocities.size;
        for (i = 0; i < c; ++i) {
            this.velocities.add(new VelocityWrapper(((DynamicsModifier[])influencer.velocities.items)[i], true));
            this.velocityTableModel.addRow(new Object[]{"Velocity " + i, true});
        }
        DefaultComboBoxModel model = (DefaultComboBoxModel)this.velocityBox.getModel();
        model.removeAllElements();
        for (Object velocityObject : this.getAvailableVelocities(this.editor.getControllerType())) {
            model.addElement(velocityObject);
        }
    }

    private Object[] getAvailableVelocities(FlameMain.ControllerType type) {
        if (type == FlameMain.ControllerType.Billboard || type == FlameMain.ControllerType.PointSprite) {
            return new String[]{VEL_TYPE_ROTATIONAL_2D, VEL_TYPE_CENTRIPETAL, VEL_TYPE_TANGENTIAL, VEL_TYPE_POLAR, VEL_TYPE_BROWNIAN};
        }
        if (type == FlameMain.ControllerType.ModelInstance || type == FlameMain.ControllerType.ParticleController) {
            return new String[]{VEL_TYPE_ROTATIONAL_3D, VEL_TYPE_CENTRIPETAL, VEL_TYPE_TANGENTIAL, VEL_TYPE_POLAR, VEL_TYPE_BROWNIAN, VEL_TYPE_FACE};
        }
        return null;
    }

    @Override
    protected void initializeComponents() {
        super.initializeComponents();
        JPanel velocitiesPanel = new JPanel();
        velocitiesPanel.setLayout(new GridBagLayout());
        JPanel sideButtons = new JPanel(new GridBagLayout());
        velocitiesPanel.add((Component)sideButtons, new GridBagConstraints(1, 0, 1, 1, 1.0, 1.0, 18, 0, new Insets(0, 0, 0, 0), 0, 0));
        this.velocityBox = new JComboBox(new DefaultComboBoxModel());
        sideButtons.add(this.velocityBox, new GridBagConstraints(0, -1, 1, 1, 0.0, 0.0, 10, 2, new Insets(0, 0, 6, 0), 0, 0));
        JButton newButton = new JButton("New");
        sideButtons.add((Component)newButton, new GridBagConstraints(0, -1, 1, 1, 0.0, 0.0, 10, 2, new Insets(0, 0, 6, 0), 0, 0));
        newButton.addActionListener(new ActionListener(){

            @Override
            public void actionPerformed(ActionEvent event) {
                DynamicsInfluencerPanel.this.createVelocity(DynamicsInfluencerPanel.this.velocityBox.getSelectedItem());
            }
        });
        JButton deleteButton = new JButton("Delete");
        sideButtons.add((Component)deleteButton, new GridBagConstraints(0, -1, 1, 1, 0.0, 0.0, 10, 2, new Insets(0, 0, 6, 0), 0, 0));
        deleteButton.addActionListener(new ActionListener(){

            @Override
            public void actionPerformed(ActionEvent event) {
                DynamicsInfluencerPanel.this.deleteVelocity();
            }
        });
        JScrollPane scroll = new JScrollPane();
        velocitiesPanel.add((Component)scroll, new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0, 17, 0, new Insets(0, 0, 0, 6), 0, 0));
        this.velocityTable = new JTable(){

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
        this.velocityTable.getTableHeader().setReorderingAllowed(false);
        this.velocityTable.setSelectionMode(0);
        scroll.setViewportView(this.velocityTable);
        this.velocityTableModel = new DefaultTableModel(new String[0][0], new String[]{"Velocity", "Active"});
        this.velocityTable.setModel(this.velocityTableModel);
        this.velocityTable.getSelectionModel().addListSelectionListener(new ListSelectionListener(){

            @Override
            public void valueChanged(ListSelectionEvent event) {
                if (event.getValueIsAdjusting()) {
                    return;
                }
                DynamicsInfluencerPanel.this.velocitySelected();
            }
        });
        this.velocityTableModel.addTableModelListener(new TableModelListener(){

            @Override
            public void tableChanged(TableModelEvent event) {
                if (event.getColumn() != 1) {
                    return;
                }
                DynamicsInfluencerPanel.this.velocityChecked(event.getFirstRow(), (Boolean)DynamicsInfluencerPanel.this.velocityTable.getValueAt(event.getFirstRow(), 1));
            }
        });
        this.emptyPanel = new ParticleValuePanel(this.editor, "", "", true, false);
        this.strengthVelocityPanel = new StrengthVelocityPanel(this.editor, null, "Life", "", "");
        this.angularVelocityPanel = new AngularVelocityPanel(this.editor, null, "Life", "", "");
        this.strengthVelocityPanel.setVisible(false);
        this.angularVelocityPanel.setVisible(false);
        this.emptyPanel.setVisible(false);
        this.strengthVelocityPanel.setIsAlwayShown(true);
        this.angularVelocityPanel.setIsAlwayShown(true);
        this.emptyPanel.setIsAlwayShown(true);
        this.emptyPanel.setValue(null);
        int i = 0;
        this.addContent(i++, 0, velocitiesPanel);
        this.addContent(i++, 0, this.strengthVelocityPanel);
        this.addContent(i++, 0, this.angularVelocityPanel);
        this.addContent(i++, 0, this.emptyPanel);
    }

    protected void velocityChecked(int index, boolean isChecked) {
        ParticleController controller = this.editor.getEmitter();
        DynamicsInfluencer influencer = controller.findInfluencer(DynamicsInfluencer.class);
        influencer.velocities.clear();
        this.velocities.get((int)index).isActive = isChecked;
        for (VelocityWrapper wrapper : this.velocities) {
            if (!wrapper.isActive) continue;
            influencer.velocities.add(wrapper.velocityValue);
        }
        this.editor.restart();
    }

    protected void velocitySelected() {
        int index = this.velocityTable.getSelectedRow();
        if (index == -1) {
            return;
        }
        DynamicsModifier velocityValue = this.velocities.get((int)index).velocityValue;
        EditorPanel velocityPanel = this.getVelocityPanel(velocityValue);
        if (this.selectedVelocityPanel != null && this.selectedVelocityPanel != velocityPanel) {
            this.selectedVelocityPanel.setVisible(false);
        }
        velocityPanel.setVisible(true);
        velocityPanel.showContent(true);
        this.selectedVelocityPanel = velocityPanel;
    }

    private EditorPanel getVelocityPanel(DynamicsModifier velocityValue) {
        EditorPanel panel = null;
        if (velocityValue instanceof DynamicsModifier.Rotational2D) {
            this.strengthVelocityPanel.setValue((DynamicsModifier.Strength)velocityValue);
            this.strengthVelocityPanel.setName("Angular Velocity");
            this.strengthVelocityPanel.setDescription("The angular speed around the billboard facing direction, in degrees/sec .");
            panel = this.strengthVelocityPanel;
        } else if (velocityValue instanceof DynamicsModifier.CentripetalAcceleration) {
            this.strengthVelocityPanel.setValue((DynamicsModifier.CentripetalAcceleration)velocityValue);
            this.strengthVelocityPanel.setName("Centripetal Acceleration");
            this.strengthVelocityPanel.setDescription("A directional acceleration, the direction is towards the origin (global), or towards the emitter position (local), in world units/sec2 .");
            panel = this.strengthVelocityPanel;
        } else if (velocityValue instanceof DynamicsModifier.TangentialAcceleration) {
            this.angularVelocityPanel.setValue((DynamicsModifier.Angular)velocityValue);
            this.angularVelocityPanel.setName("Tangetial Velocity");
            this.angularVelocityPanel.setDescription("A directional acceleration (axis and magnitude), the final direction is the cross product between particle position and the axis, in world units/sec2 .");
            panel = this.angularVelocityPanel;
        } else if (velocityValue instanceof DynamicsModifier.PolarAcceleration) {
            this.angularVelocityPanel.setValue((DynamicsModifier.Angular)velocityValue);
            this.angularVelocityPanel.setName("Polar Velocity");
            this.angularVelocityPanel.setDescription("A directional acceleration (axis and magnitude), in world units/sec2 .");
            panel = this.angularVelocityPanel;
        } else if (velocityValue instanceof DynamicsModifier.BrownianAcceleration) {
            this.strengthVelocityPanel.setValue((DynamicsModifier.Strength)velocityValue);
            this.strengthVelocityPanel.setName("Brownian Velocity");
            this.strengthVelocityPanel.setDescription("A directional acceleration which has random direction at each update, in world units/sec2.");
            panel = this.strengthVelocityPanel;
        } else if (velocityValue instanceof DynamicsModifier.Rotational3D) {
            this.angularVelocityPanel.setValue((DynamicsModifier.Angular)velocityValue);
            this.angularVelocityPanel.setName("Angular Velocity");
            this.angularVelocityPanel.setDescription("An angular velocity (axis and magnitude), in degree/sec2.");
            panel = this.angularVelocityPanel;
        } else if (velocityValue instanceof DynamicsModifier.FaceDirection) {
            this.emptyPanel.setName(VEL_TYPE_FACE);
            this.emptyPanel.setDescription("Rotates the model to face its current velocity (Do not add any other angular velocity when using this).");
            panel = this.emptyPanel;
        }
        return panel;
    }

    private DynamicsModifier createVelocityValue(Object selectedItem) {
        DynamicsModifier velocityValue = null;
        if (selectedItem == VEL_TYPE_ROTATIONAL_2D) {
            velocityValue = new DynamicsModifier.Rotational2D();
        } else if (selectedItem == VEL_TYPE_ROTATIONAL_3D) {
            velocityValue = new DynamicsModifier.Rotational3D();
        } else if (selectedItem == VEL_TYPE_CENTRIPETAL) {
            velocityValue = new DynamicsModifier.CentripetalAcceleration();
        } else if (selectedItem == VEL_TYPE_TANGENTIAL) {
            velocityValue = new DynamicsModifier.TangentialAcceleration();
        } else if (selectedItem == VEL_TYPE_POLAR) {
            velocityValue = new DynamicsModifier.PolarAcceleration();
        } else if (selectedItem == VEL_TYPE_BROWNIAN) {
            velocityValue = new DynamicsModifier.BrownianAcceleration();
        } else if (selectedItem == VEL_TYPE_FACE) {
            velocityValue = new DynamicsModifier.FaceDirection();
        }
        return velocityValue;
    }

    protected void deleteVelocity() {
        int row = this.velocityTable.getSelectedRow();
        if (row == -1) {
            return;
        }
        ParticleController controller = this.editor.getEmitter();
        DynamicsInfluencer influencer = controller.findInfluencer(DynamicsInfluencer.class);
        influencer.velocities.removeValue(this.velocities.removeIndex((int)row).velocityValue, true);
        this.velocityTableModel.removeRow(row);
        this.editor.restart();
        this.selectedVelocityPanel.setVisible(false);
        this.selectedVelocityPanel = null;
    }

    protected void createVelocity(Object selectedItem) {
        ParticleController controller = this.editor.getEmitter();
        DynamicsInfluencer influencer = controller.findInfluencer(DynamicsInfluencer.class);
        VelocityWrapper wrapper = new VelocityWrapper(this.createVelocityValue(selectedItem), true);
        this.velocities.add(wrapper);
        influencer.velocities.add(wrapper.velocityValue);
        int index = this.velocities.size - 1;
        this.velocityTableModel.addRow(new Object[]{"Velocity " + index, true});
        this.editor.restart();
        this.velocityTable.getSelectionModel().setSelectionInterval(index, index);
        this.revalidate();
        this.repaint();
    }

    protected class VelocityWrapper {
        public DynamicsModifier velocityValue;
        public boolean isActive;

        public VelocityWrapper(DynamicsModifier value, boolean isActive) {
            this.velocityValue = value;
            this.isActive = isActive;
        }
    }
}

