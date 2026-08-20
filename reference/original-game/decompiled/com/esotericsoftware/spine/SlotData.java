/*
 * Decompiled with CFR 0.152.
 */
package com.esotericsoftware.spine;

import com.badlogic.gdx.graphics.Color;
import com.esotericsoftware.spine.BlendMode;
import com.esotericsoftware.spine.BoneData;

public class SlotData {
    final int index;
    final String name;
    final BoneData boneData;
    final Color color = new Color(1.0f, 1.0f, 1.0f, 1.0f);
    String attachmentName;
    BlendMode blendMode;

    public SlotData(int index, String name, BoneData boneData) {
        if (index < 0) {
            throw new IllegalArgumentException("index must be >= 0.");
        }
        if (name == null) {
            throw new IllegalArgumentException("name cannot be null.");
        }
        if (boneData == null) {
            throw new IllegalArgumentException("boneData cannot be null.");
        }
        this.index = index;
        this.name = name;
        this.boneData = boneData;
    }

    public int getIndex() {
        return this.index;
    }

    public String getName() {
        return this.name;
    }

    public BoneData getBoneData() {
        return this.boneData;
    }

    public Color getColor() {
        return this.color;
    }

    public void setAttachmentName(String attachmentName) {
        this.attachmentName = attachmentName;
    }

    public String getAttachmentName() {
        return this.attachmentName;
    }

    public BlendMode getBlendMode() {
        return this.blendMode;
    }

    public void setBlendMode(BlendMode blendMode) {
        this.blendMode = blendMode;
    }

    public String toString() {
        return this.name;
    }
}

