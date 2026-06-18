package com.bentennyson.omnitrix.client.gui;

import com.bentennyson.omnitrix.client.ClientOmnitrixData;
import com.bentennyson.omnitrix.common.OmnitrixState;
import com.bentennyson.omnitrix.common.alien.Alien;
import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;

/**
 * Overlay holográfico do Omnitrix (estilo desenho clássico 2005).
 *
 * Aparece quando o seletor está aberto:
 *  - Círculo verde brilhante girando lentamente no centro da tela
 *  - Ícone (quadrado verde por enquanto, sem textura) do alien selecionado dentro
 *  - Nome do alien embaixo
 *  - Cor pulsando em verde
 *
 * Quando transformado, mostra timer de duração no canto.
 * Quando em cooldown, mostra contador de recarga.
 */
public class HolographicOverlay implements HudRenderCallback {

    private float rotation = 0f;

    @Override
    public void onHudRender(MatrixStack matrices, float tickDelta) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.options.hudHidden) return;

        OmnitrixState state = ClientOmnitrixData.get();
        if (!state.hasOmnitrix()) return;

        int sw = mc.getWindow().getScaledWidth();
        int sh = mc.getWindow().getScaledHeight();

        rotation += 1.5f * tickDelta;
        if (rotation > 360) rotation -= 360;

        // === Seletor aberto: holograma circular giratório ===
        if (state.isSelectorOpen() && !state.isTransformed()) {
            renderSelector(matrices, mc, sw, sh, state, tickDelta);
        }

        // === Transformado: timer ===
        if (state.isTransformed()) {
            renderTransformTimer(matrices, mc, sw, sh, state);
        }

        // === Cooldown ===
        if (state.isOnCooldown() && !state.isTransformed()) {
            renderCooldown(matrices, mc, sw, sh, state);
        }
    }

    private void renderSelector(MatrixStack matrices, MinecraftClient mc, int sw, int sh,
                                 OmnitrixState state, float tickDelta) {
        int cx = sw / 2;
        int cy = sh / 2;
        int radius = 60;

        // Brilho pulsante (sin wave)
        float pulse = (MathHelper.sin((mc.player.age + tickDelta) * 0.15f) + 1) * 0.5f;
        int alpha = (int) (180 + pulse * 60); // 180-240
        int green = 0xAA000000 | (alpha << 16) | 0x00FF66;
        int greenDim = 0x4000FF00;
        int greenBright = 0xFF66FF66;

        // Anel externo (giratório — desenha 8 marcadores ao redor)
        Alien[] aliens = Alien.values();
        Alien selected = state.getSelectedAlien();
        int selectedIdx = selected.ordinal();

        for (int i = 0; i < aliens.length; i++) {
            double angle = Math.toRadians(rotation + (i * 360.0 / aliens.length));
            int x = cx + (int) (Math.cos(angle) * radius);
            int y = cy + (int) (Math.sin(angle) * radius);
            int size = (i == selectedIdx) ? 10 : 6;
            int color = (i == selectedIdx) ? greenBright : greenDim;
            DrawableHelper.fill(matrices, x - size/2, y - size/2, x + size/2, y + size/2, color);
        }

        // Anel central (representa o "vidro" do Omnitrix)
        for (int r = radius - 5; r <= radius + 5; r += 2) {
            drawRing(matrices, cx, cy, r, green);
        }

        // Centro: símbolo "hourglass" do Omnitrix (placeholder verde)
        int innerSize = 30;
        DrawableHelper.fill(matrices,
                cx - innerSize/2, cy - innerSize/2,
                cx + innerSize/2, cy + innerSize/2,
                0xAA002200);
        // Cor do alien selecionado
        int alienColor = 0xFF000000 | selected.getColor();
        DrawableHelper.fill(matrices,
                cx - innerSize/2 + 4, cy - innerSize/2 + 4,
                cx + innerSize/2 - 4, cy + innerSize/2 - 4,
                alienColor);

        // Nome do alien
        TextRenderer tr = mc.textRenderer;
        Text name = Text.literal("§a§l" + selected.getDisplayName());
        int nw = tr.getWidth(name);
        DrawableHelper.drawTextWithShadow(matrices, tr, name, cx - nw/2, cy + radius + 16, 0xFFFFFF);

        // Instruções
        Text hint = Text.literal("§7[G] mudar  §7[J] §atransformar");
        int hw = tr.getWidth(hint);
        DrawableHelper.drawTextWithShadow(matrices, tr, hint, cx - hw/2, cy + radius + 32, 0xCCCCCC);
    }

    private void renderTransformTimer(MatrixStack matrices, MinecraftClient mc, int sw, int sh,
                                       OmnitrixState state) {
        TextRenderer tr = mc.textRenderer;
        int secs = state.getTransformTicksLeft() / 20;
        int m = secs / 60;
        int s = secs % 60;
        String txt = String.format("§a⌬ %s §7- §e%d:%02d", state.getCurrentForm().getDisplayName(), m, s);
        DrawableHelper.drawTextWithShadow(matrices, tr, Text.literal(txt), 10, 10, 0xFFFFFF);
    }

    private void renderCooldown(MatrixStack matrices, MinecraftClient mc, int sw, int sh,
                                 OmnitrixState state) {
        TextRenderer tr = mc.textRenderer;
        int secs = state.getCooldownTicks() / 20;
        String txt = String.format("§c⌬ Recarregando: §e%ds", secs);
        DrawableHelper.drawTextWithShadow(matrices, tr, Text.literal(txt), 10, 10, 0xFFFFFF);
    }

    /** Anel simples desenhado com pixels — sem precisar de textura. */
    private void drawRing(MatrixStack matrices, int cx, int cy, int radius, int color) {
        for (int deg = 0; deg < 360; deg += 3) {
            double rad = Math.toRadians(deg);
            int x = cx + (int) (Math.cos(rad) * radius);
            int y = cy + (int) (Math.sin(rad) * radius);
            DrawableHelper.fill(matrices, x, y, x + 2, y + 2, color);
        }
    }
}
