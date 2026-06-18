package com.bentennyson.omnitrix.common.event;

import com.bentennyson.omnitrix.common.OmnitrixState;
import com.bentennyson.omnitrix.common.OmnitrixStateProvider;
import com.bentennyson.omnitrix.common.network.OmnitrixNetworking;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.network.ServerPlayerEntity;

/**
 * Garante que o estado do Omnitrix sobreviva à morte (respawn) e
 * sincroniza estado com o cliente ao logar/respawnar.
 */
public class PlayerPersistenceHandler {

    public static void register() {
        // Copia estado quando o jogador respawna (morte ou viagem entre dimensões)
        ServerPlayerEvents.COPY_FROM.register((oldPlayer, newPlayer, alive) -> {
            OmnitrixState oldState = OmnitrixStateProvider.get(oldPlayer);
            OmnitrixState newState = OmnitrixStateProvider.get(newPlayer);
            // Copiar via NBT é o jeito mais seguro
            newState.fromNbt(oldState.toNbt());

            // Se morreu transformado, destransforma (não faz sentido renascer alien)
            if (!alive && newState.isTransformed()) {
                newState.endTransformation();
            }
        });

        // Ao conectar, sincronizar estado com o cliente
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayerEntity player = handler.player;
            OmnitrixNetworking.syncToClient(player);
        });

        // Ao respawnar, sincronizar
        ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> {
            OmnitrixNetworking.syncToClient(newPlayer);
        });
    }
}
