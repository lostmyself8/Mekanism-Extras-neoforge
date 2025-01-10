package com.jerry.mekextras.common;

import com.jerry.mekextras.MekanismExtras;
import mekanism.api.text.ILangEntry;
import net.minecraft.Util;
import org.jetbrains.annotations.NotNull;

public enum ExtraLang implements ILangEntry {
    MEK_EXTRAS("constants", "mod_name"),

    //JEI
    JEI_INFO_RICH_SILICON_FUEL("info", "jei.rich_silicon_fuel"),
    JEI_INFO_RICH_URANIUM_FUEL("info", "jei.rich_uranium_fuel"),

    //Reinforced Induction Matrix
    REINFORCED_MATRIX("matrix", "reinforced_induction_matrix");
    private final String key;

    ExtraLang(String type, String path) {
        this(Util.makeDescriptionId(type, MekanismExtras.rl(path)));
    }

    ExtraLang(String key) {
        this.key = key;
    }

    @Override
    public @NotNull String getTranslationKey() {
        return key;
    }
}
