package com.jerry.mekextras.api.text;

import com.jerry.mekextras.MekanismExtras;
import mekanism.api.annotations.NothingNullByDefault;
import mekanism.api.text.ILangEntry;
import net.minecraft.Util;
import net.minecraft.resources.ResourceLocation;

@NothingNullByDefault
public enum APIExtraLang implements ILangEntry {
    //Upgrades
    UPGRADE_IONIC_MEMBRANE("upgrade", "ionic_membrane"),
    UPGRADE_IONIC_MEMBRANE_DESCRIPTION("upgrade", "ionic_membrane.description");

    private final String key;

    APIExtraLang(String type, String path) {
        this(Util.makeDescriptionId(type, new ResourceLocation(MekanismExtras.MOD_ID, path)));
    }

    APIExtraLang(String key) {
        this.key = key;
    }

    @Override
    public String getTranslationKey() {
        return key;
    }
}
