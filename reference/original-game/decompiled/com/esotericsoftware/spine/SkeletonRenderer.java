/*
 * Decompiled with CFR 0.152.
 */
package com.esotericsoftware.spine;

import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.utils.Array;
import com.esotericsoftware.spine.BlendMode;
import com.esotericsoftware.spine.Bone;
import com.esotericsoftware.spine.Skeleton;
import com.esotericsoftware.spine.Slot;
import com.esotericsoftware.spine.attachments.Attachment;
import com.esotericsoftware.spine.attachments.MeshAttachment;
import com.esotericsoftware.spine.attachments.RegionAttachment;
import com.esotericsoftware.spine.attachments.SkeletonAttachment;

public class SkeletonRenderer<T extends Batch> {
    boolean premultipliedAlpha;

    public void draw(T batch, Skeleton skeleton) {
        boolean premultipliedAlpha = this.premultipliedAlpha;
        Array<Slot> drawOrder = skeleton.drawOrder;
        int n = drawOrder.size;
        for (int i = 0; i < n; ++i) {
            Skeleton attachmentSkeleton;
            Slot slot = drawOrder.get(i);
            Attachment attachment = slot.attachment;
            if (attachment instanceof RegionAttachment) {
                RegionAttachment regionAttachment = (RegionAttachment)attachment;
                float[] vertices = regionAttachment.updateWorldVertices(slot, premultipliedAlpha);
                BlendMode blendMode = slot.data.getBlendMode();
                batch.setBlendFunction(blendMode.getSource(premultipliedAlpha), blendMode.getDest());
                batch.draw(regionAttachment.getRegion().getTexture(), vertices, 0, 20);
                continue;
            }
            if (attachment instanceof MeshAttachment) {
                throw new RuntimeException("SkeletonMeshRenderer is required to render meshes.");
            }
            if (!(attachment instanceof SkeletonAttachment) || (attachmentSkeleton = ((SkeletonAttachment)attachment).getSkeleton()) == null) continue;
            Bone bone = slot.getBone();
            Bone rootBone = attachmentSkeleton.getRootBone();
            float oldScaleX = rootBone.getScaleX();
            float oldScaleY = rootBone.getScaleY();
            float oldRotation = rootBone.getRotation();
            attachmentSkeleton.setPosition(skeleton.getX() + bone.getWorldX(), skeleton.getY() + bone.getWorldY());
            rootBone.setRotation(oldRotation + bone.getWorldRotationX());
            attachmentSkeleton.updateWorldTransform();
            this.draw(batch, attachmentSkeleton);
            attachmentSkeleton.setX(0.0f);
            attachmentSkeleton.setY(0.0f);
            rootBone.setScaleX(oldScaleX);
            rootBone.setScaleY(oldScaleY);
            rootBone.setRotation(oldRotation);
        }
    }

    public void setPremultipliedAlpha(boolean premultipliedAlpha) {
        this.premultipliedAlpha = premultipliedAlpha;
    }
}

