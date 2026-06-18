package com.bentennyson.omnitrix;

import com.bentennyson.omnitrix.common.event.MeteoriteEvent;
import com.bentennyson.omnitrix.common.event.PlayerPersistenceHandler;
import com.bentennyson.omnitrix.common.event.TransformationTickHandler;
import com.bentennyson.omnitrix.common.network.OmnitrixNetworking;
import com.bentennyson.omnitrix.common.registry.ModItems;
import com.bentennyson.omnitrix.common.registry.ModSounds;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.bernie.geckolib.GeckoLib;

public class OmnitrixMod implements ModInitializer {
    public static final String MOD_ID = "omnitrix";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public static Identifier id(String path) {
        return new Identifier(MOD_ID, path);
    }

    @Override
    public void onInitialize() {
        LOGGER.info("[Omnitrix Mod] Inicializando v0.1 beta — É hora do herói!");

        // GeckoLib initialization
        GeckoLib.initialize();

        // Registries
        ModItems.register();
        ModSounds.register();

        // Networking (client <-> server packets)
        OmnitrixNetworking.registerServer();

        // Persistent player data (Omnitrix sobrevive à morte)
        PlayerPersistenceHandler.register();

        // Transformation timer ticking on server
        ServerTickEvents.END_SERVER_TICK.register(TransformationTickHandler::onServerTick);

        // Meteorite cinematic event
        MeteoriteEvent.register();

        LOGGER.info("[Omnitrix Mod] Carregado com sucesso. Os 10 aliens originais aguardam.");
    }
}
