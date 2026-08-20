/*
 * Decompiled with CFR 0.152.
 */
package com.badlogic.gdx.graphics;

import com.badlogic.gdx.graphics.VertexAttribute;
import com.badlogic.gdx.utils.GdxRuntimeException;
import java.util.Iterator;
import java.util.NoSuchElementException;

public final class VertexAttributes
implements Iterable<VertexAttribute>,
Comparable<VertexAttributes> {
    private final VertexAttribute[] attributes;
    public final int vertexSize;
    private long mask = -1L;
    private ReadonlyIterable<VertexAttribute> iterable;

    public VertexAttributes(VertexAttribute ... attributes) {
        if (attributes.length == 0) {
            throw new IllegalArgumentException("attributes must be >= 1");
        }
        VertexAttribute[] list = new VertexAttribute[attributes.length];
        for (int i = 0; i < attributes.length; ++i) {
            list[i] = attributes[i];
        }
        this.attributes = list;
        this.vertexSize = this.calculateOffsets();
    }

    public int getOffset(int usage, int defaultIfNotFound) {
        VertexAttribute vertexAttribute = this.findByUsage(usage);
        if (vertexAttribute == null) {
            return defaultIfNotFound;
        }
        return vertexAttribute.offset / 4;
    }

    public int getOffset(int usage) {
        return this.getOffset(usage, 0);
    }

    public VertexAttribute findByUsage(int usage) {
        int len = this.size();
        for (int i = 0; i < len; ++i) {
            if (this.get((int)i).usage != usage) continue;
            return this.get(i);
        }
        return null;
    }

    private int calculateOffsets() {
        int count = 0;
        for (int i = 0; i < this.attributes.length; ++i) {
            VertexAttribute attribute = this.attributes[i];
            attribute.offset = count;
            if (attribute.usage == 4) {
                count += 4;
                continue;
            }
            count += 4 * attribute.numComponents;
        }
        return count;
    }

    public int size() {
        return this.attributes.length;
    }

    public VertexAttribute get(int index) {
        return this.attributes[index];
    }

    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("[");
        for (int i = 0; i < this.attributes.length; ++i) {
            builder.append("(");
            builder.append(this.attributes[i].alias);
            builder.append(", ");
            builder.append(this.attributes[i].usage);
            builder.append(", ");
            builder.append(this.attributes[i].numComponents);
            builder.append(", ");
            builder.append(this.attributes[i].offset);
            builder.append(")");
            builder.append("\n");
        }
        builder.append("]");
        return builder.toString();
    }

    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof VertexAttributes)) {
            return false;
        }
        VertexAttributes other = (VertexAttributes)obj;
        if (this.attributes.length != other.attributes.length) {
            return false;
        }
        for (int i = 0; i < this.attributes.length; ++i) {
            if (this.attributes[i].equals(other.attributes[i])) continue;
            return false;
        }
        return true;
    }

    public int hashCode() {
        long result = 61 * this.attributes.length;
        for (int i = 0; i < this.attributes.length; ++i) {
            result = result * 61L + (long)this.attributes[i].hashCode();
        }
        return (int)(result ^ result >> 32);
    }

    public long getMask() {
        if (this.mask == -1L) {
            long result = 0L;
            for (int i = 0; i < this.attributes.length; ++i) {
                result |= (long)this.attributes[i].usage;
            }
            this.mask = result;
        }
        return this.mask;
    }

    @Override
    public int compareTo(VertexAttributes o) {
        long m2;
        if (this.attributes.length != o.attributes.length) {
            return this.attributes.length - o.attributes.length;
        }
        long m1 = this.getMask();
        if (m1 != (m2 = o.getMask())) {
            return m1 < m2 ? -1 : 1;
        }
        for (int i = this.attributes.length - 1; i >= 0; --i) {
            VertexAttribute va0 = this.attributes[i];
            VertexAttribute va1 = o.attributes[i];
            if (va0.usage != va1.usage) {
                return va0.usage - va1.usage;
            }
            if (va0.unit != va1.unit) {
                return va0.unit - va1.unit;
            }
            if (va0.numComponents != va1.numComponents) {
                return va0.numComponents - va1.numComponents;
            }
            if (va0.normalized != va1.normalized) {
                return va0.normalized ? 1 : -1;
            }
            if (va0.type == va1.type) continue;
            return va0.type - va1.type;
        }
        return 0;
    }

    @Override
    public Iterator<VertexAttribute> iterator() {
        if (this.iterable == null) {
            this.iterable = new ReadonlyIterable<VertexAttribute>(this.attributes);
        }
        return this.iterable.iterator();
    }

    private static class ReadonlyIterable<T>
    implements Iterable<T> {
        private final T[] array;
        private ReadonlyIterator iterator1;
        private ReadonlyIterator iterator2;

        public ReadonlyIterable(T[] array) {
            this.array = array;
        }

        @Override
        public Iterator<T> iterator() {
            if (this.iterator1 == null) {
                this.iterator1 = new ReadonlyIterator<T>(this.array);
                this.iterator2 = new ReadonlyIterator<T>(this.array);
            }
            if (!this.iterator1.valid) {
                this.iterator1.index = 0;
                this.iterator1.valid = true;
                this.iterator2.valid = false;
                return this.iterator1;
            }
            this.iterator2.index = 0;
            this.iterator2.valid = true;
            this.iterator1.valid = false;
            return this.iterator2;
        }
    }

    private static class ReadonlyIterator<T>
    implements Iterator<T>,
    Iterable<T> {
        private final T[] array;
        int index;
        boolean valid = true;

        public ReadonlyIterator(T[] array) {
            this.array = array;
        }

        @Override
        public boolean hasNext() {
            if (!this.valid) {
                throw new GdxRuntimeException("#iterator() cannot be used nested.");
            }
            return this.index < this.array.length;
        }

        @Override
        public T next() {
            if (this.index >= this.array.length) {
                throw new NoSuchElementException(String.valueOf(this.index));
            }
            if (!this.valid) {
                throw new GdxRuntimeException("#iterator() cannot be used nested.");
            }
            return this.array[this.index++];
        }

        @Override
        public void remove() {
            throw new GdxRuntimeException("Remove not allowed.");
        }

        public void reset() {
            this.index = 0;
        }

        @Override
        public Iterator<T> iterator() {
            return this;
        }
    }

    public static final class Usage {
        public static final int Position = 1;
        public static final int ColorUnpacked = 2;
        public static final int ColorPacked = 4;
        public static final int Normal = 8;
        public static final int TextureCoordinates = 16;
        public static final int Generic = 32;
        public static final int BoneWeight = 64;
        public static final int Tangent = 128;
        public static final int BiNormal = 256;
    }
}

