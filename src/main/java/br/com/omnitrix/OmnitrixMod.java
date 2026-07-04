package br.com.omnitrix;

import com.mojang.logging.LogUtils;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;

@Mod(OmnitrixMod.MOD_ID)
public class OmnitrixMod {

    // O identificador unico do mod (tem que ser igual ao do mods.toml)
    public static final String MOD_ID = "omnitrix";

    // O "diario de bordo" que escreve mensagens no log do jogo
    private static final Logger LOGGER = LogUtils.getLogger();

    public OmnitrixMod() {
        LOGGER.info("== Omnitrix carregado! O relogio esta pronto. ==");
    }
}
