package com.bentennyson.omnitrix.common.event;

import com.bentennyson.omnitrix.OmnitrixMod;
import com.bentennyson.omnitrix.common.OmnitrixState;
import com.bentennyson.omnitrix.common.OmnitrixStateProvider;
import com.bentennyson.omnitrix.common.registry.ModSounds;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.block.Blocks;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.Heightmap;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Evento cinematográfico do meteorito que carrega o Omnitrix.
 *
 * Comportamento:
 *  1. Risco verde no céu (partículas de fim de mundo + estrondo distante)
 *  2. Tremor (efeito de náusea curta para imersão)
 *  3. Cratera com fumaça e partículas verdes
 *  4. Permanece brilhando até ser encontrado
 *  5. Ao chegar perto, o jogador recebe o Omnitrix permanentemente
 *
 * Disparo:
 *  - /omnitrix meteorite (comando)
 *  - Aleatório a cada 12h (configurável no futuro)
 */
public class MeteoriteEvent {

    private static final Map<UUID, MeteoriteData> activeMeteors = new HashMap<>();
    private static final int CINEMATIC_TICKS = 100; // 5s de animação queda

    public static void register() {
        // Comando /omnitrix meteorite — útil pra testar e pra primeira vez
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
            dispatcher.register(CommandManager.literal("omnitrix")
                .then(CommandManager.literal("meteorite")
                    .requires(src -> src.hasPermissionLevel(0))
                    .executes(ctx -> {
                        ServerPlayerEntity p = ctx.getSource().getPlayer();
                        if (p == null) return 0;
                        spawnMeteorite(p);
                        return 1;
                    }))
                .then(CommandManager.literal("give")
                    .requires(src -> src.hasPermissionLevel(2))
                    .executes(ctx -> {
                        ServerPlayerEntity p = ctx.getSource().getPlayer();
                        if (p == null) return 0;
                        OmnitrixState state = OmnitrixStateProvider.get(p);
                        state.setHasOmnitrix(true);
                        p.sendMessage(Text.literal("§a[Omnitrix] Você agora possui o Omnitrix!"));
                        return 1;
                    }))
                .then(CommandManager.literal("remove")
                    .requires(src -> src.hasPermissionLevel(2))
                    .executes(ctx -> {
                        ServerPlayerEntity p = ctx.getSource().getPlayer();
                        if (p == null) return 0;
                        OmnitrixState state = OmnitrixStateProvider.get(p);
                        state.setHasOmnitrix(false);
                        p.sendMessage(Text.literal("§c[Omnitrix] Omnitrix removido."));
                        return 1;
                    }))
            )
        );

        // Tick: animar quedas e detectar colisão dos jogadores com o meteoro caído
        ServerTickEvents.END_SERVER_TICK.register(MeteoriteEvent::tickMeteorites);
    }

    public static void spawnMeteorite(ServerPlayerEntity player) {
        ServerWorld world = player.getWorld();
        BlockPos playerPos = player.getBlockPos();

        // Cair a ~60 blocos do jogador, ângulo aleatório
        double angle = world.random.nextDouble() * Math.PI * 2;
        int dx = (int) (Math.cos(angle) * 60);
        int dz = (int) (Math.sin(angle) * 60);
        int targetX = playerPos.getX() + dx;
        int targetZ = playerPos.getZ() + dz;
        int targetY = world.getTopY(Heightmap.Type.WORLD_SURFACE, targetX, targetZ);
        BlockPos target = new BlockPos(targetX, targetY, targetZ);

        // Anuncia para o jogador
        player.sendMessage(Text.literal("§a§l☄ Algo brilhante atravessa o céu..."));

        // Som de estrondo distante já no spawn
        world.playSound(null, target.getX(), target.getY() + 80, target.getZ(),
                ModSounds.METEORITE_FALL, SoundCategory.WEATHER, 8.0f, 0.6f);

        MeteoriteData data = new MeteoriteData();
        data.world = world;
        data.target = target;
        data.ticksLeft = CINEMATIC_TICKS;
        data.summonerId = player.getUuid();
        activeMeteors.put(UUID.randomUUID(), data);
    }

    private static void tickMeteorites(MinecraftServer server) {
        var iterator = activeMeteors.entrySet().iterator();
        while (iterator.hasNext()) {
            var entry = iterator.next();
            MeteoriteData data = entry.getValue();
            data.ticksLeft--;

            ServerWorld world = data.world;
            BlockPos target = data.target;

            // Calcula posição atual do meteoro caindo
            float progress = 1.0f - (data.ticksLeft / (float) CINEMATIC_TICKS);
            double currentY = target.getY() + 80 * (1.0 - progress);
            double currentX = target.getX();
            double currentZ = target.getZ();

            // Risco verde de partículas (cauda)
            for (int i = 0; i < 8; i++) {
                world.spawnParticles(ParticleTypes.HAPPY_VILLAGER,
                        currentX + world.random.nextGaussian() * 0.3,
                        currentY + i * 0.5,
                        currentZ + world.random.nextGaussian() * 0.3,
                        2, 0.1, 0.1, 0.1, 0.01);
                world.spawnParticles(ParticleTypes.END_ROD,
                        currentX, currentY + i * 0.4, currentZ,
                        1, 0.05, 0.05, 0.05, 0.0);
                world.spawnParticles(ParticleTypes.SMOKE,
                        currentX, currentY + i * 0.6, currentZ,
                        2, 0.2, 0.2, 0.2, 0.02);
            }

            // Impacto
            if (data.ticksLeft <= 0) {
                explodeMeteorite(world, target);
                iterator.remove();

                // Avisar o invocador
                ServerPlayerEntity p = server.getPlayerManager().getPlayer(data.summonerId);
                if (p != null) {
                    p.sendMessage(Text.literal("§a§l☄ O meteorito caiu em §e" + target.getX() + ", " + target.getY() + ", " + target.getZ() + "§a! Vá encontrá-lo."));
                    // Tremor (efeito de náusea breve simula a câmera tremendo)
                    p.addStatusEffect(new StatusEffectInstance(
                            StatusEffects.NAUSEA, 40, 0, false, false, false));
                }
            }
        }

        // Detectar jogador chegando perto de um meteorito caído (cratera)
        detectPlayerNearCrater(server);
    }

    private static void explodeMeteorite(ServerWorld world, BlockPos target) {
        // Cratera 5x3x5
        for (int x = -3; x <= 3; x++) {
            for (int z = -3; z <= 3; z++) {
                for (int y = -2; y <= 1; y++) {
                    double dist = Math.sqrt(x*x + z*z + y*y);
                    if (dist <= 3.0) {
                        BlockPos p = target.add(x, y, z);
                        if (dist < 2.0) {
                            world.setBlockState(p, Blocks.AIR.getDefaultState());
                        } else if (world.random.nextFloat() < 0.5f) {
                            world.setBlockState(p, Blocks.OBSIDIAN.getDefaultState());
                        }
                    }
                }
            }
        }
        // Marca do meteorito no centro: bloco brilhante (sea lantern como placeholder)
        world.setBlockState(target, Blocks.SEA_LANTERN.getDefaultState());

        // Boom!
        world.playSound(null, target.getX(), target.getY(), target.getZ(),
                ModSounds.METEORITE_IMPACT, SoundCategory.WEATHER, 16.0f, 0.5f);

        // Fumaça e partículas verdes pós-impacto
        for (int i = 0; i < 200; i++) {
            world.spawnParticles(ParticleTypes.HAPPY_VILLAGER,
                    target.getX() + 0.5, target.getY() + 1, target.getZ() + 0.5,
                    1, 2.0, 1.0, 2.0, 0.05);
            world.spawnParticles(ParticleTypes.LARGE_SMOKE,
                    target.getX() + 0.5, target.getY() + 1, target.getZ() + 0.5,
                    1, 1.5, 1.5, 1.5, 0.02);
        }

        OmnitrixMod.LOGGER.info("[Meteorite] Impacto em " + target);
    }

    /** Verifica continuamente se algum jogador chegou perto de um meteoro caído. */
    private static int detectorCooldown = 0;
    private static void detectPlayerNearCrater(MinecraftServer server) {
        if (detectorCooldown-- > 0) return;
        detectorCooldown = 20; // 1x por segundo

        for (ServerPlayerEntity p : server.getPlayerManager().getPlayerList()) {
            OmnitrixState st = OmnitrixStateProvider.get(p);
            if (st.hasOmnitrix()) continue;

            // Verifica se está perto de um sea_lantern (meteorito)
            BlockPos pos = p.getBlockPos();
            for (int dx = -3; dx <= 3; dx++) {
                for (int dz = -3; dz <= 3; dz++) {
                    for (int dy = -2; dy <= 2; dy++) {
                        BlockPos checkPos = pos.add(dx, dy, dz);
                        if (p.world.getBlockState(checkPos).isOf(Blocks.SEA_LANTERN)) {
                            // Garante que é um meteoro (perto de obsidiana = cratera)
                            int obsidianCount = 0;
                            for (int ox = -2; ox <= 2; ox++)
                                for (int oz = -2; oz <= 2; oz++)
                                    if (p.world.getBlockState(checkPos.add(ox, 0, oz)).isOf(Blocks.OBSIDIAN))
                                        obsidianCount++;
                            if (obsidianCount >= 3) {
                                grantOmnitrix(p, checkPos);
                                return;
                            }
                        }
                    }
                }
            }
        }
    }

    private static void grantOmnitrix(ServerPlayerEntity player, BlockPos meteoritePos) {
        OmnitrixState st = OmnitrixStateProvider.get(player);
        st.setHasOmnitrix(true);

        // Remove o meteorito (foi absorvido pelo braço)
        player.world.setBlockState(meteoritePos, Blocks.AIR.getDefaultState());

        // Efeitos cinematográficos
        ServerWorld world = (ServerWorld) player.world;
        Vec3d ppos = player.getPos();
        world.spawnParticles(ParticleTypes.FLASH, ppos.x, ppos.y + 1, ppos.z, 5, 0.5, 0.5, 0.5, 0);
        world.spawnParticles(ParticleTypes.HAPPY_VILLAGER, ppos.x, ppos.y + 1, ppos.z, 80, 1.0, 1.0, 1.0, 0.1);

        world.playSound(null, ppos.x, ppos.y, ppos.z,
                ModSounds.OMNITRIX_EQUIP, SoundCategory.PLAYERS, 1.5f, 1.0f);

        player.sendMessage(Text.literal("§a§l⌬ O Omnitrix se prendeu ao seu braço! §r§a(Pressione §eG§a para abrir, §eJ§a para transformar)"));

        OmnitrixMod.LOGGER.info("[Omnitrix] Concedido a " + player.getName().getString());
    }

    private static class MeteoriteData {
        ServerWorld world;
        BlockPos target;
        int ticksLeft;
        UUID summonerId;
    }
}
