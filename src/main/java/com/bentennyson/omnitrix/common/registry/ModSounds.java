package com.bentennyson.omnitrix.common.registry;

import com.bentennyson.omnitrix.OmnitrixMod;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;

/**
 * Sons do Omnitrix. Os arquivos .ogg ficam em assets/omnitrix/sounds/.
 *
 * ⚠️ v0.1 beta: os arquivos .ogg incluídos são SILÊNCIO (placeholder).
 * Para experiência fiel ao desenho, substitua pelos sons reais da série
 * (disponíveis em soundeffects.fandom.com/wiki/Ben_10).
 */
public class ModSounds {

    public static final SoundEvent OMNITRIX_OPEN         = register("omnitrix_open");
    public static final SoundEvent OMNITRIX_CYCLE        = register("omnitrix_cycle");
    public static final SoundEvent OMNITRIX_TRANSFORM    = register("omnitrix_transform");
    public static final SoundEvent OMNITRIX_DETRANSFORM  = register("omnitrix_detransform");
    public static final SoundEvent OMNITRIX_EQUIP        = register("omnitrix_equip");
    public static final SoundEvent OMNITRIX_FAIL         = register("omnitrix_fail");

    public static final SoundEvent METEORITE_FALL        = register("meteorite_fall");
    public static final SoundEvent METEORITE_IMPACT      = register("meteorite_impact");

    private static SoundEvent register(String name) {
        Identifier id = OmnitrixMod.id(name);
        return Registry.register(Registry.SOUND_EVENT, id, SoundEvent.of(id));
    }

    public static void register() {
        OmnitrixMod.LOGGER.info("[Omnitrix] Sons registrados.");
    }
}
