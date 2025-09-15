package com.leclowndu93150.oneko;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;

@Mod(Constants.MOD_ID)
public class Oneko {

    public Oneko(IEventBus eventBus) {
        CommonClass.init();
    }
}
