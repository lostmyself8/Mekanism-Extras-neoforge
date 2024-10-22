package com.jerry.mekanism_extras.common.util;

import com.jerry.mekanism_extras.common.tier.*;

public class ExtraEnumUtils {
    private ExtraEnumUtils() {

    }
    // Items
    public static final QIODriveAdvanceTier[] QIO_DRIVE_TIERS = QIODriveAdvanceTier.values();

    // Blocks
    public static final BTier[] BIN_TIERS = BTier.values();
    public static final ICTier[] INDUCTION_CELL_TIERS = ICTier.values();
    public static final IPTier[] INDUCTION_PROVIDER_TIERS = IPTier.values();
    public static final ECTier[] ENERGY_CUBE_TIERS = ECTier.values();
    public static final FTTier[] FLUID_TANK_TIERS = FTTier.values();
    public static final CTTier[] CHEMICAL_TANK_TIERS = CTTier.values();
    public static final RWBTier[] RADIOACTIVE_BARREL_TIER = RWBTier.values();
}