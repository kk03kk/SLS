/*
 * Decompiled with CFR 0.152.
 */
package com.esotericsoftware.spine.attachments;

import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.esotericsoftware.spine.Skin;
import com.esotericsoftware.spine.attachments.AttachmentLoader;
import com.esotericsoftware.spine.attachments.BoundingBoxAttachment;
import com.esotericsoftware.spine.attachments.MeshAttachment;
import com.esotericsoftware.spine.attachments.PathAttachment;
import com.esotericsoftware.spine.attachments.RegionAttachment;

public class AtlasAttachmentLoader
implements AttachmentLoader {
    private TextureAtlas atlas;

    public AtlasAttachmentLoader(TextureAtlas atlas) {
        if (atlas == null) {
            throw new IllegalArgumentException("atlas cannot be null.");
        }
        this.atlas = atlas;
    }

    @Override
    public RegionAttachment newRegionAttachment(Skin skin, String name, String path) {
        TextureAtlas.AtlasRegion region = this.atlas.findRegion(path);
        if (region == null) {
            throw new RuntimeException("Region not found in atlas: " + path + " (region attachment: " + name + ")");
        }
        RegionAttachment attachment = new RegionAttachment(name);
        attachment.setRegion(region);
        return attachment;
    }

    @Override
    public MeshAttachment newMeshAttachment(Skin skin, String name, String path) {
        TextureAtlas.AtlasRegion region = this.atlas.findRegion(path);
        if (region == null) {
            throw new RuntimeException("Region not found in atlas: " + path + " (mesh attachment: " + name + ")");
        }
        MeshAttachment attachment = new MeshAttachment(name);
        attachment.setRegion(region);
        return attachment;
    }

    @Override
    public BoundingBoxAttachment newBoundingBoxAttachment(Skin skin, String name) {
        return new BoundingBoxAttachment(name);
    }

    @Override
    public PathAttachment newPathAttachment(Skin skin, String name) {
        return new PathAttachment(name);
    }
}

