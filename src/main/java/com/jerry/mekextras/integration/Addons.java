package com.jerry.mekextras.integration;

import net.neoforged.fml.ModList;
import net.neoforged.fml.loading.LoadingModList;
import net.neoforged.fml.loading.moddiscovery.ModInfo;

public enum Addons {
    MEKANISMGENERATORS("Mekanism Generators");

    private final String modName;

    Addons(String modName){
        this.modName = modName;
    }

    public String getModId(){
        return name().toLowerCase();
    }

    public String getModName(){
        return modName;
    }

    public boolean isLoaded(){
        return ModList.get() != null
                ? ModList.get().isLoaded(getModId())
                : LoadingModList.get().getMods().stream().map(ModInfo::getModId).anyMatch(getModId()::equals);
    }
}
