package com.jerry.mekextras.common.tier;

import com.jerry.mekextras.common.config.ExtraConfig;
import mekanism.common.tier.CableTier;

public class CTier {
    public static long getCapacityAsLong(CableTier tier) {
        if (tier == null) return 8000L;
        return switch (tier) {
            case BASIC -> ExtraConfig.extraTierConfig.absoluteUniversalCableCapacity.get();
            case ADVANCED -> ExtraConfig.extraTierConfig.supremeUniversalCableCapacity.get();
            case ELITE -> ExtraConfig.extraTierConfig.cosmicUniversalCableCapacity.get();
            case ULTIMATE -> ExtraConfig.extraTierConfig.infiniteUniversalCableCapacity.get();
        };
    }
}
