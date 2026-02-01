package pocketutils;

import com.mojang.brigadier.Command;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageSources;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.io.Console;
import java.io.PrintWriter;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

public class GemItem extends Item {

    private final Holder<MobEffect>  effect;
    private final int amplifier;
    private static final Map<UUID, Map<String, Long>> COOLDOWNS = new HashMap<>();
    private static final Map<UUID, Integer> fireRingTick = new HashMap<>();
    final String custom_effect;

    static {
        ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {

            if (source.is(DamageTypes.FALL) && (entity instanceof Player player) && isGemActive(player, "gravity") && player.level() instanceof ServerLevel world) {

                    world.playSound(null,  entity.getX(), entity.getY(), entity.getZ(), SoundEvents.MACE_SMASH_AIR, SoundSource.PLAYERS,1.0F,1.0F);
                    world.sendParticles(ParticleTypes.GUST_EMITTER_SMALL, entity.getX(), entity.getY(), entity.getZ(), 1, 0, 0, 0, 0);
                    world.sendParticles(ParticleTypes.EXPLOSION, entity.getX(), entity.getY(), entity.getZ(), 1, 0, 0, 0, 0);

                    List<LivingEntity> nearbyList = world.getEntitiesOfClass(
                            LivingEntity.class,
                            player.getBoundingBox().inflate(3.0, 0.0, 3.0)
                    );

                    for (LivingEntity e : nearbyList) {
                        float smashDamage = 0;
                        if (e != player){
                            if (-player.getKnownSpeed().y > 0.6){
                                smashDamage = (float) (-player.getKnownSpeed().y*8);
                            }
                            e.hurtServer(world, new DamageSource(world.registryAccess().get(DamageTypes.MACE_SMASH).get()), smashDamage);
                        }
                        double dx = e.getX() - player.getX();
                        double dz = e.getZ() - player.getZ();
                        double dist = Math.sqrt(dx * dx + dz * dz);
                        dx /= dist;
                        dz /= dist;
                        e.push(dx * smashDamage*0.05, smashDamage*0.03, dz * smashDamage*0.05);
                    }

                return false;
            }

            if ((source.is(DamageTypes.PLAYER_ATTACK) || source.is(DamageTypes.MACE_SMASH)) && source.getEntity() instanceof Player attacker) {
                final Random RANDOM = new Random();

                if (entity.level() instanceof ServerLevel world){
                    if (RANDOM.nextInt(100) < 40 && isGemActive(attacker, "spell")) {
                        LightningBolt lightning = EntityType.LIGHTNING_BOLT.create(world, EntitySpawnReason.TRIGGERED);
                        if (lightning != null) {
                            lightning.setPos(entity.getX(), entity.getY(), entity.getZ());
                            world.addFreshEntity(lightning);
                        }
                    }
                    else if(RANDOM.nextInt(100) < 40 && isGemActive(attacker, "health")) {
                        Objects.requireNonNull(source.getEntity().asLivingEntity()).heal((float) (amount*0.2));
                        world.playSound(null,  entity.getX(), entity.getY(), entity.getZ(), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS,1.0F,1.0F);
                    }

                }
            }

            return true;
        });
    }

    public GemItem(Properties settings, Holder<MobEffect> effect, int amplifier, String effect_id) {
        super(settings);
        this.effect = effect;
        this.amplifier = amplifier;
        this.custom_effect = effect_id;
    }

    @Override
    public void inventoryTick(ItemStack stack, ServerLevel world, Entity entity, @Nullable EquipmentSlot slot) {
        if (world.isClientSide()) return;
        if (!(entity instanceof Player player)) return;

        if (isGemActive(player, custom_effect)) {
            if (effect != null) {
                player.addEffect(new MobEffectInstance(effect, 10, amplifier, false, true));
            }

            if (player.isShiftKeyDown() && custom_effect.equals("fire")) {
                if (!player.getCooldowns().isOnCooldown(stack)) {
                    player.getCooldowns().addCooldown(stack, 100);
                    spawnFireRing(world, player, 3.0, 6, 30);
                    List<LivingEntity> nearbyList = world.getEntitiesOfClass(
                            LivingEntity.class,
                            player.getBoundingBox().inflate(3.0, 0.0, 3.0)
                    );
                    world.playSound(null, entity.getX(), entity.getY(), entity.getZ(), SoundEvents.BLAZE_SHOOT, SoundSource.PLAYERS, 1.0F, 1.0F);
                    world.playSound(null, entity.getX(), entity.getY(), entity.getZ(), SoundEvents.BLAZE_DEATH, SoundSource.PLAYERS, 1.0F, 1.0F);
                    for (LivingEntity e : nearbyList) {
                        if (e != player) {
                            e.setRemainingFireTicks(160);
                        }
                    }
                }
            }
            if (player.isUsingItem() && custom_effect.equals("spell")) {
                System.out.println("pressed");
            }
        }
    }

    public static boolean isGemActive(Player player, String effectId) {
        return isGem(player.getMainHandItem(), effectId) || isGem(player.getOffhandItem(), effectId);
    }
    private static boolean isGem(ItemStack stack, String effectId) {
        return stack.getItem() instanceof GemItem gem && effectId.equals(gem.custom_effect);
    }

    public static void spawnFireRing(ServerLevel world, Player player, double maxRadius, int steps, int points) {
        Random rand = new Random();

        double centerX = player.getX();
        double centerY = player.getY() + 0.5;
        double centerZ = player.getZ();

        for (int t = 1; t <= steps; t++) {
            double radius = maxRadius * t / steps;

            for (int i = 0; i < points; i++) {
                double angle = 2 * Math.PI * i / points;

                double x = centerX + radius * Math.cos(angle) + ((rand.nextDouble() - 0.5) * 0.7);
                double z = centerZ + radius * Math.sin(angle) + ((rand.nextDouble() - 0.5) * 0.7);
                double y = centerY + (rand.nextDouble() - 0.5) * 0.2;

                world.sendParticles(ParticleTypes.FLAME, x, y, z, 1, 0, 0, 0, 0.0);
                world.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, x, y, z, 1, 0, 0, 0, 0.0);
                world.sendParticles(ParticleTypes.SMOKE, x, y, z, 1, 0, 0, 0, 0.0);
                world.sendParticles(ParticleTypes.LAVA, x, y, z, 1, 0, 0, 0, 0.0);
                world.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, x, y, z, 1, 0, 0, 0, 0.0);
            }
        }
    }


}
