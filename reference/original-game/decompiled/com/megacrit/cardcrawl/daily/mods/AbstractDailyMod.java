/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.daily.mods;

import com.badlogic.gdx.graphics.Texture;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.helpers.GameDataStringBuilder;
import com.megacrit.cardcrawl.helpers.ImageMaster;

public class AbstractDailyMod {
    public String name;
    public String description;
    public String modID;
    public Texture img;
    public boolean positive;
    public AbstractPlayer.PlayerClass classToExclude;
    private static final String IMG_DIR = "images/ui/run_mods/";

    public AbstractDailyMod(String setId, String name, String description, String imgUrl, boolean positive) {
        this(setId, name, description, imgUrl, positive, null);
    }

    public AbstractDailyMod(String setId, String name, String description, String imgUrl, boolean positive, AbstractPlayer.PlayerClass exclusion) {
        this.modID = setId;
        this.name = name;
        this.description = description;
        this.positive = positive;
        this.img = ImageMaster.loadImage(IMG_DIR + imgUrl);
        this.classToExclude = exclusion;
    }

    public void effect() {
    }

    public static String gameDataUploadHeader() {
        GameDataStringBuilder sb = new GameDataStringBuilder();
        sb.addFieldData("name");
        sb.addFieldData("text");
        return sb.toString();
    }

    public String gameDataUploadData() {
        GameDataStringBuilder sb = new GameDataStringBuilder();
        sb.addFieldData(this.name);
        sb.addFieldData(this.description);
        return sb.toString();
    }
}

