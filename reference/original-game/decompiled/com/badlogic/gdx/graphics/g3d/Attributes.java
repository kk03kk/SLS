/*
 * Decompiled with CFR 0.152.
 */
package com.badlogic.gdx.graphics.g3d;

import com.badlogic.gdx.graphics.g3d.Attribute;
import com.badlogic.gdx.utils.Array;
import java.util.Comparator;
import java.util.Iterator;

public class Attributes
implements Iterable<Attribute>,
Comparator<Attribute>,
Comparable<Attributes> {
    protected long mask;
    protected final Array<Attribute> attributes = new Array();
    protected boolean sorted = true;

    public final void sort() {
        if (!this.sorted) {
            this.attributes.sort(this);
            this.sorted = true;
        }
    }

    public final long getMask() {
        return this.mask;
    }

    public final Attribute get(long type) {
        if (this.has(type)) {
            for (int i = 0; i < this.attributes.size; ++i) {
                if (this.attributes.get((int)i).type != type) continue;
                return this.attributes.get(i);
            }
        }
        return null;
    }

    public final <T extends Attribute> T get(Class<T> clazz, long type) {
        return (T)this.get(type);
    }

    public final Array<Attribute> get(Array<Attribute> out, long type) {
        for (int i = 0; i < this.attributes.size; ++i) {
            if ((this.attributes.get((int)i).type & type) == 0L) continue;
            out.add(this.attributes.get(i));
        }
        return out;
    }

    public void clear() {
        this.mask = 0L;
        this.attributes.clear();
    }

    public int size() {
        return this.attributes.size;
    }

    private final void enable(long mask) {
        this.mask |= mask;
    }

    private final void disable(long mask) {
        this.mask &= mask ^ 0xFFFFFFFFFFFFFFFFL;
    }

    public final void set(Attribute attribute) {
        int idx = this.indexOf(attribute.type);
        if (idx < 0) {
            this.enable(attribute.type);
            this.attributes.add(attribute);
            this.sorted = false;
        } else {
            this.attributes.set(idx, attribute);
        }
        this.sort();
    }

    public final void set(Attribute attribute1, Attribute attribute2) {
        this.set(attribute1);
        this.set(attribute2);
    }

    public final void set(Attribute attribute1, Attribute attribute2, Attribute attribute3) {
        this.set(attribute1);
        this.set(attribute2);
        this.set(attribute3);
    }

    public final void set(Attribute attribute1, Attribute attribute2, Attribute attribute3, Attribute attribute4) {
        this.set(attribute1);
        this.set(attribute2);
        this.set(attribute3);
        this.set(attribute4);
    }

    public final void set(Attribute ... attributes) {
        for (Attribute attr : attributes) {
            this.set(attr);
        }
    }

    public final void set(Iterable<Attribute> attributes) {
        for (Attribute attr : attributes) {
            this.set(attr);
        }
    }

    public final void remove(long mask) {
        for (int i = this.attributes.size - 1; i >= 0; --i) {
            long type = this.attributes.get((int)i).type;
            if ((mask & type) != type) continue;
            this.attributes.removeIndex(i);
            this.disable(type);
            this.sorted = false;
        }
        this.sort();
    }

    public final boolean has(long type) {
        return type != 0L && (this.mask & type) == type;
    }

    protected int indexOf(long type) {
        if (this.has(type)) {
            for (int i = 0; i < this.attributes.size; ++i) {
                if (this.attributes.get((int)i).type != type) continue;
                return i;
            }
        }
        return -1;
    }

    public final boolean same(Attributes other, boolean compareValues) {
        if (other == this) {
            return true;
        }
        if (other == null || this.mask != other.mask) {
            return false;
        }
        if (!compareValues) {
            return true;
        }
        this.sort();
        other.sort();
        for (int i = 0; i < this.attributes.size; ++i) {
            if (this.attributes.get(i).equals(other.attributes.get(i))) continue;
            return false;
        }
        return true;
    }

    public final boolean same(Attributes other) {
        return this.same(other, false);
    }

    @Override
    public final int compare(Attribute arg0, Attribute arg1) {
        return (int)(arg0.type - arg1.type);
    }

    @Override
    public final Iterator<Attribute> iterator() {
        return this.attributes.iterator();
    }

    public int attributesHash() {
        this.sort();
        int n = this.attributes.size;
        long result = 71L + this.mask;
        int m = 1;
        for (int i = 0; i < n; ++i) {
            m = m * 7 & 0xFFFF;
            result += this.mask * (long)this.attributes.get(i).hashCode() * (long)m;
        }
        return (int)(result ^ result >> 32);
    }

    public int hashCode() {
        return this.attributesHash();
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof Attributes)) {
            return false;
        }
        if (other == this) {
            return true;
        }
        return this.same((Attributes)other, true);
    }

    @Override
    public int compareTo(Attributes other) {
        if (other == this) {
            return 0;
        }
        if (this.mask != other.mask) {
            return this.mask < other.mask ? -1 : 1;
        }
        this.sort();
        other.sort();
        for (int i = 0; i < this.attributes.size; ++i) {
            int c = this.attributes.get(i).compareTo(other.attributes.get(i));
            if (c == 0) continue;
            return c < 0 ? -1 : (c > 0 ? 1 : 0);
        }
        return 0;
    }
}

