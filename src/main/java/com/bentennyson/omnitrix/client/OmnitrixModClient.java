package com.bentennyson.omnitrix.client;

import com.bentennyson.omnitrix.OmnitrixMod;
import com.bentennyson.omnitrix.client.gui.HolographicOverlay;
import com.bentennyson.omnitrix.client.gui.TransformationFlashOverlay;
import com.bentennyson.omnitrix.client.keybind.OmnitrixKeybinds;
import com.bentennyson.omnitrix.client.net.OmnitrixClientNetworking;
import com.bentennyson.omnitrix.client.render.OmnitrixArmRenderer;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;

public class OmnitrixModClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        OmnitrixMod.LOGGER.info("[Omnitrix Mod] Cliente inicializado.");

        // Keybinds (G e J)
        OmnitrixKeybinds.register();

        // Tick do cliente — detectar tecla, animações
        ClientTickEvents.END_CLIENT_TICK.register(OmnitrixKeybinds::onClientTick);

        // Receber sync de estado do servidor
        OmnitrixClientNetworking.register();

        // Overlays HUD (seletor holográfico, flash verde/vermelho)
        HudRenderCallback.EVENT.register(new HolographicOverlay());
        HudRenderCallback.EVENT.register(new TransformationFlashOverlay());

        // Renderização do Omnitrix no braço do jogador
        OmnitrixArmRenderer.register();
    }
}
