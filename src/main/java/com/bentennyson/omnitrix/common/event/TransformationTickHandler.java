package com.bentennyson.omnitrix.common.event;

import com.bentennyson.omnitrix.common.OmnitrixState;
import com.bentennyson.omnitrix.common.OmnitrixStateProvider;
import com.bentennyson.omnitrix.common.alien.Alien;
import com.bentennyson.omnitrix.common.network.OmnitrixNetworking;
import com.bentennyson.omnitrix.common.registry.ModSounds;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;

/**
 * Tick global do servidor: avança timers de transformação de cada jogador
 * com Omnitrix e aplica efeitos contínuos (ex: voo do Stinkfly, fogo do Heatblast).
 */
public class TransformationTickHandler {

    public static void onServerTick(MinecraftServer server) {
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            OmnitrixState state = OmnitrixStateProvider.get(player);
            if (!state.hasOmnitrix()) continue;

            boolean wasTransformed = state.isTransformed();
            boolean wasFlashing = state.getFlashTicks() > 0;
            int prevCooldown = state.getCooldownTicks();

            boolean changed = state.tick(player);

            // Detectar transição: transformado -> destransformado (timeout natural)
            if (wasTransformed && !state.isTransformed()) {
                player.world.playSound(null, player.getX(), player.getY(), player.getZ(),
                        ModSounds.OMNITRIX_DETRANSFORM, SoundCategory.PLAYERS, 1.0f, 1.0f);
                // Remove efeitos do alien
                player.clearStatusEffects();
            }

            // Habilidades passivas contínuas do alien
            if (state.isTransformed() && state.getCurrentForm() != null) {
                applyContinuousAbilities(player, state.getCurrentForm());
            }

            // Slow-motion enquanto seletor aberto (concentrado escolhendo o alien)
            if (state.isSelectorOpen() && !state.isTransformed()) {
                player.addStatusEffect(new StatusEffectInstance(
                        StatusEffects.SLOWNESS, 4, 1, false, false, false));
            }

            if (changed) {
                OmnitrixNetworking.syncToClient(player);
            }
        }
    }

    /** Habilidades passivas contínuas, aplicadas a cada tick durante a transformação. */
    private static void applyContinuousAbilities(ServerPlayerEntity player, Alien alien) {
        switch (alien) {
            case STINKFLY -> {
                // Voo tipo criativo enquanto transformado
                if (!player.getAbilities().allowFlying) {
                    player.getAbilities().allowFlying = true;
                    player.sendAbilitiesUpdate();
                }
            }
            case HEATBLAST -> {
                // Imune a fogo total
                player.setFireTicks(0);
            }
            case GHOSTFREAK -> {
                // Pode atravessar lugares estreitos (intangibilidade leve via slow falling)
            }
            case RIPJAWS -> {
                // Cura levemente na água
                if (player.isSubmergedInWater() && player.age % 40 == 0) {
                    player.heal(1.0f);
                }
            }
            default -> {}
        }

        // Limpa voo se não for Stinkfly
        if (alien != Alien.STINKFLY && player.getAbilities().allowFlying && !player.isCreative() && !player.isSpectator()) {
            player.getAbilities().allowFlying = false;
            player.getAbilities().flying = false;
            player.sendAbilitiesUpdate();
        }
    }
}
