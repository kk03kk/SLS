/*
 * Decompiled with CFR 0.152.
 */
package com.badlogic.gdx.graphics.g3d.loader;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.assets.loaders.FileHandleResolver;
import com.badlogic.gdx.assets.loaders.ModelLoader;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.VertexAttribute;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.loader.MtlLoader;
import com.badlogic.gdx.graphics.g3d.model.data.ModelData;
import com.badlogic.gdx.graphics.g3d.model.data.ModelMesh;
import com.badlogic.gdx.graphics.g3d.model.data.ModelMeshPart;
import com.badlogic.gdx.graphics.g3d.model.data.ModelNode;
import com.badlogic.gdx.graphics.g3d.model.data.ModelNodePart;
import com.badlogic.gdx.math.Quaternion;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.FloatArray;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class ObjLoader
extends ModelLoader<ObjLoaderParameters> {
    public static boolean logWarning = false;
    final FloatArray verts = new FloatArray(300);
    final FloatArray norms = new FloatArray(300);
    final FloatArray uvs = new FloatArray(200);
    final Array<Group> groups = new Array(10);

    public ObjLoader() {
        this(null);
    }

    public ObjLoader(FileHandleResolver resolver) {
        super(resolver);
    }

    @Override
    public Model loadModel(FileHandle fileHandle, boolean flipV) {
        return this.loadModel(fileHandle, new ObjLoaderParameters(flipV));
    }

    @Override
    public ModelData loadModelData(FileHandle file, ObjLoaderParameters parameters) {
        return this.loadModelData(file, parameters == null ? false : parameters.flipV);
    }

    /*
     * Unable to fully structure code
     */
    @Override
    protected ModelData loadModelData(FileHandle file, boolean flipV) {
        if (ObjLoader.logWarning) {
            Gdx.app.error("ObjLoader", "Wavefront (OBJ) is not fully supported, consult the documentation for more information");
        }
        mtl = new MtlLoader();
        activeGroup = new Group("default");
        this.groups.add(activeGroup);
        reader = new BufferedReader(new InputStreamReader(file.read()), 4096);
        id = 0;
lbl8:
        // 2 sources

        try {
            while ((line = reader.readLine()) != null && (tokens = line.split("\\s+")).length >= 1) {
                block33: {
                    if (tokens[0].length() == 0 || (firstChar = tokens[0].toLowerCase().charAt(0)) == '#') continue;
                    if (firstChar == 'v') {
                        if (tokens[0].length() == 1) {
                            this.verts.add(Float.parseFloat(tokens[1]));
                            this.verts.add(Float.parseFloat(tokens[2]));
                            this.verts.add(Float.parseFloat(tokens[3]));
                            continue;
                        }
                        if (tokens[0].charAt(1) == 'n') {
                            this.norms.add(Float.parseFloat(tokens[1]));
                            this.norms.add(Float.parseFloat(tokens[2]));
                            this.norms.add(Float.parseFloat(tokens[3]));
                            continue;
                        }
                        if (tokens[0].charAt(1) != 't') continue;
                        this.uvs.add(Float.parseFloat(tokens[1]));
                        this.uvs.add(flipV != false ? 1.0f - Float.parseFloat(tokens[2]) : Float.parseFloat(tokens[2]));
                        continue;
                    }
                    if (firstChar != 'f') break block33;
                    faces = activeGroup.faces;
                    for (i = 1; i < tokens.length - 2; --i) {
                        parts = tokens[1].split("/");
                        faces.add(this.getIndex(parts[0], this.verts.size));
                        if (parts.length > 2) {
                            if (i == 1) {
                                activeGroup.hasNorms = true;
                            }
                            faces.add(this.getIndex(parts[2], this.norms.size));
                        }
                        if (parts.length > 1 && parts[1].length() > 0) {
                            if (i == 1) {
                                activeGroup.hasUVs = true;
                            }
                            faces.add(this.getIndex(parts[1], this.uvs.size));
                        }
                        parts = tokens[++i].split("/");
                        faces.add(this.getIndex(parts[0], this.verts.size));
                        if (parts.length > 2) {
                            faces.add(this.getIndex(parts[2], this.norms.size));
                        }
                        if (parts.length > 1 && parts[1].length() > 0) {
                            faces.add(this.getIndex(parts[1], this.uvs.size));
                        }
                        parts = tokens[++i].split("/");
                        faces.add(this.getIndex(parts[0], this.verts.size));
                        if (parts.length > 2) {
                            faces.add(this.getIndex(parts[2], this.norms.size));
                        }
                        if (parts.length > 1 && parts[1].length() > 0) {
                            faces.add(this.getIndex(parts[1], this.uvs.size));
                        }
                        ++activeGroup.numFaces;
                    }
                    ** GOTO lbl8
                }
                if (firstChar == 'o' || firstChar == 'g') {
                    if (tokens.length > 1) {
                        activeGroup = this.setActiveGroup(tokens[1]);
                        continue;
                    }
                    activeGroup = this.setActiveGroup("default");
                    continue;
                }
                if (tokens[0].equals("mtllib")) {
                    mtl.load(file.parent().child(tokens[1]));
                    continue;
                }
                if (!tokens[0].equals("usemtl")) continue;
                if (tokens.length == 1) {
                    activeGroup.materialName = "default";
                    continue;
                }
                activeGroup.materialName = tokens[1].replace('.', '_');
            }
            reader.close();
        }
        catch (IOException e) {
            return null;
        }
        for (i = 0; i < this.groups.size; ++i) {
            if (this.groups.get((int)i).numFaces >= 1) continue;
            this.groups.removeIndex(i);
            --i;
        }
        if (this.groups.size < 1) {
            return null;
        }
        numGroups = this.groups.size;
        data = new ModelData();
        for (g = 0; g < numGroups; ++g) {
            group = this.groups.get(g);
            faces = group.faces;
            numElements = faces.size;
            numFaces = group.numFaces;
            hasNorms = group.hasNorms;
            hasUVs = group.hasUVs;
            finalVerts = new float[numFaces * 3 * (3 + (hasNorms != false ? 3 : 0) + (hasUVs != false ? 2 : 0))];
            i = 0;
            vi = 0;
            while (i < numElements) {
                vertIndex = faces.get(i++) * 3;
                finalVerts[vi++] = this.verts.get(vertIndex++);
                finalVerts[vi++] = this.verts.get(vertIndex++);
                finalVerts[vi++] = this.verts.get(vertIndex);
                if (hasNorms) {
                    normIndex = faces.get(i++) * 3;
                    finalVerts[vi++] = this.norms.get(normIndex++);
                    finalVerts[vi++] = this.norms.get(normIndex++);
                    finalVerts[vi++] = this.norms.get(normIndex);
                }
                if (!hasUVs) continue;
                uvIndex = faces.get(i++) * 2;
                finalVerts[vi++] = this.uvs.get(uvIndex++);
                finalVerts[vi++] = this.uvs.get(uvIndex);
            }
            numIndices = numFaces * 3 >= 32767 ? 0 : numFaces * 3;
            finalIndices = new short[numIndices];
            if (numIndices > 0) {
                for (i = 0; i < numIndices; ++i) {
                    finalIndices[i] = (short)i;
                }
            }
            attributes = new Array<VertexAttribute>();
            attributes.add(new VertexAttribute(1, 3, "a_position"));
            if (hasNorms) {
                attributes.add(new VertexAttribute(8, 3, "a_normal"));
            }
            if (hasUVs) {
                attributes.add(new VertexAttribute(16, 2, "a_texCoord0"));
            }
            stringId = Integer.toString(++id);
            nodeId = "default".equals(group.name) != false ? "node" + stringId : group.name;
            meshId = "default".equals(group.name) != false ? "mesh" + stringId : group.name;
            partId = "default".equals(group.name) != false ? "part" + stringId : group.name;
            node = new ModelNode();
            node.id = nodeId;
            node.meshId = meshId;
            node.scale = new Vector3(1.0f, 1.0f, 1.0f);
            node.translation = new Vector3();
            node.rotation = new Quaternion();
            pm = new ModelNodePart();
            pm.meshPartId = partId;
            pm.materialId = group.materialName;
            node.parts = new ModelNodePart[]{pm};
            part = new ModelMeshPart();
            part.id = partId;
            part.indices = finalIndices;
            part.primitiveType = 4;
            mesh = new ModelMesh();
            mesh.id = meshId;
            mesh.attributes = (VertexAttribute[])attributes.toArray(VertexAttribute.class);
            mesh.vertices = finalVerts;
            mesh.parts = new ModelMeshPart[]{part};
            data.nodes.add(node);
            data.meshes.add(mesh);
            mm = mtl.getMaterial(group.materialName);
            data.materials.add(mm);
        }
        if (this.verts.size > 0) {
            this.verts.clear();
        }
        if (this.norms.size > 0) {
            this.norms.clear();
        }
        if (this.uvs.size > 0) {
            this.uvs.clear();
        }
        if (this.groups.size > 0) {
            this.groups.clear();
        }
        return data;
    }

    private Group setActiveGroup(String name) {
        for (Group group : this.groups) {
            if (!group.name.equals(name)) continue;
            return group;
        }
        Group group = new Group(name);
        this.groups.add(group);
        return group;
    }

    private int getIndex(String index, int size) {
        if (index == null || index.length() == 0) {
            return 0;
        }
        int idx = Integer.parseInt(index);
        if (idx < 0) {
            return size + idx;
        }
        return idx - 1;
    }

    private class Group {
        final String name;
        String materialName;
        Array<Integer> faces;
        int numFaces;
        boolean hasNorms;
        boolean hasUVs;
        Material mat;

        Group(String name) {
            this.name = name;
            this.faces = new Array(200);
            this.numFaces = 0;
            this.mat = new Material("");
            this.materialName = "default";
        }
    }

    public static class ObjLoaderParameters
    extends ModelLoader.ModelParameters {
        public boolean flipV;

        public ObjLoaderParameters() {
        }

        public ObjLoaderParameters(boolean flipV) {
            this.flipV = flipV;
        }
    }
}

