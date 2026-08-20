/*
 * Decompiled with CFR 0.152.
 */
package com.badlogic.gdx.graphics.g3d.loader;

import com.badlogic.gdx.assets.loaders.FileHandleResolver;
import com.badlogic.gdx.assets.loaders.ModelLoader;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.VertexAttribute;
import com.badlogic.gdx.graphics.g3d.model.data.ModelAnimation;
import com.badlogic.gdx.graphics.g3d.model.data.ModelData;
import com.badlogic.gdx.graphics.g3d.model.data.ModelMaterial;
import com.badlogic.gdx.graphics.g3d.model.data.ModelMesh;
import com.badlogic.gdx.graphics.g3d.model.data.ModelMeshPart;
import com.badlogic.gdx.graphics.g3d.model.data.ModelNode;
import com.badlogic.gdx.graphics.g3d.model.data.ModelNodeAnimation;
import com.badlogic.gdx.graphics.g3d.model.data.ModelNodeKeyframe;
import com.badlogic.gdx.graphics.g3d.model.data.ModelNodePart;
import com.badlogic.gdx.graphics.g3d.model.data.ModelTexture;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Quaternion;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ArrayMap;
import com.badlogic.gdx.utils.BaseJsonReader;
import com.badlogic.gdx.utils.GdxRuntimeException;
import com.badlogic.gdx.utils.JsonValue;

public class G3dModelLoader
extends ModelLoader<ModelLoader.ModelParameters> {
    public static final short VERSION_HI = 0;
    public static final short VERSION_LO = 1;
    protected final BaseJsonReader reader;
    private final Quaternion tempQ = new Quaternion();

    public G3dModelLoader(BaseJsonReader reader) {
        this(reader, null);
    }

    public G3dModelLoader(BaseJsonReader reader, FileHandleResolver resolver) {
        super(resolver);
        this.reader = reader;
    }

    @Override
    public ModelData loadModelData(FileHandle fileHandle, ModelLoader.ModelParameters parameters) {
        return this.parseModel(fileHandle);
    }

    public ModelData parseModel(FileHandle handle) {
        JsonValue json = this.reader.parse(handle);
        ModelData model = new ModelData();
        JsonValue version = json.require("version");
        model.version[0] = version.getShort(0);
        model.version[1] = version.getShort(1);
        if (model.version[0] != 0 || model.version[1] != 1) {
            throw new GdxRuntimeException("Model version not supported");
        }
        model.id = json.getString("id", "");
        this.parseMeshes(model, json);
        this.parseMaterials(model, json, handle.parent().path());
        this.parseNodes(model, json);
        this.parseAnimations(model, json);
        return model;
    }

    private void parseMeshes(ModelData model, JsonValue json) {
        JsonValue meshes = json.get("meshes");
        if (meshes != null) {
            model.meshes.ensureCapacity(meshes.size);
            JsonValue mesh = meshes.child;
            while (mesh != null) {
                String id;
                ModelMesh jsonMesh = new ModelMesh();
                jsonMesh.id = id = mesh.getString("id", "");
                JsonValue attributes = mesh.require("attributes");
                jsonMesh.attributes = this.parseAttributes(attributes);
                jsonMesh.vertices = mesh.require("vertices").asFloatArray();
                JsonValue meshParts = mesh.require("parts");
                Array<ModelMeshPart> parts = new Array<ModelMeshPart>();
                JsonValue meshPart = meshParts.child;
                while (meshPart != null) {
                    ModelMeshPart jsonPart = new ModelMeshPart();
                    String partId = meshPart.getString("id", null);
                    if (partId == null) {
                        throw new GdxRuntimeException("Not id given for mesh part");
                    }
                    for (ModelMeshPart other : parts) {
                        if (!other.id.equals(partId)) continue;
                        throw new GdxRuntimeException("Mesh part with id '" + partId + "' already in defined");
                    }
                    jsonPart.id = partId;
                    String type = meshPart.getString("type", null);
                    if (type == null) {
                        throw new GdxRuntimeException("No primitive type given for mesh part '" + partId + "'");
                    }
                    jsonPart.primitiveType = this.parseType(type);
                    jsonPart.indices = meshPart.require("indices").asShortArray();
                    parts.add(jsonPart);
                    meshPart = meshPart.next;
                }
                jsonMesh.parts = (ModelMeshPart[])parts.toArray(ModelMeshPart.class);
                model.meshes.add(jsonMesh);
                mesh = mesh.next;
            }
        }
    }

    private int parseType(String type) {
        if (type.equals("TRIANGLES")) {
            return 4;
        }
        if (type.equals("LINES")) {
            return 1;
        }
        if (type.equals("POINTS")) {
            return 0;
        }
        if (type.equals("TRIANGLE_STRIP")) {
            return 5;
        }
        if (type.equals("LINE_STRIP")) {
            return 3;
        }
        throw new GdxRuntimeException("Unknown primitive type '" + type + "', should be one of triangle, trianglestrip, line, linestrip, lineloop or point");
    }

    private VertexAttribute[] parseAttributes(JsonValue attributes) {
        Array<VertexAttribute> vertexAttributes = new Array<VertexAttribute>();
        int unit = 0;
        int blendWeightCount = 0;
        JsonValue value = attributes.child;
        while (value != null) {
            String attribute = value.asString();
            String attr = attribute;
            if (attr.equals("POSITION")) {
                vertexAttributes.add(VertexAttribute.Position());
            } else if (attr.equals("NORMAL")) {
                vertexAttributes.add(VertexAttribute.Normal());
            } else if (attr.equals("COLOR")) {
                vertexAttributes.add(VertexAttribute.ColorUnpacked());
            } else if (attr.equals("COLORPACKED")) {
                vertexAttributes.add(VertexAttribute.ColorPacked());
            } else if (attr.equals("TANGENT")) {
                vertexAttributes.add(VertexAttribute.Tangent());
            } else if (attr.equals("BINORMAL")) {
                vertexAttributes.add(VertexAttribute.Binormal());
            } else if (attr.startsWith("TEXCOORD")) {
                vertexAttributes.add(VertexAttribute.TexCoords(unit++));
            } else if (attr.startsWith("BLENDWEIGHT")) {
                vertexAttributes.add(VertexAttribute.BoneWeight(blendWeightCount++));
            } else {
                throw new GdxRuntimeException("Unknown vertex attribute '" + attr + "', should be one of position, normal, uv, tangent or binormal");
            }
            value = value.next;
        }
        return (VertexAttribute[])vertexAttributes.toArray(VertexAttribute.class);
    }

    private void parseMaterials(ModelData model, JsonValue json, String materialDir) {
        JsonValue materials = json.get("materials");
        if (materials != null) {
            model.materials.ensureCapacity(materials.size);
            JsonValue material = materials.child;
            while (material != null) {
                JsonValue reflection;
                JsonValue specular;
                JsonValue emissive;
                JsonValue ambient;
                ModelMaterial jsonMaterial = new ModelMaterial();
                String id = material.getString("id", null);
                if (id == null) {
                    throw new GdxRuntimeException("Material needs an id.");
                }
                jsonMaterial.id = id;
                JsonValue diffuse = material.get("diffuse");
                if (diffuse != null) {
                    jsonMaterial.diffuse = this.parseColor(diffuse);
                }
                if ((ambient = material.get("ambient")) != null) {
                    jsonMaterial.ambient = this.parseColor(ambient);
                }
                if ((emissive = material.get("emissive")) != null) {
                    jsonMaterial.emissive = this.parseColor(emissive);
                }
                if ((specular = material.get("specular")) != null) {
                    jsonMaterial.specular = this.parseColor(specular);
                }
                if ((reflection = material.get("reflection")) != null) {
                    jsonMaterial.reflection = this.parseColor(reflection);
                }
                jsonMaterial.shininess = material.getFloat("shininess", 0.0f);
                jsonMaterial.opacity = material.getFloat("opacity", 1.0f);
                JsonValue textures = material.get("textures");
                if (textures != null) {
                    JsonValue texture = textures.child;
                    while (texture != null) {
                        ModelTexture jsonTexture = new ModelTexture();
                        String textureId = texture.getString("id", null);
                        if (textureId == null) {
                            throw new GdxRuntimeException("Texture has no id.");
                        }
                        jsonTexture.id = textureId;
                        String fileName = texture.getString("filename", null);
                        if (fileName == null) {
                            throw new GdxRuntimeException("Texture needs filename.");
                        }
                        jsonTexture.fileName = materialDir + (materialDir.length() == 0 || materialDir.endsWith("/") ? "" : "/") + fileName;
                        jsonTexture.uvTranslation = this.readVector2(texture.get("uvTranslation"), 0.0f, 0.0f);
                        jsonTexture.uvScaling = this.readVector2(texture.get("uvScaling"), 1.0f, 1.0f);
                        String textureType = texture.getString("type", null);
                        if (textureType == null) {
                            throw new GdxRuntimeException("Texture needs type.");
                        }
                        jsonTexture.usage = this.parseTextureUsage(textureType);
                        if (jsonMaterial.textures == null) {
                            jsonMaterial.textures = new Array();
                        }
                        jsonMaterial.textures.add(jsonTexture);
                        texture = texture.next;
                    }
                }
                model.materials.add(jsonMaterial);
                material = material.next;
            }
        }
    }

    private int parseTextureUsage(String value) {
        if (value.equalsIgnoreCase("AMBIENT")) {
            return 4;
        }
        if (value.equalsIgnoreCase("BUMP")) {
            return 8;
        }
        if (value.equalsIgnoreCase("DIFFUSE")) {
            return 2;
        }
        if (value.equalsIgnoreCase("EMISSIVE")) {
            return 3;
        }
        if (value.equalsIgnoreCase("NONE")) {
            return 1;
        }
        if (value.equalsIgnoreCase("NORMAL")) {
            return 7;
        }
        if (value.equalsIgnoreCase("REFLECTION")) {
            return 10;
        }
        if (value.equalsIgnoreCase("SHININESS")) {
            return 6;
        }
        if (value.equalsIgnoreCase("SPECULAR")) {
            return 5;
        }
        if (value.equalsIgnoreCase("TRANSPARENCY")) {
            return 9;
        }
        return 0;
    }

    private Color parseColor(JsonValue colorArray) {
        if (colorArray.size >= 3) {
            return new Color(colorArray.getFloat(0), colorArray.getFloat(1), colorArray.getFloat(2), 1.0f);
        }
        throw new GdxRuntimeException("Expected Color values <> than three.");
    }

    private Vector2 readVector2(JsonValue vectorArray, float x, float y) {
        if (vectorArray == null) {
            return new Vector2(x, y);
        }
        if (vectorArray.size == 2) {
            return new Vector2(vectorArray.getFloat(0), vectorArray.getFloat(1));
        }
        throw new GdxRuntimeException("Expected Vector2 values <> than two.");
    }

    private Array<ModelNode> parseNodes(ModelData model, JsonValue json) {
        JsonValue nodes = json.get("nodes");
        if (nodes != null) {
            model.nodes.ensureCapacity(nodes.size);
            JsonValue node = nodes.child;
            while (node != null) {
                model.nodes.add(this.parseNodesRecursively(node));
                node = node.next;
            }
        }
        return model.nodes;
    }

    private ModelNode parseNodesRecursively(JsonValue json) {
        JsonValue children;
        JsonValue materials;
        ModelNode jsonNode = new ModelNode();
        String id = json.getString("id", null);
        if (id == null) {
            throw new GdxRuntimeException("Node id missing.");
        }
        jsonNode.id = id;
        JsonValue translation = json.get("translation");
        if (translation != null && translation.size != 3) {
            throw new GdxRuntimeException("Node translation incomplete");
        }
        jsonNode.translation = translation == null ? null : new Vector3(translation.getFloat(0), translation.getFloat(1), translation.getFloat(2));
        JsonValue rotation = json.get("rotation");
        if (rotation != null && rotation.size != 4) {
            throw new GdxRuntimeException("Node rotation incomplete");
        }
        jsonNode.rotation = rotation == null ? null : new Quaternion(rotation.getFloat(0), rotation.getFloat(1), rotation.getFloat(2), rotation.getFloat(3));
        JsonValue scale = json.get("scale");
        if (scale != null && scale.size != 3) {
            throw new GdxRuntimeException("Node scale incomplete");
        }
        jsonNode.scale = scale == null ? null : new Vector3(scale.getFloat(0), scale.getFloat(1), scale.getFloat(2));
        String meshId = json.getString("mesh", null);
        if (meshId != null) {
            jsonNode.meshId = meshId;
        }
        if ((materials = json.get("parts")) != null) {
            jsonNode.parts = new ModelNodePart[materials.size];
            int i = 0;
            JsonValue material = materials.child;
            while (material != null) {
                ModelNodePart nodePart = new ModelNodePart();
                String meshPartId = material.getString("meshpartid", null);
                String materialId = material.getString("materialid", null);
                if (meshPartId == null || materialId == null) {
                    throw new GdxRuntimeException("Node " + id + " part is missing meshPartId or materialId");
                }
                nodePart.materialId = materialId;
                nodePart.meshPartId = meshPartId;
                JsonValue bones = material.get("bones");
                if (bones != null) {
                    nodePart.bones = new ArrayMap(true, bones.size, String.class, Matrix4.class);
                    int j = 0;
                    JsonValue bone = bones.child;
                    while (bone != null) {
                        String nodeId = bone.getString("node", null);
                        if (nodeId == null) {
                            throw new GdxRuntimeException("Bone node ID missing");
                        }
                        Matrix4 transform = new Matrix4();
                        JsonValue val = bone.get("translation");
                        if (val != null && val.size >= 3) {
                            transform.translate(val.getFloat(0), val.getFloat(1), val.getFloat(2));
                        }
                        if ((val = bone.get("rotation")) != null && val.size >= 4) {
                            transform.rotate(this.tempQ.set(val.getFloat(0), val.getFloat(1), val.getFloat(2), val.getFloat(3)));
                        }
                        if ((val = bone.get("scale")) != null && val.size >= 3) {
                            transform.scale(val.getFloat(0), val.getFloat(1), val.getFloat(2));
                        }
                        nodePart.bones.put(nodeId, transform);
                        bone = bone.next;
                        ++j;
                    }
                }
                jsonNode.parts[i] = nodePart;
                material = material.next;
                ++i;
            }
        }
        if ((children = json.get("children")) != null) {
            jsonNode.children = new ModelNode[children.size];
            int i = 0;
            JsonValue child = children.child;
            while (child != null) {
                jsonNode.children[i] = this.parseNodesRecursively(child);
                child = child.next;
                ++i;
            }
        }
        return jsonNode;
    }

    private void parseAnimations(ModelData model, JsonValue json) {
        JsonValue animations = json.get("animations");
        if (animations == null) {
            return;
        }
        model.animations.ensureCapacity(animations.size);
        JsonValue anim = animations.child;
        while (anim != null) {
            JsonValue nodes = anim.get("bones");
            if (nodes != null) {
                ModelAnimation animation = new ModelAnimation();
                model.animations.add(animation);
                animation.nodeAnimations.ensureCapacity(nodes.size);
                animation.id = anim.getString("id");
                JsonValue node = nodes.child;
                while (node != null) {
                    ModelNodeAnimation nodeAnim = new ModelNodeAnimation();
                    animation.nodeAnimations.add(nodeAnim);
                    nodeAnim.nodeId = node.getString("boneId");
                    JsonValue keyframes = node.get("keyframes");
                    if (keyframes != null && keyframes.isArray()) {
                        JsonValue keyframe = keyframes.child;
                        while (keyframe != null) {
                            JsonValue scale;
                            JsonValue rotation;
                            float keytime = keyframe.getFloat("keytime", 0.0f) / 1000.0f;
                            JsonValue translation = keyframe.get("translation");
                            if (translation != null && translation.size == 3) {
                                if (nodeAnim.translation == null) {
                                    nodeAnim.translation = new Array();
                                }
                                ModelNodeKeyframe tkf = new ModelNodeKeyframe();
                                tkf.keytime = keytime;
                                tkf.value = new Vector3(translation.getFloat(0), translation.getFloat(1), translation.getFloat(2));
                                nodeAnim.translation.add(tkf);
                            }
                            if ((rotation = keyframe.get("rotation")) != null && rotation.size == 4) {
                                if (nodeAnim.rotation == null) {
                                    nodeAnim.rotation = new Array();
                                }
                                ModelNodeKeyframe rkf = new ModelNodeKeyframe();
                                rkf.keytime = keytime;
                                rkf.value = new Quaternion(rotation.getFloat(0), rotation.getFloat(1), rotation.getFloat(2), rotation.getFloat(3));
                                nodeAnim.rotation.add(rkf);
                            }
                            if ((scale = keyframe.get("scale")) != null && scale.size == 3) {
                                if (nodeAnim.scaling == null) {
                                    nodeAnim.scaling = new Array();
                                }
                                ModelNodeKeyframe skf = new ModelNodeKeyframe();
                                skf.keytime = keytime;
                                skf.value = new Vector3(scale.getFloat(0), scale.getFloat(1), scale.getFloat(2));
                                nodeAnim.scaling.add(skf);
                            }
                            keyframe = keyframe.next;
                        }
                    } else {
                        JsonValue scalingKF;
                        JsonValue rotationKF;
                        JsonValue translationKF = node.get("translation");
                        if (translationKF != null && translationKF.isArray()) {
                            nodeAnim.translation = new Array();
                            nodeAnim.translation.ensureCapacity(translationKF.size);
                            JsonValue keyframe = translationKF.child;
                            while (keyframe != null) {
                                ModelNodeKeyframe kf = new ModelNodeKeyframe();
                                nodeAnim.translation.add(kf);
                                kf.keytime = keyframe.getFloat("keytime", 0.0f) / 1000.0f;
                                JsonValue translation = keyframe.get("value");
                                if (translation != null && translation.size >= 3) {
                                    kf.value = new Vector3(translation.getFloat(0), translation.getFloat(1), translation.getFloat(2));
                                }
                                keyframe = keyframe.next;
                            }
                        }
                        if ((rotationKF = node.get("rotation")) != null && rotationKF.isArray()) {
                            nodeAnim.rotation = new Array();
                            nodeAnim.rotation.ensureCapacity(rotationKF.size);
                            JsonValue keyframe = rotationKF.child;
                            while (keyframe != null) {
                                ModelNodeKeyframe kf = new ModelNodeKeyframe();
                                nodeAnim.rotation.add(kf);
                                kf.keytime = keyframe.getFloat("keytime", 0.0f) / 1000.0f;
                                JsonValue rotation = keyframe.get("value");
                                if (rotation != null && rotation.size >= 4) {
                                    kf.value = new Quaternion(rotation.getFloat(0), rotation.getFloat(1), rotation.getFloat(2), rotation.getFloat(3));
                                }
                                keyframe = keyframe.next;
                            }
                        }
                        if ((scalingKF = node.get("scaling")) != null && scalingKF.isArray()) {
                            nodeAnim.scaling = new Array();
                            nodeAnim.scaling.ensureCapacity(scalingKF.size);
                            JsonValue keyframe = scalingKF.child;
                            while (keyframe != null) {
                                ModelNodeKeyframe kf = new ModelNodeKeyframe();
                                nodeAnim.scaling.add(kf);
                                kf.keytime = keyframe.getFloat("keytime", 0.0f) / 1000.0f;
                                JsonValue scaling = keyframe.get("value");
                                if (scaling != null && scaling.size >= 3) {
                                    kf.value = new Vector3(scaling.getFloat(0), scaling.getFloat(1), scaling.getFloat(2));
                                }
                                keyframe = keyframe.next;
                            }
                        }
                    }
                    node = node.next;
                }
            }
            anim = anim.next;
        }
    }
}

