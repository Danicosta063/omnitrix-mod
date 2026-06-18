package com.bentennyson.omnitrix.common.network;

import com.bentennyson.omnitrix.OmnitrixMod;
import com.bentennyson.omnitrix.common.OmnitrixState;
import com.bentennyson.omnitrix.common.OmnitrixStateProvider;
import com.bentennyson.omnitrix.common.alien.Alien;
import com.bentennyson.omnitrix.common.registry.ModSounds;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

/**
 * Pacotes de rede do Omnitrix.
 *
 * CLIENTE → SERVIDOR:
 *  - OPEN_SELECTOR (tecla G): abre o seletor holográfico
 *  - CYCLE_ALIEN (segurar G): muda o alien selecionado
 *  - CONFIRM_TRANSFORM (tecla J): confirma transformação
 *
 * SERVIDOR → CLIENTE:
 *  - SYNC_STATE: estado completo do Omnitrix do jogador
 */
public class OmnitrixNetworking {

    public static final Identifier C2S_OPEN_SELECTOR     = OmnitrixMod.id("c2s_open_selector");
    public static final Identifier C2S_CYCLE_ALIEN       = OmnitrixMod.id("c2s_cycle_alien");
    public static final Identifier C2S_CONFIRM_TRANSFORM = OmnitrixMod.id("c2s_confirm_transform");
    public static final Identifier S2C_SYNC_STATE        = OmnitrixMod.id("s2c_sync_state");

    public static void registerServer() {
        // G pressionado uma vez — abre seletor
        ServerPlayNetworking.registerGlobalReceiver(C2S_OPEN_SELECTOR, (server, player, handler, buf, sender) -> {
            server.execute(() -> handleOpenSelector(player));
        });

        // G segurado — cicla alien
        ServerPlayNetworking.registerGlobalReceiver(C2S_CYCLE_ALIEN, (server, player, handler, buf, sender) -> {
            boolean forward = buf.readBoolean();
            server.execute(() -> handleCycleAlien(player, forward));
        });

        // J pressionado — confirma transformação
        ServerPlayNetworking.registerGlobalReceiver(C2S_CONFIRM_TRANSFORM, (server, player, handler, buf, sender) -> {
            server.execute(() -> handleConfirmTransform(player));
        });
    }

    private static void handleOpenSelector(ServerPlayerEntity player) {
        OmnitrixState s = OmnitrixStateProvider.get(player);
        if (!s.hasOmnitrix()) {
            player.sendMessage(Text.literal("§c[Omnitrix] Você ainda não encontrou o Omnitrix!"), true);
            return;
        }
        if (s.isTransformed()) {
            player.sendMessage(Text.literal("§e[Omnitrix] Você já está transformado em §a" + s.getCurrentForm().getDisplayName() + "§e."), true);
            return;
        }
        if (s.isSelectorOpen()) {
            // Ao tocar G de novo com seletor aberto: cicla pro próximo alien
            s.setSelectedAlien(s.getSelectedAlien().next());
            player.world.playSound(null, player.getX(), player.getY(), player.getZ(),
                    ModSounds.OMNITRIX_CYCLE, SoundCategory.PLAYERS, 0.6f, 1.0f);
        } else {
            s.setSelectorOpen(true);
            player.world.playSound(null, player.getX(), player.getY(), player.getZ(),
                    ModSounds.OMNITRIX_OPEN, SoundCategory.PLAYERS, 1.0f, 1.0f);
            player.sendMessage(Text.literal("§a⌬ Seletor aberto — alien: §e" + s.getSelectedAlien().getDisplayName()), true);
        }
        syncToClient(player);
    }

    private static void handleCycleAlien(ServerPlayerEntity player, boolean forward) {
        OmnitrixState s = OmnitrixStateProvider.get(player);
        if (!s.hasOmnitrix() || s.isTransformed()) return;

        // "Não pode voltar atrás" do design — uma vez aberto, é obrigatório transformar
        // mas pode trocar o alien selecionado livremente
        if (!s.isSelectorOpen()) s.setSelectorOpen(true);

        Alien next = forward ? s.getSelectedAlien().next() : s.getSelectedAlien().previous();
        s.setSelectedAlien(next);

        player.world.playSound(null, player.getX(), player.getY(), player.getZ(),
                ModSounds.OMNITRIX_CYCLE, SoundCategory.PLAYERS, 0.5f, 1.0f);
        syncToClient(player);
    }

    private static void handleConfirmTransform(ServerPlayerEntity player) {
        OmnitrixState s = OmnitrixStateProvider.get(player);
        if (!s.hasOmnitrix()) return;

        // Já transformado? Destransforma (tapa no relógio durante a transformação)
        if (s.isTransformed()) {
            // Force end (com cooldown normal)
            s.endTransformation();
            player.clearStatusEffects();
            player.world.playSound(null, player.getX(), player.getY(), player.getZ(),
                    ModSounds.OMNITRIX_DETRANSFORM, SoundCategory.PLAYERS, 1.0f, 1.0f);
            syncToClient(player);
            return;
        }

        // Em cooldown? Pequena chance de alien aleatório (fiel ao desenho)
        Alien toTransform;
        if (s.isOnCooldown()) {
            if (player.world.random.nextFloat() < OmnitrixState.RANDOM_ALIEN_CHANCE) {
                // Alien aleatório
                Alien[] all = Alien.values();
                toTransform = all[player.world.random.nextInt(all.length)];
                player.sendMessage(Text.literal("§c⌬ Mau funcionamento! Transformação aleatória: §e" + toTransform.getDisplayName()), true);
                // Zera cooldown pra permitir a transformação
                s.endTransformation();
                s.tick(player); // não conta, só pra normalizar
            } else {
                player.sendMessage(Text.literal("§c⌬ Omnitrix recarregando... §7(" + (s.getCooldownTicks() / 20) + "s)"), true);
                return;
            }
        } else {
            if (!s.isSelectorOpen()) {
                player.sendMessage(Text.literal("§e⌬ Abra o seletor primeiro com §6G§e!"), true);
                return;
            }
            toTransform = s.getSelectedAlien();
        }

        // Som de transformação + aplicar
        player.world.playSound(null, player.getX(), player.getY(), player.getZ(),
                ModSounds.OMNITRIX_TRANSFORM, SoundCategory.PLAYERS, 1.2f, 1.0f);
        s.startTransformation(toTransform);
        toTransform.applyTo(player);
        player.sendMessage(Text.literal("§a§l⌬ Transformado em §e" + toTransform.getDisplayName() + "§a§l!"), true);
        syncToClient(player);
    }

    /** Envia estado completo do Omnitrix do jogador pro cliente dele. */
    public static void syncToClient(ServerPlayerEntity player) {
        OmnitrixState s = OmnitrixStateProvider.get(player);
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeNbt(s.toNbt());
        ServerPlayNetworking.send(player, S2C_SYNC_STATE, buf);
    }
}
