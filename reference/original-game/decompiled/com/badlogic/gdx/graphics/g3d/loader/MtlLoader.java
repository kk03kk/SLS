/*
 * Decompiled with CFR 0.152.
 */
package com.badlogic.gdx.graphics.g3d.loader;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g3d.model.data.ModelMaterial;
import com.badlogic.gdx.graphics.g3d.model.data.ModelTexture;
import com.badlogic.gdx.utils.Array;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

class MtlLoader {
    public Array<ModelMaterial> materials = new Array();

    MtlLoader() {
    }

    public void load(FileHandle file) {
        String curMatName = "default";
        Color difcolor = Color.WHITE;
        Color speccolor = Color.WHITE;
        float opacity = 1.0f;
        float shininess = 0.0f;
        String texFilename = null;
        if (file == null || !file.exists()) {
            return;
        }
        BufferedReader reader = new BufferedReader(new InputStreamReader(file.read()), 4096);
        try {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] tokens;
                if (line.length() > 0 && line.charAt(0) == '\t') {
                    line = line.substring(1).trim();
                }
                if ((tokens = line.split("\\s+"))[0].length() == 0 || tokens[0].charAt(0) == '#') continue;
                String key = tokens[0].toLowerCase();
                if (key.equals("newmtl")) {
                    ModelMaterial mat = new ModelMaterial();
                    mat.id = curMatName;
                    mat.diffuse = new Color(difcolor);
                    mat.specular = new Color(speccolor);
                    mat.opacity = opacity;
                    mat.shininess = shininess;
                    if (texFilename != null) {
                        ModelTexture tex = new ModelTexture();
                        tex.usage = 2;
                        tex.fileName = new String(texFilename);
                        if (mat.textures == null) {
                            mat.textures = new Array(1);
                        }
                        mat.textures.add(tex);
                    }
                    this.materials.add(mat);
                    if (tokens.length > 1) {
                        curMatName = tokens[1];
                        curMatName = curMatName.replace('.', '_');
                    } else {
                        curMatName = "default";
                    }
                    difcolor = Color.WHITE;
                    speccolor = Color.WHITE;
                    opacity = 1.0f;
                    shininess = 0.0f;
                    continue;
                }
                if (key.equals("kd") || key.equals("ks")) {
                    float r = Float.parseFloat(tokens[1]);
                    float g = Float.parseFloat(tokens[2]);
                    float b = Float.parseFloat(tokens[3]);
                    float a = 1.0f;
                    if (tokens.length > 4) {
                        a = Float.parseFloat(tokens[4]);
                    }
                    if (tokens[0].toLowerCase().equals("kd")) {
                        difcolor = new Color();
                        difcolor.set(r, g, b, a);
                        continue;
                    }
                    speccolor = new Color();
                    speccolor.set(r, g, b, a);
                    continue;
                }
                if (key.equals("tr") || key.equals("d")) {
                    opacity = Float.parseFloat(tokens[1]);
                    continue;
                }
                if (key.equals("ns")) {
                    shininess = Float.parseFloat(tokens[1]);
                    continue;
                }
                if (!key.equals("map_kd")) continue;
                texFilename = file.parent().child(tokens[1]).path();
            }
            reader.close();
        }
        catch (IOException e) {
            return;
        }
        ModelMaterial mat = new ModelMaterial();
        mat.id = curMatName;
        mat.diffuse = new Color(difcolor);
        mat.specular = new Color(speccolor);
        mat.opacity = opacity;
        mat.shininess = shininess;
        if (texFilename != null) {
            ModelTexture tex = new ModelTexture();
            tex.usage = 2;
            tex.fileName = new String(texFilename);
            if (mat.textures == null) {
                mat.textures = new Array(1);
            }
            mat.textures.add(tex);
        }
        this.materials.add(mat);
    }

    public ModelMaterial getMaterial(String name) {
        for (ModelMaterial m : this.materials) {
            if (!m.id.equals(name)) continue;
            return m;
        }
        ModelMaterial mat = new ModelMaterial();
        mat.id = name;
        mat.diffuse = new Color(Color.WHITE);
        this.materials.add(mat);
        return mat;
    }
}

