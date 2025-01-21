package com.jerry.mekextras.common.config;

import mekanism.common.config.BaseMekanismConfig;
import mekanism.common.config.value.CachedLongValue;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.ModConfigSpec;

public class ExtraUsageConfig extends BaseMekanismConfig {

    private final ModConfigSpec configSpec;

    public final CachedLongValue advanceElectricPump;

    public ExtraUsageConfig() {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        builder.comment("Machine Energy Usage Config. This config is synced from server to client.").push("usage");

        advanceElectricPump = CachedLongValue.definedMin(this, builder, "Energy per operation tick (Joules).", "advanceElectricPump",
                1_000L, 1);

        builder.pop();
        configSpec = builder.build();
    }

    @Override
    public String getFileName() {
        return "extras_machine_usage";
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
