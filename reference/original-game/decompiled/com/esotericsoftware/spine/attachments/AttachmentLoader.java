/*
 * Decompiled with CFR 0.152.
 */
package com.esotericsoftware.spine.attachments;

import com.esotericsoftware.spine.Skin;
import com.esotericsoftware.spine.attachments.BoundingBoxAttachment;
import com.esotericsoftware.spine.attachments.MeshAttachment;
import com.esotericsoftware.spine.attachments.PathAttachment;
import com.esotericsoftware.spine.attachments.RegionAttachment;

public interface AttachmentLoader {
    public RegionAttachment newRegionAttachment(Skin var1, String var2, String var3);

    public MeshAttachment newMeshAttachment(Skin var1, String var2, String var3);

    public BoundingBoxAttachment newBoundingBoxAttachment(Skin var1, String var2);

    public PathAttachment newPathAttachment(Skin var1, String var2);
}

