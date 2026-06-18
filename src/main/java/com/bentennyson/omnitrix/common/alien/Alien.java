package com.bentennyson.omnitrix.common.alien;

import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Identifier;

import java.util.function.Consumer;

import static com.bentennyson.omnitrix.OmnitrixMod.MOD_ID;

/**
 * Os 10 aliens originais da 1ª temporada de Ben 10 (2005),
 * na ordem de aparição na série.
 *
 * Cada alien tem:
 *  - id: usado em NBT/network
 *  - displayName: aparece na GUI holográfica
 *  - color: cor temática (hex) — usada nos efeitos visuais
 *  - effects: efeitos aplicados ao jogador transformado
 *
 * v0.1 beta: sem texturas dos aliens (foco no Omnitrix).
 * Efeitos representam as habilidades de cada um no desenho.
 */
public enum Alien {

    // 1. Heatblast (Pyronita) — episódio 1, "And Then There Were 10"
    HEATBLAST("heatblast", "Heatblast", 0xFF5A00, p -> {
        p.addStatusEffect(new StatusEffectInstance(StatusEffects.FIRE_RESISTANCE, 200, 0, false, false, true));
        p.addStatusEffect(new StatusEffectInstance(StatusEffects.STRENGTH,        200, 1, false, false, true));
        // Toca fogo onde pisa (lidado no handler)
    }),

    // 2. Wildmutt (Vulpimancer) — episódio 1
    WILDMUTT("wildmutt", "Wildmutt", 0xE08020, p -> {
        p.addStatusEffect(new StatusEffectInstance(StatusEffects.NIGHT_VISION, 200, 0, false, false, true));
        p.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED,        200, 2, false, false, true));
        p.addStatusEffect(new StatusEffectInstance(StatusEffects.JUMP_BOOST,   200, 2, false, false, true));
        p.addStatusEffect(new StatusEffectInstance(StatusEffects.STRENGTH,     200, 2, false, false, true));
        // Cego para mobs (passive — ele não tem olhos no desenho), mas com night vision visualizamos
    }),

    // 3. Diamondhead (Petrosapiente) — episódio 2, "Washington B.C."
    DIAMONDHEAD("diamondhead", "Diamondhead", 0x66E0E0, p -> {
        p.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE,   200, 3, false, false, true));
        p.addStatusEffect(new StatusEffectInstance(StatusEffects.STRENGTH,     200, 2, false, false, true));
        p.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS,     200, 0, false, false, true));
    }),

    // 4. XLR8 (Kineceleran) — episódio 3, "Washington B.C."
    XLR8("xlr8", "XLR8", 0x0066FF, p -> {
        p.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED,      200, 5, false, false, true));
        p.addStatusEffect(new StatusEffectInstance(StatusEffects.JUMP_BOOST, 200, 3, false, false, true));
        p.addStatusEffect(new StatusEffectInstance(StatusEffects.HASTE,      200, 2, false, false, true));
    }),

    // 5. Grey Matter (Galvan) — episódio 4, "Permanent Retirement"
    GREY_MATTER("grey_matter", "Grey Matter", 0x9CB0B5, p -> {
        // Pequeno e inteligente — no jogo: invisibilidade leve, mineração rápida, dano fraco
        p.addStatusEffect(new StatusEffectInstance(StatusEffects.HASTE,        200, 4, false, false, true));
        p.addStatusEffect(new StatusEffectInstance(StatusEffects.JUMP_BOOST,   200, 3, false, false, true));
        p.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED,        200, 1, false, false, true));
        p.addStatusEffect(new StatusEffectInstance(StatusEffects.WEAKNESS,     200, 1, false, false, true));
        // Tamanho reduzido lidado no AttributeModifier (futuro: hitbox real)
    }),

    // 6. Four Arms (Tetramand) — episódio 5, "Hunted"
    FOUR_ARMS("four_arms", "Four Arms", 0xC83030, p -> {
        p.addStatusEffect(new StatusEffectInstance(StatusEffects.STRENGTH,   200, 4, false, false, true));
        p.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, 200, 2, false, false, true));
        p.addStatusEffect(new StatusEffectInstance(StatusEffects.HEALTH_BOOST, 200, 3, false, false, true));
        p.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS,   200, 0, false, false, true));
    }),

    // 7. Stinkfly (Lepidopterran) — episódio 6, "Tourist Trap"
    STINKFLY("stinkfly", "Stinkfly", 0x80E040, p -> {
        p.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOW_FALLING, 200, 0, false, false, true));
        p.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED,        200, 2, false, false, true));
        p.addStatusEffect(new StatusEffectInstance(StatusEffects.JUMP_BOOST,   200, 4, false, false, true));
        // Voo real implementado no TransformationTickHandler (creative flight while transformed)
    }),

    // 8. Ripjaws (Piscciss Volann) — episódio 7, "Kevin 11"
    RIPJAWS("ripjaws", "Ripjaws", 0x2080C0, p -> {
        p.addStatusEffect(new StatusEffectInstance(StatusEffects.WATER_BREATHING, 200, 0, false, false, true));
        p.addStatusEffect(new StatusEffectInstance(StatusEffects.DOLPHINS_GRACE,  200, 2, false, false, true));
        p.addStatusEffect(new StatusEffectInstance(StatusEffects.STRENGTH,        200, 2, false, false, true));
        p.addStatusEffect(new StatusEffectInstance(StatusEffects.CONDUIT_POWER,   200, 0, false, false, true));
    }),

    // 9. Upgrade (Galvanic Mechamorph) — episódio 8, "The Alliance"
    UPGRADE("upgrade", "Upgrade", 0x202020, p -> {
        p.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE,  200, 2, false, false, true));
        p.addStatusEffect(new StatusEffectInstance(StatusEffects.REGENERATION, 200, 1, false, false, true));
        p.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED,       200, 1, false, false, true));
        // No desenho ele se funde com tecnologia — efeito visual no render
    }),

    // 10. Ghostfreak (Ectonurita) — episódio 9, "Last Laugh"
    GHOSTFREAK("ghostfreak", "Ghostfreak", 0xE0E0E0, p -> {
        p.addStatusEffect(new StatusEffectInstance(StatusEffects.INVISIBILITY, 200, 0, false, false, true));
        p.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED,        200, 2, false, false, true));
        p.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOW_FALLING, 200, 0, false, false, true));
        p.addStatusEffect(new StatusEffectInstance(StatusEffects.NIGHT_VISION, 200, 0, false, false, true));
    });

    private final String id;
    private final String displayName;
    private final int color;
    private final Consumer<PlayerEntity> applyEffects;

    Alien(String id, String displayName, int color, Consumer<PlayerEntity> applyEffects) {
        this.id = id;
        this.displayName = displayName;
        this.color = color;
        this.applyEffects = applyEffects;
    }

    public String getId() { return id; }
    public String getDisplayName() { return displayName; }
    public int getColor() { return color; }

    public void applyTo(PlayerEntity player) {
        applyEffects.accept(player);
    }

    public Identifier getTexture() {
        return new Identifier(MOD_ID, "textures/entity/aliens/" + id + ".png");
    }

    public Identifier getIconTexture() {
        return new Identifier(MOD_ID, "textures/gui/aliens/" + id + ".png");
    }

    public static Alien fromId(String id) {
        if (id == null) return HEATBLAST;
        for (Alien a : values()) {
            if (a.id.equals(id)) return a;
        }
        return HEATBLAST;
    }

    public Alien next() {
        Alien[] values = values();
        return values[(this.ordinal() + 1) % values.length];
    }

    public Alien previous() {
        Alien[] values = values();
        return values[(this.ordinal() - 1 + values.length) % values.length];
    }
}
