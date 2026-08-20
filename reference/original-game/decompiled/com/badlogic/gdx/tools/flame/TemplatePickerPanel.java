/*
 * Decompiled with CFR 0.152.
 */
package com.badlogic.gdx.tools.flame;

import com.badlogic.gdx.tools.flame.EditorPanel;
import com.badlogic.gdx.tools.flame.FlameMain;
import com.badlogic.gdx.tools.flame.LoaderButton;
import com.badlogic.gdx.utils.Array;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.Insets;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.DefaultTableModel;

public class TemplatePickerPanel<T>
extends EditorPanel<Array<T>>
implements LoaderButton.Listener<T> {
    Array<T> loadedTemplates;
    Array<T> excludedTemplates;
    Class<T> type;
    JTable templatesTable;
    DefaultTableModel templatesTableModel;
    boolean isOneModelSelectedRequired = true;
    boolean isMultipleSelectionAllowed = true;
    Listener listener;
    int lastSelected = -1;

    public TemplatePickerPanel(FlameMain editor, Array<T> value, Listener listener, Class<T> type) {
        this(editor, value, listener, type, null, true, true);
    }

    public TemplatePickerPanel(FlameMain editor, Array<T> value, Listener listener, Class<T> type, LoaderButton<T> loaderButton) {
        this(editor, value, listener, type, loaderButton, true, true);
    }

    public TemplatePickerPanel(FlameMain editor, Array<T> value, Listener listener, Class<T> type, LoaderButton<T> loaderButton, boolean isOneModelSelectedRequired, boolean isMultipleSelectionAllowed) {
        super(editor, "", "");
        this.type = type;
        this.listener = listener;
        this.isOneModelSelectedRequired = isOneModelSelectedRequired;
        this.isMultipleSelectionAllowed = isMultipleSelectionAllowed;
        this.loadedTemplates = new Array();
        this.excludedTemplates = new Array();
        this.initializeComponents(type, loaderButton);
        this.setValue(value);
    }

    @Override
    public void setValue(Array<T> value) {
        super.setValue(value);
        if (value == null) {
            return;
        }
        if (!this.isMultipleSelectionAllowed && value.size > 1) {
            throw new RuntimeException("Multiple selection must be enabled to ensure consistency between picked and available models.");
        }
        for (int i = 0; i < value.size; ++i) {
            T model = value.get(i);
            int index = this.loadedTemplates.indexOf(model, true);
            if (index <= -1) continue;
            EditorPanel.setValue(this.templatesTableModel, true, index, 1);
            this.lastSelected = index;
        }
    }

    public void setOneModelSelectionRequired(boolean isOneModelSelectionRequired) {
        this.isOneModelSelectedRequired = isOneModelSelectionRequired;
    }

    public void setMultipleSelectionAllowed(boolean isMultipleSelectionAllowed) {
        this.isMultipleSelectionAllowed = isMultipleSelectionAllowed;
    }

    public void setExcludedTemplates(Array<T> excludedTemplates) {
        this.excludedTemplates.clear();
        this.excludedTemplates.addAll(excludedTemplates);
    }

    public void setLoadedTemplates(Array<T> templates) {
        this.loadedTemplates.clear();
        this.loadedTemplates.addAll(templates);
        this.loadedTemplates.removeAll(this.excludedTemplates, true);
        this.templatesTableModel.getDataVector().removeAllElements();
        int i = 0;
        for (T template : templates) {
            this.templatesTableModel.addRow(new Object[]{this.getTemplateName(template, i), false});
            ++i;
        }
        this.lastSelected = -1;
        this.setValue((Array)this.value);
    }

    protected String getTemplateName(T template, int index) {
        String name = this.editor.assetManager.getAssetFileName(template);
        return name == null ? "template " + index : name;
    }

    public void reloadTemplates() {
        this.setLoadedTemplates(this.editor.assetManager.getAll(this.type, new Array()));
    }

    protected void initializeComponents(Class<T> type, LoaderButton<T> loaderButton) {
        int i = 0;
        if (loaderButton != null) {
            loaderButton.setListener(this);
            this.contentPanel.add(loaderButton, new GridBagConstraints(0, i++, 1, 1, 1.0, 0.0, 10, 1, new Insets(0, 0, 0, 6), 0, 0));
        }
        JScrollPane scroll = new JScrollPane();
        this.contentPanel.add((Component)scroll, new GridBagConstraints(0, i, 1, 1, 1.0, 0.0, 10, 1, new Insets(0, 0, 0, 6), 0, 0));
        this.templatesTable = new JTable(){

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
        this.templatesTable.getTableHeader().setReorderingAllowed(false);
        this.templatesTable.setSelectionMode(0);
        scroll.setViewportView(this.templatesTable);
        this.templatesTableModel = new DefaultTableModel(new String[0][0], new String[]{"Template", "Selected"});
        this.templatesTable.setModel(this.templatesTableModel);
        this.reloadTemplates();
        this.templatesTableModel.addTableModelListener(new TableModelListener(){

            @Override
            public void tableChanged(TableModelEvent event) {
                if (event.getColumn() != 1) {
                    return;
                }
                int row = event.getFirstRow();
                boolean checked = (Boolean)TemplatePickerPanel.this.templatesTable.getValueAt(row, 1);
                if (TemplatePickerPanel.this.isOneModelSelectedRequired && ((Array)TemplatePickerPanel.this.value).size == 1 && !checked) {
                    EditorPanel.setValue(TemplatePickerPanel.this.templatesTableModel, true, row, 1);
                    return;
                }
                TemplatePickerPanel.this.templateChecked(row, checked);
            }
        });
    }

    protected void templateChecked(int index, Boolean isChecked) {
        T template = this.loadedTemplates.get(index);
        if (isChecked.booleanValue()) {
            if (!this.isMultipleSelectionAllowed && this.lastSelected > -1) {
                ((Array)this.value).removeValue(this.loadedTemplates.get(this.lastSelected), true);
                EditorPanel.setValue(this.templatesTableModel, false, this.lastSelected, 1);
            }
            ((Array)this.value).add(template);
            this.lastSelected = index;
        } else {
            ((Array)this.value).removeValue(template, true);
        }
        this.listener.onTemplateChecked(template, isChecked);
    }

    @Override
    public void onResourceLoaded(T model) {
        this.reloadTemplates();
        if (this.lastSelected == -1 && this.isOneModelSelectedRequired) {
            this.templateChecked(this.loadedTemplates.size - 1, true);
        } else {
            this.setValue((Array)this.value);
        }
        this.revalidate();
        this.repaint();
    }

    public static interface Listener<T> {
        public void onTemplateChecked(T var1, boolean var2);
    }
}

