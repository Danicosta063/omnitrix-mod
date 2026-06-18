package com.bentennyson.omnitrix.client.keybind;

import com.bentennyson.omnitrix.client.net.OmnitrixClientNetworking;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

/**
 * Teclas:
 *  - G: abre o seletor holográfico. Segurar = ciclar entre aliens automaticamente.
 *  - J: confirma transformação (tapa no relógio).
 *
 * Ambas configuráveis em Opções > Controles > "Omnitrix".
 */
public class OmnitrixKeybinds {

    public static KeyBinding KEY_SELECTOR;  // G
    public static KeyBinding KEY_TRANSFORM; // J

    private static int holdTicks = 0;
    private static int cycleCooldown = 0;
    private static boolean wasSelectorPressed = false;
    private static boolean wasTransformPressed = false;

    public static void register() {
        KEY_SELECTOR = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.omnitrix.selector",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_G,
                "category.omnitrix"
        ));

        KEY_TRANSFORM = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.omnitrix.transform",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_J,
                "category.omnitrix"
        ));
    }

    public static void onClientTick(MinecraftClient client) {
        if (client.player == null) return;

        // === G: SELETOR ===
        boolean pressedG = KEY_SELECTOR.isPressed();
        if (pressedG && !wasSelectorPressed) {
            // tecla apertada agora — abre/cicla
            OmnitrixClientNetworking.sendOpenSelector();
            holdTicks = 0;
        }
        if (pressedG) {
            holdTicks++;
            // Segurar G por mais de 8 ticks (0.4s) começa a ciclar continuamente
            if (holdTicks > 8) {
                if (--cycleCooldown <= 0) {
                    OmnitrixClientNetworking.sendCycleAlien(true);
                    cycleCooldown = 5; // cicla a cada 5 ticks (0.25s)
                }
            }
        } else {
            holdTicks = 0;
            cycleCooldown = 0;
        }
        wasSelectorPressed = pressedG;

        // === J: TRANSFORMAR ===
        boolean pressedJ = KEY_TRANSFORM.isPressed();
        if (pressedJ && !wasTransformPressed) {
            OmnitrixClientNetworking.sendConfirmTransform();
        }
        wasTransformPressed = pressedJ;
    }
}
