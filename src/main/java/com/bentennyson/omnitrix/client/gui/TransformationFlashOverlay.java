package com.bentennyson.omnitrix.client.gui;

import com.bentennyson.omnitrix.client.ClientOmnitrixData;
import com.bentennyson.omnitrix.common.OmnitrixState;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.util.math.MatrixStack;

/**
 * Flash de tela durante a transformação:
 *  - Verde por 2 segundos quando transforma
 *  - Vermelho por 2 segundos quando destransforma
 *
 * Fade in/out suave nos primeiros e últimos 5 ticks.
 */
public class TransformationFlashOverlay implements HudRenderCallback {

    @Override
    public void onHudRender(MatrixStack matrices, float tickDelta) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) return;

        OmnitrixState state = ClientOmnitrixData.get();
        if (state.getFlashTicks() <= 0) return;

        int sw = mc.getWindow().getScaledWidth();
        int sh = mc.getWindow().getScaledHeight();

        int ticks = state.getFlashTicks();
        int maxTicks = OmnitrixState.FLASH_DURATION_TICKS;

        // Alpha fade: começo e fim suaves, meio cheio
        float progress = ticks / (float) maxTicks;
        float alpha;
        if (progress > 0.8f) {
            alpha = (1.0f - progress) * 5f; // entrada (últimos 0.2 do contador)
        } else if (progress < 0.2f) {
            alpha = progress * 5f; // saída (primeiros 0.2)
        } else {
            alpha = 1.0f;
        }
        alpha = Math.max(0f, Math.min(1f, alpha)) * 0.85f; // até 85% opaco no pico

        int a = (int) (alpha * 255);
        // Verde (transformação) ou vermelho (destransformação)
        int color = state.isFlashRed()
                ? (a << 24) | 0x00CC0000
                : (a << 24) | 0x0000FF00;

        DrawableHelper.fill(matrices, 0, 0, sw, sh, color);
    }
}
