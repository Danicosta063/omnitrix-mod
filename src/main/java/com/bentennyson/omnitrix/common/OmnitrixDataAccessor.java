package com.bentennyson.omnitrix.common;

/**
 * Interface implementada via Mixin no PlayerEntity para anexar OmnitrixState ao jogador.
 * Garante que o estado persiste com o jogador (NBT) e sobrevive a morte.
 */
public interface OmnitrixDataAccessor {
    OmnitrixState omnitrix$getState();
    void omnitrix$setState(OmnitrixState state);
}
