package com.bentennyson.omnitrix.mixin;

import com.bentennyson.omnitrix.common.OmnitrixDataAccessor;
import com.bentennyson.omnitrix.common.OmnitrixState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Anexa OmnitrixState ao PlayerEntity e o serializa no NBT.
 *
 * IMPORTANTE: Como o NBT é parte do jogador (e não do inventário),
 * o Omnitrix sobrevive à morte automaticamente — exatamente como na série.
 */
@Mixin(PlayerEntity.class)
public abstract class PlayerEntityMixin implements OmnitrixDataAccessor {

    @Unique
    private OmnitrixState omnitrix$state = new OmnitrixState();

    @Override
    public OmnitrixState omnitrix$getState() {
        return omnitrix$state;
    }

    @Override
    public void omnitrix$setState(OmnitrixState state) {
        this.omnitrix$state = state;
    }

    @Inject(method = "writeCustomDataToNbt", at = @At("HEAD"))
    private void omnitrix$writeNbt(NbtCompound nbt, CallbackInfo ci) {
        if (omnitrix$state != null) {
            nbt.put("OmnitrixData", omnitrix$state.toNbt());
        }
    }

    @Inject(method = "readCustomDataFromNbt", at = @At("HEAD"))
    private void omnitrix$readNbt(NbtCompound nbt, CallbackInfo ci) {
        if (nbt.contains("OmnitrixData")) {
            if (omnitrix$state == null) omnitrix$state = new OmnitrixState();
            omnitrix$state.fromNbt(nbt.getCompound("OmnitrixData"));
        }
    }
}
