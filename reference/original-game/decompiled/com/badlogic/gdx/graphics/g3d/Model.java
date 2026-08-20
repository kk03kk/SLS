/*
 * Decompiled with CFR 0.152.
 */
package com.badlogic.gdx.graphics.g3d;

import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.graphics.g3d.Attribute;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.attributes.BlendingAttribute;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.attributes.FloatAttribute;
import com.badlogic.gdx.graphics.g3d.attributes.TextureAttribute;
import com.badlogic.gdx.graphics.g3d.model.Animation;
import com.badlogic.gdx.graphics.g3d.model.MeshPart;
import com.badlogic.gdx.graphics.g3d.model.Node;
import com.badlogic.gdx.graphics.g3d.model.NodeAnimation;
import com.badlogic.gdx.graphics.g3d.model.NodeKeyframe;
import com.badlogic.gdx.graphics.g3d.model.NodePart;
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
import com.badlogic.gdx.graphics.g3d.utils.TextureDescriptor;
import com.badlogic.gdx.graphics.g3d.utils.TextureProvider;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Quaternion;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.collision.BoundingBox;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ArrayMap;
import com.badlogic.gdx.utils.BufferUtils;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.GdxRuntimeException;
import com.badlogic.gdx.utils.ObjectMap;
import java.io.Serializable;

public class Model
implements Disposable {
    public final Array<Material> materials = new Array();
    public final Array<Node> nodes = new Array();
    public final Array<Animation> animations = new Array();
    public final Array<Mesh> meshes = new Array();
    public final Array<MeshPart> meshParts = new Array();
    protected final Array<Disposable> disposables = new Array();
    private ObjectMap<NodePart, ArrayMap<String, Matrix4>> nodePartBones = new ObjectMap();

    public Model() {
    }

    public Model(ModelData modelData) {
        this(modelData, new TextureProvider.FileTextureProvider());
    }

    public Model(ModelData modelData, TextureProvider textureProvider) {
        this.load(modelData, textureProvider);
    }

    protected void load(ModelData modelData, TextureProvider textureProvider) {
        this.loadMeshes(modelData.meshes);
        this.loadMaterials(modelData.materials, textureProvider);
        this.loadNodes(modelData.nodes);
        this.loadAnimations(modelData.animations);
        this.calculateTransforms();
    }

    protected void loadAnimations(Iterable<ModelAnimation> modelAnimations) {
        for (ModelAnimation anim : modelAnimations) {
            Animation animation = new Animation();
            animation.id = anim.id;
            for (ModelNodeAnimation nanim : anim.nodeAnimations) {
                Node node = this.getNode(nanim.nodeId);
                if (node == null) continue;
                NodeAnimation nodeAnim = new NodeAnimation();
                nodeAnim.node = node;
                if (nanim.translation != null) {
                    nodeAnim.translation = new Array();
                    nodeAnim.translation.ensureCapacity(nanim.translation.size);
                    for (ModelNodeKeyframe<Serializable> modelNodeKeyframe : nanim.translation) {
                        if (modelNodeKeyframe.keytime > animation.duration) {
                            animation.duration = modelNodeKeyframe.keytime;
                        }
                        nodeAnim.translation.add(new NodeKeyframe<Vector3>(modelNodeKeyframe.keytime, new Vector3(modelNodeKeyframe.value == null ? node.translation : (Vector3)modelNodeKeyframe.value)));
                    }
                }
                if (nanim.rotation != null) {
                    nodeAnim.rotation = new Array();
                    nodeAnim.rotation.ensureCapacity(nanim.rotation.size);
                    for (ModelNodeKeyframe<Serializable> modelNodeKeyframe : nanim.rotation) {
                        if (modelNodeKeyframe.keytime > animation.duration) {
                            animation.duration = modelNodeKeyframe.keytime;
                        }
                        nodeAnim.rotation.add(new NodeKeyframe<Quaternion>(modelNodeKeyframe.keytime, new Quaternion(modelNodeKeyframe.value == null ? node.rotation : (Quaternion)modelNodeKeyframe.value)));
                    }
                }
                if (nanim.scaling != null) {
                    nodeAnim.scaling = new Array();
                    nodeAnim.scaling.ensureCapacity(nanim.scaling.size);
                    for (ModelNodeKeyframe<Serializable> modelNodeKeyframe : nanim.scaling) {
                        if (modelNodeKeyframe.keytime > animation.duration) {
                            animation.duration = modelNodeKeyframe.keytime;
                        }
                        nodeAnim.scaling.add(new NodeKeyframe<Vector3>(modelNodeKeyframe.keytime, new Vector3(modelNodeKeyframe.value == null ? node.scale : (Vector3)modelNodeKeyframe.value)));
                    }
                }
                if (!(nodeAnim.translation != null && nodeAnim.translation.size > 0 || nodeAnim.rotation != null && nodeAnim.rotation.size > 0) && (nodeAnim.scaling == null || nodeAnim.scaling.size <= 0)) continue;
                animation.nodeAnimations.add(nodeAnim);
            }
            if (animation.nodeAnimations.size <= 0) continue;
            this.animations.add(animation);
        }
    }

    protected void loadNodes(Iterable<ModelNode> modelNodes) {
        this.nodePartBones.clear();
        for (ModelNode modelNode : modelNodes) {
            this.nodes.add(this.loadNode(modelNode));
        }
        for (ObjectMap.Entry entry : this.nodePartBones.entries()) {
            if (((NodePart)entry.key).invBoneBindTransforms == null) {
                ((NodePart)entry.key).invBoneBindTransforms = new ArrayMap(Node.class, Matrix4.class);
            }
            ((NodePart)entry.key).invBoneBindTransforms.clear();
            for (ObjectMap.Entry b : ((ArrayMap)entry.value).entries()) {
                ((NodePart)entry.key).invBoneBindTransforms.put(this.getNode((String)b.key), new Matrix4((Matrix4)b.value).inv());
            }
        }
    }

    protected Node loadNode(ModelNode modelNode) {
        Node node = new Node();
        node.id = modelNode.id;
        if (modelNode.translation != null) {
            node.translation.set(modelNode.translation);
        }
        if (modelNode.rotation != null) {
            node.rotation.set(modelNode.rotation);
        }
        if (modelNode.scale != null) {
            node.scale.set(modelNode.scale);
        }
        if (modelNode.parts != null) {
            for (ModelNodePart modelNodePart : modelNode.parts) {
                MeshPart meshPart = null;
                Material meshMaterial = null;
                if (modelNodePart.meshPartId != null) {
                    for (MeshPart part : this.meshParts) {
                        if (!modelNodePart.meshPartId.equals(part.id)) continue;
                        meshPart = part;
                        break;
                    }
                }
                if (modelNodePart.materialId != null) {
                    for (Material material : this.materials) {
                        if (!modelNodePart.materialId.equals(material.id)) continue;
                        meshMaterial = material;
                        break;
                    }
                }
                if (meshPart == null || meshMaterial == null) {
                    throw new GdxRuntimeException("Invalid node: " + node.id);
                }
                if (meshPart == null || meshMaterial == null) continue;
                NodePart nodePart = new NodePart();
                nodePart.meshPart = meshPart;
                nodePart.material = meshMaterial;
                node.parts.add(nodePart);
                if (modelNodePart.bones == null) continue;
                this.nodePartBones.put(nodePart, modelNodePart.bones);
            }
        }
        if (modelNode.children != null) {
            for (ModelNode child : modelNode.children) {
                node.addChild(this.loadNode(child));
            }
        }
        return node;
    }

    protected void loadMeshes(Iterable<ModelMesh> meshes) {
        for (ModelMesh mesh : meshes) {
            this.convertMesh(mesh);
        }
    }

    protected void convertMesh(ModelMesh modelMesh) {
        int numIndices = 0;
        for (ModelMeshPart part : modelMesh.parts) {
            numIndices += part.indices.length;
        }
        VertexAttributes attributes = new VertexAttributes(modelMesh.attributes);
        int numVertices = modelMesh.vertices.length / (attributes.vertexSize / 4);
        Mesh mesh = new Mesh(true, numVertices, numIndices, attributes);
        this.meshes.add(mesh);
        this.disposables.add(mesh);
        BufferUtils.copy(modelMesh.vertices, mesh.getVerticesBuffer(), modelMesh.vertices.length, 0);
        int offset = 0;
        mesh.getIndicesBuffer().clear();
        for (ModelMeshPart part : modelMesh.parts) {
            MeshPart meshPart = new MeshPart();
            meshPart.id = part.id;
            meshPart.primitiveType = part.primitiveType;
            meshPart.offset = offset;
            meshPart.size = part.indices.length;
            meshPart.mesh = mesh;
            mesh.getIndicesBuffer().put(part.indices);
            offset += meshPart.size;
            this.meshParts.add(meshPart);
        }
        mesh.getIndicesBuffer().position(0);
        for (MeshPart part : this.meshParts) {
            part.update();
        }
    }

    protected void loadMaterials(Iterable<ModelMaterial> modelMaterials, TextureProvider textureProvider) {
        for (ModelMaterial mtl : modelMaterials) {
            this.materials.add(this.convertMaterial(mtl, textureProvider));
        }
    }

    protected Material convertMaterial(ModelMaterial mtl, TextureProvider textureProvider) {
        Material result = new Material();
        result.id = mtl.id;
        if (mtl.ambient != null) {
            result.set((Attribute)new ColorAttribute(ColorAttribute.Ambient, mtl.ambient));
        }
        if (mtl.diffuse != null) {
            result.set((Attribute)new ColorAttribute(ColorAttribute.Diffuse, mtl.diffuse));
        }
        if (mtl.specular != null) {
            result.set((Attribute)new ColorAttribute(ColorAttribute.Specular, mtl.specular));
        }
        if (mtl.emissive != null) {
            result.set((Attribute)new ColorAttribute(ColorAttribute.Emissive, mtl.emissive));
        }
        if (mtl.reflection != null) {
            result.set((Attribute)new ColorAttribute(ColorAttribute.Reflection, mtl.reflection));
        }
        if (mtl.shininess > 0.0f) {
            result.set((Attribute)new FloatAttribute(FloatAttribute.Shininess, mtl.shininess));
        }
        if (mtl.opacity != 1.0f) {
            result.set((Attribute)new BlendingAttribute(770, 771, mtl.opacity));
        }
        ObjectMap<String, Texture> textures = new ObjectMap<String, Texture>();
        if (mtl.textures != null) {
            for (ModelTexture tex : mtl.textures) {
                Texture texture;
                if (textures.containsKey(tex.fileName)) {
                    texture = (Texture)textures.get(tex.fileName);
                } else {
                    texture = textureProvider.load(tex.fileName);
                    textures.put(tex.fileName, texture);
                    this.disposables.add(texture);
                }
                TextureDescriptor<Texture> descriptor = new TextureDescriptor<Texture>(texture);
                descriptor.minFilter = texture.getMinFilter();
                descriptor.magFilter = texture.getMagFilter();
                descriptor.uWrap = texture.getUWrap();
                descriptor.vWrap = texture.getVWrap();
                float offsetU = tex.uvTranslation == null ? 0.0f : tex.uvTranslation.x;
                float offsetV = tex.uvTranslation == null ? 0.0f : tex.uvTranslation.y;
                float scaleU = tex.uvScaling == null ? 1.0f : tex.uvScaling.x;
                float scaleV = tex.uvScaling == null ? 1.0f : tex.uvScaling.y;
                switch (tex.usage) {
                    case 2: {
                        result.set((Attribute)new TextureAttribute(TextureAttribute.Diffuse, descriptor, offsetU, offsetV, scaleU, scaleV));
                        break;
                    }
                    case 5: {
                        result.set((Attribute)new TextureAttribute(TextureAttribute.Specular, descriptor, offsetU, offsetV, scaleU, scaleV));
                        break;
                    }
                    case 8: {
                        result.set((Attribute)new TextureAttribute(TextureAttribute.Bump, descriptor, offsetU, offsetV, scaleU, scaleV));
                        break;
                    }
                    case 7: {
                        result.set((Attribute)new TextureAttribute(TextureAttribute.Normal, descriptor, offsetU, offsetV, scaleU, scaleV));
                        break;
                    }
                    case 4: {
                        result.set((Attribute)new TextureAttribute(TextureAttribute.Ambient, descriptor, offsetU, offsetV, scaleU, scaleV));
                        break;
                    }
                    case 3: {
                        result.set((Attribute)new TextureAttribute(TextureAttribute.Emissive, descriptor, offsetU, offsetV, scaleU, scaleV));
                        break;
                    }
                    case 10: {
                        result.set((Attribute)new TextureAttribute(TextureAttribute.Reflection, descriptor, offsetU, offsetV, scaleU, scaleV));
                    }
                }
            }
        }
        return result;
    }

    public void manageDisposable(Disposable disposable) {
        if (!this.disposables.contains(disposable, true)) {
            this.disposables.add(disposable);
        }
    }

    public Iterable<Disposable> getManagedDisposables() {
        return this.disposables;
    }

    @Override
    public void dispose() {
        for (Disposable disposable : this.disposables) {
            disposable.dispose();
        }
    }

    public void calculateTransforms() {
        int i;
        int n = this.nodes.size;
        for (i = 0; i < n; ++i) {
            this.nodes.get(i).calculateTransforms(true);
        }
        for (i = 0; i < n; ++i) {
            this.nodes.get(i).calculateBoneTransforms(true);
        }
    }

    public BoundingBox calculateBoundingBox(BoundingBox out) {
        out.inf();
        return this.extendBoundingBox(out);
    }

    public BoundingBox extendBoundingBox(BoundingBox out) {
        int n = this.nodes.size;
        for (int i = 0; i < n; ++i) {
            this.nodes.get(i).extendBoundingBox(out);
        }
        return out;
    }

    public Animation getAnimation(String id) {
        return this.getAnimation(id, true);
    }

    public Animation getAnimation(String id, boolean ignoreCase) {
        int n = this.animations.size;
        if (ignoreCase) {
            for (int i = 0; i < n; ++i) {
                Animation animation = this.animations.get(i);
                if (!animation.id.equalsIgnoreCase(id)) continue;
                return animation;
            }
        } else {
            for (int i = 0; i < n; ++i) {
                Animation animation = this.animations.get(i);
                if (!animation.id.equals(id)) continue;
                return animation;
            }
        }
        return null;
    }

    public Material getMaterial(String id) {
        return this.getMaterial(id, true);
    }

    public Material getMaterial(String id, boolean ignoreCase) {
        int n = this.materials.size;
        if (ignoreCase) {
            for (int i = 0; i < n; ++i) {
                Material material = this.materials.get(i);
                if (!material.id.equalsIgnoreCase(id)) continue;
                return material;
            }
        } else {
            for (int i = 0; i < n; ++i) {
                Material material = this.materials.get(i);
                if (!material.id.equals(id)) continue;
                return material;
            }
        }
        return null;
    }

    public Node getNode(String id) {
        return this.getNode(id, true);
    }

    public Node getNode(String id, boolean recursive) {
        return this.getNode(id, recursive, false);
    }

    public Node getNode(String id, boolean recursive, boolean ignoreCase) {
        return Node.getNode(this.nodes, id, recursive, ignoreCase);
    }
}

