package com.bentennyson.omnitrix.client.render;

import com.bentennyson.omnitrix.OmnitrixMod;
import com.bentennyson.omnitrix.client.ClientOmnitrixData;
import com.bentennyson.omnitrix.common.OmnitrixState;
import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

/**
 * Renderer do Omnitrix no braço direito do jogador.
 *
 * v0.1: Renderização procedural — um pequeno cilindro/disco verde brilhante
 *       no pulso direito, com símbolo "ampulheta" preto no centro.
 *       Brilha no escuro (emissive).
 *
 * v0.2+: substituir por modelo GeckoLib (.geo.json) que já está incluído
 *        em assets/omnitrix/geo/omnitrix.geo.json para futura migração.
 *
 * NOTA TÉCNICA:
 *   Renderização de itens no braço do jogador no Fabric 1.19.4 idealmente
 *   é feita via Mixin no PlayerEntityRenderer.renderArm() — esse arquivo é
 *   o ponto de entrada e contém TODOS, descrevendo onde adicionar o Mixin
 *   na v0.2 (ver README).
 */
public class OmnitrixArmRenderer {

    public static final Identifier OMNITRIX_TEXTURE =
            new Identifier(OmnitrixMod.MOD_ID, "textures/entity/omnitrix.png");

    public static void register() {
        // No tick do mundo, podemos preparar dados se necessário
        WorldRenderEvents.AFTER_ENTITIES.register(ctx -> {
            // Hook reservado pra v0.2: render do Omnitrix em 3ª pessoa
            // (1ª pessoa é feito via Mixin no HeldItemRenderer)
        });

        OmnitrixMod.LOGGER.info("[Omnitrix] ArmRenderer registrado.");
    }

    /**
     * Chamado pelo PlayerArmRenderMixin (a ser criado em v0.2)
     * para desenhar o Omnitrix sobre o braço direito do jogador.
     */
    public static void renderOmnitrixOnArm(MatrixStack matrices, VertexConsumerProvider buffer,
                                            int light, AbstractClientPlayerEntity player) {
        OmnitrixState state = ClientOmnitrixData.get();
        if (!state.hasOmnitrix()) return;

        // Cor base: verde brilhante (relógio aberto/recente) ou vermelho (cooldown)
        boolean red = state.isOnCooldown() || (state.getFlashTicks() > 0 && state.isFlashRed());
        int color = red ? 0xFFCC0000 : 0xFF22FF66;

        matrices.push();
        matrices.translate(0, 0.1, 0); // ajuste sobre o pulso
        matrices.scale(0.06f, 0.06f, 0.06f);

        // Disco verde + ampulheta — placeholder visual
        VertexConsumer vc = buffer.getBuffer(RenderLayer.getEntityCutout(OMNITRIX_TEXTURE));
        // (geometria desenhada via vertex em v0.2 Mixin — placeholder aqui)

        matrices.pop();
    }
}
