/*
 * Decompiled with CFR 0.152.
 */
package com.badlogic.gdx.scenes.scene2d.utils;

import com.badlogic.gdx.scenes.scene2d.utils.Selection;
import com.badlogic.gdx.scenes.scene2d.utils.UIUtils;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ObjectSet;

public class ArraySelection<T>
extends Selection<T> {
    private Array<T> array;
    private boolean rangeSelect = true;
    private int rangeStart;

    public ArraySelection(Array<T> array) {
        this.array = array;
    }

    @Override
    public void choose(T item) {
        if (item == null) {
            throw new IllegalArgumentException("item cannot be null.");
        }
        if (this.isDisabled) {
            return;
        }
        int index = this.array.indexOf(item, false);
        if (this.selected.size > 0 && this.rangeSelect && this.multiple && UIUtils.shift()) {
            int oldRangeState = this.rangeStart;
            this.snapshot();
            int start = this.rangeStart;
            int end = index;
            if (start > end) {
                int temp = end;
                end = start;
                start = temp;
            }
            if (!UIUtils.ctrl()) {
                this.selected.clear();
            }
            for (int i = start; i <= end; ++i) {
                this.selected.add(this.array.get(i));
            }
            if (this.fireChangeEvent()) {
                this.rangeStart = oldRangeState;
                this.revert();
            }
            this.cleanup();
            return;
        }
        this.rangeStart = index;
        super.choose(item);
    }

    public boolean getRangeSelect() {
        return this.rangeSelect;
    }

    public void setRangeSelect(boolean rangeSelect) {
        this.rangeSelect = rangeSelect;
    }

    public void validate() {
        Array array = this.array;
        if (array.size == 0) {
            this.clear();
            return;
        }
        ObjectSet.ObjectSetIterator iter = this.items().iterator();
        while (iter.hasNext()) {
            Object selected = iter.next();
            if (array.contains(selected, false)) continue;
            iter.remove();
        }
        if (this.required && this.selected.size == 0) {
            this.set(array.first());
        }
    }
}

