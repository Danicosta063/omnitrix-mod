package com.bentennyson.omnitrix.client.net;

import com.bentennyson.omnitrix.client.ClientOmnitrixData;
import com.bentennyson.omnitrix.common.network.OmnitrixNetworking;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.nbt.NbtCompound;

public class OmnitrixClientNetworking {

    public static void register() {
        ClientPlayNetworking.registerGlobalReceiver(OmnitrixNetworking.S2C_SYNC_STATE,
            (client, handler, buf, sender) -> {
                NbtCompound nbt = buf.readNbt();
                client.execute(() -> {
                    if (nbt != null) {
                        ClientOmnitrixData.get().fromNbt(nbt);
                    }
                });
            });
    }

    public static void sendOpenSelector() {
        ClientPlayNetworking.send(OmnitrixNetworking.C2S_OPEN_SELECTOR,
                net.fabricmc.fabric.api.networking.v1.PacketByteBufs.empty());
    }

    public static void sendCycleAlien(boolean forward) {
        var buf = net.fabricmc.fabric.api.networking.v1.PacketByteBufs.create();
        buf.writeBoolean(forward);
        ClientPlayNetworking.send(OmnitrixNetworking.C2S_CYCLE_ALIEN, buf);
    }

    public static void sendConfirmTransform() {
        ClientPlayNetworking.send(OmnitrixNetworking.C2S_CONFIRM_TRANSFORM,
                net.fabricmc.fabric.api.networking.v1.PacketByteBufs.empty());
    }
}
