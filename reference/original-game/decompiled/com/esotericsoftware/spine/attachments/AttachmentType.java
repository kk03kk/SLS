/*
 * Decompiled with CFR 0.152.
 */
package com.esotericsoftware.spine.attachments;

public enum AttachmentType {
    region,
    boundingbox,
    mesh,
    linkedmesh,
    path;

    public static AttachmentType[] values;

    static {
        values = AttachmentType.values();
    }
}

