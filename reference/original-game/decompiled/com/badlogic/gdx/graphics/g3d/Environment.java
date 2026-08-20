/*
 * Decompiled with CFR 0.152.
 */
package com.badlogic.gdx.graphics.g3d;

import com.badlogic.gdx.graphics.g3d.Attribute;
import com.badlogic.gdx.graphics.g3d.Attributes;
import com.badlogic.gdx.graphics.g3d.attributes.DirectionalLightsAttribute;
import com.badlogic.gdx.graphics.g3d.attributes.PointLightsAttribute;
import com.badlogic.gdx.graphics.g3d.attributes.SpotLightsAttribute;
import com.badlogic.gdx.graphics.g3d.environment.BaseLight;
import com.badlogic.gdx.graphics.g3d.environment.DirectionalLight;
import com.badlogic.gdx.graphics.g3d.environment.PointLight;
import com.badlogic.gdx.graphics.g3d.environment.ShadowMap;
import com.badlogic.gdx.graphics.g3d.environment.SpotLight;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.GdxRuntimeException;

public class Environment
extends Attributes {
    public ShadowMap shadowMap;

    public Environment add(BaseLight ... lights) {
        for (BaseLight light : lights) {
            this.add(light);
        }
        return this;
    }

    public Environment add(Array<BaseLight> lights) {
        for (BaseLight light : lights) {
            this.add(light);
        }
        return this;
    }

    public Environment add(BaseLight light) {
        if (light instanceof DirectionalLight) {
            this.add((DirectionalLight)light);
        } else if (light instanceof PointLight) {
            this.add((PointLight)light);
        } else if (light instanceof SpotLight) {
            this.add((SpotLight)light);
        } else {
            throw new GdxRuntimeException("Unknown light type");
        }
        return this;
    }

    public Environment add(DirectionalLight light) {
        DirectionalLightsAttribute dirLights = (DirectionalLightsAttribute)this.get(DirectionalLightsAttribute.Type);
        if (dirLights == null) {
            dirLights = new DirectionalLightsAttribute();
            this.set((Attribute)dirLights);
        }
        dirLights.lights.add(light);
        return this;
    }

    public Environment add(PointLight light) {
        PointLightsAttribute pointLights = (PointLightsAttribute)this.get(PointLightsAttribute.Type);
        if (pointLights == null) {
            pointLights = new PointLightsAttribute();
            this.set((Attribute)pointLights);
        }
        pointLights.lights.add(light);
        return this;
    }

    public Environment add(SpotLight light) {
        SpotLightsAttribute spotLights = (SpotLightsAttribute)this.get(SpotLightsAttribute.Type);
        if (spotLights == null) {
            spotLights = new SpotLightsAttribute();
            this.set((Attribute)spotLights);
        }
        spotLights.lights.add(light);
        return this;
    }

    public Environment remove(BaseLight ... lights) {
        for (BaseLight light : lights) {
            this.remove(light);
        }
        return this;
    }

    public Environment remove(Array<BaseLight> lights) {
        for (BaseLight light : lights) {
            this.remove(light);
        }
        return this;
    }

    public Environment remove(BaseLight light) {
        if (light instanceof DirectionalLight) {
            this.remove((DirectionalLight)light);
        } else if (light instanceof PointLight) {
            this.remove((PointLight)light);
        } else if (light instanceof SpotLight) {
            this.remove((SpotLight)light);
        } else {
            throw new GdxRuntimeException("Unknown light type");
        }
        return this;
    }

    public Environment remove(DirectionalLight light) {
        if (this.has(DirectionalLightsAttribute.Type)) {
            DirectionalLightsAttribute dirLights = (DirectionalLightsAttribute)this.get(DirectionalLightsAttribute.Type);
            dirLights.lights.removeValue(light, false);
            if (dirLights.lights.size == 0) {
                this.remove(DirectionalLightsAttribute.Type);
            }
        }
        return this;
    }

    public Environment remove(PointLight light) {
        if (this.has(PointLightsAttribute.Type)) {
            PointLightsAttribute pointLights = (PointLightsAttribute)this.get(PointLightsAttribute.Type);
            pointLights.lights.removeValue(light, false);
            if (pointLights.lights.size == 0) {
                this.remove(PointLightsAttribute.Type);
            }
        }
        return this;
    }

    public Environment remove(SpotLight light) {
        if (this.has(SpotLightsAttribute.Type)) {
            SpotLightsAttribute spotLights = (SpotLightsAttribute)this.get(SpotLightsAttribute.Type);
            spotLights.lights.removeValue(light, false);
            if (spotLights.lights.size == 0) {
                this.remove(SpotLightsAttribute.Type);
            }
        }
        return this;
    }
}

