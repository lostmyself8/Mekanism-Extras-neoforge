package com.jerry.mekextras.common.registry;

import com.jerry.mekextras.MekanismExtras;
import com.jerry.mekextras.common.ExtraChemicalConstants;
import mekanism.api.chemical.Chemical;
import mekanism.common.registration.impl.ChemicalDeferredRegister;
import mekanism.common.registration.impl.DeferredChemical;
import net.neoforged.bus.api.IEventBus;

public class ExtraGases {
    private ExtraGases() {

    }

    public static final ChemicalDeferredRegister EXTRA_GASES = new ChemicalDeferredRegister(MekanismExtras.MOD_ID);

    public static final DeferredChemical<Chemical> MOLTEN_THERMONUCLEAR = EXTRA_GASES.register(ExtraChemicalConstants.MOLTEN_THERMONUCLEAR);

    public static void register(IEventBus eventBus) {
        EXTRA_GASES.register(eventBus);
    }
}
