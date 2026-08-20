/*
 * Decompiled with CFR 0.152.
 */
package com.esotericsoftware.spine;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.DataInput;
import com.badlogic.gdx.utils.FloatArray;
import com.badlogic.gdx.utils.IntArray;
import com.badlogic.gdx.utils.SerializationException;
import com.esotericsoftware.spine.Animation;
import com.esotericsoftware.spine.BlendMode;
import com.esotericsoftware.spine.BoneData;
import com.esotericsoftware.spine.Event;
import com.esotericsoftware.spine.EventData;
import com.esotericsoftware.spine.IkConstraintData;
import com.esotericsoftware.spine.PathConstraintData;
import com.esotericsoftware.spine.SkeletonData;
import com.esotericsoftware.spine.SkeletonJson;
import com.esotericsoftware.spine.Skin;
import com.esotericsoftware.spine.SlotData;
import com.esotericsoftware.spine.TransformConstraintData;
import com.esotericsoftware.spine.attachments.AtlasAttachmentLoader;
import com.esotericsoftware.spine.attachments.Attachment;
import com.esotericsoftware.spine.attachments.AttachmentLoader;
import com.esotericsoftware.spine.attachments.AttachmentType;
import com.esotericsoftware.spine.attachments.BoundingBoxAttachment;
import com.esotericsoftware.spine.attachments.MeshAttachment;
import com.esotericsoftware.spine.attachments.PathAttachment;
import com.esotericsoftware.spine.attachments.RegionAttachment;
import com.esotericsoftware.spine.attachments.VertexAttachment;
import java.io.EOFException;
import java.io.IOException;

public class SkeletonBinary {
    public static final int BONE_ROTATE = 0;
    public static final int BONE_TRANSLATE = 1;
    public static final int BONE_SCALE = 2;
    public static final int BONE_SHEAR = 3;
    public static final int SLOT_ATTACHMENT = 0;
    public static final int SLOT_COLOR = 1;
    public static final int PATH_POSITION = 0;
    public static final int PATH_SPACING = 1;
    public static final int PATH_MIX = 2;
    public static final int CURVE_LINEAR = 0;
    public static final int CURVE_STEPPED = 1;
    public static final int CURVE_BEZIER = 2;
    private static final Color tempColor = new Color();
    private final AttachmentLoader attachmentLoader;
    private float scale = 1.0f;
    private Array<SkeletonJson.LinkedMesh> linkedMeshes = new Array();

    public SkeletonBinary(TextureAtlas atlas) {
        this.attachmentLoader = new AtlasAttachmentLoader(atlas);
    }

    public SkeletonBinary(AttachmentLoader attachmentLoader) {
        if (attachmentLoader == null) {
            throw new IllegalArgumentException("attachmentLoader cannot be null.");
        }
        this.attachmentLoader = attachmentLoader;
    }

    public float getScale() {
        return this.scale;
    }

    public void setScale(float scale) {
        this.scale = scale;
    }

    public SkeletonData readSkeletonData(FileHandle file) {
        if (file == null) {
            throw new IllegalArgumentException("file cannot be null.");
        }
        float scale = this.scale;
        SkeletonData skeletonData = new SkeletonData();
        skeletonData.name = file.nameWithoutExtension();
        DataInput input = new DataInput(file.read(512)){
            private char[] chars;
            {
                this.chars = new char[32];
            }

            @Override
            public String readString() throws IOException {
                int byteCount = this.readInt(true);
                switch (byteCount) {
                    case 0: {
                        return null;
                    }
                    case 1: {
                        return "";
                    }
                }
                if (this.chars.length < --byteCount) {
                    this.chars = new char[byteCount];
                }
                char[] chars = this.chars;
                int charCount = 0;
                int i = 0;
                block9: while (i < byteCount) {
                    int b = this.read();
                    switch (b >> 4) {
                        case -1: {
                            throw new EOFException();
                        }
                        case 12: 
                        case 13: {
                            chars[charCount++] = (char)((b & 0x1F) << 6 | this.read() & 0x3F);
                            i += 2;
                            continue block9;
                        }
                        case 14: {
                            chars[charCount++] = (char)((b & 0xF) << 12 | (this.read() & 0x3F) << 6 | this.read() & 0x3F);
                            i += 3;
                            continue block9;
                        }
                    }
                    chars[charCount++] = (char)b;
                    ++i;
                }
                return new String(chars, 0, charCount);
            }
        };
        try {
            int i;
            Object data;
            Object data2;
            int i2;
            skeletonData.hash = input.readString();
            if (skeletonData.hash.isEmpty()) {
                skeletonData.hash = null;
            }
            skeletonData.version = input.readString();
            if (skeletonData.version.isEmpty()) {
                skeletonData.version = null;
            }
            skeletonData.width = input.readFloat();
            skeletonData.height = input.readFloat();
            boolean nonessential = input.readBoolean();
            if (nonessential) {
                skeletonData.imagesPath = input.readString();
                if (skeletonData.imagesPath.isEmpty()) {
                    skeletonData.imagesPath = null;
                }
            }
            int n = input.readInt(true);
            for (i2 = 0; i2 < n; ++i2) {
                String name = input.readString();
                BoneData parent = i2 == 0 ? null : skeletonData.bones.get(input.readInt(true));
                data2 = new BoneData(i2, name, parent);
                ((BoneData)data2).rotation = input.readFloat();
                ((BoneData)data2).x = input.readFloat() * scale;
                ((BoneData)data2).y = input.readFloat() * scale;
                ((BoneData)data2).scaleX = input.readFloat();
                ((BoneData)data2).scaleY = input.readFloat();
                ((BoneData)data2).shearX = input.readFloat();
                ((BoneData)data2).shearY = input.readFloat();
                ((BoneData)data2).length = input.readFloat() * scale;
                ((BoneData)data2).inheritRotation = input.readBoolean();
                ((BoneData)data2).inheritScale = input.readBoolean();
                if (nonessential) {
                    Color.rgba8888ToColor(((BoneData)data2).color, input.readInt());
                }
                skeletonData.bones.add((BoneData)data2);
            }
            n = input.readInt(true);
            for (i2 = 0; i2 < n; ++i2) {
                String slotName = input.readString();
                BoneData boneData = skeletonData.bones.get(input.readInt(true));
                data2 = new SlotData(i2, slotName, boneData);
                Color.rgba8888ToColor(((SlotData)data2).color, input.readInt());
                ((SlotData)data2).attachmentName = input.readString();
                ((SlotData)data2).blendMode = BlendMode.values[input.readInt(true)];
                skeletonData.slots.add((SlotData)data2);
            }
            n = input.readInt(true);
            for (i2 = 0; i2 < n; ++i2) {
                data = new IkConstraintData(input.readString());
                int nn = input.readInt(true);
                for (int ii = 0; ii < nn; ++ii) {
                    ((IkConstraintData)data).bones.add(skeletonData.bones.get(input.readInt(true)));
                }
                ((IkConstraintData)data).target = skeletonData.bones.get(input.readInt(true));
                ((IkConstraintData)data).mix = input.readFloat();
                ((IkConstraintData)data).bendDirection = input.readByte();
                skeletonData.ikConstraints.add((IkConstraintData)data);
            }
            n = input.readInt(true);
            for (i2 = 0; i2 < n; ++i2) {
                data = new TransformConstraintData(input.readString());
                int nn = input.readInt(true);
                for (int ii = 0; ii < nn; ++ii) {
                    ((TransformConstraintData)data).bones.add(skeletonData.bones.get(input.readInt(true)));
                }
                ((TransformConstraintData)data).target = skeletonData.bones.get(input.readInt(true));
                ((TransformConstraintData)data).offsetRotation = input.readFloat();
                ((TransformConstraintData)data).offsetX = input.readFloat() * scale;
                ((TransformConstraintData)data).offsetY = input.readFloat() * scale;
                ((TransformConstraintData)data).offsetScaleX = input.readFloat();
                ((TransformConstraintData)data).offsetScaleY = input.readFloat();
                ((TransformConstraintData)data).offsetShearY = input.readFloat();
                ((TransformConstraintData)data).rotateMix = input.readFloat();
                ((TransformConstraintData)data).translateMix = input.readFloat();
                ((TransformConstraintData)data).scaleMix = input.readFloat();
                ((TransformConstraintData)data).shearMix = input.readFloat();
                skeletonData.transformConstraints.add((TransformConstraintData)data);
            }
            n = input.readInt(true);
            for (i2 = 0; i2 < n; ++i2) {
                data = new PathConstraintData(input.readString());
                int nn = input.readInt(true);
                for (int ii = 0; ii < nn; ++ii) {
                    ((PathConstraintData)data).bones.add(skeletonData.bones.get(input.readInt(true)));
                }
                ((PathConstraintData)data).target = skeletonData.slots.get(input.readInt(true));
                ((PathConstraintData)data).positionMode = PathConstraintData.PositionMode.values[input.readInt(true)];
                ((PathConstraintData)data).spacingMode = PathConstraintData.SpacingMode.values[input.readInt(true)];
                ((PathConstraintData)data).rotateMode = PathConstraintData.RotateMode.values[input.readInt(true)];
                ((PathConstraintData)data).offsetRotation = input.readFloat();
                ((PathConstraintData)data).position = input.readFloat();
                if (((PathConstraintData)data).positionMode == PathConstraintData.PositionMode.fixed) {
                    ((PathConstraintData)data).position *= scale;
                }
                ((PathConstraintData)data).spacing = input.readFloat();
                if (((PathConstraintData)data).spacingMode == PathConstraintData.SpacingMode.length || ((PathConstraintData)data).spacingMode == PathConstraintData.SpacingMode.fixed) {
                    ((PathConstraintData)data).spacing *= scale;
                }
                ((PathConstraintData)data).rotateMix = input.readFloat();
                ((PathConstraintData)data).translateMix = input.readFloat();
                skeletonData.pathConstraints.add((PathConstraintData)data);
            }
            Skin defaultSkin = this.readSkin(input, "default", nonessential);
            if (defaultSkin != null) {
                skeletonData.defaultSkin = defaultSkin;
                skeletonData.skins.add(defaultSkin);
            }
            int n2 = input.readInt(true);
            for (i = 0; i < n2; ++i) {
                skeletonData.skins.add(this.readSkin(input, input.readString(), nonessential));
            }
            n2 = this.linkedMeshes.size;
            for (i = 0; i < n2; ++i) {
                Skin skin;
                SkeletonJson.LinkedMesh linkedMesh = this.linkedMeshes.get(i);
                Skin skin2 = skin = linkedMesh.skin == null ? skeletonData.getDefaultSkin() : skeletonData.findSkin(linkedMesh.skin);
                if (skin == null) {
                    throw new SerializationException("Skin not found: " + linkedMesh.skin);
                }
                Attachment parent = skin.getAttachment(linkedMesh.slotIndex, linkedMesh.parent);
                if (parent == null) {
                    throw new SerializationException("Parent mesh not found: " + linkedMesh.parent);
                }
                linkedMesh.mesh.setParentMesh((MeshAttachment)parent);
                linkedMesh.mesh.updateUVs();
            }
            this.linkedMeshes.clear();
            n2 = input.readInt(true);
            for (i = 0; i < n2; ++i) {
                EventData data3 = new EventData(input.readString());
                data3.intValue = input.readInt(false);
                data3.floatValue = input.readFloat();
                data3.stringValue = input.readString();
                skeletonData.events.add(data3);
            }
            n2 = input.readInt(true);
            for (i = 0; i < n2; ++i) {
                this.readAnimation(input.readString(), input, skeletonData);
            }
        }
        catch (IOException ex) {
            throw new SerializationException("Error reading skeleton file.", ex);
        }
        finally {
            try {
                input.close();
            }
            catch (IOException iOException) {}
        }
        skeletonData.bones.shrink();
        skeletonData.slots.shrink();
        skeletonData.skins.shrink();
        skeletonData.events.shrink();
        skeletonData.animations.shrink();
        skeletonData.ikConstraints.shrink();
        return skeletonData;
    }

    private Skin readSkin(DataInput input, String skinName, boolean nonessential) throws IOException {
        int slotCount = input.readInt(true);
        if (slotCount == 0) {
            return null;
        }
        Skin skin = new Skin(skinName);
        for (int i = 0; i < slotCount; ++i) {
            int slotIndex = input.readInt(true);
            int nn = input.readInt(true);
            for (int ii = 0; ii < nn; ++ii) {
                String name = input.readString();
                skin.addAttachment(slotIndex, name, this.readAttachment(input, skin, slotIndex, name, nonessential));
            }
        }
        return skin;
    }

    private Attachment readAttachment(DataInput input, Skin skin, int slotIndex, String attachmentName, boolean nonessential) throws IOException {
        float scale = this.scale;
        String name = input.readString();
        if (name == null) {
            name = attachmentName;
        }
        AttachmentType type = AttachmentType.values[input.readByte()];
        switch (type) {
            case region: {
                RegionAttachment region;
                String path = input.readString();
                float rotation = input.readFloat();
                float x = input.readFloat();
                float y = input.readFloat();
                float scaleX = input.readFloat();
                float scaleY = input.readFloat();
                float width = input.readFloat();
                float height = input.readFloat();
                int color = input.readInt();
                if (path == null) {
                    path = name;
                }
                if ((region = this.attachmentLoader.newRegionAttachment(skin, name, path)) == null) {
                    return null;
                }
                region.setPath(path);
                region.setX(x * scale);
                region.setY(y * scale);
                region.setScaleX(scaleX);
                region.setScaleY(scaleY);
                region.setRotation(rotation);
                region.setWidth(width * scale);
                region.setHeight(height * scale);
                Color.rgba8888ToColor(region.getColor(), color);
                region.updateOffset();
                return region;
            }
            case boundingbox: {
                int vertexCount = input.readInt(true);
                Vertices vertices = this.readVertices(input, vertexCount);
                int color = nonessential ? input.readInt() : 0;
                BoundingBoxAttachment box = this.attachmentLoader.newBoundingBoxAttachment(skin, name);
                if (box == null) {
                    return null;
                }
                box.setWorldVerticesLength(vertexCount << 1);
                box.setVertices(vertices.vertices);
                box.setBones(vertices.bones);
                if (nonessential) {
                    Color.rgba8888ToColor(box.getColor(), color);
                }
                return box;
            }
            case mesh: {
                MeshAttachment mesh;
                String path = input.readString();
                int color = input.readInt();
                int vertexCount = input.readInt(true);
                float[] uvs = this.readFloatArray(input, vertexCount << 1, 1.0f);
                short[] triangles = this.readShortArray(input);
                Vertices vertices = this.readVertices(input, vertexCount);
                int hullLength = input.readInt(true);
                short[] edges = null;
                float width = 0.0f;
                float height = 0.0f;
                if (nonessential) {
                    edges = this.readShortArray(input);
                    width = input.readFloat();
                    height = input.readFloat();
                }
                if (path == null) {
                    path = name;
                }
                if ((mesh = this.attachmentLoader.newMeshAttachment(skin, name, path)) == null) {
                    return null;
                }
                mesh.setPath(path);
                Color.rgba8888ToColor(mesh.getColor(), color);
                mesh.setBones(vertices.bones);
                mesh.setVertices(vertices.vertices);
                mesh.setWorldVerticesLength(vertexCount << 1);
                mesh.setTriangles(triangles);
                mesh.setRegionUVs(uvs);
                mesh.updateUVs();
                mesh.setHullLength(hullLength << 1);
                if (nonessential) {
                    mesh.setEdges(edges);
                    mesh.setWidth(width * scale);
                    mesh.setHeight(height * scale);
                }
                return mesh;
            }
            case linkedmesh: {
                MeshAttachment mesh;
                String path = input.readString();
                int color = input.readInt();
                String skinName = input.readString();
                String parent = input.readString();
                boolean inheritDeform = input.readBoolean();
                float width = 0.0f;
                float height = 0.0f;
                if (nonessential) {
                    width = input.readFloat();
                    height = input.readFloat();
                }
                if (path == null) {
                    path = name;
                }
                if ((mesh = this.attachmentLoader.newMeshAttachment(skin, name, path)) == null) {
                    return null;
                }
                mesh.setPath(path);
                Color.rgba8888ToColor(mesh.getColor(), color);
                mesh.setInheritDeform(inheritDeform);
                if (nonessential) {
                    mesh.setWidth(width * scale);
                    mesh.setHeight(height * scale);
                }
                this.linkedMeshes.add(new SkeletonJson.LinkedMesh(mesh, skinName, slotIndex, parent));
                return mesh;
            }
            case path: {
                boolean closed = input.readBoolean();
                boolean constantSpeed = input.readBoolean();
                int vertexCount = input.readInt(true);
                Vertices vertices = this.readVertices(input, vertexCount);
                float[] lengths = new float[vertexCount / 3];
                int n = lengths.length;
                for (int i = 0; i < n; ++i) {
                    lengths[i] = input.readFloat() * scale;
                }
                int color = nonessential ? input.readInt() : 0;
                PathAttachment path = this.attachmentLoader.newPathAttachment(skin, name);
                if (path == null) {
                    return null;
                }
                path.setClosed(closed);
                path.setConstantSpeed(constantSpeed);
                path.setWorldVerticesLength(vertexCount << 1);
                path.setVertices(vertices.vertices);
                path.setBones(vertices.bones);
                path.setLengths(lengths);
                if (nonessential) {
                    Color.rgba8888ToColor(path.getColor(), color);
                }
                return path;
            }
        }
        return null;
    }

    private Vertices readVertices(DataInput input, int vertexCount) throws IOException {
        int verticesLength = vertexCount << 1;
        Vertices vertices = new Vertices();
        if (!input.readBoolean()) {
            vertices.vertices = this.readFloatArray(input, verticesLength, this.scale);
            return vertices;
        }
        FloatArray weights = new FloatArray(verticesLength * 3 * 3);
        IntArray bonesArray = new IntArray(verticesLength * 3);
        for (int i = 0; i < vertexCount; ++i) {
            int boneCount = input.readInt(true);
            bonesArray.add(boneCount);
            for (int ii = 0; ii < boneCount; ++ii) {
                bonesArray.add(input.readInt(true));
                weights.add(input.readFloat() * this.scale);
                weights.add(input.readFloat() * this.scale);
                weights.add(input.readFloat());
            }
        }
        vertices.vertices = weights.toArray();
        vertices.bones = bonesArray.toArray();
        return vertices;
    }

    private float[] readFloatArray(DataInput input, int n, float scale) throws IOException {
        float[] array = new float[n];
        if (scale == 1.0f) {
            for (int i = 0; i < n; ++i) {
                array[i] = input.readFloat();
            }
        } else {
            for (int i = 0; i < n; ++i) {
                array[i] = input.readFloat() * scale;
            }
        }
        return array;
    }

    private short[] readShortArray(DataInput input) throws IOException {
        int n = input.readInt(true);
        short[] array = new short[n];
        for (int i = 0; i < n; ++i) {
            array[i] = input.readShort();
        }
        return array;
    }

    private void readAnimation(String name, DataInput input, SkeletonData skeletonData) {
        Array<Animation.Timeline> timelines = new Array<Animation.Timeline>();
        float scale = this.scale;
        float duration = 0.0f;
        try {
            int eventCount;
            int frameIndex;
            int frameCount;
            int index;
            Animation.Timeline timeline;
            int frameCount2;
            byte timelineType;
            int ii;
            int nn;
            int i;
            int n = input.readInt(true);
            for (i = 0; i < n; ++i) {
                int slotIndex = input.readInt(true);
                nn = input.readInt(true);
                block15: for (ii = 0; ii < nn; ++ii) {
                    timelineType = input.readByte();
                    frameCount2 = input.readInt(true);
                    switch (timelineType) {
                        case 1: {
                            int frameIndex2;
                            timeline = new Animation.ColorTimeline(frameCount2);
                            ((Animation.ColorTimeline)timeline).slotIndex = slotIndex;
                            for (frameIndex2 = 0; frameIndex2 < frameCount2; ++frameIndex2) {
                                float time = input.readFloat();
                                Color.rgba8888ToColor(tempColor, input.readInt());
                                ((Animation.ColorTimeline)timeline).setFrame(frameIndex2, time, SkeletonBinary.tempColor.r, SkeletonBinary.tempColor.g, SkeletonBinary.tempColor.b, SkeletonBinary.tempColor.a);
                                if (frameIndex2 >= frameCount2 - 1) continue;
                                this.readCurve(input, frameIndex2, (Animation.CurveTimeline)timeline);
                            }
                            timelines.add(timeline);
                            duration = Math.max(duration, ((Animation.ColorTimeline)timeline).getFrames()[(frameCount2 - 1) * 5]);
                            continue block15;
                        }
                        case 0: {
                            int frameIndex2;
                            timeline = new Animation.AttachmentTimeline(frameCount2);
                            ((Animation.AttachmentTimeline)timeline).slotIndex = slotIndex;
                            for (frameIndex2 = 0; frameIndex2 < frameCount2; ++frameIndex2) {
                                ((Animation.AttachmentTimeline)timeline).setFrame(frameIndex2, input.readFloat(), input.readString());
                            }
                            timelines.add(timeline);
                            duration = Math.max(duration, ((Animation.AttachmentTimeline)timeline).getFrames()[frameCount2 - 1]);
                        }
                    }
                }
            }
            n = input.readInt(true);
            for (i = 0; i < n; ++i) {
                int boneIndex = input.readInt(true);
                nn = input.readInt(true);
                block19: for (ii = 0; ii < nn; ++ii) {
                    timelineType = input.readByte();
                    frameCount2 = input.readInt(true);
                    switch (timelineType) {
                        case 0: {
                            timeline = new Animation.RotateTimeline(frameCount2);
                            ((Animation.RotateTimeline)timeline).boneIndex = boneIndex;
                            for (int frameIndex3 = 0; frameIndex3 < frameCount2; ++frameIndex3) {
                                ((Animation.RotateTimeline)timeline).setFrame(frameIndex3, input.readFloat(), input.readFloat());
                                if (frameIndex3 >= frameCount2 - 1) continue;
                                this.readCurve(input, frameIndex3, (Animation.CurveTimeline)timeline);
                            }
                            timelines.add(timeline);
                            duration = Math.max(duration, ((Animation.RotateTimeline)timeline).getFrames()[(frameCount2 - 1) * 2]);
                            continue block19;
                        }
                        case 1: 
                        case 2: 
                        case 3: {
                            float timelineScale = 1.0f;
                            if (timelineType == 2) {
                                timeline = new Animation.ScaleTimeline(frameCount2);
                            } else if (timelineType == 3) {
                                timeline = new Animation.ShearTimeline(frameCount2);
                            } else {
                                timeline = new Animation.TranslateTimeline(frameCount2);
                                timelineScale = scale;
                            }
                            ((Animation.TranslateTimeline)timeline).boneIndex = boneIndex;
                            for (int frameIndex4 = 0; frameIndex4 < frameCount2; ++frameIndex4) {
                                ((Animation.TranslateTimeline)timeline).setFrame(frameIndex4, input.readFloat(), input.readFloat() * timelineScale, input.readFloat() * timelineScale);
                                if (frameIndex4 >= frameCount2 - 1) continue;
                                this.readCurve(input, frameIndex4, (Animation.CurveTimeline)timeline);
                            }
                            timelines.add(timeline);
                            duration = Math.max(duration, ((Animation.TranslateTimeline)timeline).getFrames()[(frameCount2 - 1) * 3]);
                            continue block19;
                        }
                    }
                }
            }
            n = input.readInt(true);
            for (i = 0; i < n; ++i) {
                index = input.readInt(true);
                frameCount = input.readInt(true);
                Animation.IkConstraintTimeline timeline2 = new Animation.IkConstraintTimeline(frameCount);
                timeline2.ikConstraintIndex = index;
                for (frameIndex = 0; frameIndex < frameCount; ++frameIndex) {
                    timeline2.setFrame(frameIndex, input.readFloat(), input.readFloat(), input.readByte());
                    if (frameIndex >= frameCount - 1) continue;
                    this.readCurve(input, frameIndex, timeline2);
                }
                timelines.add(timeline2);
                duration = Math.max(duration, timeline2.getFrames()[(frameCount - 1) * 3]);
            }
            n = input.readInt(true);
            for (i = 0; i < n; ++i) {
                index = input.readInt(true);
                frameCount = input.readInt(true);
                Animation.TransformConstraintTimeline timeline3 = new Animation.TransformConstraintTimeline(frameCount);
                timeline3.transformConstraintIndex = index;
                for (frameIndex = 0; frameIndex < frameCount; ++frameIndex) {
                    timeline3.setFrame(frameIndex, input.readFloat(), input.readFloat(), input.readFloat(), input.readFloat(), input.readFloat());
                    if (frameIndex >= frameCount - 1) continue;
                    this.readCurve(input, frameIndex, timeline3);
                }
                timelines.add(timeline3);
                duration = Math.max(duration, timeline3.getFrames()[(frameCount - 1) * 5]);
            }
            n = input.readInt(true);
            for (i = 0; i < n; ++i) {
                index = input.readInt(true);
                PathConstraintData data = skeletonData.getPathConstraints().get(index);
                int nn2 = input.readInt(true);
                block27: for (int ii2 = 0; ii2 < nn2; ++ii2) {
                    byte timelineType2 = input.readByte();
                    int frameCount3 = input.readInt(true);
                    switch (timelineType2) {
                        case 0: 
                        case 1: {
                            Animation.CurveTimeline timeline4;
                            float timelineScale = 1.0f;
                            if (timelineType2 == 1) {
                                timeline4 = new Animation.PathConstraintSpacingTimeline(frameCount3);
                                if (data.spacingMode == PathConstraintData.SpacingMode.length || data.spacingMode == PathConstraintData.SpacingMode.fixed) {
                                    timelineScale = scale;
                                }
                            } else {
                                timeline4 = new Animation.PathConstraintPositionTimeline(frameCount3);
                                if (data.positionMode == PathConstraintData.PositionMode.fixed) {
                                    timelineScale = scale;
                                }
                            }
                            ((Animation.PathConstraintPositionTimeline)timeline4).pathConstraintIndex = index;
                            for (int frameIndex5 = 0; frameIndex5 < frameCount3; ++frameIndex5) {
                                ((Animation.PathConstraintPositionTimeline)timeline4).setFrame(frameIndex5, input.readFloat(), input.readFloat() * timelineScale);
                                if (frameIndex5 >= frameCount3 - 1) continue;
                                this.readCurve(input, frameIndex5, timeline4);
                            }
                            timelines.add(timeline4);
                            duration = Math.max(duration, ((Animation.PathConstraintPositionTimeline)timeline4).getFrames()[(frameCount3 - 1) * 2]);
                            continue block27;
                        }
                        case 2: {
                            Animation.CurveTimeline timeline4 = new Animation.PathConstraintMixTimeline(frameCount3);
                            ((Animation.PathConstraintMixTimeline)timeline4).pathConstraintIndex = index;
                            for (int frameIndex6 = 0; frameIndex6 < frameCount3; ++frameIndex6) {
                                ((Animation.PathConstraintMixTimeline)timeline4).setFrame(frameIndex6, input.readFloat(), input.readFloat(), input.readFloat());
                                if (frameIndex6 >= frameCount3 - 1) continue;
                                this.readCurve(input, frameIndex6, timeline4);
                            }
                            timelines.add(timeline4);
                            duration = Math.max(duration, ((Animation.PathConstraintMixTimeline)timeline4).getFrames()[(frameCount3 - 1) * 3]);
                            continue block27;
                        }
                    }
                }
            }
            n = input.readInt(true);
            for (i = 0; i < n; ++i) {
                Skin skin = skeletonData.skins.get(input.readInt(true));
                int nn3 = input.readInt(true);
                for (int ii3 = 0; ii3 < nn3; ++ii3) {
                    int slotIndex = input.readInt(true);
                    int nnn = input.readInt(true);
                    for (int iii = 0; iii < nnn; ++iii) {
                        VertexAttachment attachment = (VertexAttachment)skin.getAttachment(slotIndex, input.readString());
                        boolean weighted = attachment.getBones() != null;
                        float[] vertices = attachment.getVertices();
                        int deformLength = weighted ? vertices.length / 3 * 2 : vertices.length;
                        int frameCount4 = input.readInt(true);
                        Animation.DeformTimeline timeline5 = new Animation.DeformTimeline(frameCount4);
                        timeline5.slotIndex = slotIndex;
                        timeline5.attachment = attachment;
                        for (int frameIndex7 = 0; frameIndex7 < frameCount4; ++frameIndex7) {
                            float[] deform;
                            float time = input.readFloat();
                            int end = input.readInt(true);
                            if (end == 0) {
                                deform = weighted ? new float[deformLength] : vertices;
                            } else {
                                int v;
                                deform = new float[deformLength];
                                int start = input.readInt(true);
                                end += start;
                                if (scale == 1.0f) {
                                    for (v = start; v < end; ++v) {
                                        deform[v] = input.readFloat();
                                    }
                                } else {
                                    for (v = start; v < end; ++v) {
                                        deform[v] = input.readFloat() * scale;
                                    }
                                }
                                if (!weighted) {
                                    int vn = deform.length;
                                    for (v = 0; v < vn; ++v) {
                                        int n2 = v;
                                        deform[n2] = deform[n2] + vertices[v];
                                    }
                                }
                            }
                            timeline5.setFrame(frameIndex7, time, deform);
                            if (frameIndex7 >= frameCount4 - 1) continue;
                            this.readCurve(input, frameIndex7, timeline5);
                        }
                        timelines.add(timeline5);
                        duration = Math.max(duration, timeline5.getFrames()[frameCount4 - 1]);
                    }
                }
            }
            int drawOrderCount = input.readInt(true);
            if (drawOrderCount > 0) {
                Animation.DrawOrderTimeline timeline6 = new Animation.DrawOrderTimeline(drawOrderCount);
                int slotCount = skeletonData.slots.size;
                for (int i2 = 0; i2 < drawOrderCount; ++i2) {
                    int ii4;
                    float time = input.readFloat();
                    int offsetCount = input.readInt(true);
                    int[] drawOrder = new int[slotCount];
                    for (int ii5 = slotCount - 1; ii5 >= 0; --ii5) {
                        drawOrder[ii5] = -1;
                    }
                    int[] unchanged = new int[slotCount - offsetCount];
                    int originalIndex = 0;
                    int unchangedIndex = 0;
                    for (ii4 = 0; ii4 < offsetCount; ++ii4) {
                        int slotIndex = input.readInt(true);
                        while (originalIndex != slotIndex) {
                            unchanged[unchangedIndex++] = originalIndex++;
                        }
                        drawOrder[originalIndex + input.readInt((boolean)true)] = originalIndex++;
                    }
                    while (originalIndex < slotCount) {
                        unchanged[unchangedIndex++] = originalIndex++;
                    }
                    for (ii4 = slotCount - 1; ii4 >= 0; --ii4) {
                        if (drawOrder[ii4] != -1) continue;
                        drawOrder[ii4] = unchanged[--unchangedIndex];
                    }
                    timeline6.setFrame(i2, time, drawOrder);
                }
                timelines.add(timeline6);
                duration = Math.max(duration, timeline6.getFrames()[drawOrderCount - 1]);
            }
            if ((eventCount = input.readInt(true)) > 0) {
                Animation.EventTimeline timeline7 = new Animation.EventTimeline(eventCount);
                for (int i3 = 0; i3 < eventCount; ++i3) {
                    float time = input.readFloat();
                    EventData eventData = skeletonData.events.get(input.readInt(true));
                    Event event = new Event(time, eventData);
                    event.intValue = input.readInt(false);
                    event.floatValue = input.readFloat();
                    event.stringValue = input.readBoolean() ? input.readString() : eventData.stringValue;
                    timeline7.setFrame(i3, event);
                }
                timelines.add(timeline7);
                duration = Math.max(duration, timeline7.getFrames()[eventCount - 1]);
            }
        }
        catch (IOException ex) {
            throw new SerializationException("Error reading skeleton file.", ex);
        }
        timelines.shrink();
        skeletonData.animations.add(new Animation(name, timelines, duration));
    }

    private void readCurve(DataInput input, int frameIndex, Animation.CurveTimeline timeline) throws IOException {
        switch (input.readByte()) {
            case 1: {
                timeline.setStepped(frameIndex);
                break;
            }
            case 2: {
                this.setCurve(timeline, frameIndex, input.readFloat(), input.readFloat(), input.readFloat(), input.readFloat());
            }
        }
    }

    void setCurve(Animation.CurveTimeline timeline, int frameIndex, float cx1, float cy1, float cx2, float cy2) {
        timeline.setCurve(frameIndex, cx1, cy1, cx2, cy2);
    }

    static class Vertices {
        int[] bones;
        float[] vertices;

        Vertices() {
        }
    }
}

