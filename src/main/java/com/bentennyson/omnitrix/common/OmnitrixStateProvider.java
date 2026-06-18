package com.bentennyson.omnitrix.common;

import net.minecraft.entity.player.PlayerEntity;

import java.util.WeakHashMap;
import java.util.Map;

/**
 * Acessa o OmnitrixState de um jogador.
 *
 * No Fabric (sem capabilities como Forge) usamos:
 *  - Mixin no PlayerEntity para guardar o estado (ver OmnitrixDataAccessor)
 *  - Persistência via writeCustomDataToNbt / readCustomDataFromNbt (via Mixin)
 *
 * Esta classe é a porta de entrada — sempre use OmnitrixStateProvider.get(player).
 */
public final class OmnitrixStateProvider {

    // Fallback in-memory cache (caso mixin não esteja carregado por algum motivo)
    private static final Map<PlayerEntity, OmnitrixState> FALLBACK = new WeakHashMap<>();

    private OmnitrixStateProvider() {}

    public static OmnitrixState get(PlayerEntity player) {
        if (player instanceof OmnitrixDataAccessor accessor) {
            OmnitrixState state = accessor.omnitrix$getState();
            if (state == null) {
                state = new OmnitrixState();
                accessor.omnitrix$setState(state);
            }
            return state;
        }
        return FALLBACK.computeIfAbsent(player, p -> new OmnitrixState());
    }
}
