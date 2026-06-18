package com.bentennyson.omnitrix.common;

import com.bentennyson.omnitrix.common.alien.Alien;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.entity.player.PlayerEntity;

/**
 * Estado completo do Omnitrix de um jogador.
 *
 * Salvo no NBT persistente do jogador via PlayerPersistenceHandler.
 * Sobrevive a morte, log out, troca de dimensão.
 */
public class OmnitrixState {

    /** Jogador encontrou o meteorito e está com o Omnitrix preso no braço. */
    private boolean hasOmnitrix = false;

    /** Alien atualmente selecionado no seletor (não necessariamente transformado). */
    private Alien selectedAlien = Alien.HEATBLAST;

    /**
     * Estado do seletor:
     *  - false = relógio fechado (vermelho), pronto pra abrir com G
     *  - true  = seletor aberto (verde), círculo girando, esperando J pra transformar
     */
    private boolean selectorOpen = false;

    /** Está atualmente transformado em algum alien. */
    private boolean transformed = false;
    private Alien currentForm = null;

    /** Ticks restantes na transformação (2 min = 2400 ticks). */
    private int transformTicksLeft = 0;

    /** Cooldown de recarga (1 min = 1200 ticks). 0 = pronto pra usar. */
    private int cooldownTicks = 0;

    /** Tela verde/vermelha flash (40 ticks = 2s). */
    private int flashTicks = 0;
    private boolean flashRed = false; // false=verde (transformação), true=vermelho (destransformação/timeout)

    // === Constantes (mecânica fiel ao desenho clássico) ===
    public static final int TRANSFORM_DURATION_TICKS = 20 * 60 * 2;  // 2 minutos
    public static final int COOLDOWN_TICKS           = 20 * 60 * 1;  // 1 minuto
    public static final int FLASH_DURATION_TICKS     = 40;           // 2 segundos
    public static final float RANDOM_ALIEN_CHANCE    = 0.20f;        // 20% se forçar durante cooldown

    // === Getters / Setters ===
    public boolean hasOmnitrix() { return hasOmnitrix; }
    public void setHasOmnitrix(boolean v) { hasOmnitrix = v; }

    public Alien getSelectedAlien() { return selectedAlien; }
    public void setSelectedAlien(Alien a) { selectedAlien = a; }

    public boolean isSelectorOpen() { return selectorOpen; }
    public void setSelectorOpen(boolean v) { selectorOpen = v; }

    public boolean isTransformed() { return transformed; }
    public Alien getCurrentForm() { return currentForm; }

    public int getTransformTicksLeft() { return transformTicksLeft; }
    public int getCooldownTicks() { return cooldownTicks; }
    public int getFlashTicks() { return flashTicks; }
    public boolean isFlashRed() { return flashRed; }

    public boolean isOnCooldown() { return cooldownTicks > 0; }

    /** Inicia transformação no alien dado, gera flash verde, marca duração. */
    public void startTransformation(Alien alien) {
        this.transformed = true;
        this.currentForm = alien;
        this.transformTicksLeft = TRANSFORM_DURATION_TICKS;
        this.flashTicks = FLASH_DURATION_TICKS;
        this.flashRed = false;
        this.selectorOpen = false;
    }

    /** Destransforma — flash vermelho, ativa cooldown. */
    public void endTransformation() {
        this.transformed = false;
        this.currentForm = null;
        this.transformTicksLeft = 0;
        this.cooldownTicks = COOLDOWN_TICKS;
        this.flashTicks = FLASH_DURATION_TICKS;
        this.flashRed = true;
    }

    /** Tick de servidor — avança contadores. */
    public boolean tick(PlayerEntity player) {
        boolean changed = false;

        if (transformed && transformTicksLeft > 0) {
            transformTicksLeft--;
            // Re-aplica efeitos a cada 8s (160 ticks) pra não sumirem
            if (transformTicksLeft % 160 == 0 && currentForm != null) {
                currentForm.applyTo(player);
            }
            if (transformTicksLeft <= 0) {
                endTransformation();
            }
            changed = true;
        }

        if (cooldownTicks > 0) {
            cooldownTicks--;
            changed = true;
        }

        if (flashTicks > 0) {
            flashTicks--;
            changed = true;
        }

        return changed;
    }

    // === Serialization ===
    public NbtCompound toNbt() {
        NbtCompound tag = new NbtCompound();
        tag.putBoolean("HasOmnitrix", hasOmnitrix);
        tag.putString("SelectedAlien", selectedAlien.getId());
        tag.putBoolean("SelectorOpen", selectorOpen);
        tag.putBoolean("Transformed", transformed);
        if (currentForm != null) tag.putString("CurrentForm", currentForm.getId());
        tag.putInt("TransformTicksLeft", transformTicksLeft);
        tag.putInt("CooldownTicks", cooldownTicks);
        tag.putInt("FlashTicks", flashTicks);
        tag.putBoolean("FlashRed", flashRed);
        return tag;
    }

    public void fromNbt(NbtCompound tag) {
        hasOmnitrix = tag.getBoolean("HasOmnitrix");
        selectedAlien = Alien.fromId(tag.getString("SelectedAlien"));
        selectorOpen = tag.getBoolean("SelectorOpen");
        transformed = tag.getBoolean("Transformed");
        currentForm = tag.contains("CurrentForm") ? Alien.fromId(tag.getString("CurrentForm")) : null;
        transformTicksLeft = tag.getInt("TransformTicksLeft");
        cooldownTicks = tag.getInt("CooldownTicks");
        flashTicks = tag.getInt("FlashTicks");
        flashRed = tag.getBoolean("FlashRed");
    }
}
