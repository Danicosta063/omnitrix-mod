package com.bendez.omnitrix;

import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;
import com.mojang.logging.LogUtils;

@Mod(OmnitrixMod.MOD_ID)
public class OmnitrixMod {

    public static final String MOD_ID = "omnitrix";

    private static final Logger LOGGER = LogUtils.getLogger();

    public OmnitrixMod() {
        LOGGER.info("======================================");
        LOGGER.info("  Omnitrix carregado! Pronto pra acao.");
        LOGGER.info("  Um mod fiel ao Ben 10 Classico.");
        LOGGER.info("======================================");
    }
}
