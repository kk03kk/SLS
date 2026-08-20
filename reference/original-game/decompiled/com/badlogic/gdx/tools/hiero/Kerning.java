/*
 * Decompiled with CFR 0.152.
 */
package com.badlogic.gdx.tools.hiero;

import com.badlogic.gdx.utils.IntArray;
import com.badlogic.gdx.utils.IntIntMap;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;

class Kerning {
    private TTFInputStream input;
    private float scale;
    private int headOffset = -1;
    private int kernOffset = -1;
    private int gposOffset = -1;
    private IntIntMap kernings = new IntIntMap();

    Kerning() {
    }

    public void load(InputStream inputStream, int fontSize) throws IOException {
        if (inputStream == null) {
            throw new IllegalArgumentException("inputStream cannot be null.");
        }
        this.input = new TTFInputStream(inputStream);
        inputStream.close();
        this.readTableDirectory();
        if (this.headOffset == -1) {
            throw new IOException("HEAD table not found.");
        }
        this.readHEAD(fontSize);
        if (this.gposOffset != -1) {
            this.input.seek(this.gposOffset);
            this.readGPOS();
        }
        if (this.kernOffset != -1) {
            this.input.seek(this.kernOffset);
            this.readKERN();
        }
        this.input.close();
        this.input = null;
    }

    public IntIntMap getKernings() {
        return this.kernings;
    }

    private void storeKerningOffset(int firstGlyphCode, int secondGlyphCode, int offset) {
        int value = Math.round((float)offset * this.scale);
        if (value == 0) {
            return;
        }
        int key = firstGlyphCode << 16 | secondGlyphCode;
        this.kernings.put(key, value);
    }

    private void readTableDirectory() throws IOException {
        this.input.skip(4L);
        int tableCount = this.input.readUnsignedShort();
        this.input.skip(6L);
        byte[] tagBytes = new byte[4];
        for (int i = 0; i < tableCount; ++i) {
            tagBytes[0] = this.input.readByte();
            tagBytes[1] = this.input.readByte();
            tagBytes[2] = this.input.readByte();
            tagBytes[3] = this.input.readByte();
            this.input.skip(4L);
            int offset = (int)this.input.readUnsignedLong();
            this.input.skip(4L);
            String tag = new String(tagBytes, "ISO-8859-1");
            if (tag.equals("head")) {
                this.headOffset = offset;
                continue;
            }
            if (tag.equals("kern")) {
                this.kernOffset = offset;
                continue;
            }
            if (!tag.equals("GPOS")) continue;
            this.gposOffset = offset;
        }
    }

    private void readHEAD(int fontSize) throws IOException {
        this.input.seek(this.headOffset + 8 + 8 + 2);
        int unitsPerEm = this.input.readUnsignedShort();
        this.scale = (float)fontSize / (float)unitsPerEm;
    }

    private void readKERN() throws IOException {
        this.input.seek(this.kernOffset + 2);
        for (int subTableCount = this.input.readUnsignedShort(); subTableCount > 0; --subTableCount) {
            this.input.skip(4L);
            int tupleIndex = this.input.readUnsignedShort();
            if ((tupleIndex & 1) == 0 || (tupleIndex & 2) != 0 || (tupleIndex & 4) != 0) {
                return;
            }
            if (tupleIndex >> 8 != 0) continue;
            int kerningCount = this.input.readUnsignedShort();
            this.input.skip(6L);
            while (kerningCount-- > 0) {
                int firstGlyphCode = this.input.readUnsignedShort();
                int secondGlyphCode = this.input.readUnsignedShort();
                short offset = this.input.readShort();
                this.storeKerningOffset(firstGlyphCode, secondGlyphCode, offset);
            }
        }
    }

    private void readGPOS() throws IOException {
        this.input.seek(this.gposOffset + 4 + 2 + 2);
        int lookupListOffset = this.input.readUnsignedShort();
        this.input.seek(this.gposOffset + lookupListOffset);
        int lookupListPosition = this.input.getPosition();
        int lookupCount = this.input.readUnsignedShort();
        int[] lookupOffsets = this.input.readUnsignedShortArray(lookupCount);
        for (int i = 0; i < lookupCount; ++i) {
            int lookupPosition = lookupListPosition + lookupOffsets[i];
            this.input.seek(lookupPosition);
            int type = this.input.readUnsignedShort();
            this.readSubtables(type, lookupPosition);
        }
    }

    private void readSubtables(int type, int lookupPosition) throws IOException {
        this.input.skip(2L);
        int subTableCount = this.input.readUnsignedShort();
        int[] subTableOffsets = this.input.readUnsignedShortArray(subTableCount);
        for (int i = 0; i < subTableCount; ++i) {
            int subTablePosition = lookupPosition + subTableOffsets[i];
            this.readSubtable(type, subTablePosition);
        }
    }

    private void readSubtable(int type, int subTablePosition) throws IOException {
        this.input.seek(subTablePosition);
        if (type == 2) {
            this.readPairAdjustmentSubtable(subTablePosition);
        } else if (type == 9) {
            this.readExtensionPositioningSubtable(subTablePosition);
        }
    }

    private void readPairAdjustmentSubtable(int subTablePosition) throws IOException {
        int type = this.input.readUnsignedShort();
        if (type == 1) {
            this.readPairPositioningAdjustmentFormat1(subTablePosition);
        } else if (type == 2) {
            this.readPairPositioningAdjustmentFormat2(subTablePosition);
        }
    }

    private void readExtensionPositioningSubtable(int subTablePosition) throws IOException {
        int type = this.input.readUnsignedShort();
        if (type == 1) {
            this.readExtensionPositioningFormat1(subTablePosition);
        }
    }

    private void readPairPositioningAdjustmentFormat1(long subTablePosition) throws IOException {
        int coverageOffset = this.input.readUnsignedShort();
        int valueFormat1 = this.input.readUnsignedShort();
        int valueFormat2 = this.input.readUnsignedShort();
        int pairSetCount = this.input.readUnsignedShort();
        int[] pairSetOffsets = this.input.readUnsignedShortArray(pairSetCount);
        this.input.seek((int)(subTablePosition + (long)coverageOffset));
        int[] coverage = this.readCoverageTable();
        pairSetCount = Math.min(pairSetCount, coverage.length);
        for (int i = 0; i < pairSetCount; ++i) {
            int firstGlyph = coverage[i];
            this.input.seek((int)(subTablePosition + (long)pairSetOffsets[i]));
            int pairValueCount = this.input.readUnsignedShort();
            for (int j = 0; j < pairValueCount; ++j) {
                int secondGlyph = this.input.readUnsignedShort();
                int xAdvance1 = this.readXAdvanceFromValueRecord(valueFormat1);
                this.readXAdvanceFromValueRecord(valueFormat2);
                if (xAdvance1 == 0) continue;
                this.storeKerningOffset(firstGlyph, secondGlyph, xAdvance1);
            }
        }
    }

    private void readPairPositioningAdjustmentFormat2(int subTablePosition) throws IOException {
        int i;
        int coverageOffset = this.input.readUnsignedShort();
        int valueFormat1 = this.input.readUnsignedShort();
        int valueFormat2 = this.input.readUnsignedShort();
        int classDefOffset1 = this.input.readUnsignedShort();
        int classDefOffset2 = this.input.readUnsignedShort();
        int class1Count = this.input.readUnsignedShort();
        int class2Count = this.input.readUnsignedShort();
        int position = this.input.getPosition();
        this.input.seek(subTablePosition + coverageOffset);
        int[] coverage = this.readCoverageTable();
        this.input.seek(position);
        IntArray[] glyphsByClass1 = this.readClassDefinition(subTablePosition + classDefOffset1, class1Count);
        IntArray[] glyphsByClass2 = this.readClassDefinition(subTablePosition + classDefOffset2, class2Count);
        this.input.seek(position);
        for (i = 0; i < coverage.length; ++i) {
            int glyph = coverage[i];
            boolean found = false;
            for (int j = 1; j < class1Count && !found; ++j) {
                found = glyphsByClass1[j].contains(glyph);
            }
            if (found) continue;
            glyphsByClass1[0].add(glyph);
        }
        for (i = 0; i < class1Count; ++i) {
            for (int j = 0; j < class2Count; ++j) {
                int xAdvance1 = this.readXAdvanceFromValueRecord(valueFormat1);
                this.readXAdvanceFromValueRecord(valueFormat2);
                if (xAdvance1 == 0) continue;
                for (int k = 0; k < glyphsByClass1[i].size; ++k) {
                    int glyph1 = glyphsByClass1[i].items[k];
                    for (int l = 0; l < glyphsByClass2[j].size; ++l) {
                        int glyph2 = glyphsByClass2[j].items[l];
                        this.storeKerningOffset(glyph1, glyph2, xAdvance1);
                    }
                }
            }
        }
    }

    private void readExtensionPositioningFormat1(int subTablePosition) throws IOException {
        int lookupType = this.input.readUnsignedShort();
        int lookupPosition = subTablePosition + (int)this.input.readUnsignedLong();
        this.readSubtable(lookupType, lookupPosition);
    }

    private IntArray[] readClassDefinition(int position, int classCount) throws IOException {
        this.input.seek(position);
        IntArray[] glyphsByClass = new IntArray[classCount];
        for (int i = 0; i < classCount; ++i) {
            glyphsByClass[i] = new IntArray();
        }
        int classFormat = this.input.readUnsignedShort();
        if (classFormat == 1) {
            this.readClassDefinitionFormat1(glyphsByClass);
        } else if (classFormat == 2) {
            this.readClassDefinitionFormat2(glyphsByClass);
        } else {
            throw new IOException("Unknown class definition table type " + classFormat);
        }
        return glyphsByClass;
    }

    private void readClassDefinitionFormat1(IntArray[] glyphsByClass) throws IOException {
        int startGlyph = this.input.readUnsignedShort();
        int glyphCount = this.input.readUnsignedShort();
        int[] classValueArray = this.input.readUnsignedShortArray(glyphCount);
        for (int i = 0; i < glyphCount; ++i) {
            int glyph = startGlyph + i;
            int glyphClass = classValueArray[i];
            if (glyphClass >= glyphsByClass.length) continue;
            glyphsByClass[glyphClass].add(glyph);
        }
    }

    private void readClassDefinitionFormat2(IntArray[] glyphsByClass) throws IOException {
        int classRangeCount = this.input.readUnsignedShort();
        for (int i = 0; i < classRangeCount; ++i) {
            int start = this.input.readUnsignedShort();
            int end = this.input.readUnsignedShort();
            int glyphClass = this.input.readUnsignedShort();
            if (glyphClass >= glyphsByClass.length) continue;
            for (int glyph = start; glyph <= end; ++glyph) {
                glyphsByClass[glyphClass].add(glyph);
            }
        }
    }

    private int[] readCoverageTable() throws IOException {
        int format = this.input.readUnsignedShort();
        if (format == 1) {
            int glyphCount = this.input.readUnsignedShort();
            int[] glyphArray = this.input.readUnsignedShortArray(glyphCount);
            return glyphArray;
        }
        if (format == 2) {
            int rangeCount = this.input.readUnsignedShort();
            IntArray glyphArray = new IntArray();
            for (int i = 0; i < rangeCount; ++i) {
                int start = this.input.readUnsignedShort();
                int end = this.input.readUnsignedShort();
                this.input.skip(2L);
                for (int glyph = start; glyph <= end; ++glyph) {
                    glyphArray.add(glyph);
                }
            }
            return glyphArray.shrink();
        }
        throw new IOException("Unknown coverage table format " + format);
    }

    private int readXAdvanceFromValueRecord(int valueFormat) throws IOException {
        int xAdvance = 0;
        for (int mask = 1; mask <= 32768 && mask <= valueFormat; mask <<= 1) {
            if ((valueFormat & mask) == 0) continue;
            short value = this.input.readShort();
            if (mask != 4) continue;
            xAdvance = value;
        }
        return xAdvance;
    }

    private static class TTFInputStream
    extends ByteArrayInputStream {
        public TTFInputStream(InputStream input) throws IOException {
            super(TTFInputStream.readAllBytes(input));
        }

        private static byte[] readAllBytes(InputStream input) throws IOException {
            int numRead;
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buffer = new byte[16384];
            while ((numRead = input.read(buffer, 0, buffer.length)) != -1) {
                out.write(buffer, 0, numRead);
            }
            return out.toByteArray();
        }

        public int getPosition() {
            return this.pos;
        }

        public void seek(int position) {
            this.pos = position;
        }

        public int readUnsignedByte() throws IOException {
            int b = this.read();
            if (b == -1) {
                throw new EOFException("Unexpected end of file.");
            }
            return b;
        }

        public byte readByte() throws IOException {
            return (byte)this.readUnsignedByte();
        }

        public int readUnsignedShort() throws IOException {
            return (this.readUnsignedByte() << 8) + this.readUnsignedByte();
        }

        public short readShort() throws IOException {
            return (short)this.readUnsignedShort();
        }

        public long readUnsignedLong() throws IOException {
            long value = this.readUnsignedByte();
            value = (value << 8) + (long)this.readUnsignedByte();
            value = (value << 8) + (long)this.readUnsignedByte();
            value = (value << 8) + (long)this.readUnsignedByte();
            return value;
        }

        public int[] readUnsignedShortArray(int count) throws IOException {
            int[] shorts = new int[count];
            for (int i = 0; i < count; ++i) {
                shorts[i] = this.readUnsignedShort();
            }
            return shorts;
        }
    }
}

