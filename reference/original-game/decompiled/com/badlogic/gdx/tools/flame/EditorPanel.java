/*
 * Decompiled with CFR 0.152.
 */
package com.badlogic.gdx.tools.flame;

import com.badlogic.gdx.tools.flame.FlameMain;
import com.badlogic.gdx.tools.flame.Slider;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JToggleButton;
import javax.swing.event.ChangeListener;
import javax.swing.event.TableModelListener;
import javax.swing.table.DefaultTableModel;

public abstract class EditorPanel<T>
extends JPanel {
    private String name;
    private String description;
    protected T value;
    private JPanel titlePanel;
    JToggleButton activeButton;
    JPanel contentPanel;
    JToggleButton advancedButton;
    JButton removeButton;
    JPanel advancedPanel;
    private boolean hasAdvanced;
    JLabel nameLabel;
    JLabel descriptionLabel;
    protected boolean isAlwaysActive;
    protected boolean isAlwaysShown = false;
    protected boolean isRemovable;
    protected FlameMain editor;

    public EditorPanel(FlameMain editor, String name, String description, boolean alwaysActive, boolean isRemovable) {
        this.editor = editor;
        this.name = name;
        this.description = description;
        this.isRemovable = isRemovable;
        this.isAlwaysActive = alwaysActive;
        this.initializeComponents();
        this.showContent(false);
    }

    public EditorPanel(FlameMain editor, String name, String description) {
        this(editor, name, description, true, false);
    }

    protected void activate() {
    }

    public void showContent(boolean visible) {
        this.contentPanel.setVisible(visible);
        this.advancedPanel.setVisible(visible && this.advancedButton.isSelected());
        this.advancedButton.setVisible(visible && this.hasAdvanced);
        this.descriptionLabel.setText(visible ? this.description : "");
    }

    public void setIsAlwayShown(boolean isAlwaysShown) {
        this.showContent(isAlwaysShown);
        this.isAlwaysShown = isAlwaysShown;
        this.titlePanel.setCursor(null);
    }

    public void update(FlameMain editor) {
    }

    public void setHasAdvanced(boolean hasAdvanced) {
        this.hasAdvanced = hasAdvanced;
        this.advancedButton.setVisible(hasAdvanced);
    }

    public JPanel getContentPanel() {
        return this.contentPanel;
    }

    public JPanel getAdvancedPanel() {
        return this.advancedPanel;
    }

    @Override
    public String getName() {
        return this.name;
    }

    public void setEmbedded() {
        GridBagLayout layout = (GridBagLayout)this.getLayout();
        GridBagConstraints constraints = layout.getConstraints(this.contentPanel);
        constraints.insets = new Insets(0, 0, 0, 0);
        layout.setConstraints(this.contentPanel, constraints);
        this.titlePanel.setVisible(false);
    }

    protected void initializeComponents() {
        this.setLayout(new GridBagLayout());
        this.titlePanel = new JPanel(new GridBagLayout());
        this.add((Component)this.titlePanel, new GridBagConstraints(1, 0, 1, 1, 1.0, 0.0, 17, 2, new Insets(3, 0, 3, 0), 0, 0));
        this.titlePanel.setCursor(Cursor.getPredefinedCursor(12));
        this.nameLabel = new JLabel(this.name);
        this.titlePanel.add((Component)this.nameLabel, new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0, 17, 0, new Insets(3, 6, 3, 6), 0, 0));
        this.nameLabel.setFont(this.nameLabel.getFont().deriveFont(1));
        this.descriptionLabel = new JLabel(this.description);
        this.titlePanel.add((Component)this.descriptionLabel, new GridBagConstraints(1, 0, 1, 1, 1.0, 0.0, 17, 0, new Insets(3, 6, 3, 6), 0, 0));
        this.advancedButton = new JToggleButton("Advanced");
        this.titlePanel.add((Component)this.advancedButton, new GridBagConstraints(2, 0, 1, 1, 0.0, 0.0, 10, 0, new Insets(0, 0, 0, 6), 0, 0));
        this.advancedButton.setVisible(false);
        this.activeButton = new JToggleButton("Active");
        this.titlePanel.add((Component)this.activeButton, new GridBagConstraints(3, 0, 1, 1, 0.0, 0.0, 10, 0, new Insets(0, 0, 0, 6), 0, 0));
        this.removeButton = new JButton("X");
        this.titlePanel.add((Component)this.removeButton, new GridBagConstraints(4, 0, 1, 1, 0.0, 0.0, 10, 0, new Insets(0, 0, 0, 6), 0, 0));
        this.contentPanel = new JPanel(new GridBagLayout());
        this.add((Component)this.contentPanel, new GridBagConstraints(1, 1, 1, 1, 0.0, 0.0, 10, 1, new Insets(0, 6, 6, 6), 0, 0));
        this.contentPanel.setVisible(false);
        this.advancedPanel = new JPanel(new GridBagLayout());
        this.add((Component)this.advancedPanel, new GridBagConstraints(1, 2, 1, 1, 0.0, 0.0, 10, 1, new Insets(0, 6, 6, 6), 0, 0));
        this.advancedPanel.setVisible(false);
        this.titlePanel.addMouseListener(new MouseAdapter(){

            @Override
            public void mouseClicked(MouseEvent event) {
                if (!EditorPanel.this.isAlwaysShown) {
                    EditorPanel.this.showContent(!EditorPanel.this.contentPanel.isVisible());
                }
            }
        });
        this.activeButton.addActionListener(new ActionListener(){

            @Override
            public void actionPerformed(ActionEvent event) {
                EditorPanel.this.activate();
            }
        });
        this.advancedButton.addActionListener(new ActionListener(){

            @Override
            public void actionPerformed(ActionEvent event) {
                EditorPanel.this.advancedPanel.setVisible(EditorPanel.this.advancedButton.isSelected());
            }
        });
        this.removeButton.addActionListener(new ActionListener(){

            @Override
            public void actionPerformed(ActionEvent event) {
                EditorPanel.this.removePanel();
            }
        });
    }

    protected void removePanel() {
        Container parent = this.getParent();
        parent.remove(this);
        parent.validate();
        parent.repaint();
    }

    @Override
    public void setName(String name) {
        this.name = name;
        this.nameLabel.setText(name);
    }

    public void setDescription(String desc) {
        this.description = desc;
        this.descriptionLabel.setText(desc);
    }

    protected void addContent(int row, int column, JComponent component) {
        this.addContent(row, column, component, true, 10, 1);
    }

    protected void addContent(int row, int column, JComponent component, boolean addBorder) {
        this.addContent(row, column, component, addBorder, 10, 1);
    }

    protected void addContent(int row, int column, JComponent component, int anchor, int fill) {
        this.addContent(row, column, component, true, anchor, fill);
    }

    protected void addContent(int row, int column, JComponent component, boolean addBorders, int anchor, int fill, float wx, float wy) {
        EditorPanel.addContent(this.contentPanel, row, column, component, addBorders, anchor, fill, wx, wy);
    }

    protected void addContent(int row, int column, JComponent component, boolean addBorders, int anchor, int fill) {
        this.addContent(row, column, component, addBorders, anchor, fill, 1.0f, 1.0f);
    }

    public void setValue(T value) {
        this.value = value;
        this.activeButton.setVisible(value == null ? false : !this.isAlwaysActive);
        this.removeButton.setVisible(this.isRemovable);
    }

    public static void addContent(JPanel panel, int row, int column, JComponent component, boolean addBorders, int anchor, int fill, float wx, float wy) {
        if (addBorders) {
            component.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, Color.black));
        }
        panel.add((Component)component, new GridBagConstraints(column, row, 1, 1, wx, wy, anchor, fill, new Insets(0, 0, 0, 0), 0, 0));
    }

    protected static <K> void setValue(JSpinner spinner, K object) {
        ChangeListener[] listeners = spinner.getChangeListeners();
        ChangeListener listener = null;
        if (listeners != null && listeners.length > 0) {
            listener = listeners[0];
            spinner.removeChangeListener(listener);
        }
        spinner.setValue(object);
        if (listener != null) {
            spinner.addChangeListener(listener);
        }
    }

    protected static void setValue(JCheckBox checkBox, boolean isSelected) {
        ActionListener[] listeners = checkBox.getActionListeners();
        ActionListener listener = null;
        if (listeners != null && listeners.length > 0) {
            listener = listeners[0];
            checkBox.removeActionListener(listener);
        }
        checkBox.setSelected(isSelected);
        if (listener != null) {
            checkBox.addActionListener(listener);
        }
    }

    protected static <K> void setValue(Slider slider, float value) {
        ChangeListener[] listeners = slider.spinner.getChangeListeners();
        ChangeListener listener = null;
        if (listeners != null && listeners.length > 0) {
            listener = listeners[0];
            slider.spinner.removeChangeListener(listener);
        }
        slider.setValue(value);
        if (listener != null) {
            slider.spinner.addChangeListener(listener);
        }
    }

    protected static void setValue(DefaultTableModel tableModel, Object value, int row, int column) {
        TableModelListener[] listeners = tableModel.getTableModelListeners();
        TableModelListener listener = null;
        if (listeners != null && listeners.length > 0) {
            listener = listeners[0];
            tableModel.removeTableModelListener(listener);
        }
        tableModel.setValueAt(value, row, column);
        if (listener != null) {
            tableModel.addTableModelListener(listener);
        }
    }
}

