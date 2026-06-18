package com.bentennyson.omnitrix.client;

import com.bentennyson.omnitrix.common.OmnitrixState;

/**
 * Cópia do estado do Omnitrix no cliente (sincronizada do servidor).
 * Usado para renderização e HUD — não é a fonte da verdade.
 */
public final class ClientOmnitrixData {

    private static final OmnitrixState STATE = new OmnitrixState();

    private ClientOmnitrixData() {}

    public static OmnitrixState get() {
        return STATE;
    }
}
