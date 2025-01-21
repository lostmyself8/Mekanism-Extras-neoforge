package com.jerry.mekextras.common.config;

import mekanism.common.config.BaseMekanismConfig;
import mekanism.common.config.value.CachedLongValue;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.ModConfigSpec;

public class ExtraStorageConfig extends BaseMekanismConfig {

    private final ModConfigSpec configSpec;

    public final CachedLongValue advanceElectricPump;

    public ExtraStorageConfig() {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        builder.comment("Machine Energy Storage Config. This config is synced from server to client.").push("storage");

        advanceElectricPump = CachedLongValue.definedMin(this, builder, "Base energy storage (Joules).", "advanceElectricPump",
                400_000L,1);

        builder.pop();
        configSpec = builder.build();
    }

    @Override
    public String getFileName() {
        return "extras_machine_storage";
    }

    @Override
    public ModConfigSpec getConfigSpec() {
        return configSpec;
    }

    @Override
    public ModConfig.Type getConfigType() {
        return ModConfig.Type.SERVER;
    }
}
