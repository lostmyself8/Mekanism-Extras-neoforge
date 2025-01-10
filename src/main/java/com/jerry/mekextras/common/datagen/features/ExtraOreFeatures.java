package com.jerry.mekextras.common.datagen.features;

import com.jerry.mekextras.MekanismExtras;
import com.jerry.mekextras.common.registry.ExtraBlocks;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.OreConfiguration;
import net.minecraft.world.level.levelgen.structure.templatesystem.BlockMatchTest;
import net.minecraft.world.level.levelgen.structure.templatesystem.RuleTest;

import java.util.List;

public class ExtraOreFeatures {
    public static ResourceKey<ConfiguredFeature<?, ?>> NAQUADAH_ORE_KEY = resourceKey("naquadah_ore");
    public static ResourceKey<ConfiguredFeature<?, ?>> END_NAQUADAH_ORE_KEY = resourceKey("end_naquadah_ore");

    public static void bootStrap(BootstrapContext<ConfiguredFeature<?, ?>> context) {
        RuleTest bedstoneReplace = new BlockMatchTest(Blocks.BEDROCK);
        RuleTest endstoneReplace = new BlockMatchTest(Blocks.END_STONE);

        List<OreConfiguration.TargetBlockState> overWorldNaquadahOres = List.of(OreConfiguration.target(bedstoneReplace, ExtraBlocks.NAQUADAH_ORE.getBlock().defaultBlockState()));
        register(context, NAQUADAH_ORE_KEY, Feature.ORE, new OreConfiguration(overWorldNaquadahOres, 4));
        List<OreConfiguration.TargetBlockState> endNaquadahOres = List.of(OreConfiguration.target(endstoneReplace, ExtraBlocks.END_NAQUADAH_ORE.getBlock().defaultBlockState()));
        register(context, END_NAQUADAH_ORE_KEY, Feature.ORE, new OreConfiguration(endNaquadahOres, 8));
    }

    private static ResourceKey<ConfiguredFeature<?, ?>> resourceKey(String name) {
        return ResourceKey.create(Registries.CONFIGURED_FEATURE, ResourceLocation.fromNamespaceAndPath(MekanismExtras.MOD_ID, name));
    }

    private static <FC extends FeatureConfiguration, F extends Feature<FC>> void register(BootstrapContext<ConfiguredFeature<?, ?>> context,
                                                                                          ResourceKey<ConfiguredFeature<?, ?>> key,
                                                                                          F feature, FC configuration) {
        context.register(key, new ConfiguredFeature<>(feature, configuration));
    }
}
